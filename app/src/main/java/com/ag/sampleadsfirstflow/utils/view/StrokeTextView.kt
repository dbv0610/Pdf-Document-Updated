package com.ag.sampleadsfirstflow.utils.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.ag.sampleadsfirstflow.R

class StrokeTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val strokePaint: Paint = Paint()

    init {
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 4f // Độ dày của viền
        strokePaint.color = resources.getColor(android.R.color.black) // Màu của viền
        strokePaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        val textPaint = paint

        // Vẽ viền
        textPaint.style = Paint.Style.STROKE
        textPaint.strokeWidth = 4f // Độ dày của viền
        setTextColor(ContextCompat.getColor(context, R.color.black)) // Màu của viền
        setText(text)
        super.onDraw(canvas)

        // Vẽ text
        textPaint.style = Paint.Style.FILL
        setTextColor(ContextCompat.getColor(context, R.color.white)) // Màu của text
        super.onDraw(canvas)
    }
}
