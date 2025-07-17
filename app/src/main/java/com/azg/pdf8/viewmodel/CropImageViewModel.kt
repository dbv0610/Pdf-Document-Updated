package com.azg.pdf8.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CropImageViewModel() : ViewModel() {
    private val _isEdited = MutableStateFlow<Boolean>(false)
    val isEdited = _isEdited.asStateFlow()
    fun updateIsEdited() {
        _isEdited.value = true
    }

    private val _bitmapLoaderState: MutableStateFlow<BitmapLoaderState> =
        MutableStateFlow(BitmapLoaderState.IDLE)
    val bitmapLoaderState: StateFlow<BitmapLoaderState> = _bitmapLoaderState.asStateFlow()
    fun loadBitmapFromUri(context: Context, uri: Uri) {
        this.viewModelScope.launch(Dispatchers.Default) {
            _bitmapLoaderState.value = BitmapLoaderState.LOADING
            bitmapFromUri(context, uri).onSuccess {
                _bitmapLoaderState.value = BitmapLoaderState.SUCCESS(it)
            }.onFailure {
                _bitmapLoaderState.value = BitmapLoaderState.ERROR(it.message.toString())
            }
        }
    }

    sealed class BitmapLoaderState {
        data object IDLE : BitmapLoaderState()
        data object LOADING : BitmapLoaderState()
        data class SUCCESS(val bitmap: Bitmap) : BitmapLoaderState()
        data class ERROR(val message: String) : BitmapLoaderState()
    }

    suspend fun bitmapFromUri(context: Context, uri: Uri): Result<Bitmap> =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmapFromUriInternal(context, uri)
                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun loadBitmapFromFilePath(filePath: String): Result<Bitmap?> =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmapFromFilePathInternal(filePath)
                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun rotateBitmap(bitmap: Bitmap, degrees: Float): Result<Bitmap> =
        withContext(Dispatchers.Default) {
            try {
                if (bitmap.isRecycled) {
                    return@withContext Result.failure(IllegalStateException("Source bitmap is recycled"))
                }
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )

                Result.success(rotatedBitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun cropBitmap(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            if (bitmap.isRecycled) {
                return@withContext Result.failure(IllegalStateException("Source bitmap is recycled"))
            }
            val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
            Result.success(croppedBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadBitmapFromUriInternal(context: Context, uri: Uri): Bitmap =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw IllegalStateException("Cannot open input stream")
        }

    private suspend fun loadBitmapFromFilePathInternal(filePath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val imageFile = File(filePath)
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                bitmap
            } catch (e: Exception) {
                throw e
            }
        }

    data class BitmapConfig(
        val maxWidth: Int = 2048,
        val maxHeight: Int = 2048,
        val compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        val quality: Int = 90,
        val config: Bitmap.Config = Bitmap.Config.ARGB_8888,
        val maxMemoryPercent: Float = 0.25f,
        val cacheSizePercent: Float = 0.125f
    ) {
        companion object {
            val DEFAULT = BitmapConfig()
            val HIGH_QUALITY = BitmapConfig(
                maxWidth = 4096,
                maxHeight = 4096,
                quality = 95
            )
            val LOW_MEMORY = BitmapConfig(
                maxWidth = 1024,
                maxHeight = 1024,
                quality = 75,
                maxMemoryPercent = 0.15f,
                cacheSizePercent = 0.075f
            )
        }
    }
}