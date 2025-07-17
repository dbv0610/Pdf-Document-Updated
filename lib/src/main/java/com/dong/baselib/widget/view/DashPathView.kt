package com.dong.baselib.widget.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.dong.baselib.R

class DashPathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private var dpColor: Int = Color.GRAY
    private var dpWidth: Float = 1.2f
    private var dpSDistance: Float = 10f
    private var dpSLength: Float = 10f
    private var dpStrokeCap: Paint.Cap = Paint.Cap.ROUND
    private var dpOrientation: Int = 1 // 1: horizontal, 2: vertical
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DashPathView,
            0, 0
        ).apply {
            try {
                dpColor = getColor(R.styleable.DashPathView_dpColor, dpColor)
                dpWidth = getDimension(R.styleable.DashPathView_dpWidth, dpWidth)
                dpSDistance = getDimension(R.styleable.DashPathView_dpSDistance, dpSDistance)
                dpSLength = getDimension(R.styleable.DashPathView_dpSLength, dpSLength)

                dpStrokeCap = when (getInt(R.styleable.DashPathView_dpStrokeCap, 1)) {
                    1 -> Paint.Cap.ROUND
                    2 -> Paint.Cap.SQUARE
                    3 -> Paint.Cap.BUTT
                    else -> Paint.Cap.ROUND
                }

                dpOrientation = getInt(R.styleable.DashPathView_dpOrientation, 1)
            } finally {
                recycle()
            }
        }

        paint.apply {
            color = dpColor
            strokeWidth = dpWidth
            style = Paint.Style.STROKE
            strokeCap = dpStrokeCap
            pathEffect = DashPathEffect(floatArrayOf(dpSLength, dpSDistance), 0f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dpOrientation == 1) {
            canvas.drawLine(
                0f, height / 2f, width.toFloat(), height / 2f, paint
            )
        } else {
            canvas.drawLine(
                width / 2f, 0f, width / 2f, height.toFloat(), paint
            )
        }
    }
}
