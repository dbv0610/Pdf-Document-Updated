package com.dong.baselib.base

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.viewbinding.ViewBinding
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dpToPx

class Popup<VB : ViewBinding> private constructor(
    private val context: Context,
    private val bindingInflater: (LayoutInflater) -> VB
) {
    private var width: Int = WindowManager.LayoutParams.WRAP_CONTENT
    private var height: Int = WindowManager.LayoutParams.WRAP_CONTENT

    private var locationX: Int = 0
    private var locationY: Int = 0
    private var onViewBinder: ((VB, PopupWindow) -> Unit)? = null

    private var autoClose: Boolean = false
    private var time: Long = 1500

    companion object {
        fun <VB : ViewBinding> inflater(
            bindingInflater: (LayoutInflater) -> VB,
            context: Context
        ): Popup<VB> {
            return Popup(context, bindingInflater)
        }
    }

    fun size(sizeProvider: (width: Int, height: Int) -> Unit): Popup<VB> {
        val inflater = LayoutInflater.from(context)
        val popupView = bindingInflater(inflater).root
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        sizeProvider(popupView.measuredWidth, popupView.measuredHeight)
        return this
    }

    fun autoClose(autoClose: Boolean = false, time: Long = 1500): Popup<VB> {
        this.autoClose = autoClose
        this.time = time
        return this
    }

    fun resetSize(
        newWidth: Int = WindowManager.LayoutParams.WRAP_CONTENT,
        newHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT
    ): Popup<VB> {
        width = newWidth
        height = newHeight
        return this
    }

    fun location(x: Int, y: Int): Popup<VB> {
        locationX = x
        locationY = y
        return this
    }

    fun setView(onViewBinder: (VB, PopupWindow) -> Unit): Popup<VB> {
        this.onViewBinder = onViewBinder
        return this
    }
    fun dismiss() {
        popupWindow?.dismiss()
    }

    private var popupWindow: PopupWindow? = null

    fun show() {
        val inflater = LayoutInflater.from(context)
        val popupView = bindingInflater(inflater)
        val rootView = popupView.root
        popupWindow = PopupWindow(rootView, width, height, true)
        popupWindow?.let {
            it.isOutsideTouchable = true
            it.isFocusable = true
            onViewBinder?.invoke(popupView, it)
            it.showAtLocation(rootView, Gravity.NO_GRAVITY, locationX, locationY)
//            popupView.root.s {
//                popupWindow?.dismiss()
//            }
        if (autoClose) {
            Handler(Looper.getMainLooper()).postDelayed({
                it.dismiss()
            }, time)
        }
        }

    }

    fun showAt(view: View) {
        val inflater = LayoutInflater.from(context)
        val popupView = bindingInflater(inflater)
        val rootView = popupView.root
        popupWindow = PopupWindow(rootView, width, height, true)

        popupWindow?.let {
            it.isOutsideTouchable = true
            it.isFocusable = true
            onViewBinder?.invoke(popupView, it)
            popupView.root.click {
                popupWindow?.dismiss()
            }
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val viewX = location[0]
            val viewY = location[1]

            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            rootView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val popupWidth = rootView.measuredWidth
            val popupHeight = rootView.measuredHeight

            val adjustedX = if (viewX + locationX + popupWidth > screenWidth) {
                screenWidth - popupWidth - (popupWidth / 5f).toInt() + 32.dpToPx().toInt()
            } else {
                viewX + locationX - (popupWidth / 5f).toInt() + 32.dpToPx().toInt()
            }

            val adjustedY = if (viewY + locationY + popupHeight > screenHeight) {
                screenHeight - popupHeight - 24.dpToPx().toInt()
            } else {
                viewY + locationY + view.height + 5 - 24.dpToPx().toInt()
            }

            it.showAtLocation(view, Gravity.NO_GRAVITY, adjustedX, adjustedY)

            if (autoClose) {
                Handler(Looper.getMainLooper()).postDelayed({
                    it.dismiss()
                }, time)
            }
        }
    }

}