package com.dong.baselib.base

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
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

class PopupHelper<V : ViewBinding> private constructor(
    private val context: Context,
    private val inflateBinding: (LayoutInflater) -> V
) {
    private var onBind: (V, PopupWindow) -> Unit = { _, _ -> }

    companion object {
        @JvmStatic
        fun <V : ViewBinding> with(
            context: Context,
            inflateBinding: (LayoutInflater) -> V
        ): PopupHelper<V> {
            return PopupHelper(context, inflateBinding)
        }
    }

    fun onBind(callback: (binding: V, popup: PopupWindow) -> Unit): PopupHelper<V> {
        this.onBind = callback
        return this
    }

    fun show(anchor: View) {
        val inflater = LayoutInflater.from(context)
        val binding = inflateBinding(inflater)
        val popup = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setOnDismissListener { context.hideKeyboard() }
            binding.root.setOnClickListener { dismiss() }
        }

        onBind(binding, popup)

        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(anchor.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = binding.root.height
        anchor.post {
            val screenPos = IntArray(2)
            anchor.getLocationOnScreen(screenPos)
            val anchorY = screenPos[1]
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels
            val popupHeightGuess = popupHeight

            if (anchorY + anchor.height + popupHeightGuess < screenHeight - 4 * context.statusBarHeight) {
                popup.showAsDropDown(anchor)
            } else {
                val yOffset = anchorY - popupHeightGuess - context.statusBarHeight
                popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, 0, yOffset)
            }
        }
    }

    private val Context.statusBarHeight: Int
        get() {
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }

    private fun Context.hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow((this as? Activity)?.currentFocus?.windowToken, 0)
    }
}
