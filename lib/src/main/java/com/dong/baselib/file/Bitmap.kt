@file:Suppress("DEPRECATION")

package com.dong.baselib.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


fun bitmapToBase64(bitmap: Bitmap): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 35, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun Bitmap.toBase64(): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 35, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}


fun View.bitmapFromView(): Bitmap{
    this.isDrawingCacheEnabled= true
    val bitmap = this.drawingCache
    this.isDrawingCacheEnabled=false
    return bitmap
}
fun ViewGroup.bitmapFromView(): Bitmap{
    this.isDrawingCacheEnabled= true
    val bitmap = this.drawingCache
    this.isDrawingCacheEnabled=false
    return bitmap
}



fun base64ToBitmap(base64String: String): Bitmap? {
    val byteArray = Base64.decode(base64String, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
}

fun captureView(view: View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}

fun captureViewAfterLayout(view: View, onBitmapCaptured: (Bitmap?) -> Unit) {
    fun captureView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
    Log.d("CaptureError", "capturing view after layout")
    view.viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            
            view.viewTreeObserver.removeOnGlobalLayoutListener(this)

            if (view.width > 0 && view.height > 0) {
                val bitmap = captureView(view)
                Log.e("CaptureError", "Image successfully captured")
                onBitmapCaptured(bitmap)
            } else {
                Log.e("CaptureError", "View width or height is zero")
                onBitmapCaptured(null)
            }
        }
    })
}


fun captureViewAfterLayout(view: View, context: Context, onFileCaptured: (File?) -> Unit) {
    fun captureView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): File? {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, "captured_view_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
            file
        } catch (e: IOException) {
            Log.e("CaptureError", "Failed to save bitmap to cache: ${e.message}")
            null
        }
    }

    Log.d("CaptureError", "Capturing view after layout")
    view.viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            if (view.width > 0 && view.height > 0) {
                val bitmap = captureView(view)
                Log.d("CaptureError", "Image successfully captured")
                val savedFile = saveBitmapToCache(context, bitmap)

                if (savedFile != null) {
                    Log.d(
                        "CaptureError",
                        "Image successfully saved to cache: ${savedFile.absolutePath}"
                    )
                } else {
                    Log.e("CaptureError", "Failed to save image to cache")
                }

                onFileCaptured(savedFile)
            } else {
                Log.e("CaptureError", "View width or height is zero")
                onFileCaptured(null)
            }
        }
    })
}