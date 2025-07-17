@file:Suppress("DEPRECATION")

package com.dong.baselib.file

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dong.baselib.listener.FileListener
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
    val directory = context.filesDir
    val file = File(directory, "$fileName.png")
    
    return try {
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        file.absolutePath
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun Activity.saveImageToInternalStorage(bitmap: Bitmap, fileName: String): String? {
    val directory = this.filesDir
    val file = File(directory, "$fileName.png")
    
    return try {
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        file.absolutePath
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}


fun Activity.shareImages(imagePaths: MutableList<String>) {
    val imageUris = ArrayList<Uri>()
    
    for (path in imagePaths) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        imageUris.add(uri)
    }
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, "Share images via"))
}

fun shareImages(context: Context, imagePaths: MutableList<String>) {
    val imageUris = ArrayList<Uri>()
    
    for (path in imagePaths) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        imageUris.add(uri)
    }
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share images via"))
}

fun Activity.bitmapFromUri(uri: Uri): Bitmap? {
    return try {
        val inputStream = this.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream).also {
            inputStream?.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Bitmap.resizeWidth(newWidth: Int): Bitmap {
    val aspectRatio = this.height.toFloat() / this.width
    val targetHeight = (newWidth * aspectRatio).toInt()
    return Bitmap.createScaledBitmap(this, newWidth, targetHeight, true)
}

fun Bitmap.resizeHeight(newHeight: Int): Bitmap {
    val aspectRatio = this.width.toFloat() / this.height
    val targetWidth = (newHeight * aspectRatio).toInt()
    return Bitmap.createScaledBitmap(this, targetWidth, newHeight, true)
}

fun Bitmap.resize(newWidth: Int, newHeight: Int): Bitmap {
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}


fun Activity.openFromAssets(filePath: String, callback: ((Bitmap?) -> Unit)) {
    try {
        val inputStream = this.assets.open(filePath)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        callback.invoke(bitmap)
        inputStream.close()
    } catch (e: IOException) {
        callback.invoke(null)
        e.printStackTrace()
    }
}

fun moveImageToGallery(activity: Activity, cacheFilePath: String, folderPath: String): Uri? {
    val cacheFile = File(cacheFilePath)
    
    if (!cacheFile.exists()) {
        return null
    }
    
    val contentResolver = activity.contentResolver
    var uri: Uri?
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // For Android 10 and above
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cacheFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            // Extract relative path from folderPath
            val relativePath = when {
                folderPath.contains("Pictures") -> folderPath.substringAfter("Pictures")
                folderPath.contains("DCIM") -> folderPath.substringAfter("DCIM")
                else -> File.separator + "Pictures" + File.separator + cacheFile.parentFile?.name
            }.trim(File.separatorChar)
            
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "Pictures${File.separator}$relativePath"
            )
        }
        
        uri = try {
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?.also { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        cacheFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error saving image", e)
            null
        }
    } else {
        // For Android 9 and below
        val directory = File(folderPath)
        
        try {
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val newFile = File(directory, cacheFile.name)
            cacheFile.copyTo(newFile, overwrite = true)
            
            uri = Uri.fromFile(newFile)
            // Notify media scanner
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = uri
            }
            activity.sendBroadcast(mediaScanIntent)
        } catch (e: IOException) {
            Log.e("FileUtils", "Error saving image", e)
            uri = null
        }
    }
    return uri
}

