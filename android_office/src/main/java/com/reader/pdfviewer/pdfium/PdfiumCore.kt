package com.reader.pdfviewer.pdfium

import android.graphics.Color
import androidx.annotation.ColorInt
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.RectF
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Surface
import com.reader.pdfviewer.pdfium.util.Size
import java.io.FileDescriptor
import java.io.IOException
import java.lang.reflect.Field

class PdfiumCore(ctx: Context) {
    private external fun nativeOpenDocument(fd: Int, password: String?): Long

    private external fun nativeOpenMemDocument(data: ByteArray?, password: String?): Long

    private external fun nativeCloseDocument(docPtr: Long)

    private external fun nativeGetPageCount(docPtr: Long): Int

    private external fun nativeLoadPage(docPtr: Long, pageIndex: Int): Long

    private external fun nativeLoadPages(docPtr: Long, fromIndex: Int, toIndex: Int): LongArray

    private external fun nativeClosePage(pagePtr: Long)

    private external fun nativeClosePages(pagesPtr: LongArray?)

    private external fun nativeGetPageWidthPixel(pagePtr: Long, dpi: Int): Int

    private external fun nativeGetPageHeightPixel(pagePtr: Long, dpi: Int): Int

    private external fun nativeGetPageWidthPoint(pagePtr: Long): Int

    private external fun nativeGetPageHeightPoint(pagePtr: Long): Int

    //private native long nativeGetNativeWindow(Surface surface);
    //private native void nativeRenderPage(long pagePtr, long nativeWindowPtr);
    private external fun nativeRenderPage(
        pagePtr: Long, surface: Surface?, dpi: Int,
        startX: Int, startY: Int,
        drawSizeHor: Int, drawSizeVer: Int,
        renderAnnot: Boolean
    )

    private external fun nativeRenderPageBitmap(
        pagePtr: Long, bitmap: Bitmap?, dpi: Int,
        startX: Int, startY: Int,
        drawSizeHor: Int, drawSizeVer: Int,
        renderAnnot: Boolean
    )

    private external fun nativeGetDocumentMetaText(docPtr: Long, tag: String?): String?

    private external fun nativeGetFirstChildBookmark(docPtr: Long, bookmarkPtr: Long?): Long?

    private external fun nativeGetSiblingBookmark(docPtr: Long, bookmarkPtr: Long): Long?

    private external fun nativeGetBookmarkTitle(bookmarkPtr: Long): String?

    private external fun nativeGetBookmarkDestIndex(docPtr: Long, bookmarkPtr: Long): Long

    private external fun nativeGetPageSizeByIndex(docPtr: Long, pageIndex: Int, dpi: Int): Size?

    private external fun nativeGetPageLinks(pagePtr: Long): LongArray

    private external fun nativeGetDestPageIndex(docPtr: Long, linkPtr: Long): Int?

    private external fun nativeGetLinkURI(docPtr: Long, linkPtr: Long): String?

    private external fun nativeGetLinkRect(linkPtr: Long): RectF?

    private external fun nativePageCoordsToDevice(
        pagePtr: Long, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, pageX: Double, pageY: Double
    ): Point

    private external fun nativeLoadTextPage(pagePtr: Long): Long

    private external fun nativeCloseTextPage(textPagePtr: Long)

    private external fun nativeTextCountChars(textPagePtr: Long): Int

    private external fun nativeTextGetText(textPagePtr: Long, startIndex: Int, count: Int): String?

    private external fun nativeTextGetCharIndexAtPos(
        textPagePtr: Long, x: Double, y: Double, toleranceX: Double, toleranceY: Double
    ): Int

    private external fun nativeTextGetCharBox(textPagePtr: Long, index: Int): RectF?

    private external fun nativeAddTextMarkupAnnot(
        pagePtr: Long, subtype: Int, quads: FloatArray, r: Int, g: Int, b: Int, a: Int, name: String
    ): Boolean

