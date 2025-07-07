package com.azg.pdf8.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.azg.pdf8.R

class PdfHighlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.ruler_style
) : View(context, attrs, defStyleAttr){
    private var highlightRects = mutableListOf<RectF>()
    private val highlightPaint = Paint().apply {
        color = Color.YELLOW
        alpha = 100
        style = Paint.Style.FILL
    }

    fun setHighlightRects(rects: List<RectF>) {
        this.highlightRects = rects.toMutableList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        highlightRects.forEach { rect ->
            canvas.drawRect(rect, highlightPaint)
        }
    }
}