fun moveImageToGallery(
    activity: Activity,
    cacheFilePath: String,
    folderPath: String,
    mimeType: String = "image/png"
): Uri? {
    val cacheFile = File(cacheFilePath)
    
    if (!cacheFile.exists()) {
        return null
    }
    
    val contentResolver = activity.contentResolver
    var uri: Uri?
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val relativePath = when {
            folderPath.contains("Pictures") ->
                "Pictures" + folderPath.substringAfter("Pictures")
            
            folderPath.contains("DCIM") ->
                "DCIM" + folderPath.substringAfter("DCIM")
            
            else -> "Pictures" + File.separator + folderPath.substringAfterLast(File.separator)
        }
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cacheFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        
        uri = try {
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?.also { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        cacheFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    // Clear pending flag
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error saving image", e)
            null
        }
    } else {
        val directory = File(folderPath)
        
        try {
            if (!directory.exists() && !directory.mkdirs()) {
                Log.e("FileUtils", "Failed to create directory")
                return null
            }
            
            val newFile = File(directory, cacheFile.name)
            if (newFile.exists()) {
                newFile.delete()
            }
            
            cacheFile.copyTo(newFile, overwrite = true)
            uri = Uri.fromFile(newFile)
            
            // Notify media scanner
            MediaScannerConnection.scanFile(
                activity,
                arrayOf(newFile.absolutePath),
                arrayOf(mimeType)
            ) { path, uri ->
                Log.d("FileUtils", "Scanned $path : $uri")
            }
        } catch (e: IOException) {
            Log.e("FileUtils", "Error saving image", e)
            uri = null
        }
    }
    return uri
}

fun Activity.openFromAssets(filePath: String): Bitmap? {
    try {
        val inputStream = this.assets.open(filePath)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return bitmap
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun copyQrToClipBoard(context: Context, value: String) {
    val clipboard =
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copy Value", value)
    clipboard.setPrimaryClip(clip)
    
}

fun Activity.copyQrToClipBoard(value: String) {
    val clipboard =
        getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copy Value", value)
    clipboard.setPrimaryClip(clip)
    
}


fun saveFileByBitmap(
    context: Context,
    bitmapCreate: Bitmap?,
    fileName: String,
    totalDirection: String,
    listener: FileListener? = null
) {
    bitmapCreate?.let { bitmap ->
        val savedUri = saveImageToGallery(context, bitmap, fileName, totalDirection)
        if (savedUri == null) {
            listener?.onSaveSuccess("$savedUri")
        } else {
            listener?.onSaveError("Not found image file $savedUri")
        }
    }
}

fun Activity.saveFileByBitmap(
    bitmapCreate: Bitmap?,
    fileName: String,
    totalDirection: String,
    listener: FileListener? = null
) {
    bitmapCreate?.let { bitmap ->
        val savedUri = saveImageToGallery(this, bitmap, fileName, totalDirection)
        if (savedUri == null) {
            listener?.onSaveSuccess("$savedUri")
        } else {
            listener?.onSaveError("Not found image file $savedUri")
        }
    }
}

fun Activity.copyToClipBoard(value: String) {
    val clipboard =
        getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("QR Code Text", value)
    clipboard.setPrimaryClip(clip)
}


fun saveImageToGallery(
    activity: Activity,
    bitmap: Bitmap,
    fileName: String,
    folderName: String = "Data"
): Uri? {
    val contentResolver = activity.contentResolver
    val uri: Uri?
    
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}${File.separator}$folderName"
            )
            // Add pending flag
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        
        uri = try {
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                // Clear pending flag
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error saving image", e)
            null
        }
    } else {
        @Suppress("DEPRECATION")
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            folderName
        )
        
        try {
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val imageFile = File(directory, fileName)
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            
            uri = Uri.fromFile(imageFile)
            // Notify media scanner
            MediaScannerConnection.scanFile(
                activity,
                arrayOf(imageFile.absolutePath),
                arrayOf("image/png")
            ) { path, uri ->
                Log.d("FileUtils", "Scanned $path : $uri")
            }
        } catch (e: IOException) {
            Log.e("FileUtils", "Error saving image", e)
            return null
        }
    }
    return uri
}

fun Context.saveImageToDownloads(
    activity: Activity,
    bitmap: Bitmap,
    fileName: String,
    folderName: String = "QrSan-QrGenerator"
): Uri? {
    val contentResolver = activity.contentResolver
    val uri: Uri?
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}${File.separator}$folderName"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        
        uri = try {
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                // Clear pending flag
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error saving image", e)
            null
        }
    } else {
        @Suppress("DEPRECATION")
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            folderName
        )
        
        try {
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val imageFile = File(directory, fileName)
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            
            uri = Uri.fromFile(imageFile)
            // Notify system about new file
            activity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = uri
            })
        } catch (e: IOException) {
            Log.e("FileUtils", "Error saving image", e)
            return null
        }
    }
    return uri
}