    private external fun nativeAddInkAnnot(pagePtr: Long, points: FloatArray, width: Float,
        r: Int, g: Int, b: Int, a: Int, name: String): Boolean
    private external fun nativeRemoveAnnotByName(pagePtr: Long, name: String): Boolean
    private external fun nativeDeviceToPageCoords(pagePtr: Long, startX: Int, startY: Int,
        sizeX: Int, sizeY: Int, rotate: Int, deviceX: Int, deviceY: Int): android.graphics.PointF?

    private external fun nativeAddFreeTextAnnot(docPtr: Long, pagePtr: Long, text: String, fontPath: String?,
        fontSize: Float, x: Float, y: Float, r: Int, g: Int, b: Int, a: Int, name: String): FloatArray?
    private external fun nativeAddImageAnnot(docPtr: Long, pagePtr: Long, bitmap: Bitmap,
        left: Float, top: Float, right: Float, bottom: Float, name: String): Boolean
    private external fun nativeGetAnnots(pagePtr: Long): Array<String?>
    private external fun nativeRemoveAnnotAt(pagePtr: Long, index: Int): Boolean

    private external fun nativeEditSnapshot(docPtr: Long): ByteArray?
    private external fun nativeOpenEditSnapshot(snapshot: ByteArray): Long

    private external fun nativeSaveAsCopy(docPtr: Long, path: String): Boolean


    private val mCurrentDpi: Int

    /** Context needed to get screen density  */
    init {
        mCurrentDpi = ctx.resources.displayMetrics.densityDpi
    }

    /** Create new document from file with password  */
    /** Create new document from file  */
    @JvmOverloads
    @Throws(IOException::class)
    fun newDocument(fd: ParcelFileDescriptor, password: String? = null): PdfDocument {
        val document = PdfDocument()
        document.parcelFileDescriptor = fd
        synchronized(lock) {
            document.mNativeDocPtr = nativeOpenDocument(getNumFd(fd), password)
        }

        return document
    }

    /** Create new document from bytearray with password  */
    /** Create new document from bytearray  */
    @JvmOverloads
    @Throws(IOException::class)
    fun newDocument(data: ByteArray?, password: String? = null): PdfDocument {
        val document = PdfDocument()
        synchronized(lock) {
            document.mNativeDocPtr = nativeOpenMemDocument(data, password)
        }
        return document
    }

    /** Get total numer of pages in document  */
    fun getPageCount(doc: PdfDocument): Int {
        synchronized(lock) {
            return nativeGetPageCount(doc.mNativeDocPtr)
        }
    }

