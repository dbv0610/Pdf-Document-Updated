/*
 * 文件名称:          IUpdateStatusListener.java
 *
 * 编译器:            android2.2
 * 时间:              下午3:06:28
 */
package com.wxiwei.office.macro

/**
 * update status listener
 */
interface UpdateStatusListener {

    /**
     * update status for UI, ex. changes in the current slide
     */
    fun updateStatus()

    /**
     * callback this method after zoom change
     */
    fun changeZoom()

    /**
     *
     */
    fun changePage()

    /**
     *
     */
    fun completeLayout()

    /**
     * @param views
     */
    fun updateViewImage(views: Array<Int?>?)

    companion object {
        //header or footer contains vector graph,so need update all pages
        const val ALLPages: Byte = -1
    }
}
