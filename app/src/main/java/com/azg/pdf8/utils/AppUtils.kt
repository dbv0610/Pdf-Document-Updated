package com.azg.pdf8.utils

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.azg.pdf8.database.FavoriteDao
import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ScanState {
    object Start : ScanState()
    data class Progress(val processed: Int, val total: Int) : ScanState()
    data class Success(val list: List<RecentDocument>) : ScanState()
    data class Error(val throwable: Throwable) : ScanState()
}

object AppUtils {
    val pdfExtensions = listOf("pdf")
    val excelExtensions = listOf("xls", "xlsx", "xlsm")
    val docExtensions = listOf("doc", "docx")
    val pptExtensions = listOf("ppt", "pptx")
    val documentPath: String
        get() {
            val filePath = File(
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/Pdf Reader"
            )
            if (!filePath.exists()) filePath.mkdir()
            return filePath.absolutePath
        }

    fun getSdStorageDirectories(context: Context): List<String> {
        val results = mutableListOf<String>()
        context.getExternalFilesDirs(null)?.forEach { file ->
            file?.let {
                val path = file.path.substringBefore("/Android")
                if (Environment.isExternalStorageRemovable(file)) {
                    results.add(path)
                }
            }
        }
        if (results.isEmpty()) {
            val output = runCatching {
                ProcessBuilder()
                    .command("mount | grep /dev/block/vold")
                    .redirectErrorStream(true)
                    .start()
                    .inputStream
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrElse {
                it.printStackTrace()
                ""
            }

            if (output.isNotBlank()) {
                output.lines().forEach { line ->
                    runCatching {
                        val mountPoint = line.split(" ")[2]
                        results.add(mountPoint)
                    }.onFailure { it.printStackTrace() }
                }
            }
        }
        return results
    }
    fun <T> scanMedia(
        context: Context,
        fileFilter: (File) -> Boolean,
        buildModel: (File) -> T
    ): Flow<T> = flow {
        val storagePaths = AppUtils.getSdStorageDirectories(context).toMutableList()
        val externalStorage = Environment.getExternalStorageDirectory()
        if (externalStorage.exists()) storagePaths.add(externalStorage.absolutePath)

        val directories = ArrayDeque<File>()
        storagePaths
            .map(::File)
            .filter { it.exists() && it.isDirectory }
            .forEach(directories::add)

        while (directories.isNotEmpty()) {
            val dir = directories.removeFirstOrNull() ?: continue
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    if (!file.name.startsWith(".")) directories.add(file)
                } else if (file.isFile && fileFilter(file)) {
                    emit(buildModel(file))
                }
            }
        }
    }
        .onEach { delay(1) }
        .flowOn(Dispatchers.IO)


    fun scanAllDocumentsFlow(
        context: Context,
        recentDao: RecentDao,
        favoriteDao: FavoriteDao
    ): Flow<RecentDocument> =
        scanMedia(
            context,
            fileFilter = { file ->
                when (file.extension.lowercase()) {
                    "pdf", "doc", "docx", "xls",
                    "xlsx", "ppt", "pptx" -> true
                    else -> false
                }
            },
            buildModel = { file ->
                val id = file.absolutePath.hashCode().toLong()
                val type = when (file.extension.lowercase()) {
                    "pdf" -> DocumentType.Pdf
                    "doc", "docx" -> DocumentType.Doc
                    "xls", "xlsx" -> DocumentType.Excel
                    "ppt", "pptx" -> DocumentType.Ppt
                    else -> DocumentType.Doc
                }
                RecentDocument(
                    mediaId      = id,
                    path         = file.absolutePath,
                    lastModified = file.lastModified(),
                    size         = file.length(),
                    type         = type
                )
            }
        ).onEach { doc ->
            recentDao.updatePath(doc.mediaId.toInt(), doc.path)
            favoriteDao.updateName(doc.mediaId.toInt(), doc.path)
        }

    fun scanImagesFlow(context: Context): Flow<ScanState> = flow {
        emit(ScanState.Start)
        val result = mutableListOf<RecentDocument>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            val cursor = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.query(uri, projection, null, null, sortOrder)
                } catch (e: SecurityException) {
                    throw e
                }
            }

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
                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val path: String = if (dataCol != -1) {
                        it.getString(dataCol) ?: ContentUris.withAppendedId(uri, id).toString()
                    } else {
                        ContentUris.withAppendedId(uri, id).toString()
                    }
                    val lastModified = it.getLong(dateCol) * 1000L
                    val size = it.getLong(sizeCol)
                    result.add(
                        RecentDocument(
                            mediaId = id,
                            path = path,
                            lastModified = lastModified,
                            size = size,
                            type = DocumentType.Image
                        )
                    )
                    processed++
                    emit(ScanState.Progress(processed = processed, total = total))
                }
            }
            emit(ScanState.Success(result))
        } catch (e: Throwable) {
            emit(ScanState.Error(e))
        }
    }.flowOn(Dispatchers.IO).onEach { delay(1) }
}

fun formatDateByMillis(millis: Long): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
    return sdf.format(Date(millis))
}

fun formatTimeByMillis(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    return sdf.format(Date(millis))
}
