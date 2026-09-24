/*
 * 文件名称:          PDFView.java
 *
 * 编译器:            android2.2
 * 时间:              下午7:26:45
 */
package com.wxiwei.office.pdf

import com.wxiwei.office.system.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.wxiwei.office.common.IOfficeToPicture
import com.wxiwei.office.constant.EventConstant
import com.wxiwei.office.fc.pdf.PDFLib
import com.wxiwei.office.system.IControl
import com.wxiwei.office.system.IFind
import com.wxiwei.office.system.SysKit
import com.wxiwei.office.system.beans.pagelist.APageListItem
import com.wxiwei.office.system.beans.pagelist.APageListView
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener
import com.wxiwei.office.simpletext.control.SafeAsyncTask

/**
 * PDF document rending
 */
class PDFView : FrameLayout, IPageListViewListener {

    private var preShowPageIndex = -1

    //
    private var control: IControl? = null

    //
    private var find: PDFFind? = null

    //
    private var pdfLib: PDFLib? = null

    //
    private var listView: APageListView? = null

    //
    private var pagesSize: Array<Rect>? = null

    // 绘制器
    private var paint: Paint? = null

    //
    private var exportTask: SafeAsyncTask<Void?, Any?, Bitmap?>? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, pdfLib: PDFLib, control: IControl) : super(context) {
        this.control = control
        this.pdfLib = pdfLib

        val listView = APageListView(context, this)
        this.listView = listView
        addView(listView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        find = PDFFind(this)

        val paint = Paint()
        this.paint = paint
        paint.isAntiAlias = true
        paint.typeface = Typeface.SANS_SERIF
        paint.textSize = 24f

        if (!pdfLib.hasPasswordSync()) {
            pagesSize = pdfLib.allPagesSize
        }
    }

    override fun setBackgroundColor(color: Int) {
        super.setBackgroundColor(color)
        listView?.setBackgroundColor(color)
    }

    override fun setBackgroundResource(resid: Int) {
        super.setBackgroundResource(resid)
        listView?.setBackgroundResource(resid)
    }

    @Suppress("DEPRECATION")
    override fun setBackgroundDrawable(d: Drawable?) {
        super.setBackgroundDrawable(d)
        listView?.setBackgroundDrawable(d)
    }

    /**
     * (non-Javadoc)
     * @see ViewGroup#dispatchDraw(Canvas)
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawPageNubmer(canvas)
    }

    fun init() {
        if (pdfLib!!.hasPasswordSync()) {
            PasswordDialog(control!!, pdfLib!!).show()
        }
    }

    fun setZoom(zoom: Float, pointX: Int, pointY: Int) {
        listView!!.setZoom(zoom, pointX, pointY)
    }

    /**
     * set fit size for PPT，Word view mode, PDf
     *
     * @param  value  fit size mode
     *          = 0, fit size of get minimum value of pageWidth / viewWidth and pageHeight / viewHeight;
     *          = 1, fit size of pageWidth
     *          = 2, fit size of PageHeight
     */
    fun setFitSize(value: Int) {
        listView!!.setFitSize(value)
    }

    /**
     * get fit size statue
     *
     * @return fit size statue
     *          = 0, left/right and top/bottom don't alignment
     *          = 1, top/bottom alignment
     *          = 2, left/right alignment
     *          = 3, left/right and top/bottom alignment
     */
    fun getFitSizeState(): Int {
        return listView!!.fitSizeState
    }

    fun getZoom(): Float {
        return listView!!.zoom
    }

    fun getFitZoom(): Float {
        return listView!!.fitZoom
    }

    /**
     * get current display page number (base 1)
     *
     * @return page number (base 1)
     */
    fun getCurrentPageNumber(): Int {
        return listView!!.currentPageNumber
    }

    fun getFind(): IFind {
        return this.find!!
    }

    fun getPDFLib(): PDFLib {
        return this.pdfLib!!
    }

    fun getListView(): APageListView {
        return this.listView!!
    }

    fun nextPageView() {
        listView!!.nextPageView()
    }

    fun previousPageview() {
        listView!!.previousPageview()
    }

    /**
     * switch page for page index (base 0)
     */
    fun showPDFPageForIndex(index: Int) {
        listView!!.showPDFPageForIndex(index)
    }

    /**
     * page to image for page number (base 1)
     *
     * @return bitmap raw data
     */
    fun pageToImage(pageNumber: Int): Bitmap? {
        if (pageNumber <= 0 || pageNumber > getPageCount()) {
            return null
        }
        val rect = getPageSize(pageNumber - 1)!!
        val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888)

        pdfLib!!.drawPageSync(
            bitmap, pageNumber - 1, rect.width().toFloat(), rect.height().toFloat(),
            0, 0, rect.width(), rect.height(), 1
        )

        return bitmap
    }

    /**
     * page to image for page number (base 1)
     *
     * @return bitmap raw data
     */
    fun getThumbnail(pageNumber: Int, zoom: Float): Bitmap? {
        if (pageNumber <= 0 || pageNumber > getPageCount()) {
            return null
        }
        val rect = getPageSize(pageNumber - 1)!!
        val w = (rect.width() * zoom).toInt()
        val h = (rect.height() * zoom).toInt()
        var bitmap: Bitmap? = null
        try {
            bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888)
            pdfLib!!.drawPageSync(bitmap, pageNumber - 1, w.toFloat(), h.toFloat(), 0, 0, w, h, 1)
        } catch (e: OutOfMemoryError) {
            control!!.sysKit.errorKit.writerLog(e)
        }
        return bitmap
    }

    /**
     * specific area of page to image. if area is not completely contained in the page, return null
     */
    fun pageAreaToImage(
        pageNumber: Int, srcLeft: Int, srcTop: Int, srcWidth: Int, srcHeight: Int,
        desWidth: Int, desHeight: Int
    ): Bitmap? {
        if (pageNumber <= 0 || pageNumber > getPageCount()) {
            return null
        }
        //Rectangle rect = pdfLib.getPageSizeSync(pageNumber - 1);
        val rect = getPageSize(pageNumber - 1)!!
        if (!SysKit.isValidateRect(rect.width(), rect.height(), srcLeft, srcTop, srcWidth, srcHeight)) {
            return null
        }
        val paintZoom = Math.min(desWidth / srcWidth.toFloat(), desHeight / srcHeight.toFloat())

        val bitmap: Bitmap?
        try {
            bitmap = Bitmap.createBitmap(
                (srcWidth * paintZoom).toInt(), (srcHeight * paintZoom).toInt(), Config.ARGB_8888
            )
        } catch (e: OutOfMemoryError) {
            return null
        }
        if (bitmap == null) {
            return null
        }
        pdfLib!!.drawPageSync(
            bitmap, pageNumber - 1, rect.width() * paintZoom, rect.height() * paintZoom,
            (srcLeft * paintZoom).toInt(), (srcTop * paintZoom).toInt(),
            (srcWidth * paintZoom).toInt(), (srcHeight * paintZoom).toInt(), 1
        )

        return bitmap
    }

    fun passwordVerified() {
        if (listView != null) {
            pagesSize = pdfLib!!.allPagesSize
            control!!.mainFrame.openFileFinish()
            listView!!.init()
        }
    }

    override fun getPageCount(): Int {
        return pdfLib!!.pageCountSync
    }

    override fun getPageListItem(position: Int, convertView: View?, parent: ViewGroup?): APageListItem {
        val rect = getPageSize(position)!!
        return PDFPageListItem(listView!!, control!!, rect.width(), rect.height())
    }

    override fun getPageSize(pageIndex: Int): Rect? {
        val pagesSize = this.pagesSize
        if (pagesSize == null || pageIndex < 0 || pageIndex >= pagesSize.size) {
            return null
        }
        return pagesSize[pageIndex]
    }

    override fun exportImage(pageItem: APageListItem, srcBitmap: Bitmap?) {
        if (getControl() == null || srcBitmap == null) {
            return
        }
        val find = this.find!!
        if (find.isSetPointToVisible()) {
            find.setSetPointToVisible(false)
            val rectF = find.getSearchResult()
            if (rectF != null && rectF.isNotEmpty()) {
                if (!listView!!.isPointVisibleOnScreen(rectF[0].left.toInt(), rectF[0].top.toInt())) {
                    listView!!.setItemPointVisibleOnScreen(rectF[0].left.toInt(), rectF[0].top.toInt())
                    return
                }
            }
        }
        if (exportTask != null) {
            exportTask!!.cancel(true)
            exportTask = null
        }
        exportTask = object : SafeAsyncTask<Void?, Any?, Bitmap?>() {
            private var isCancal = false

            override fun doInBackground(vararg v: Void?): Bitmap? {
                if (control == null || pdfLib == null) {
                    return null
                }
                try {
                    val otp = control!!.officeToPicture
                    if (otp != null && otp.modeType == IOfficeToPicture.VIEW_CHANGE_END) {
                        val rW = Math.min(width, srcBitmap.width)
                        val rH = Math.min(height, srcBitmap.height)
                        val dstBitmap = otp.getBitmap(rW, rH) ?: return null
                        val canvas = Canvas(dstBitmap)
                        // don't zoom
                        var dx = 0
                        var dy = 0
                        val left = pageItem.left
                        val top = pageItem.top
                        if (dstBitmap.width == rW && dstBitmap.height == rH) {
                            if (srcBitmap.width != rW || srcBitmap.height != rH) {
                                dx = Math.min(0, pageItem.left)
                                dy = Math.min(0, pageItem.top)
                            }
                            canvas.drawBitmap(srcBitmap, dx.toFloat(), dy.toFloat(), paint)
                            canvas.translate(
                                -(Math.max(left, 0) - left).toFloat(),
                                -(Math.max(top, 0) - top).toFloat()
                            )
                            control!!.sysKit.calloutManager.drawPath(canvas, pageItem.pageIndex, getZoom())
                        }
                        // zoom
                        else {
                            val matrix = Matrix()
                            val xZoom = dstBitmap.width / rW.toFloat()
                            val yZoom = dstBitmap.height / rH.toFloat()
                            matrix.postScale(xZoom, yZoom)
                            if ((getZoom() * 1000000).toInt() == 1000000) {
                                matrix.postTranslate(
                                    Math.min(pageItem.left, 0).toFloat(),
                                    Math.min(pageItem.top, 0).toFloat()
                                )
                                dx = Math.min(0, (pageItem.left * xZoom).toInt())
                                dy = Math.min(0, (pageItem.top * yZoom).toInt())
                            }
                            try {
                                val scaleBitmp = Bitmap.createBitmap(
                                    srcBitmap, 0, 0,
                                    srcBitmap.width, srcBitmap.height, matrix, true
                                )
                                canvas.drawBitmap(scaleBitmp, dx.toFloat(), dy.toFloat(), paint)
                            } catch (e: OutOfMemoryError) {
                                canvas.drawBitmap(srcBitmap, matrix, paint)
                            }
                            canvas.translate(
                                -(Math.max(left, 0) - left).toFloat(),
                                -(Math.max(top, 0) - top).toFloat()
                            )
                            control!!.sysKit.calloutManager.drawPath(canvas, pageItem.pageIndex, getZoom())
                        }
                        return dstBitmap
                    }
                } catch (e: Exception) {
                }
                return null
            }

            override fun onPreExecute() {
            }

            override fun onCancelled() {
                isCancal = true
            }

            override fun onPostExecute(bitmap: Bitmap?) {
                try {
                    if (bitmap != null) {
                        if (control == null || isCancal) {
                            return
                        }
                        val otp = control!!.officeToPicture
                        if (otp != null && otp.modeType == IOfficeToPicture.VIEW_CHANGE_END) {
                            otp.callBack(bitmap)
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
        //exportTask.execute(null);
    }

    fun getSanpshot(destBitmap: Bitmap?): Bitmap? {
        if (destBitmap == null) {
            return null
        }
        val pageItem = listView!!.currentPageView ?: return null
        val rW = Math.min(width, pageItem.width)
        val rH = Math.min(height, pageItem.height)
        val xZoom = destBitmap.width / rW.toFloat()
        val yZoom = destBitmap.height / rH.toFloat()
        val left = (pageItem.left * xZoom).toInt()
        val top = (pageItem.top * yZoom).toInt()
        val tX = Math.max(left, 0) - left
        val tY = Math.max(top, 0) - top
        val tW = pageItem.pageWidth * xZoom * getZoom()
        val tH = pageItem.pageHeight * yZoom * getZoom()

        pdfLib!!.drawPageSync(
            destBitmap, pageItem.pageIndex,
            tW, tH, tX, tY,
            destBitmap.width, destBitmap.height, 1
        )
        val paint = this.paint!!
        if (tX == 0 && tW < destBitmap.width && rW == pageItem.width) {
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            val span = destBitmap.width - tW
            val canvas = Canvas(destBitmap)
            canvas.drawRect(
                destBitmap.width - span, 0f,
                destBitmap.width.toFloat(), destBitmap.height.toFloat(), paint
            )
        }
        return destBitmap
    }

    override fun isInit(): Boolean {
        return !pdfLib!!.hasPasswordSync()
    }

    /**
     * @return
     * true fitzoom may be larger than 100% but smaller than the max zoom
     * false fitzoom can not larger than 100%
     */
    override fun isIgnoreOriginalSize(): Boolean {
        return control!!.mainFrame.isIgnoreOriginalSize
    }

    /**
     * page list view moving position
     */
    override fun getPageListViewMovingPosition(): Byte {
        return control!!.mainFrame.pageListViewMovingPosition
    }

    override fun getModel(): Any? {
        return pdfLib
    }

    fun getControl(): IControl? {
        return this.control
    }

    /**
     * event method, office engine dispatch
     *
     * @param       v             event source
     * @param       e1            MotionEvent instance
     * @param       e2            MotionEvent instance
     * @param       velocityX     x axis velocity
     * @param       velocityY     y axis velocity
     * @param       eventMethodType  event method
     */
    override fun onEventMethod(
        v: View?, e1: MotionEvent?, e2: MotionEvent?,
        velocityX: Float, velocityY: Float, eventMethodType: Byte
    ): Boolean {
        return control!!.mainFrame.onEventMethod(v, e1, e2, velocityX, velocityY, eventMethodType)
    }

    override fun updateStutus(obj: Any?) {
        control!!.actionEvent(EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS, obj)
    }

    override fun resetSearchResult(pageItem: APageListItem) {
        val find = this.find
        if (find != null) {
            if (pageItem.pageIndex != find.getPageIndex()) {
                find.resetSearchResult()
            }
        }
    }

    override fun isTouchZoom(): Boolean {
        return control!!.mainFrame.isTouchZoom
    }

    override fun isShowZoomingMsg(): Boolean {
        return control!!.mainFrame.isShowZoomingMsg
    }

    override fun changeZoom() {
        control!!.mainFrame.changeZoom()
    }

    override fun setDrawPictrue(isDrawPictrue: Boolean) {
    }

    /**
     *  set change page flag, Only when effectively the PageSize greater than ViewSize.
     *  (for PPT, word print mode, PDF)
     */
    override fun isChangePage(): Boolean {
        return control!!.mainFrame.isChangePage
    }

    /**
     * 绘制页信息
     */
    private fun drawPageNubmer(canvas: Canvas) {
        val paint = this.paint!!
        if (control!!.mainFrame.isDrawPageNumber) {
            val pn = (listView!!.currentPageNumber).toString() + " / " + pdfLib!!.pageCountSync
            val w = paint.measureText(pn).toInt()
            val h = (paint.descent() - paint.ascent()).toInt()
            val x = (width - w) / 2
            var y = (height - h) - 20

            val drawable: Drawable = SysKit.getPageNubmerDrawable()
            drawable.setBounds(x - 10, y - 10, x + w + 10, y + h + 10)
            drawable.draw(canvas)

            y -= paint.ascent().toInt()
            canvas.drawText(pn, x.toFloat(), y.toFloat(), paint)
        }

        if (listView!!.isInit && preShowPageIndex != listView!!.currentPageNumber) {
            control!!.mainFrame.changePage()
            preShowPageIndex = listView!!.currentPageNumber
        }
    }

    fun dispose() {
        if (find != null) {
            find!!.dispose()
            find = null
        }
        if (pdfLib != null) {
            pdfLib!!.setStopFlagSync(1)
            pdfLib = null
        }
        if (listView != null) {
            listView!!.dispose()
        }
        control = null
    }
}
