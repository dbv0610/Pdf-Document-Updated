package com.azg.pdf8.utils

import android.content.ContentUris
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
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

    fun scanAllDocumentsFlow(context: Context): Flow<List<RecentDocument>> = flow {
        val result = mutableListOf<RecentDocument>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val docMimeMap = mapOf(
            "application/pdf" to DocumentType.Pdf,
            "application/msword" to DocumentType.Doc,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to DocumentType.Doc,
            "application/vnd.ms-excel" to DocumentType.Excel,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to DocumentType.Excel,
            "application/vnd.ms-powerpoint" to DocumentType.Ppt,
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" to DocumentType.Ppt
        )
        val selection = docMimeMap.keys.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        }
        val selectionArgs = docMimeMap.keys.toTypedArray()

        withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val pathCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (it.moveToNext()) {
                    val mediaId = it.getLong(idCol)
                    val path = it.getString(pathCol)
                    val lastModified = it.getLong(dateCol) * 1000
                    val size = it.getLong(sizeCol)
                    val mime = it.getString(mimeCol)
                    val type = docMimeMap[mime] ?: DocumentType.Doc

                    result.add(
                        RecentDocument(
                            mediaId = mediaId,
                            path = path,
                            lastModified = lastModified,
                            size = size,
                            type = type
                        )
                    )
                }
            }
        }

        emit(result)
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

fun getMimeType(filePath: String): String {
    val extension = filePath.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (extension) {
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
        "txt", "js", "kt", "py", "java" -> "text/plain"
        "mp3", "wav", "m4a", "aac", "ogg", "flac", "amr" -> "audio/*"
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"
        "mp4", "3gp", "avi", "mkv" -> "video/*"
        else -> "*/*"
    }
}