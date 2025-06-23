package com.ag.sampleadsfirstflow.utils.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import com.ag.sampleadsfirstflow.R

class IndicatorView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var indicatorCount = 5
    private var indicatorSpacing = 20f
    private var indicatorRadius = 20f
    private var indicatorWidth = 0f
    private var indicatorActive = 0
    private var indicatorScale = 1
    private var indicatorPoint = mutableListOf<Float>()
    private var paintDefault = Paint()
    private var paintActive = Paint()

    init {
        val typedArray =
            getContext().theme.obtainStyledAttributes(attrs, R.styleable.IndicatorView, 0, 0)
        try {
            indicatorCount = typedArray.getInteger(R.styleable.IndicatorView_max_count, indicatorCount)
            indicatorActive =
                typedArray.getInteger(R.styleable.IndicatorView_active_count, indicatorActive)
            indicatorSpacing =
                typedArray.getDimension(R.styleable.IndicatorView_spacing, indicatorSpacing)
            indicatorRadius = typedArray.getDimension(R.styleable.IndicatorView_radius, indicatorRadius)
            indicatorScale =
                typedArray.getInteger(R.styleable.IndicatorView_scale_value, indicatorScale)
            paintDefault.color = typedArray.getColor(
                R.styleable.IndicatorView_default_color,
                ContextCompat.getColor(getContext(), R.color.color_indicator_default)
            )
            paintActive.color = typedArray.getColor(
                R.styleable.IndicatorView_active_color,
                ContextCompat.getColor(getContext(), R.color.color_indicator_active)
            )
        } finally {
            typedArray.recycle()
        }

        val viewTreeObserver = viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (width > 0 && height > 0) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this)
                        indicatorWidth = height.toFloat()
                        if (indicatorCount > 1) {
                            indicatorPoint.add(0f)
                            for (i in 1 until indicatorCount) {
                                indicatorPoint.add((indicatorSpacing + indicatorWidth) * i)
                            }
                        }
                        if (resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL) {
                            indicatorPoint.reverse()
                        }
                    }
                }
            })
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (indicatorCount > 0 && indicatorPoint.size > 0) {
            for (i in 0 until indicatorCount) {
                val paint = if (i == indicatorActive * indicatorScale) {
                    paintActive
                } else {
                    paintDefault
                }
                canvas.drawCircle(
                    indicatorPoint[i] + indicatorWidth / 2,
                    height.toFloat() / 2,
                    indicatorWidth / 2,
                    paint
                )
            }
        }
    }

    fun setIndicatorMaxCount(count: Int) {
        this.indicatorCount = count
        invalidate()
    }

    fun setIndicatorActive(number: Int) {
        this.indicatorActive = number
        invalidate()
    }

    fun setIndicatorDefaultColor(color: Int) {
        this.paintDefault.color = color
        invalidate()
    }

    fun setIndicatorActiveColor(color: Int) {
        this.paintActive.color = color
        invalidate()
    }

    fun setIndicatorRadius(radius: Float) {
        this.indicatorRadius = radius
        invalidate()
    }

    fun setIndicatorScale(scale: Int) {
        this.indicatorScale = scale
        invalidate()
    }

    fun setIndicatorSpacing(spacing: Float) {
        this.indicatorSpacing = spacing
        invalidate()
    }
}