@Suppress("DEPRECATION")
fun Context.saveImageToDownloads(
    bitmap: Bitmap,
    fileName: String,
    totalDirection: String,
    callback: ((String) -> Unit)? = null
) {
    val cacheFile = File(cacheDir, fileName)
    
    try {
        FileOutputStream(cacheFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        val contentResolver = contentResolver
        var finalUri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}${File.separator}$totalDirection")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            
            finalUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            finalUri?.let { savedUri ->
                contentResolver.openOutputStream(savedUri)?.use { outputStream ->
                    FileInputStream(cacheFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(savedUri, contentValues, null, null)
            } ?: callback?.invoke("")
        } else {
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                totalDirection
            )
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            
            val finalFile = File(downloadsDir, fileName)
            cacheFile.copyTo(finalFile, overwrite = true)
            finalUri = Uri.fromFile(finalFile)
            MediaScannerConnection.scanFile(
                this,
                arrayOf(finalFile.absolutePath),
                arrayOf("image/png")
            ) { _, _ -> }
        }
        cacheFile.delete()
        
        val imagePath = getRealPathFromUri(this, finalUri)
        callback?.invoke(imagePath ?: "")
        
    } catch (e: Exception) {
        e.printStackTrace()
        callback?.invoke("")
    }
}



@Suppress("DEPRECATION")
fun Context.saveImageToDownloads(
    cachePath: String,
    fileName: String,
    totalDirection: String,
    callback: ((String) -> Unit)? = null
) {
    val contentResolver = this.contentResolver
    var uri: Uri? = null
    
    try {
        val cacheFile = File(cachePath)
        if (!cacheFile.exists()) {
            callback?.invoke("")
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}${File.separator}$totalDirection")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            
            uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { savedUri ->
                contentResolver.openOutputStream(savedUri)?.use { outputStream ->
                    FileInputStream(cacheFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(savedUri, contentValues, null, null)
                
                val path = getMediaStorePath(savedUri)
                callback?.invoke(path)
            } ?: callback?.invoke("")
        } else {
            val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), totalDirection)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            
            val newFile = File(downloadsDir, fileName)
            cacheFile.copyTo(newFile, overwrite = true)
            uri = Uri.fromFile(newFile)
            
            MediaScannerConnection.scanFile(
                this,
                arrayOf(newFile.absolutePath),
                arrayOf("image/png")
            ) { _, scannedUri ->
                callback?.invoke(newFile.absolutePath)
            }
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error saving image", e)
        callback?.invoke("")
    }
}

private fun Context.getMediaStorePath(uri: Uri): String {
    return try {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val projection = arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME)
                var path = ""
                
                contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
                        val fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                        path = "${Environment.getExternalStorageDirectory()}/${relativePath}${fileName}"
                    }
                }
                path
            }
            else -> {
                getRealPathFromUriLegacy(this, uri) ?: ""
            }
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error getting media path", e)
        ""
    }
}

private fun getRealPathFromUriLegacy(context: Context, uri: Uri?): String? {
    if (uri == null) return null
    
    return when {
        DocumentsContract.isDocumentUri(context, uri) -> {
            when {
                isExternalStorageDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    
                    if ("primary".equals(type, ignoreCase = true)) {
                        "${Environment.getExternalStorageDirectory()}/${split[1]}"
                    } else null
                }
                isDownloadsDocument(uri) -> {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        id.toLong()
                    )
                    getDataColumn(context, contentUri, null, null)
                }
                isMediaDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]
                    val contentUri = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    }
                    contentUri?.let { getDataColumn(context, it, "_id=?", arrayOf(split[1])) }
                }
                else -> null
            }
        }
        "content".equals(uri.scheme, ignoreCase = true) -> {
            getDataColumn(context, uri, null, null)
        }
        "file".equals(uri.scheme, ignoreCase = true) -> {
            uri.path
        }
        else -> null
    }
}

