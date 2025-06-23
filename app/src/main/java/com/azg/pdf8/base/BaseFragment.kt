package com.azg.pdf8.base

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView
import androidx.viewbinding.ViewBinding
import com.dong.baselib.base.BaseFragment
import kotlin.math.abs

abstract class BaseFragment<VB : ViewBinding>(
    override val bindingFactory: (LayoutInflater) -> VB,
    override var isFullSc: Boolean = false
) : BaseFragment<VB>(bindingFactory, isFullSc) {
    private var yDown = 0f
    private var isMove = false

    @SuppressLint("ClickableViewAccessibility")
    fun hideKeyboardScrollView(scrollView: NestedScrollView, action: (() -> Unit)? = {}) {
        scrollView.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    yDown = motionEvent.y
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMove) {
                        hideKeyboard()
                        action?.invoke()
                    }
                    isMove = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val yMove = motionEvent.y
                    val distY: Float = yMove - yDown
                    if (abs(distY) >= 10) {
                        isMove = true
                    }
                }
            }
            false
        }
    }

}
