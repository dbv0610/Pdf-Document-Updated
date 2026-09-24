package com.wxiwei.office.system

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import java.io.File

interface IMainFrame {
    companion object {
        const val ON_TOUCH: Byte = 0
        const val ON_DOWN: Byte = 1
        const val ON_SHOW_PRESS: Byte = 2
        const val ON_SINGLE_TAP_UP: Byte = 3
        const val ON_SCROLL: Byte = 4
        const val ON_LONG_PRESS: Byte = 5
        const val ON_FLING: Byte = 6
        const val ON_SINGLE_TAP_CONFIRMED: Byte = 7
        const val ON_DOUBLE_TAP: Byte = 8
        const val ON_DOUBLE_TAP_EVENT: Byte = 9
        const val ON_CLICK: Byte = 10
    }

    fun getActivity(): Activity
    fun doActionEvent(actionID: Int, obj: Any?): Boolean
    fun openFileFinish()
    fun updateToolsbarStatus()
    fun setFindBackForwardState(state: Boolean)
    fun getBottomBarHeight(): Int
    fun getTopBarHeight(): Int
    fun getAppName(): String
    fun getTemporaryDirectory(): File?
    fun onEventMethod(v: View?, e1: MotionEvent?, e2: MotionEvent?, xValue: Float, yValue: Float, eventMethodType: Byte): Boolean
    fun isDrawPageNumber(): Boolean
    fun isShowZoomingMsg(): Boolean
    fun isPopUpErrorDlg(): Boolean
    fun isShowPasswordDlg(): Boolean
    fun isShowProgressBar(): Boolean
    fun isShowFindDlg(): Boolean
    fun isShowTXTEncodeDlg(): Boolean
    fun getTXTDefaultEncode(): String?
    fun isTouchZoom(): Boolean
    fun isZoomAfterLayoutForWord(): Boolean
    fun getWordDefaultView(): Byte
    fun getLocalString(resName: String): String?
    fun changeZoom()
    fun changePage()
    fun completeLayout()
    fun error(errorCode: Int)
    fun fullScreen(fullscreen: Boolean)
    fun showProgressBar(visible: Boolean)
    fun updateViewImages(viewList: List<Int?>)
    fun isChangePage(): Boolean
    fun setWriteLog(saveLog: Boolean)
    fun isWriteLog(): Boolean
    fun setThumbnail(isThumbnail: Boolean)
    fun isThumbnail(): Boolean
    fun getViewBackground(): Any?
    fun setIgnoreOriginalSize(ignoreOriginalSize: Boolean)
    fun isIgnoreOriginalSize(): Boolean
    fun getPageListViewMovingPosition(): Byte
    fun dispose()
}

val IMainFrame.activity: Activity
    get() = getActivity()
val IMainFrame.isDrawPageNumber: Boolean
    get() = isDrawPageNumber()
val IMainFrame.isShowZoomingMsg: Boolean
    get() = isShowZoomingMsg()
val IMainFrame.isPopUpErrorDlg: Boolean
    get() = isPopUpErrorDlg()
val IMainFrame.isShowPasswordDlg: Boolean
    get() = isShowPasswordDlg()
val IMainFrame.isShowProgressBar: Boolean
    get() = isShowProgressBar()
val IMainFrame.isShowFindDlg: Boolean
    get() = isShowFindDlg()
val IMainFrame.isShowTXTEncodeDlg: Boolean
    get() = isShowTXTEncodeDlg()
val IMainFrame.isTouchZoom: Boolean
    get() = isTouchZoom()
val IMainFrame.isZoomAfterLayoutForWord: Boolean
    get() = isZoomAfterLayoutForWord()
val IMainFrame.isChangePage: Boolean
    get() = isChangePage()
val IMainFrame.isThumbnail: Boolean
    get() = isThumbnail()
val IMainFrame.viewBackground: Any?
    get() = getViewBackground()
val IMainFrame.isIgnoreOriginalSize: Boolean
    get() = isIgnoreOriginalSize()
val IMainFrame.pageListViewMovingPosition: Byte
    get() = getPageListViewMovingPosition()
