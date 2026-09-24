/*
 * 文件名称:          PDFFind.java
 *
 * 编译器:            android2.2
 * 时间:              下午7:38:43
 */
package com.wxiwei.office.pdf

import com.wxiwei.office.system.*

import android.app.ProgressDialog
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.widget.Toast
import com.wxiwei.office.constant.PDFConstant
import com.wxiwei.office.macro.DialogListener
import com.wxiwei.office.simpletext.control.SafeAsyncTask
import com.wxiwei.office.system.IFind
import com.wxiwei.office.system.beans.pagelist.APageListItem

/**
 * find content
 */
class PDFFind(pdfView: PDFView) : IFind {

    @JvmField
    protected var paint: Paint = Paint()

    //toast
    protected var toast: Toast? = Toast.makeText(pdfView.context, "", Toast.LENGTH_SHORT)

    //
    private var isSetPointToVisible = false

    //
    private var isStartSearch = false

    //
    private var isCancel = false

    //
    private var pageIndex = 0

    //
    private var query: String? = null

    //
    private var pdfView: PDFView? = pdfView

    //
    private var searchResult: Array<RectF>? = null

    //
    private var safeSearchTask: SafeAsyncTask<Void?, Int, Array<RectF>?>? = null

    init {
        paint.color = PDFConstant.HIGHLIGHT_COLOR
    }

    override fun find(value: String?): Boolean {
        if (value == null) {
            return false
        }
        isStartSearch = true
        this.query = value
        pageIndex = pdfView!!.getCurrentPageNumber() - 1
        search(1)
        return true
    }

    override fun findBackward(): Boolean {
        if (query == null) {
            return false
        }
        isStartSearch = false
        if (pageIndex == 0) {
            toast!!.setText(pdfView!!.getControl()!!.mainFrame.getLocalString("DIALOG_FIND_TO_BEGIN"))
            toast!!.show()
            return false
        }
        pageIndex--
        search(-1)
        return true
    }

    override fun findForward(): Boolean {
        if (query == null) {
            return false
        }
        isStartSearch = false
        if (pageIndex + 1 >= pdfView!!.getPageCount()) {
            toast!!.setText(pdfView!!.getControl()!!.mainFrame.getLocalString("DIALOG_FIND_TO_END"))
            toast!!.show()
            return false
        }
        pageIndex++
        search(1)
        return true
    }