    /** Open page and store native pointer in [PdfDocument]  */
    fun openPage(doc: PdfDocument, pageIndex: Int): Long {
        val pagePtr: Long
        synchronized(lock) {
            pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex)
            doc.mNativePagesPtr[pageIndex] = pagePtr
            return pagePtr
        }
    }

    /** Open range of pages and store native pointers in [PdfDocument]  */
    fun openPage(doc: PdfDocument, fromIndex: Int, toIndex: Int): LongArray {
        val pagesPtr: LongArray
        synchronized(lock) {
            pagesPtr = nativeLoadPages(doc.mNativeDocPtr, fromIndex, toIndex)
            var pageIndex = fromIndex
            for (page in pagesPtr) {
                if (pageIndex > toIndex) break
                doc.mNativePagesPtr[pageIndex] = page
                pageIndex++
            }
            return pagesPtr
        }
    }

    /**
     * Get page width in pixels. <br></br>
     * This method requires page to be opened.
     */
    fun getPageWidth(doc: PdfDocument, index: Int): Int {
        synchronized(lock) {
            val pagePtr: Long?
            if ((doc.mNativePagesPtr.get(index).also { pagePtr = it }) != null) {
                return nativeGetPageWidthPixel(pagePtr!!, mCurrentDpi)
            }
            return 0
        }
    }

    /**
     * Get page height in pixels. <br></br>
     * This method requires page to be opened.
     */
    fun getPageHeight(doc: PdfDocument, index: Int): Int {
        synchronized(lock) {
            val pagePtr: Long?
            if ((doc.mNativePagesPtr.get(index).also { pagePtr = it }) != null) {
                return nativeGetPageHeightPixel(pagePtr!!, mCurrentDpi)
            }
            return 0
        }
    }

    /**
     * Get page width in PostScript points (1/72th of an inch).<br></br>
     * This method requires page to be opened.
     */
    fun getPageWidthPoint(doc: PdfDocument, index: Int): Int {
        synchronized(lock) {
            val pagePtr: Long?
            if ((doc.mNativePagesPtr.get(index).also { pagePtr = it }) != null) {
                return nativeGetPageWidthPoint(pagePtr!!)
            }
            return 0
        }
    }

    /**
     * Get page height in PostScript points (1/72th of an inch).<br></br>
     * This method requires page to be opened.
     */
    fun getPageHeightPoint(doc: PdfDocument, index: Int): Int {
        synchronized(lock) {
            val pagePtr: Long?
            if ((doc.mNativePagesPtr.get(index).also { pagePtr = it }) != null) {
                return nativeGetPageHeightPoint(pagePtr!!)
            }
            return 0
        }
    }

    /**
     * Get size of page in pixels.<br></br>
     * This method does not require given page to be opened.
     */
    fun getPageSize(doc: PdfDocument, index: Int): Size? {
        synchronized(lock) {
            return nativeGetPageSizeByIndex(doc.mNativeDocPtr, index, mCurrentDpi)
        }
    }

    /**
     * Render page fragment on [Surface]. This method allows to render annotations.<br></br>
     * Page must be opened before rendering.
     */
    /**
     * Render page fragment on [Surface].<br></br>
     * Page must be opened before rendering.
     */
    @JvmOverloads
    fun renderPage(
        doc: PdfDocument, surface: Surface?, pageIndex: Int,
        startX: Int, startY: Int, drawSizeX: Int, drawSizeY: Int,
        renderAnnot: Boolean = false
    ) {
        synchronized(lock) {
            try {
                //nativeRenderPage(doc.mNativePagesPtr.get(pageIndex), surface, mCurrentDpi);
                nativeRenderPage(
                    doc.mNativePagesPtr.get(pageIndex)!!, surface, mCurrentDpi,
                    startX, startY, drawSizeX, drawSizeY, renderAnnot
                )
            } catch (e: NullPointerException) {
                Log.e(TAG, "mContext may be null")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Exception throw from native")
                e.printStackTrace()
            }
        }
    }

    /**
     * Render page fragment on [Bitmap]. This method allows to render annotations.<br></br>
     * Page must be opened before rendering.
     * 
     * 
     * For more info see [renderPageBitmap]
     */
    /**
     * Render page fragment on [Bitmap].<br></br>
     * Page must be opened before rendering.
     * 
     * 
     * Supported bitmap configurations:
     * 
     *  * ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
     *  * RGB_565 - little worse quality, twice less memory usage
     * 
     */
    @JvmOverloads
    fun renderPageBitmap(
        doc: PdfDocument, bitmap: Bitmap?, pageIndex: Int,
        startX: Int, startY: Int, drawSizeX: Int, drawSizeY: Int,
        renderAnnot: Boolean = false
    ) {
        synchronized(lock) {
            try {
                nativeRenderPageBitmap(
                    doc.mNativePagesPtr.get(pageIndex)!!, bitmap, mCurrentDpi,
                    startX, startY, drawSizeX, drawSizeY, renderAnnot
                )
            } catch (e: NullPointerException) {
                Log.e(TAG, "mContext may be null")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Exception throw from native")
                e.printStackTrace()
            }
        }
    }

    /**
     * Add a text markup annotation (underline, strikeout...) to an opened page.
     * [quads] holds 8 values per quad in page coordinates: top left, top right, bottom left, bottom right.
     */
    fun addTextMarkupAnnot(doc: PdfDocument, pageIndex: Int, subtype: Int, quads: FloatArray, @ColorInt color: Int, name: String): Boolean {
        synchronized(lock) {
            val pagePtr = doc.mNativePagesPtr[pageIndex] ?: return false
            return nativeAddTextMarkupAnnot(
                pagePtr, subtype, quads,
                Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color), name
            )
        }
    }

    /** Add a named ink stroke in PDF page coordinates to the document in memory. */
    fun addInkAnnot(doc: PdfDocument, pageIndex: Int, points: FloatArray, width: Float, @ColorInt color: Int, name: String): Boolean {
        synchronized(lock) {
            val page = doc.mNativePagesPtr[pageIndex] ?: return false
            return nativeAddInkAnnot(page, points, width, Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color), name)
        }
    }

    /** Remove the annotation whose NM entry equals [name]. */
    fun removeAnnotByName(doc: PdfDocument, pageIndex: Int, name: String): Boolean {
        synchronized(lock) {
            val page = doc.mNativePagesPtr[pageIndex] ?: return false
            return nativeRemoveAnnotByName(page, name)
        }
    }

    /** Map device coordinates through PDFium, respecting page rotation. */
    fun deviceToPageCoords(doc: PdfDocument, pageIndex: Int, startX: Int, startY: Int, sizeX: Int, sizeY: Int,
        rotate: Int, deviceX: Int, deviceY: Int): android.graphics.PointF? {
        synchronized(lock) {
            val page = doc.mNativePagesPtr[pageIndex] ?: return null
            return nativeDeviceToPageCoords(page, startX, startY, sizeX, sizeY, rotate, deviceX, deviceY)
        }
    }

    /** Add text objects with an embedded font and return PDF bounds. */
    fun addFreeText(doc: PdfDocument, page: Int, text: String, fontPath: String?, size: Float,
        x: Float, y: Float, color: Int, name: String): RectF? = synchronized(lock) {
        val ptr = doc.mNativePagesPtr[page] ?: return@synchronized null
        nativeAddFreeTextAnnot(doc.mNativeDocPtr, ptr, text, fontPath, size, x, y,
            Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color), name)
            ?.let { RectF(it[0], it[1], it[2], it[3]) }
    }

    /** Add an image stamp in PDF coordinates. */
    fun addImage(doc: PdfDocument, page: Int, rect: RectF, bitmap: Bitmap, name: String): Boolean = synchronized(lock) {
        val ptr = doc.mNativePagesPtr[page] ?: return@synchronized false
        nativeAddImageAnnot(doc.mNativeDocPtr, ptr, bitmap, rect.left, rect.top, rect.right, rect.bottom, name)
    }

    /** Read annotation metadata from an opened document page. */
    fun getAnnotations(doc: PdfDocument, page: Int): List<com.reader.pdfviewer.model.PdfAnnotationInfo> = synchronized(lock) {
        val ptr = doc.mNativePagesPtr[page] ?: return@synchronized emptyList()
        nativeGetAnnots(ptr).toList().chunked(2).mapNotNull { row ->
            val v = row[0]?.split(' ') ?: return@mapNotNull null
            com.reader.pdfviewer.model.PdfAnnotationInfo(page, v[0].toInt(), v[1].toInt(),
                RectF(v[2].toFloat(), v[3].toFloat(), v[4].toFloat(), v[5].toFloat()), row[1]?.takeIf { it.isNotEmpty() })
        }
    }

    /** Remove an annotation by its current index. */
    fun removeAnnotAt(doc: PdfDocument, page: Int, index: Int): Boolean = synchronized(lock) {
        val ptr = doc.mNativePagesPtr[page] ?: return@synchronized false
        nativeRemoveAnnotAt(ptr, index)
    }

    /** Preserve arbitrary imported annotation dictionaries for deletion undo. */
    internal fun editSnapshot(doc: PdfDocument): ByteArray? = synchronized(lock) {
        if (doc.mNativeDocPtr == 0L) null else nativeEditSnapshot(doc.mNativeDocPtr)
    }

    /** Replace the in-memory document atomically, preserving the set of opened pages. */
    internal fun restoreEditSnapshot(doc: PdfDocument, snapshot: ByteArray): Boolean = synchronized(lock) {
        val replacement = nativeOpenEditSnapshot(snapshot)
        if (replacement == 0L) return@synchronized false
        val pages = HashMap<Int, Long>()
        try {
            for (page in doc.mNativePagesPtr.keys.filterNotNull()) {
                pages[page] = nativeLoadPage(replacement, page)
            }
        } catch (e: Exception) {
            pages.values.forEach { nativeClosePage(it) }
            nativeCloseDocument(replacement)
            return@synchronized false
        }
        doc.mNativeTextPagesPtr.values.forEach { nativeCloseTextPage(it!!) }
        doc.mNativeTextPagesPtr.clear()
        doc.mNativePagesPtr.values.forEach { nativeClosePage(it!!) }
        doc.mNativePagesPtr.clear()
        nativeCloseDocument(doc.mNativeDocPtr)
        doc.mNativeDocPtr = replacement
        doc.mNativePagesPtr.putAll(pages)
        true
    }

    /** Write the document with its in memory changes to [path]  */
    fun saveAsCopy(doc: PdfDocument, path: String): Boolean {
        synchronized(lock) {
            if (doc.mNativeDocPtr == 0L) {
                return false
            }
            return nativeSaveAsCopy(doc.mNativeDocPtr, path)
        }
    }

    /** Release native resources and opened file  */
    fun closeDocument(doc: PdfDocument) {
        synchronized(lock) {
            for (index in doc.mNativePagesPtr.keys) {
                nativeClosePage(doc.mNativePagesPtr.get(index)!!)
            }
            doc.mNativePagesPtr.clear()
            for (textPagePtr in doc.mNativeTextPagesPtr.values) {
                nativeCloseTextPage(textPagePtr!!)
            }
            doc.mNativeTextPagesPtr.clear()

            nativeCloseDocument(doc.mNativeDocPtr)
            doc.mNativeDocPtr = 0
            if (doc.parcelFileDescriptor != null) { //if document was loaded from file
                try {
                    doc.parcelFileDescriptor!!.close()
                } catch (e: IOException) {
                    /* ignore */
                }
                doc.parcelFileDescriptor = null
            }
        }
    }

    /** Get metadata for given document  */
    fun getDocumentMeta(doc: PdfDocument): PdfDocument.Meta {
        synchronized(lock) {
            val meta = PdfDocument.Meta()
            meta.title = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Title")
            meta.author = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Author")
            meta.subject = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Subject")
            meta.keywords = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Keywords")
            meta.creator = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Creator")
            meta.producer = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Producer")
            meta.creationDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "CreationDate")
            meta.modDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "ModDate")
            return meta
        }
    }

    /** Get table of contents (bookmarks) for given document  */
    fun getTableOfContents(doc: PdfDocument): MutableList<PdfDocument.Bookmark?> {
        synchronized(lock) {
            val topLevel: MutableList<PdfDocument.Bookmark?> = ArrayList<PdfDocument.Bookmark?>()
            val first = nativeGetFirstChildBookmark(doc.mNativeDocPtr, null)
            if (first != null) {
                recursiveGetBookmark(topLevel, doc, first)
            }
            return topLevel
        }
    }

    private fun recursiveGetBookmark(tree: MutableList<PdfDocument.Bookmark?>, doc: PdfDocument, bookmarkPtr: Long) {
        val bookmark = PdfDocument.Bookmark()
        bookmark.mNativePtr = bookmarkPtr
        bookmark.title = nativeGetBookmarkTitle(bookmarkPtr)
        bookmark.pageIdx = nativeGetBookmarkDestIndex(doc.mNativeDocPtr, bookmarkPtr)
        tree.add(bookmark)

        val child = nativeGetFirstChildBookmark(doc.mNativeDocPtr, bookmarkPtr)
        if (child != null) {
            recursiveGetBookmark(bookmark.children, doc, child)
        }

        val sibling = nativeGetSiblingBookmark(doc.mNativeDocPtr, bookmarkPtr)
        if (sibling != null) {
            recursiveGetBookmark(tree, doc, sibling)
        }
    }

    /** Get all links from given page  */
    fun getPageLinks(doc: PdfDocument, pageIndex: Int): MutableList<PdfDocument.Link?> {
        synchronized(lock) {
            val links: MutableList<PdfDocument.Link?> = ArrayList<PdfDocument.Link?>()
            val nativePagePtr = doc.mNativePagesPtr.get(pageIndex)
            if (nativePagePtr == null) {
                return links
            }
            val linkPtrs = nativeGetPageLinks(nativePagePtr)
            for (linkPtr in linkPtrs) {
                val index = nativeGetDestPageIndex(doc.mNativeDocPtr, linkPtr)
                val uri = nativeGetLinkURI(doc.mNativeDocPtr, linkPtr)

                val rect = nativeGetLinkRect(linkPtr)
                if (rect != null && (index != null || uri != null)) {
                    links.add(PdfDocument.Link(rect, index, uri))
                }
            }
            return links
        }
    }

    /**
     * Map page coordinates to device screen coordinates
     * 
     * @param doc       pdf document
     * @param pageIndex index of page
     * @param startX    left pixel position of the display area in device coordinates
     * @param startY    top pixel position of the display area in device coordinates
     * @param sizeX     horizontal size (in pixels) for displaying the page
     * @param sizeY     vertical size (in pixels) for displaying the page
     * @param rotate    page orientation: 0 (normal), 1 (rotated 90 degrees clockwise),
     * 2 (rotated 180 degrees), 3 (rotated 90 degrees counter-clockwise)
     * @param pageX     X value in page coordinates
     * @param pageY     Y value in page coordinate
     * @return mapped coordinates
     */
    fun mapPageCoordsToDevice(
        doc: PdfDocument, pageIndex: Int, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, pageX: Double, pageY: Double
    ): Point {
        val pagePtr: Long = doc.mNativePagesPtr.get(pageIndex)!!
        return nativePageCoordsToDevice(pagePtr, startX, startY, sizeX, sizeY, rotate, pageX, pageY)
    }

    /**
     * @return mapped coordinates
     * @see mapPageCoordsToDevice
     */
    fun mapRectToDevice(
        doc: PdfDocument, pageIndex: Int, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, coords: RectF
    ): RectF {
        val leftTop = mapPageCoordsToDevice(
            doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
            coords.left.toDouble(), coords.top.toDouble()
        )
        val rightBottom = mapPageCoordsToDevice(
            doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
            coords.right.toDouble(), coords.bottom.toDouble()
        )
        return RectF(leftTop.x.toFloat(), leftTop.y.toFloat(), rightBottom.x.toFloat(), rightBottom.y.toFloat())
    }

    /** Get text page pointer, loading it on first use. Page must be opened before.  */
    private fun textPagePtr(doc: PdfDocument, pageIndex: Int): Long? {
        doc.mNativeTextPagesPtr.get(pageIndex)?.let { return it }
        val pagePtr = doc.mNativePagesPtr.get(pageIndex) ?: return null
        val textPagePtr = nativeLoadTextPage(pagePtr)
        if (textPagePtr == 0L) {
            return null
        }
        doc.mNativeTextPagesPtr.put(pageIndex, textPagePtr)
        return textPagePtr
    }

    /** Get number of characters on page. Page must be opened before.  */
    fun getPageTextCount(doc: PdfDocument, pageIndex: Int): Int {
        synchronized(lock) {
            val textPagePtr = textPagePtr(doc, pageIndex) ?: return 0
            return nativeTextCountChars(textPagePtr)
        }
    }

    /** Get whole text of page. Page must be opened before.  */
    fun getPageText(doc: PdfDocument, pageIndex: Int): String? {
        synchronized(lock) {
            val textPagePtr = textPagePtr(doc, pageIndex) ?: return null
            return nativeTextGetText(textPagePtr, 0, nativeTextCountChars(textPagePtr))
        }
    }

    /**
     * Render the whole page into [bitmap] without keeping the page open, for one-off uses such as OCR.
     * @return false when the page cannot be loaded
     */
    fun renderPageBitmapOnce(doc: PdfDocument, bitmap: Bitmap, pageIndex: Int): Boolean {
        synchronized(lock) {
            if (doc.mNativeDocPtr == 0L) {
                return false
            }
            doc.mNativePagesPtr[pageIndex]?.let { pagePtr ->
                nativeRenderPageBitmap(pagePtr, bitmap, mCurrentDpi, 0, 0, bitmap.width, bitmap.height, false)
                return true
            }
            val pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex)
            if (pagePtr == 0L) {
                return false
            }
            try {
                nativeRenderPageBitmap(pagePtr, bitmap, mCurrentDpi, 0, 0, bitmap.width, bitmap.height, false)
                return true
            } finally {
                nativeClosePage(pagePtr)
            }
        }
    }

    /**
     * Get whole text of page without keeping it open, for one-off reads such as a document search.
     * An opened page is read as usual, any other page is loaded, read and closed right away.
     */
    fun readPageText(doc: PdfDocument, pageIndex: Int): String? {
        synchronized(lock) {
            if (doc.mNativeDocPtr == 0L) {
                return null
            }
            if (doc.mNativePagesPtr.containsKey(pageIndex)) {
                return getPageText(doc, pageIndex)
            }
            val pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex)
            if (pagePtr == 0L) {
                return null
            }
            try {
                val textPagePtr = nativeLoadTextPage(pagePtr)
                if (textPagePtr == 0L) {
                    return null
                }
                try {
                    return nativeTextGetText(textPagePtr, 0, nativeTextCountChars(textPagePtr))
                } finally {
                    nativeCloseTextPage(textPagePtr)
                }
            } finally {
                nativeClosePage(pagePtr)
            }
        }
    }

    /** Get [count] characters of page text starting at [startIndex]. Page must be opened before.  */
    fun getPageText(doc: PdfDocument, pageIndex: Int, startIndex: Int, count: Int): String? {
        synchronized(lock) {
            val textPagePtr = textPagePtr(doc, pageIndex) ?: return null
            return nativeTextGetText(textPagePtr, startIndex, count)
        }
    }

    /**
     * Get index of character at or near given position in page coordinates.
     * @return character index or -1 if none was found
     */
    fun getCharIndexAtCoord(
        doc: PdfDocument, pageIndex: Int, x: Double, y: Double, toleranceX: Double, toleranceY: Double
    ): Int {
        synchronized(lock) {
            val textPagePtr = textPagePtr(doc, pageIndex) ?: return -1
            return nativeTextGetCharIndexAtPos(textPagePtr, x, y, toleranceX, toleranceY)
        }
    }

    /** Get bounding box of character in page coordinates. Page must be opened before.  */
    fun getCharBox(doc: PdfDocument, pageIndex: Int, charIndex: Int): RectF? {
        synchronized(lock) {
            val textPagePtr = textPagePtr(doc, pageIndex) ?: return null
            return nativeTextGetCharBox(textPagePtr, charIndex)
        }
    }

    companion object {
        private val TAG: String = PdfiumCore::class.java.getName()
        private val FD_CLASS: Class<*> = FileDescriptor::class.java
        private const val FD_FIELD_NAME = "descriptor"

        init {
            try {
                System.loadLibrary("c++_shared")
                System.loadLibrary("pdfium")
                System.loadLibrary("jniPdfium")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native libraries failed to load - " + e)
            }
        }

        /* synchronize native methods */
        private val lock = Any()
        private var mFdField: Field? = null
        fun getNumFd(fdObj: ParcelFileDescriptor): Int {
            try {
                if (mFdField == null) {
                    mFdField = FD_CLASS.getDeclaredField(FD_FIELD_NAME)
                    mFdField!!.setAccessible(true)
                }

                return mFdField!!.getInt(fdObj.getFileDescriptor())
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
                return -1
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                return -1
            }
        }
    }
}