private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
    val column = "_data"
    val projection = arrayOf(column)
    
    return try {
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(column))
            } else null
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error getting data column", e)
        null
    }
}

private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}



fun getRealPathFromUri(activity: Context, uri: Uri?): String? {
    if (uri == null) return null
    return if (uri.scheme == "content") {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            cursor = activity.contentResolver.query(uri, projection, null, null, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    return it.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        null
    } else {
        uri.path
    }
}


fun shareFileWithPath(activity: Activity, path: String) {
    val file = File(path)
    
    if (file.exists()) {
        val fileUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.provider",
            file
        )
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "image/png"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        activity.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    }
}

fun moveImageToGallery(context: Context, cacheFilePath: String, totalDirection: String): Uri? {
    val cacheFile = File(cacheFilePath)
    
    if (!cacheFile.exists()) {
        return null
    }
    
    val contentResolver = context.contentResolver
    val uri: Uri?
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, cacheFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, totalDirection)
        }
        
        uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                cacheFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
        }
    } else {
        
        val fileDir = File(totalDirection)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        
        val newFile = File(fileDir, cacheFile.name)
        try {
            cacheFile.copyTo(newFile, overwrite = true)
            uri = Uri.fromFile(newFile)
            
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = uri
            context.sendBroadcast(mediaScanIntent)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
    return uri
}


fun saveImageToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    totalDirection: String
): Uri? {
    val contentResolver = context.contentResolver
    val uri: Uri?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, totalDirection)
        }
        uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
    } else {
        
        val qrCodesDir = File(totalDirection)
        if (!qrCodesDir.exists()) {
            qrCodesDir.mkdirs()
        }
        val imageFile = File(qrCodesDir, fileName)
        try {
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            uri = Uri.fromFile(imageFile)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = uri
        context.sendBroadcast(mediaScanIntent)
    }
    return uri
}

fun loadImageFromAssets(context: Context, fileName: String): Drawable? {
    return try {
        val inputStream = context.assets.open(fileName)
        Drawable.createFromStream(inputStream, null)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap? {
    return try {
        val inputStream = context.assets.open(fileName)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}


fun saveBitmapToCache(activity: Activity, bitmap: Bitmap): File {
    val cacheDir = activity.cacheDir
    val file = File(cacheDir, "${System.currentTimeMillis()}resized_image.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, out)
    }
    return file
}

fun saveDrawableToCache(context: Context, drawableId: Int): String? {
    val drawable = ContextCompat.getDrawable(context, drawableId)
    val bitmap = (drawable as BitmapDrawable).bitmap
    val cacheDir = context.cacheDir
    val file = File(cacheDir, "${System.currentTimeMillis()}drawable_image.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, out)
    }
    return file.absolutePath
}

@SuppressLint("UseCompatLoadingForDrawables")
fun getIconFromData(activity: Activity, icon: Any): Drawable? {
    return when (icon) {
        is Int -> activity.getDrawable(icon)
        is Bitmap -> BitmapDrawable(activity.resources, icon)
        is String -> {
            val file = File(icon)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(icon)
                BitmapDrawable(activity.resources, bitmap)
            } else {
                loadImageFromAssets(activity, icon)
            }
        }
        
        else -> null
    }
}

@Throws(IOException::class)
fun getBitmapFromAssets(context: Context, fileName: String?): Bitmap {
    val assetManager = context.assets
    
    val istr = assetManager.open(fileName!!)
    val bitmap = BitmapFactory.decodeStream(istr)
    istr.close()
    
    return bitmap
}


fun shareFileWithPath(context: Context, path: String) {
    val file = File(path)
    
    if (file.exists()) {
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "image/png"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Files"))
    }
}

fun shareFilesWithPaths(context: Context, paths: List<String>) {
    val uris = paths.mapNotNull { path ->
        val file = File(path)
        if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } else {
            null
        }
    }
    
    if (uris.isNotEmpty()) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            type = "*/*" // Use "*/*" if files are of different types
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Files"))
    } else {
        Toast.makeText(context, "No valid files to share", Toast.LENGTH_SHORT).show()
    }
}


