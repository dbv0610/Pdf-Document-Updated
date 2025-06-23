package com.azg.pdf8.widget

import android.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.dong.baselib.widget.fromColor

class StrokeTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val strokePaint: Paint = Paint()

    init {
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 4f
        strokePaint.color = resources.getColor(R.color.black)
        strokePaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        val textPaint = paint
        textPaint.style = Paint.Style.STROKE
        textPaint.strokeWidth = 4f
        setTextColor(fromColor("000000"))
        setText(text)
        super.onDraw(canvas)
        textPaint.style = Paint.Style.FILL
        setTextColor(fromColor("#ffffff"))
        super.onDraw(canvas)
    }
}