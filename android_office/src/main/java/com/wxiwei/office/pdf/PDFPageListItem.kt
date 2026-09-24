/*
 * 文件名称:          PDFPageView.java
 *
 * 编译器:            android2.2
 * 时间:              下午9:36:57
 */
package com.wxiwei.office.pdf

import com.wxiwei.office.system.*

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import com.wxiwei.office.constant.EventConstant
import com.wxiwei.office.constant.PDFConstant
import com.wxiwei.office.fc.pdf.PDFLib
import com.wxiwei.office.simpletext.control.SafeAsyncTask
import com.wxiwei.office.system.IControl
import com.wxiwei.office.system.beans.pagelist.APageListItem
import com.wxiwei.office.system.beans.pagelist.APageListView

/**
 * PDF document page view
 */
class PDFPageListItem(listView: APageListView, control: IControl, pageWidth: Int, pageHeight: Int) :
    APageListItem(listView, pageWidth, pageHeight) {

    //
    private var isOriginalBitmapValid = false

    //
    private val isAutoTest: Boolean

    // the width of repaint component
    private var viewWidth = 0

    // the height of repaint component
    private var viewHeight = 0

    // Image rendered at minimum zoom
    private var originalImageView: ImageView? = null

    // Image rendered at minimum zoom
    private var originalBitmap: Bitmap? = null

    // draw original page image
    private var darwOriginalPageTask: SafeAsyncTask<Void?, Void?, Bitmap?>? = null

    // repaint image view
    private var repaintImageView: ImageView? = null

    // repaint synchronized task
    private var repaintSyncTask: SafeAsyncTask<RepaintAreaInfo, Void, RepaintAreaInfo?>? = null

    // View size on the basis of which the patch was created
    // parent component size
    private var repaintArea: Rect? = null

    //
    private var searchView: View? = null

    //
    private val lib: PDFLib

    //
    private var mBusyIndicator: ProgressBar? = null

    init {
        this.listView = listView
        this.control = control
        this.lib = listView.model as PDFLib
        this.isAutoTest = control.isAutoTest
        this.setBackgroundColor(PDFConstant.BACKGROUND_COLOR)
    }

    /**
     * @see android.view.ViewGroup#onLayout(boolean, int, int, int, int)
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val w = right - left
        val h = bottom - top
        originalImageView?.layout(0, 0, w, h)
        searchView?.layout(0, 0, w, h)
        if (viewWidth != w || viewHeight != h) {
            viewHeight = 0
            viewWidth = 0
            repaintArea = null
            repaintImageView?.setImageBitmap(null)
        } else {
            repaintImageView?.layout(
                repaintArea!!.left, repaintArea!!.top, repaintArea!!.right, repaintArea!!.bottom
            )
        }
//        if (control.getMainFrame().isShowProgressBar() || true)
        run {
            val mBusyIndicator = this.mBusyIndicator
            if (mBusyIndicator != null) {
                val x: Int
                val y: Int
                if (w > listView.width) {
                    x = (listView.width - PDFConstant.BUSY_SIZE) / 2 - left
                } else {
                    x = (w - PDFConstant.BUSY_SIZE) / 2
                }
                if (h > listView.height) {
                    y = (listView.height - PDFConstant.BUSY_SIZE) / 2 - top
                } else {
                    y = (h - PDFConstant.BUSY_SIZE) / 2
                }
                mBusyIndicator.layout(x, y, x + PDFConstant.BUSY_SIZE, y + PDFConstant.BUSY_SIZE)
            }
        }
    }

    /**
     * @param pIndex     page index (base 0)
     * @param pWidth     page width of after scaled
     * @param pHeight    page height of after scaled
     */
    override fun setPageItemRawData(pIndex: Int, pWidth: Int, pHeight: Int) {
        super.setPageItemRawData(pIndex, pWidth, pHeight)
        isOriginalBitmapValid = false
        // Cancel pending render task
        if (darwOriginalPageTask != null) {
            darwOriginalPageTask!!.cancel(true)
            darwOriginalPageTask = null
        }
        if (originalImageView == null) {
            val originalImageView: ImageView = object : ImageView(listView.context) {
                override fun isOpaque(): Boolean {
                    return true
                }

                public override fun onDraw(canvas: Canvas) {
                    try {
                        super.onDraw(canvas)
                    } catch (e: Exception) {
                    }
                }
            }
            this.originalImageView = originalImageView
            originalImageView.scaleType = ImageView.ScaleType.FIT_XY
            addView(originalImageView)
        }

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        if (pageWidth <= 0 || pageHeight <= 0) {
            return
        }
        // When hardware accelerated, updates to the bitmap seem to be
        // ignored, so we recreate it. There may be another way around this
        // that we are yet to find.
        originalImageView!!.setImageBitmap(null)

        val zoom = listView.fitZoom
        if (originalBitmap == null
            || originalBitmap!!.width != (pageWidth * zoom).toInt()
            || originalBitmap!!.height != (pageHeight * zoom).toInt()
        ) {
            val bW = (pageWidth * zoom).toInt()
            val bH = (pageHeight * zoom).toInt()
            try {
                if (!listView.isInitZoom) {
                    listView.setZoom(zoom, false)
                }
                if (originalBitmap != null) {
                    while (!lib.isDrawPageSyncFinished) {
                        try {
                            Thread.sleep(100)
                        } catch (e: Exception) {
                        }
                    }
                    originalBitmap!!.recycle()
                }
                originalBitmap = Bitmap.createBitmap(bW, bH, Bitmap.Config.ARGB_8888)
            } catch (e: OutOfMemoryError) {
                System.gc()
                try {
                    Thread.sleep(50)
                    originalBitmap = Bitmap.createBitmap(bW, bH, Bitmap.Config.ARGB_8888)
                } catch (ee: Exception) {
                    return
                }
            }
        }

        val own: APageListItem = this
        // Render the page in the background
        darwOriginalPageTask = object : SafeAsyncTask<Void?, Void?, Bitmap?>() {
            private var isCancel = false

            override fun doInBackground(vararg v: Void?): Bitmap? {
                try {
                    if (originalBitmap == null) {
                        return null
                    }
                    Thread.sleep(if (pageIndex == listView.currentPageNumber - 1) 500L else 1000L)

                    if (isCancel) {
                        return null
                    }
                    lib.drawPageSync(
                        originalBitmap, pageIndex,
                        originalBitmap!!.width.toFloat(), originalBitmap!!.height.toFloat(),
                        0, 0, originalBitmap!!.width, originalBitmap!!.height, 1
                    )
                    return originalBitmap
                } catch (e: Exception) {
                    return null
                }
            }

            override fun onPreExecute() {
                originalImageView!!.setImageBitmap(null)
                if (mBusyIndicator == null) {
                    val mBusyIndicator = ProgressBar(context)
                    this@PDFPageListItem.mBusyIndicator = mBusyIndicator
                    mBusyIndicator.isIndeterminate = true
                    mBusyIndicator.setBackgroundResource(android.R.drawable.progress_horizontal)
                    addView(mBusyIndicator)
                    mBusyIndicator.visibility = VISIBLE
                } else {
                    mBusyIndicator!!.visibility = VISIBLE
                }
            }

            override fun onCancelled() {
                isCancel = true
            }

            override fun onPostExecute(v: Bitmap?) {
                try {
                    mIsBlank = false
                    isOriginalBitmapValid = true
                    if (listView != null) {
                        if (mBusyIndicator != null) {
                            mBusyIndicator!!.visibility = INVISIBLE
                        }
                    }
                    listView.setDoRequstLayout(false)
                    originalImageView!!.setImageBitmap(originalBitmap)
                    listView.setDoRequstLayout(true)
                    invalidate()
                    if (listView != null) {
                        if ((listView.zoom * 100).toInt() == 100 || (isInit && pIndex == 0)) {
                            if (v != null) {
                                if (isInit && pIndex == 0) {
                                    listView.postRepaint(listView.currentPageView)
                                } else {
                                    listView.exportImage(own, originalBitmap)
                                }
                            }
                        }
                        isInit = false
                        // auto test
                        if (isAutoTest) {
                            control.actionEvent(EventConstant.SYS_AUTO_TEST_FINISH_ID, true)
                        }
                    }
                } catch (e: NullPointerException) {
                }
            }
        }

        darwOriginalPageTask!!.safeExecute()

        if (searchView == null) {
            val searchView: View = object : View(context) {
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    val find = control.find as? PDFFind
                    if (find != null && !mIsBlank) {
                        find.drawHighlight(canvas, 0, 0, own)
                    }
                }
            }
            this.searchView = searchView
            @Suppress("DEPRECATION")
            addView(searchView, LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT))
        }
    }

    override fun releaseResources() {
        super.releaseResources()
        isOriginalBitmapValid = false
        // Cancel pending render task
        if (darwOriginalPageTask != null) {
            darwOriginalPageTask!!.cancel(true)
            darwOriginalPageTask = null
        }

        if (repaintSyncTask != null) {
            repaintSyncTask!!.cancel(true)
            repaintSyncTask = null
        }
        originalImageView?.setImageBitmap(null)

        repaintImageView?.setImageBitmap(null)

        if (control.mainFrame.isShowProgressBar || true) {
            if (mBusyIndicator != null) {
                mBusyIndicator!!.visibility = VISIBLE
            }
        }
    }

    /**
     * black page
     *
     * @param pIndex page index (base 0)
     */
    override fun blank(pIndex: Int) {
        super.blank(pIndex)
        isOriginalBitmapValid = false
        // Cancel pending render task
        if (darwOriginalPageTask != null) {
            darwOriginalPageTask!!.cancel(true)
            darwOriginalPageTask = null
        }

        if (repaintSyncTask != null) {
            repaintSyncTask!!.cancel(true)
            repaintSyncTask = null
        }
        originalImageView?.setImageBitmap(null)

        repaintImageView?.setImageBitmap(null)

        if (mBusyIndicator != null) {
            mBusyIndicator!!.visibility = VISIBLE
        }
    }

    override fun setLinkHighlighting(vlaue: Boolean) {
        /*if (mSearchView != null)
        {
            mSearchView.invalidate();
        }*/
    }

    /**
     * get hyperlink count assign location
     *
     * @param x     x axis value
     * @param y     y axis value
     * @return hyperlink count
     */
    fun getHyperlinkCount(x: Float, y: Float): Int {
        val scale = width / pageWidth.toFloat()
        val docRelX = (x - left) / scale
        val docRelY = (y - top) / scale

        return lib.getHyperlinkCountSync(pageIndex, docRelX, docRelY)
    }

    /**
     * added reapint image view
     */
    override fun addRepaintImageView(bmp: Bitmap?) {
        val viewArea = Rect(left, top, right, bottom)
        // If the viewArea's size matches the unzoomed size, there is no need for an hq patch
        if (viewArea.width() != pageWidth || viewArea.height() != pageHeight
            || (originalBitmap != null && (listView.zoom.toInt()) * 100 == 100
                    && (originalBitmap!!.width != pageWidth || originalBitmap!!.height != pageHeight))
        ) {
            //Point patchViewSize = new Point(viewArea.width(), viewArea.height());
            val paintArea = Rect(0, 0, listView.width, listView.height)

            // Intersect and test that there is an intersection
            if (!paintArea.intersect(viewArea)) {
                return
            }

            // Offset patch area to be relative to the view top left
            paintArea.offset(-viewArea.left, -viewArea.top)

            // If being asked for the same area as last time, nothing to do
            if (paintArea == repaintArea
                && viewHeight == viewArea.width()
                && viewHeight == viewArea.height()
            ) {
                return
            }

            // Stop the drawing of previous patch if still going
            if (repaintSyncTask != null) {
                repaintSyncTask!!.cancel(true)
                repaintSyncTask = null
            }

            // Create and add the image view if not already done
            if (repaintImageView == null) {
                val repaintImageView: ImageView = object : AppCompatImageView(listView.context) {
                    override fun isOpaque(): Boolean {
                        return true
                    }

                    public override fun onDraw(canvas: Canvas) {
                        try {
                            super.onDraw(canvas)
                        } catch (e: Exception) {
                        }
                    }
                }
                this.repaintImageView = repaintImageView

                repaintImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                repaintImageView.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                //addView(repaintImageView);
            }
            val own = this
            val repaintSyncTask = object : SafeAsyncTask<RepaintAreaInfo, Void, RepaintAreaInfo?>() {
                /**
                 * @see android.os.AsyncTask#doInBackground(Params[])
                 */
                override fun doInBackground(vararg v: RepaintAreaInfo): RepaintAreaInfo? {
                    try {
                        lib.drawPageSync(
                            v[0].bm, pageIndex,
                            v[0].viewWidth.toFloat(), v[0].viewHeight.toFloat(),
                            v[0].repaintArea.left, v[0].repaintArea.top,
                            v[0].repaintArea.width(), v[0].repaintArea.height(), 1
                        )
                        return v[0]
                    } catch (e: Exception) {
                        return null
                    }
                }

                /**
                 * @see android.os.AsyncTask#onPostExecute(Object)
                 */
                override fun onPostExecute(v: RepaintAreaInfo?) {
                    try {
                        viewWidth = v!!.viewWidth
                        viewHeight = v.viewHeight
                        repaintArea = v.repaintArea
                        // dipose
                        val repaintImageView = this@PDFPageListItem.repaintImageView!!
                        val d = repaintImageView.drawable
                        if (d is BitmapDrawable) {
                            if (d.bitmap != null) {
                                while (!lib.isDrawPageSyncFinished) {
                                    try {
                                        Thread.sleep(100)
                                    } catch (e: Exception) {
                                    }
                                }
                                d.bitmap.recycle()
                            }
                            listView.setDoRequstLayout(false)
                            repaintImageView.setImageBitmap(null)
                            repaintImageView.setImageBitmap(v.bm)
                            listView.setDoRequstLayout(true)
                        }
                        //requestLayout();
                        // Calling requestLayout here doesn't lead to a later call to layout. No idea
                        // why, but apparently others have run into the problem.
                        repaintImageView.layout(
                            repaintArea!!.left, repaintArea!!.top,
                            repaintArea!!.right, repaintArea!!.bottom
                        )
                        if (repaintImageView.parent == null) {
                            addView(repaintImageView)
                            searchView?.bringToFront()
                        }
                        invalidate()
                        if (listView != null) {
                            listView.exportImage(own, v.bm)
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            this.repaintSyncTask = repaintSyncTask

            try {
                val bitmap = Bitmap.createBitmap(paintArea.width(), paintArea.height(), Bitmap.Config.ARGB_8888)
                repaintSyncTask.safeExecute(RepaintAreaInfo(bitmap, viewArea.width(), viewArea.height(), paintArea))
            } catch (e: OutOfMemoryError) {
                if (repaintImageView != null) {
                    val d = repaintImageView!!.drawable
                    if (d is BitmapDrawable) {
                        if (d.bitmap != null) {
                            while (!lib.isDrawPageSyncFinished) {
                                try {
                                    Thread.sleep(100)
                                } catch (ee: Exception) {
                                }
                            }
                            d.bitmap.recycle()
                        }
                    }
                }
                System.gc()
                try {
                    Thread.sleep(50)
                    val bitmap = Bitmap.createBitmap(paintArea.width(), paintArea.height(), Bitmap.Config.ARGB_8888)
                    repaintSyncTask.safeExecute(RepaintAreaInfo(bitmap, viewArea.width(), viewArea.height(), paintArea))
                } catch (e2: OutOfMemoryError) {
                } catch (aa: Exception) {
                }
            }
        } else if (!mIsBlank) {
            if (isOriginalBitmapValid) {
                listView.exportImage(this, originalBitmap)
            }
        }
    }

    /**
     * remove reapint image view
     */
    override fun removeRepaintImageView() {
        // Stop the drawing of the patch if still going
        if (repaintSyncTask != null) {
            repaintSyncTask!!.cancel(true)
            repaintSyncTask = null
        }

        viewHeight = 0
        viewWidth = 0
        repaintArea = null
        repaintImageView?.setImageBitmap(null)
    }

    fun drawSerachView(canvas: Canvas) {
        searchView?.draw(canvas)
    }

    override fun dispose() {
        if (darwOriginalPageTask != null) {
            darwOriginalPageTask!!.cancel(true)
            darwOriginalPageTask = null
        }
        if (repaintSyncTask != null) {
            repaintSyncTask!!.cancel(true)
            repaintSyncTask = null
        }
        if (originalImageView != null) {
            val d = originalImageView!!.drawable
            if (d is BitmapDrawable) {
                if (d.bitmap != null) {
                    while (!lib.isDrawPageSyncFinished) {
                        try {
                            Thread.sleep(100)
                        } catch (e: Exception) {
                        }
                    }
                    d.bitmap.recycle()
                }
            }
            originalImageView!!.setImageBitmap(null)
        }

        if (repaintImageView != null) {
            val d = repaintImageView!!.drawable
            if (d is BitmapDrawable) {
                if (d.bitmap != null) {
                    while (!lib.isDrawPageSyncFinished) {
                        try {
                            Thread.sleep(100)
                        } catch (e: Exception) {
                        }
                    }
                    d.bitmap.recycle()
                }
            }
            repaintImageView!!.setImageBitmap(null)
        }
    }
}
