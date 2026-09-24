package com.wxiwei.office.macro

import android.graphics.Bitmap
import android.graphics.Rect
import com.wxiwei.office.fc.pdf.PDFLib

class PDFLibKit {

    /**
     * Construct
     *
     * @param filename
     *
     * @throws Exception
     */
    @Synchronized
    @Throws(Exception::class)
    fun openFileSync(filename: String) {
        lib!!.openFileSync(filename)
    }

    /**
     * get page count
     *
     * @return page count
     */
    fun getPageCountSync(): Int {
        return lib!!.pageCountSync
    }

    fun getAllPagesSize(): Array<Rect> {
        return lib!!.allPagesSize
    }

    /**
     * draw page to bitmap
     *
     * @param bitmap        Bitmap instance
     * @param pageIndex     The page index (base 0)
     * @param pageWidth     The page width of after scaling
     * @param pageHeight    The page height of after scaling
     * @param paintX        The paint X axis
     * @param paintY        The paint Y axis
     * @param paintWidth    The paint width
     * @param paintHeight   The paint height
     */
    @Synchronized
    fun drawPageSync(
        bitmap: Bitmap, pageIndex: Int, pageWidth: Float, pageHeight: Float,
        paintX: Int, paintY: Int, paintWidth: Int, paintHeight: Int, drawObject: Int
    ) {
        lib!!.drawPageSync(bitmap, pageIndex, pageWidth, pageHeight, paintX, paintY, paintWidth, paintHeight, drawObject)
    }

    /**
     * is this PDF document password?
     *
     * @return      = true     have password
     *               = false    no password
     */
    @Synchronized
    fun hasPasswordSync(): Boolean {
        return lib!!.hasPasswordSync()
    }

    /**
     * Authenticate password the PDF document
     *
     * @param   password
     * @return  = true  correct
     *           = false    wrong
     */
    @Synchronized
    fun authenticatePasswordSync(password: String): Boolean {
        return lib!!.authenticatePasswordSync(password)
    }

    /**
     * @param flag
     */
    fun setStopFlagSync(flag: Int) {
        lib!!.setStopFlagSync(flag)
    }

    /**
     * dispose memory
     */
    @Synchronized
    fun dispose() {
        lib = null
    }

    companion object {
        private val kit = PDFLibKit()
        private var lib: PDFLib? = PDFLib.getPDFLib()

        @JvmStatic
        fun instance(): PDFLibKit {
            return kit
        }
    }
}
