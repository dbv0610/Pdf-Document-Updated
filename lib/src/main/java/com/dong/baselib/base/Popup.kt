package com.dong.baselib.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.viewbinding.ViewBinding
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Popup<VB : ViewBinding> private constructor(
    private val context: Context,
    private val bindingInflater: (LayoutInflater) -> VB
) {
    private var width = ViewGroup.LayoutParams.WRAP_CONTENT
    private var height = ViewGroup.LayoutParams.WRAP_CONTENT
    private var offsetX = 0
    private var offsetY = 0
    private var dimBehind = false
    private var outsideTouchable = true
    private var focusable = true
    private var autoCloseMillis = 0L
    private var onViewBinder: ((VB, PopupWindow) -> Unit)? = null
    private var popupWindow: PopupWindow? = null

    companion object {
        @JvmStatic
        fun <VB : ViewBinding> with(
            context: Context,
            bindingInflater: (LayoutInflater) -> VB
        ) = Popup(context, bindingInflater)
    }

    fun size(w: Int, h: Int) = apply { width = w; height = h }
    fun offset(x: Int = 0, y: Int = 0) = apply { offsetX = x; offsetY = y }
    fun dimBehind(enable: Boolean = true) = apply { dimBehind = enable }
    fun outsideTouchable(enable: Boolean) = apply { outsideTouchable = enable }
    fun focusable(enable: Boolean) = apply { focusable = enable }
    fun autoCloseAfter(millis: Long) = apply { autoCloseMillis = millis }
    fun bindView(callback: (VB, PopupWindow) -> Unit) = apply { onViewBinder = callback }
    fun dismiss() = popupWindow?.dismiss()

    fun show(anchor: View) {
        val binding = bindingInflater(LayoutInflater.from(context))
        popupWindow = PopupWindow(binding.root, width, height, focusable).apply {
            isOutsideTouchable = outsideTouchable
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        if (dimBehind) {
            val container = popupWindow!!.contentView.rootView
            val lp = (container.layoutParams as WindowManager.LayoutParams)
                .apply { flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND; dimAmount = 0.5f }
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .updateViewLayout(container, lp)
        }
        onViewBinder?.invoke(binding, popupWindow!!)
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(anchor.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupH = binding.root.measuredHeight
        val loc = IntArray(2).also { anchor.getLocationOnScreen(it) }
        val screenH = context.resources.displayMetrics.heightPixels
        val showBelow = loc[1] + anchor.height + popupH + offsetY <= screenH
        val x = loc[0] + offsetX
        val y = if (showBelow) loc[1] + anchor.height + offsetY else loc[1] - popupH - offsetY
        popupWindow!!.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        if (autoCloseMillis > 0) CoroutineScope(Dispatchers.Main).launch {
            delay(autoCloseMillis); popupWindow?.dismiss()
        }
    }
}


class PopupDataHelper<V : ViewBinding, T> private constructor(
    private val context: Context,
    private val inflateBinding: (LayoutInflater) -> V
) {
    private var onBindData: (V, PopupWindow, T?) -> Unit = { _, _, _ -> }

    companion object {
        @JvmStatic
        fun <V : ViewBinding, T> with(
            context: Context,
            inflateBinding: (LayoutInflater) -> V
        ): PopupDataHelper<V, T> {
            return PopupDataHelper(context, inflateBinding)
        }
    }

    fun onBindData(callback: (binding: V, popup: PopupWindow, data: T?) -> Unit): PopupDataHelper<V, T> {
        this.onBindData = callback
        return this
    }

    fun show(anchor: View, data: T? = null) {
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
        onBindData(binding, popup, data)
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(anchor.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = binding.root.height
        anchor.post {
            val screenPos = IntArray(2).also { anchor.getLocationOnScreen(it) }
            val anchorY = screenPos[1]

            val dm = Resources.getSystem().displayMetrics
            val screenHeight = dm.heightPixels

            val sbh = context.statusBarHeight
            val nbh = context.navigationBarHeight

            val availableBelow = screenHeight - sbh - nbh - anchorY - anchor.height

            if (availableBelow >= popupHeight) {
                popup.showAsDropDown(anchor)
            } else {
                val yOffset = anchorY - popupHeight - sbh
                popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, 0, yOffset)
            }
        }
    }

    // extend your class with nav-bar inset:
    private val Context.navigationBarHeight: Int
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        get() {
            val resId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }

    private val Context.statusBarHeight: Int
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        get() {
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }

    private fun Context.hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow((this as? Activity)?.currentFocus?.windowToken, 0)
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
            val screenPos = IntArray(2).also { anchor.getLocationOnScreen(it) }
            val anchorY = screenPos[1]

            val dm = Resources.getSystem().displayMetrics
            val screenHeight = dm.heightPixels

            val sbh = context.statusBarHeight
            val nbh = context.navigationBarHeight

            val availableBelow = screenHeight - sbh - nbh - anchorY - anchor.height

            if (availableBelow >= popupHeight) {
                popup.showAsDropDown(anchor)
            } else {
                val yOffset = anchorY - popupHeight - sbh
                popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, 0, yOffset)
            }
        }
    }
    private val Context.navigationBarHeight: Int
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        get() {
            val resId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }


    private val Context.statusBarHeight: Int
        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        get() {
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }

    private fun Context.hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow((this as? Activity)?.currentFocus?.windowToken, 0)
    }

}
