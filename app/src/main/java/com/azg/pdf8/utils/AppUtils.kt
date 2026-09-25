package com.azg.pdf8.utils

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.azg.pdf8.database.FavoriteDao
import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

sealed class ScanState {
    object Start : ScanState()
    data class Progress(val processed: Int, val total: Int) : ScanState()
    data class Success(val list: List<RecentDocument>) : ScanState()
    data class Error(val throwable: Throwable) : ScanState()
}
sealed class FileTypeFilter {
    abstract fun accepts(type: DocumentType): Boolean

    data object All : FileTypeFilter() {
        override fun accepts(type: DocumentType) = true
    }

    data class Only(val types: Set<DocumentType>) : FileTypeFilter() {
        constructor(vararg types: DocumentType) : this(types.toSet())
        override fun accepts(type: DocumentType) = type in types
    }
}
object AppUtils {
    val pdfExtensions = listOf("pdf")
    val excelExtensions = listOf("xls", "xlsx", "xlsm")
    val docExtensions = listOf("doc", "docx")
    val pptExtensions = listOf("ppt", "pptx")
    val txtExtensions = listOf("txt")

    private val extensionToType: Map<String, DocumentType> = buildMap {
        pdfExtensions.forEach { put(it, DocumentType.Pdf) }
        docExtensions.forEach { put(it, DocumentType.Doc) }
        excelExtensions.forEach { put(it, DocumentType.Excel) }
        pptExtensions.forEach { put(it, DocumentType.Ppt) }
        txtExtensions.forEach { put(it, DocumentType.Txt) }
    }

    fun documentTypeOf(file: File): DocumentType? =
        extensionToType[file.extension.lowercase(Locale.ROOT)]

    private fun documentTypeOf(path: String): DocumentType? =
        extensionToType[path.substringAfterLast('.', "").lowercase(Locale.ROOT)]

    private val filesUri: Uri
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

    private fun fallbackId(path: String): Long =
        -(path.hashCode().toLong() and 0x7FFFFFFFL) - 1L

    private fun isLocalPath(path: String): Boolean = path.startsWith("/")

    fun hasStoragePermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    @get:WorkerThread
    val documentPath: String
        get() {
            val filePath = File(
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/Pdf Reader"
            )
            if (!filePath.exists()) filePath.mkdirs()
            return filePath.absolutePath
        }

    suspend fun getSdStorageDirectories(context: Context): List<String> =
        withContext(Dispatchers.IO) {
            context.getExternalFilesDirs(null).filterNotNull()
                .filter { Environment.isExternalStorageRemovable(it) }
                .map { it.path.substringBefore("/Android/") }
                .distinct()
        }

    suspend fun ensureDocumentDirectory(): String = withContext(Dispatchers.IO) {
        documentPath
    }

