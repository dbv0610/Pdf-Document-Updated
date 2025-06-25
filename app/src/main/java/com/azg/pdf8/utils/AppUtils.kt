package com.azg.pdf8.utils

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.azg.pdf8.model.DocumentModel
import com.azg.pdf8.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Locale

object AppUtils {
    val pdfExtensions = listOf("pdf")
    val excelExtensions = listOf("xls", "xlsx", "xlsm")
    val docExtensions = listOf("doc", "docx")
    val pptExtensions = listOf("ppt", "pptx")

    fun scanAllDocumentsFlow(context: Context): Flow<List<DocumentModel>> = flow {
        val result = mutableListOf<DocumentModel>()
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
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
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
                        DocumentModel(
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