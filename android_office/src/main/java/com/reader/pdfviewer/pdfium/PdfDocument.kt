package com.reader.pdfviewer.pdfium

import android.graphics.RectF
import android.os.ParcelFileDescriptor
import android.util.ArrayMap

class PdfDocument  /*package*/
internal constructor() {
    class Meta {
        var title: String? = null
        var author: String? = null
        var subject: String? = null
        var keywords: String? = null
        var creator: String? = null
        var producer: String? = null
        var creationDate: String? = null
        var modDate: String? = null
    }

    class Bookmark {
        val children: MutableList<Bookmark?> = ArrayList<Bookmark?>()
        var title: String? = null
        var pageIdx: Long = 0
        var mNativePtr: Long = 0

        fun hasChildren(): Boolean {
            return !children.isEmpty()
        }
    }

    class Link(val bounds: RectF?, val destPageIdx: Int?, val uri: String?)

    /*package*/
    var mNativeDocPtr: Long = 0
    /*package*/
    var parcelFileDescriptor: ParcelFileDescriptor? = null

    /*package*/
    val mNativePagesPtr: MutableMap<Int?, Long?> = ArrayMap<Int?, Long?>()

    /*package*/
    val mNativeTextPagesPtr: MutableMap<Int?, Long?> = ArrayMap<Int?, Long?>()

    fun hasPage(index: Int): Boolean {
        return mNativePagesPtr.containsKey(index)
    }
}
