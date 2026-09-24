/*
 * 文件名称:          PatchInfo.java
 *
 * 编译器:            android2.2
 * 时间:              下午9:45:39
 */
package com.wxiwei.office.pdf

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * @param bm            repaint bitmap instance
 * @param viewWidth     the width of repaint component
 * @param viewHeight    the height of repaint component
 * @param repaintArea   repaint area
 */
class RepaintAreaInfo(
    @JvmField var bm: Bitmap,
    @JvmField var viewWidth: Int,
    @JvmField var viewHeight: Int,
    @JvmField var repaintArea: Rect
)
