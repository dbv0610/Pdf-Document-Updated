package com.azg.pdf8.model

import android.graphics.Bitmap

data class DocumentPage(
    val index: Int,
    val bitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: Throwable? = null
) {
    companion object {
        fun loading(index: Int) = DocumentPage(index, isLoading = true)
        fun error(index: Int, error: Throwable) = DocumentPage(index, error = error)
    }
}