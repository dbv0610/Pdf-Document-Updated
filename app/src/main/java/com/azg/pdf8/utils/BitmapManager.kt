package com.azg.pdf8.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.util.UUID

object BitmapManager {
    fun Bitmap?.recycleSafely() {
        if (this != null && !isRecycled) {
            this.recycle()
        }
    }

    fun Bitmap?.isValid(): Boolean {
        return this != null && !isRecycled
    }

    private var editBitmap: Bitmap? = null
    fun setEditBitmap(bitmap: Bitmap?) {
        editBitmap = bitmap
    }

    fun recycleEditBitmap() {
        editBitmap?.recycleSafely()
        editBitmap = null
    }

    fun getEditBitmap() = editBitmap
    data class ItemBitmapImage(
        val id: String = generateUniqueId(),
        val bitmap: Bitmap,
    )

    fun generateUniqueId(): String = UUID.randomUUID().toString()

    private var tempBitmap: Bitmap? = null
    fun getTempBitmap() = tempBitmap
    fun setTempBitmap(bitmap: Bitmap?) {
        tempBitmap = bitmap
    }

    fun recycleTempBitmap() {
        tempBitmap?.recycleSafely()
        tempBitmap = null
    }

    var srcUri: Uri? = null
    var desUri: Uri? = null
    private val itemBitmapImages: MutableList<ItemBitmapImage> = mutableListOf()
    fun getImageItems() = itemBitmapImages
    fun removeImageItem(position: Int) {
        itemBitmapImages.removeAt(position)
    }

    fun setImageItems(items: List<ItemBitmapImage>) {
        itemBitmapImages.clear()
        itemBitmapImages.addAll(items)
    }

    fun addImageItem(items: ItemBitmapImage) {
        itemBitmapImages.add(items)
    }

    fun addImageItem(items: List<ItemBitmapImage>) {
        itemBitmapImages.addAll(items)
    }

    fun updateNewBitmap(position: Int, bitmap: Bitmap?) {
        if (bitmap != null) {
            val tempList = itemBitmapImages.toMutableList()
            itemBitmapImages.clear()
            itemBitmapImages.addAll(tempList.mapIndexed { index, imageItem ->
                if (index == position) {
                    imageItem.copy(bitmap = bitmap)
                } else {
                    imageItem
                }
            })
        }
    }

    fun clear() {
        editBitmap?.recycleSafely()
        editBitmap = null
        itemBitmapImages.forEach {
            it.bitmap.recycleSafely()
        }
        itemBitmapImages.clear()
    }

    fun generateDesUri(context: Context) {
        val desUri = Uri.fromFile(File(context.cacheDir, "temp_des_image.jpg"))
        this.desUri = desUri
    }
}