fun shareFileBitmap(context: Context, fileName: String, bitmapCreate: Bitmap?) {
    bitmapCreate?.let { bitmap ->
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, fileName)
        try {
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "image/png"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
            Handler(Looper.getMainLooper()).postDelayed({
                if (file.exists()) {
                    file.delete()
                }
            }, 5000)
            
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


fun getPathFromUri(context: Context, uri: Uri): String? {
    // Handle different URI types
    return when {
        // File URI
        uri.scheme == "file" -> {
            uri.path
        }
        
        // Content URI
        uri.scheme == "content" -> {
            try {
                when {
                    // Check for Google Photos
                    isGooglePhotosUri(uri) -> {
                        uri.lastPathSegment
                    }
                    
                    // Check for Downloads Provider
                    isDownloadsDocument(uri) -> {
                        getDownloadsPath(context, uri)
                    }
                    
                    // Check for Media Provider
                    isMediaDocument(uri) -> {
                        getMediaPath(context, uri)
                    }
                    
                    // Handle other content URIs
                    else -> {
                        getContentPath(context, uri)
                    }
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Error getting path from URI", e)
                null
            }
        }
        
        else -> null
    }
}

private fun getContentPath(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    cursor.getString(columnIndex)
                } else null
            }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error getting content path", e)
        // Fallback to copying the file
        copyFileToTemp(context, uri)
    }
}

private fun getDownloadsPath(context: Context, uri: Uri): String? {
    val id = DocumentsContract.getDocumentId(uri)
    if (id.startsWith("raw:")) {
        return id.substring(4)
    }
    
    val contentUri = ContentUris.withAppendedId(
        Uri.parse("content://downloads/public_downloads"),
        id.toLong()
    )
    return getContentPath(context, contentUri)
}

private fun getMediaPath(context: Context, uri: Uri): String? {
    val id = DocumentsContract.getDocumentId(uri)
    val split = id.split(":")
    val type = split[0]
    val contentUri = when (type.lowercase()) {
        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        else -> null
    }
    
    return contentUri?.let {
        val selection = "_id=?"
        val selectionArgs = arrayOf(split[1])
        getContentPath(context, it, selection, selectionArgs)
    }
}

private fun getContentPath(
    context: Context,
    uri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null
): String? {
    return context.contentResolver.query(
        uri,
        arrayOf(MediaStore.Images.Media.DATA),
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.getString(columnIndex)
        } else null
    }
}

private fun copyFileToTemp(context: Context, uri: Uri): String? {
    return try {
        val tempFile = File(
            context.cacheDir,
            "temp_${System.currentTimeMillis()}_${uri.lastPathSegment}"
        )
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile.absolutePath
    } catch (e: Exception) {
        Log.e("FileUtils", "Error copying file", e)
        null
    }
}

private fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}


data class FileInfo(
    val path: String,
    val name: String,
    val mimeType: String,
    val size: Long
)

fun getFileInfoFromUri(context: Context, uri: Uri): FileInfo? {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val path = cursor.getStringOrNull(0) ?: copyFileToTemp(context, uri) ?: return null
                FileInfo(
                    path = path,
                    name = cursor.getStringOrNull(1) ?: File(path).name,
                    mimeType = cursor.getStringOrNull(2) ?: getMimeType(path),
                    size = cursor.getLongOrNull(3) ?: File(path).length()
                )
            } else null
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error getting file info", e)
        null
    }
}

private fun getMimeType(path: String): String {
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path))
        ?: "application/octet-stream"
}

private fun Cursor.getStringOrNull(columnIndex: Int): String? {
    return try {
        getString(columnIndex)
    } catch (e: Exception) {
        null
    }
}

private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
    return try {
        getLong(columnIndex)
    } catch (e: Exception) {
        null
    }
}