    fun notifyMediaScanner(context: Context, vararg paths: String) {
        MediaScannerConnection.scanFile(context.applicationContext, paths, null, null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun scanPaths(context: Context, paths: List<String>): Map<String, Uri?> {
        if (paths.isEmpty()) return emptyMap()
        val results = mutableMapOf<String, Uri?>()
        withTimeoutOrNull(10_000L.milliseconds) {
            suspendCancellableCoroutine { cont ->
                MediaScannerConnection.scanFile(
                    context.applicationContext, paths.toTypedArray(), null
                ) { path, uri ->
                    val done = synchronized(results) {
                        results[path] = uri
                        results.size >= paths.size
                    }
                    if (done && cont.isActive) cont.resume(Unit)
                }
            }
        }
        return synchronized(results) { results.toMap() }
    }

    private fun queryMediaId(context: Context, path: String): Long? =
        context.contentResolver.query(
            filesUri,
            arrayOf(MediaStore.Files.FileColumns._ID),
            "${MediaStore.Files.FileColumns.DATA} = ?",
            arrayOf(path),
            null
        )?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }

    private fun loadMediaIds(context: Context, extensions: Collection<String>): Map<String, Long> {
        if (extensions.isEmpty()) return emptyMap()
        val dataCol = MediaStore.Files.FileColumns.DATA
        val selection = extensions.joinToString(" OR ") { "$dataCol LIKE ?" }
        val args = extensions.map { "%.$it" }.toTypedArray()
        val result = HashMap<String, Long>()
        context.contentResolver.query(
            filesUri,
            arrayOf(MediaStore.Files.FileColumns._ID, dataCol),
            selection,
            args,
            null
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val pathIdx = c.getColumnIndexOrThrow(dataCol)
            while (c.moveToNext()) {
                val path = c.getString(pathIdx) ?: continue
                result[path] = c.getLong(idIdx)
            }
        }
        return result
    }

    fun scanAllDocumentsFlow(
        context: Context,
        recentDao: RecentDao,
        favoriteDao: FavoriteDao,
        filter: FileTypeFilter = FileTypeFilter.All
    ): Flow<RecentDocument> = flow {
        val extensions = extensionToType.filterValues(filter::accepts).keys
        if (extensions.isEmpty()) return@flow

        val sync = PathSync.load(recentDao, favoriteDao)

        val idCol = MediaStore.Files.FileColumns._ID
        val dataCol = MediaStore.Files.FileColumns.DATA
        val dateColName = MediaStore.Files.FileColumns.DATE_MODIFIED
        val sizeColName = MediaStore.Files.FileColumns.SIZE
        val projection = arrayOf(idCol, dataCol, dateColName, sizeColName)

        val selection = extensions.joinToString(" OR ", "(", ")") { "$dataCol LIKE ?" } +
                " AND $sizeColName > 0"
        val selectionArgs = extensions.map { "%.$it" }.toTypedArray()
        val sortOrder = "$dateColName DESC"

        context.contentResolver.query(filesUri, projection, selection, selectionArgs, sortOrder)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(idCol)
            val pathIdx = c.getColumnIndexOrThrow(dataCol)
            val dateIdx = c.getColumnIndexOrThrow(dateColName)
            val sizeIdx = c.getColumnIndexOrThrow(sizeColName)

            while (c.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val path = c.getString(pathIdx) ?: continue
                if (path.contains("/.")) continue
                val type = documentTypeOf(path) ?: continue

                val doc = RecentDocument(
                    mediaId = c.getLong(idIdx),
                    path = path,
                    lastModified = c.getLong(dateIdx) * 1000L,
                    size = c.getLong(sizeIdx),
                    type = type
                )
                sync.update(doc)
                emit(doc)
            }
        }
    }.flowOn(Dispatchers.IO)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun observeDocuments(
        context: Context,
        recentDao: RecentDao,
        favoriteDao: FavoriteDao,
        filter: FileTypeFilter = FileTypeFilter.All
    ): Flow<List<RecentDocument>> {
        val flowOn = callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            context.contentResolver.registerContentObserver(filesUri, true, observer)
            trySend(Unit)
            awaitClose { context.contentResolver.unregisterContentObserver(observer) }
        }
            .debounce(300L.milliseconds)
            .mapLatest {
                pruneMissingFiles(recentDao, favoriteDao)
                scanAllDocumentsFlow(context, recentDao, favoriteDao, filter).toList()
            }
            .flowOn<List<RecentDocument>>(Dispatchers.IO)
        return flowOn
    }

    fun <T> scanMedia(
        context: Context,
        fileFilter: (File) -> Boolean,
        buildModel: (File) -> T
    ): Flow<T> = flow {
        val storagePaths = getSdStorageDirectories(context).toMutableList()
        val externalStorage = Environment.getExternalStorageDirectory()
        if (externalStorage.exists()) storagePaths.add(externalStorage.absolutePath)
        emitAll(scanFilesFlow(storagePaths.map(::File), fileFilter, buildModel))
    }.flowOn(Dispatchers.IO)

    fun scanAllDocumentsByFileSystemFlow(
        context: Context,
        recentDao: RecentDao,
        favoriteDao: FavoriteDao,
        filter: FileTypeFilter = FileTypeFilter.All
    ): Flow<RecentDocument> = flow {
        val extensions = extensionToType.filterValues(filter::accepts).keys
        if (extensions.isEmpty()) return@flow

        val sync = PathSync.load(recentDao, favoriteDao)
        val mediaIds = loadMediaIds(context, extensions)

        scanMedia(
            context,
            fileFilter = { file -> documentTypeOf(file)?.let(filter::accepts) == true },
            buildModel = { file ->
                val path = file.absolutePath
                RecentDocument(
                    mediaId = mediaIds[path] ?: fallbackId(path),
                    path = path,
                    lastModified = file.lastModified(),
                    size = file.length(),
                    type = documentTypeOf(file) ?: DocumentType.Doc
                )
            }
        ).collect { doc ->
            sync.update(doc)
            emit(doc)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun renameDocument(
        context: Context,
        doc: RecentDocument,
        newName: String,
        recentDao: RecentDao,
        favoriteDao: FavoriteDao
    ): RecentDocument? = withContext(Dispatchers.IO) {
        val oldFile = File(doc.path)
        val parent = oldFile.parentFile ?: return@withContext null
        val newFile = File(parent, newName)
        if (!oldFile.exists() || newFile.exists()) return@withContext null
        if (!oldFile.renameTo(newFile)) return@withContext null

        val newPath = newFile.absolutePath
        val scanned = scanPaths(context, listOf(doc.path, newPath))
        val newId = scanned[newPath]?.let(ContentUris::parseId)?.takeIf { it > 0 }
            ?: queryMediaId(context, newPath)
            ?: fallbackId(newPath)

        val oldIdInt = doc.mediaId.toInt()
        val newIdInt = newId.toInt()
        if (oldIdInt != newIdInt) {
            recentDao.updateId(oldIdInt, newIdInt)
            favoriteDao.updateId(oldIdInt, newIdInt)
        }
        recentDao.updatePath(newIdInt, newPath)
        favoriteDao.updateName(newIdInt, newPath)

        doc.copy(
            mediaId = newId,
            path = newPath,
            lastModified = newFile.lastModified()
        )
    }

    suspend fun deleteDocument(
        context: Context,
        doc: RecentDocument,
        recentDao: RecentDao,
        favoriteDao: FavoriteDao
    ): Boolean = withContext(Dispatchers.IO) {
        val file = File(doc.path)
        if (file.exists() && !file.delete()) return@withContext false
        scanPaths(context, listOf(doc.path))
        recentDao.deleteById(doc.mediaId.toInt())
        favoriteDao.deleteById(doc.mediaId.toInt())
        true
    }

    suspend fun pruneMissingFiles(recentDao: RecentDao, favoriteDao: FavoriteDao) =
        withContext(Dispatchers.IO) {
            recentDao.getAll().first()
                .filter { isLocalPath(it.path) && !File(it.path).exists() }
                .forEach { recentDao.deleteById(it.mediaId.toInt()) }
            favoriteDao.getAll().first()
                .filter { isLocalPath(it.path) && !File(it.path).exists() }
                .forEach { favoriteDao.deleteById(it.mediaId.toInt()) }
        }

    suspend fun migrateLegacyIds(
        context: Context,
        recentDao: RecentDao,
        favoriteDao: FavoriteDao
    ) = withContext(Dispatchers.IO) {
        val mediaIds = loadMediaIds(context, extensionToType.keys)

        recentDao.getAll().first().forEach { entry ->
            if (!isLocalPath(entry.path)) return@forEach
            if (entry.mediaId != entry.path.hashCode().toLong()) return@forEach
            val oldId = entry.mediaId.toInt()
            when {
                !File(entry.path).exists() -> recentDao.deleteById(oldId)
                else -> {
                    val newId = (mediaIds[entry.path] ?: fallbackId(entry.path)).toInt()
                    if (newId != oldId) recentDao.updateId(oldId, newId)
                }
            }
        }

        favoriteDao.getAll().first().forEach { entry ->
            if (!isLocalPath(entry.path)) return@forEach
            if (entry.mediaId != entry.path.hashCode().toLong()) return@forEach
            val oldId = entry.mediaId.toInt()
            when {
                !File(entry.path).exists() -> favoriteDao.deleteById(oldId)
                else -> {
                    val newId = (mediaIds[entry.path] ?: fallbackId(entry.path)).toInt()
                    if (newId != oldId) favoriteDao.updateId(oldId, newId)
                }
            }
        }
    }

    private class PathSync(
        private val recentDao: RecentDao,
        private val favoriteDao: FavoriteDao,
        private val recentPaths: Map<Long, String>,
        private val favoritePaths: Map<Long, String>
    ) {
        suspend fun update(doc: RecentDocument) {
            if (recentPaths[doc.mediaId]?.let { it != doc.path } == true) {
                recentDao.updatePath(doc.mediaId.toInt(), doc.path)
            }
            if (favoritePaths[doc.mediaId]?.let { it != doc.path } == true) {
                favoriteDao.updateName(doc.mediaId.toInt(), doc.path)
            }
        }

        companion object {
            suspend fun load(recentDao: RecentDao, favoriteDao: FavoriteDao) = PathSync(
                recentDao,
                favoriteDao,
                recentDao.getAll().first().associate { it.mediaId to it.path },
                favoriteDao.getAll().first().associate { it.mediaId to it.path }
            )
        }
    }

    fun scanImagesFlow(context: Context): Flow<ScanState> = flow {
        emit(ScanState.Start)
        val result = mutableListOf<RecentDocument>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)

        if (cursor == null) {
            emit(ScanState.Success(emptyList()))
            return@flow
        }

        cursor.use {
            val total = it.count
            emit(ScanState.Progress(processed = 0, total = total))
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = it.getColumnIndex(MediaStore.Images.Media.DATA)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            var processed = 0
            var lastProgressAt = SystemClock.elapsedRealtime()
            while (it.moveToNext()) {
                currentCoroutineContext().ensureActive()
                val id = it.getLong(idCol)
                val path: String = if (dataCol != -1) {
                    it.getString(dataCol) ?: ContentUris.withAppendedId(uri, id).toString()
                } else {
                    ContentUris.withAppendedId(uri, id).toString()
                }
                val lastModified = it.getLong(dateCol) * 1000L
                val size = it.getLong(sizeCol)
                if (size != 0L) {
                    result.add(
                        RecentDocument(
                            mediaId = id,
                            path = path,
                            lastModified = lastModified,
                            size = size,
                            type = DocumentType.Image
                        )
                    )
                }
                processed++
                val now = SystemClock.elapsedRealtime()
                if (processed == total || now - lastProgressAt >= 100L) {
                    emit(ScanState.Progress(processed = processed, total = total))
                    lastProgressAt = now
                }
            }
        }
        emit(ScanState.Success(result))
    }.catch { e ->
        emit(ScanState.Error(e))
    }.flowOn(Dispatchers.IO)
}

internal fun <T> scanFilesFlow(
    roots: List<File>,
    fileFilter: (File) -> Boolean,
    buildModel: (File) -> T,
): Flow<T> = flow {
    val directories = ArrayDeque<File>()
    val visited = HashSet<String>()
    roots.forEach(directories::addLast)
    while (directories.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        val directory = directories.removeFirst()
        val canonicalPath = try {
            directory.canonicalPath
        } catch (_: IOException) {
            continue
        } catch (_: SecurityException) {
            continue
        }
        if (!visited.add(canonicalPath)) continue
        val children = try {
            directory.listFiles()
        } catch (_: SecurityException) {
            null
        } ?: continue
        for (file in children) {
            currentCoroutineContext().ensureActive()
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) directories.addLast(file)
            } else if (file.isFile && fileFilter(file)) {
                emit(buildModel(file))
            }
        }
    }
}.flowOn(Dispatchers.IO)

fun formatDateByMillis(millis: Long): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
    return sdf.format(Date(millis))
}

fun formatTimeByMillis(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    return sdf.format(Date(millis))
}
