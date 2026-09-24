/*
 * 文件名称:          PDFControl.java
 *
 * 编译器:            android2.2
 * 时间:              下午5:45:18
 */
package com.wxiwei.office.pdf

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import com.wxiwei.office.common.ICustomDialog
import com.wxiwei.office.common.IOfficeToPicture
import com.wxiwei.office.constant.EventConstant
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.java.awt.Rectangle
import com.wxiwei.office.fc.pdf.PDFLib
import com.wxiwei.office.system.AbstractControl
import com.wxiwei.office.system.IControl
import com.wxiwei.office.system.IFind
import com.wxiwei.office.system.IMainFrame
import com.wxiwei.office.system.SysKit
import com.wxiwei.office.system.*

/**
 * PDF application control
 */
class PDFControl(mainControl: IControl, pdfLib: PDFLib, filePath: String?) : AbstractControl() {

    private var isDispose = false

    //
    private var mainControl: IControl? = mainControl

    //
    private var pdfView: PDFView? =
        PDFView(mainControl.mainFrame.activity.applicationContext, pdfLib, this)

    override fun canBackLayout(): Boolean {
        return false
    }

    override fun setLayoutThreadDied(isDied: Boolean) {
    }

    override fun setStopDraw(isStopDraw: Boolean) {
    }

    /**
     * action派发
     *
     * @param actionID 动作ID
     * @param obj 动作ID的Value
     */
    override fun actionEvent(actionID: Int, obj: Any?) {
        when (actionID) {
            EventConstant.PDF_SHOW_PAGE -> {
                val pn = obj as Int
                if (pn >= 0 && pn < pdfView!!.getPageCount()) {
                    pdfView!!.showPDFPageForIndex(pn)
                }
            }

            EventConstant.APP_PASSWORD_OK_INIT -> pdfView!!.post {
                if (!isDispose) {
                    pdfView!!.passwordVerified()
                }
            }

            EventConstant.SYS_SET_PROGRESS_BAR_ID -> pdfView!!.post {
                if (!isDispose) {
                    //getActivity().setProgressBarIndeterminateVisibility((Boolean)obj);
                    mainControl!!.mainFrame.showProgressBar(obj as Boolean)
                }
            }

            EventConstant.SYS_INIT_ID -> pdfView!!.init()

            EventConstant.APP_ZOOM_ID -> {
                val params = obj as IntArray
                pdfView!!.setZoom(params[0] / MainConstant.STANDARD_RATE.toFloat(), params[1], params[2])
            }

            // 更新toolsbar button状态
            EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS -> pdfView!!.post {
                if (!isDispose) {
                    getMainFrame().updateToolsbarStatus()
                }
            }

            // 布局完成
            EventConstant.SYS_AUTO_TEST_FINISH_ID -> if (isAutoTest()) {
                getMainFrame().getActivity().onBackPressed()
            }

            EventConstant.APP_GENERATED_PICTURE_ID -> {
                //pdfView.createPicture();
            }

            EventConstant.APP_PAGE_UP_ID -> pdfView!!.previousPageview()

            EventConstant.APP_PAGE_DOWN_ID -> pdfView!!.nextPageView()

            EventConstant.APP_SET_FIT_SIZE_ID -> pdfView!!.setFitSize(obj as Int)

            EventConstant.APP_INIT_CALLOUTVIEW_ID -> pdfView!!.getListView().currentPageView.initCalloutView()

            else -> {}
        }
    }

    /**
     * 得到action的状态
     *
     * @return obj
     */
    override fun getActionValue(actionID: Int, obj: Any?): Any? {
        when (actionID) {
            EventConstant.APP_ZOOM_ID -> return pdfView!!.getZoom()

            EventConstant.APP_FIT_ZOOM_ID -> return pdfView!!.getFitZoom()

            EventConstant.APP_COUNT_PAGES_ID -> return pdfView!!.getPageCount()

            EventConstant.APP_CURRENT_PAGE_NUMBER_ID -> return pdfView!!.getCurrentPageNumber()

            EventConstant.PDF_PAGE_TO_IMAGE -> return pdfView!!.pageToImage(obj as Int)

            EventConstant.APP_PAGEAREA_TO_IMAGE -> {
                if (obj is IntArray && obj.size == 7) {
                    return pdfView!!.pageAreaToImage(obj[0], obj[1], obj[2], obj[3], obj[4], obj[5], obj[6])
                }
            }

            EventConstant.PDF_GET_PAGE_SIZE -> {
                val rect: Rect? = pdfView!!.getPageSize(obj as Int - 1)
                if (rect != null) {
                    return Rectangle(0, 0, rect.width(), rect.height())
                }
                return null
            }

            EventConstant.APP_THUMBNAIL_ID -> {
                if (obj is IntArray) {
                    if (obj.size < 2 || obj[1] <= 0) {
                        return null
                    }
                    return pdfView!!.getThumbnail(obj[0], obj[1] / MainConstant.STANDARD_RATE.toFloat())
                }
            }

            EventConstant.APP_AUTHENTICATE_PASSWORD -> {
                if (pdfView != null && obj != null) {
                    return pdfView!!.getPDFLib().authenticatePasswordSync(obj as String)
                }
            }

            EventConstant.APP_GET_HYPERLINK_URL_ID -> {
                if (pdfView != null && obj != null) {
                    val pageIndex = obj as Int - 1
                    if (pageIndex >= 0 && pageIndex < pdfView!!.getPDFLib().pageCountSync) {
                        val infos = pdfView!!.getPDFLib().getHyperlinkInfoSync(pageIndex)
                        if (infos != null && infos.isNotEmpty()) {
                            val s = ArrayList<String>()
                            for (info in infos) {
                                if (info != null) {
                                    s.add(info.url)
                                }
                            }
                            return if (s.size > 0) {
                                s.toTypedArray()
                            } else {
                                null
                            }
                        }
                    }
                    return null
                }
            }

            EventConstant.APP_GET_FIT_SIZE_STATE_ID -> {
                if (pdfView != null) {
                    return pdfView!!.getFitSizeState()
                }
            }

            EventConstant.APP_GET_SNAPSHOT_ID -> {
                if (pdfView != null) {
                    return pdfView!!.getSanpshot(obj as Bitmap?)
                }
            }

            else -> {}
        }
        return null
    }

    override fun getView(): View {
        return pdfView!!
    }

    override fun getMainFrame(): IMainFrame {
        return mainControl!!.getMainFrame()
    }

    override fun getActivity(): Activity {
        return getMainFrame().getActivity()
    }

    override fun getFind(): IFind {
        return pdfView!!.getFind()
    }

    override fun isAutoTest(): Boolean {
        return mainControl!!.isAutoTest()
    }

    override fun getOfficeToPicture(): IOfficeToPicture? {
        return mainControl!!.getOfficeToPicture()
    }

    override fun getCustomDialog(): ICustomDialog? {
        return mainControl!!.getCustomDialog()
    }

    override fun getApplicationType(): Byte {
        return MainConstant.APPLICATION_TYPE_PDF
    }

    override fun getSysKit(): SysKit {
        return mainControl!!.getSysKit()
    }

    override fun isEndFile(): Boolean {
        return false
    }

    override fun dispose() {
        isDispose = true
        pdfView!!.dispose()
        pdfView = null
        mainControl = null
    }
}
