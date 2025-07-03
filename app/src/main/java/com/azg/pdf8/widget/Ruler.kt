package com.azg.pdf8.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import com.azg.pdf8.R
import com.dong.baselib.canvas.drawTriangle
import com.dong.baselib.widget.dpToPx
import com.dong.baselib.widget.fromColor
import com.dong.baselib.widget.red
import com.dong.baselib.widget.screenWidth
import com.dong.baselib.widget.white
import kotlin.math.roundToInt

class ScrollRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.ruler_style
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    val rulerView = RulerDrawView(context)
    val triangleH = 10.dpToPx()
    private var hasScrolledOnce = false

    init {
        clipToPadding = false
        setPadding(0, 0, 0, 0)
        addView(rulerView)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!hasScrolledOnce) {
            hasScrolledOnce = true
            val midCm = rulerView.maxCm / 2f
            val midPx = (midCm * rulerView.oneCmPx).roundToInt()
            post { scrollTo(midPx, 0) }
        }
    }

    fun getCurrentCm(): Float {
        val initialOffset = paddingLeft + (context.screenWidth / 2f)
        val centerXInContent = scrollX + width / 2f - initialOffset
        return centerXInContent / rulerView.oneCmPx
    }

    private var onValueChange: ((Float) -> Unit)? = null

    fun setOnRulerChangeListener(listener: (cm: Float) -> Unit) {
        onValueChange = listener
        listener(getCurrentCm())
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val currentCm = getCurrentCm()
        onValueChange?.invoke(currentCm)
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        val fixedX = scrollX + width / 2f
        val bottomY = height.toFloat()
        val topY = bottomY - triangleH
        val triPath = Path().apply {
            reset()
            moveTo(fixedX, topY)
            lineTo(fixedX + triangleH * 1.25f / 2f, bottomY)
            lineTo(fixedX - triangleH * 1.25f / 2f, bottomY)
            close()
        }
        canvas.drawPath(triPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = red
            style = Paint.Style.FILL
        })
    }
}

class RulerDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.ruler_style
) : View(context, attrs, defStyleAttr) {
    var maxCm = 360
        set(value) {
            field = value
            invalidate()
        }
    private val oneInchPx = resources.displayMetrics.xdpi
    val oneCmPx = oneInchPx / 2.54f
    private val largeTickH = 20f.dpToPx(context)
    private val medTickH = 16f.dpToPx(context)
    private val smallTickH = 12f.dpToPx(context)
    private val baselineOff = 8f.dpToPx(context)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1.5f.dpToPx(context)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f.dpToPx(context)
        textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredH =
            (largeTickH + textPaint.textSize + baselineOff + paddingTop + paddingBottom).toInt()
        val h = resolveSize(desiredH, heightMeasureSpec)
        val contentW = (oneCmPx * maxCm + paddingLeft + paddingRight).toInt()
        val w = resolveSize(contentW, widthMeasureSpec)
        setMeasuredDimension(w + context.screenWidth, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft.toFloat() + context.screenWidth / 2f
        for (i in 0..maxCm) {
            val x = left + i * oneCmPx
            canvas.drawLine(
                x,
                0f,
                x, largeTickH, tickPaint
            )
            if (i < maxCm) {
                for (mm in 1 until 10) {
                    val xx = x + mm * oneCmPx / 10f
                    val height = if (mm % 5 == 0) medTickH else smallTickH
                    canvas.drawLine(
                        xx,
                        0f,
                        xx,
                        height,
                        tickPaint
                    )
                }
            }
        }
    }
}

private fun Float.dpToPx(ctx: Context): Float =
    this * ctx.resources.displayMetrics.density