    /**
     * search of assign content
     *
     * @param direction    = 1  forward
     *                     = -1  backward
     */
    private fun search(direction: Int) {
        if (safeSearchTask != null) {
            safeSearchTask!!.cancel(true)
            safeSearchTask = null
        }
        isSetPointToVisible = false
        searchResult = null
        isCancel = false
        val searchCount = if (direction > 0) pdfView!!.getPageCount() - pageIndex else pageIndex
        val showFindDlg = pdfView!!.getControl()!!.mainFrame.isShowFindDlg

        val progressDialog = ProgressDialog(pdfView!!.getControl()!!.activity)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setCancelable(false)
        progressDialog.setTitle(pdfView!!.getControl()!!.mainFrame.getLocalString("DIALOG_PDF_SEARCHING"))
        progressDialog.max = searchCount
        progressDialog.setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                isCancel = true
                if (safeSearchTask != null) {
                    safeSearchTask!!.cancel(true)
                    safeSearchTask = null
                }
            }
            true
        })

        safeSearchTask = object : SafeAsyncTask<Void?, Int, Array<RectF>?>() {

            override fun doInBackground(vararg params: Void?): Array<RectF>? {
                try {
                    var index = 1
                    while (pageIndex in 0 until pdfView!!.getPageCount() && !isCancelled()) {
                        publishProgress(index++)
                        val searchHits = pdfView!!.getPDFLib().searchContentSync(pageIndex, query)
                        if (searchHits != null && searchHits.isNotEmpty()) {
                            return searchHits
                        }
                        pageIndex += direction
                    }
                } catch (e: Exception) {
                    return null
                }
                return null
            }

            override fun onCancelled() {
                super.onCancelled()
                if (showFindDlg) {
                    progressDialog.cancel()
                } else {
                    val dlgListener = pdfView!!.getControl()!!.customDialog
                    dlgListener?.dismissDialog(DialogListener.DIALOGTYPE_FIND)
                }
            }

            override fun onProgressUpdate(vararg values: Int?) {
                super.onProgressUpdate(*values)
                if (showFindDlg) {
                    progressDialog.progress = values[0]!!
                }
            }

            override fun onPreExecute() {
                super.onPreExecute()
                if (showFindDlg) {
                    pdfView!!.postDelayed({
                        if (!isCancel) {
                            progressDialog.show()
                            progressDialog.progress = 1
                        }
                    }, 0)
                } else {
                    val dlgListener = pdfView!!.getControl()!!.customDialog
                    dlgListener?.showDialog(DialogListener.DIALOGTYPE_FIND)
                }
            }

            override fun onPostExecute(result: Array<RectF>?) {
                if (showFindDlg) {
                    progressDialog.cancel()
                } else {
                    val dlgListener = pdfView!!.getControl()!!.customDialog
                    dlgListener?.dismissDialog(DialogListener.DIALOGTYPE_LOADING)
                }
                if (result != null) {
                    searchResult = result
                    // Ask the ReaderView to move to the resulting page
                    if (pdfView!!.getCurrentPageNumber() - 1 != pageIndex) {
                        pdfView!!.getListView().showPDFPageForIndex(pageIndex)
                        isSetPointToVisible = true
                    } else {
                        if (pdfView!!.getListView().isPointVisibleOnScreen(result[0].left.toInt(), result[0].top.toInt())) {
                            pdfView!!.invalidate()
                        } else {
                            pdfView!!.getListView().setItemPointVisibleOnScreen(result[0].left.toInt(), result[0].top.toInt())
                        }
                    }
                } else if (showFindDlg) {
                    var str: String? = ""
                    if (isStartSearch) {
                        pdfView!!.getControl()!!.mainFrame.setFindBackForwardState(false)
                        str = pdfView!!.getControl()!!.mainFrame.getLocalString("DIALOG_FIND_NOT_FOUND")
                    } else if (direction > 0) {
                        str = pdfView!!.getControl()!!.mainFrame.getLocalString("DIALOG_FIND_TO_END")
                    } else if (direction < 0) {
                        str = pdfView!!.getControl()!!.mainFrame.getLocalString("DIALOG_FIND_TO_BEGIN")
                    }
                    if (str != null && str.isNotEmpty()) {
                        toast!!.setText(str)
                        toast!!.show()
                    }
                }
            }
        }
        safeSearchTask!!.safeExecute(null)
    }

    fun drawHighlight(canvas: Canvas, dx: Int, dy: Int, item: APageListItem) {
        if (this.pageIndex == item.pageIndex) {
            val scale = item.width / item.pageWidth.toFloat()
            val searchResult = this.searchResult
            if (searchResult != null && searchResult.isNotEmpty()) {
                for (rect in searchResult) {
                    canvas.drawRect(
                        rect.left * scale + dx * scale, rect.top * scale + dy * scale,
                        rect.right * scale + dx * scale, rect.bottom * scale + dy * scale, paint
                    )
                }
            }
        }
    }

    fun getSearchResult(): Array<RectF>? {
        return this.searchResult
    }

    override fun resetSearchResult() {
        this.searchResult = null
    }

    override fun getPageIndex(): Int {
        return this.pageIndex
    }

    /**
     * @return Returns the isSetPointToVisible.
     */
    fun isSetPointToVisible(): Boolean {
        return isSetPointToVisible
    }

    /**
     * @param isSetPointToVisible The isSetPointToVisible to set.
     */
    fun setSetPointToVisible(isSetPointToVisible: Boolean) {
        this.isSetPointToVisible = isSetPointToVisible
    }

    override fun dispose() {
        pdfView = null
        toast = null
    }
}
