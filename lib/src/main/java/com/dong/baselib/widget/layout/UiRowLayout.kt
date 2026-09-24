package com.dong.baselib.widget.layout

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.graphics.withClip
import com.dong.baselib.R
import androidx.core.graphics.toColorInt

class UiRowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var cornerRadius = 0f
    private var isGradient = false
    private var bgGradientStart = Color.TRANSPARENT
    private var bgGradientCenter = Color.TRANSPARENT
    private var bgGradientEnd = Color.TRANSPARENT
    private var bgGradientOrientation = GradientOrientation.TOP_TO_BOTTOM
    private var bgColorLight = Color.TRANSPARENT
    private var bgColorDark = Color.TRANSPARENT
    // Stroke
    private var stWidth = 0f
    private var stColorLight = Color.TRANSPARENT
    private var stColorDark = Color.TRANSPARENT
    private var strokeGradient: IntArray? = null
    private var strokeGradientOrientation = GradientOrientation.LEFT_TO_RIGHT
    private var isDashed = false
    private var dashSpace = 10f
    // Drawing helpers
    private val clipPath = Path()
    private val strokeRectF = RectF()
    private var justifyContent: Int = 0

    init {
        context.obtainStyledAttributes(attrs, R.styleable.UiRowLayout).apply {
            try {
                // Radius
                cornerRadius = getDimension(R.styleable.UiRowLayout_cornerRadius, 0f)
                // Background
                isGradient = getBoolean(R.styleable.UiRowLayout_bgIsGradient, false)
                bgGradientStart =
                    getColor(R.styleable.UiRowLayout_bgGradientStart, Color.TRANSPARENT)
                bgGradientCenter =
                    getColor(R.styleable.UiRowLayout_bgGradientCenter, Color.TRANSPARENT)
                bgGradientEnd =
                    getColor(R.styleable.UiRowLayout_bgGradientEnd, Color.TRANSPARENT)
                bgColorLight = getColor(R.styleable.UiRowLayout_bgColorLight, Color.TRANSPARENT)
                bgColorDark = getColor(R.styleable.UiRowLayout_bgColorDark, Color.TRANSPARENT)
                bgGradientOrientation =
                    getInt(R.styleable.UiRowLayout_bgGdOrientation, 0).toGradientOrientation()
                val bgColorAll = getColor(R.styleable.UiRowLayout_bgColorAll, Color.TRANSPARENT)
                if (bgColorAll != Color.TRANSPARENT) {
                    bgColorLight = bgColorAll
                    bgColorDark = bgColorAll
                }
                // Stroke
                stWidth = getDimension(R.styleable.UiRowLayout_strokeWidth, 0f)
                stColorLight = getColor(R.styleable.UiRowLayout_stColorLight, Color.TRANSPARENT)
                stColorDark = getColor(R.styleable.UiRowLayout_stColorDark, Color.TRANSPARENT)
                val stColorAll = getColor(R.styleable.UiLinearLayout_stColorAll, Color.TRANSPARENT)
                if (stColorAll != Color.TRANSPARENT) {
                    stColorLight = stColorAll
                    stColorDark = stColorAll
                }
                isDashed = getBoolean(R.styleable.UiRowLayout_strokeDistance, false)
                dashSpace = getDimension(R.styleable.UiRowLayout_distanceSpace, 10f)
                val strokeGdColors = getString(R.styleable.UiRowLayout_strokeGradient)
                strokeGradient = strokeGdColors?.split(" ")
                    ?.mapNotNull { if (it.isValidHexColor()) it.toColorInt() else null }
                    ?.toIntArray()

                justifyContent = getInt(R.styleable.UiRowLayout_justifyContent, 0)

                strokeGradientOrientation =
                    getInt(R.styleable.UiRowLayout_strokeGdOrientation, 6).toStrokeOrientation()

                orientation = HORIZONTAL
                updateJustifyContent()
            } finally {
                recycle()
            }
        }
    }

    private fun updateJustifyContent() {
        gravity = when (justifyContent) {
            1 -> Gravity.CENTER_HORIZONTAL
            2 -> Gravity.END
            3 -> Gravity.START or Gravity.CENTER_VERTICAL
            4 -> Gravity.START or Gravity.CENTER_VERTICAL
            5 -> Gravity.START or Gravity.CENTER_VERTICAL
            else -> Gravity.START
        }
        requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (justifyContent in 3..5) {
            val childCount = childCount
            if (childCount > 1) {
                val totalWidth = width - paddingStart - paddingEnd
                var totalChildrenWidth = 0
                for (i in 0 until childCount) {
                    totalChildrenWidth += getChildAt(i).measuredWidth
                }
                if (justifyContent == 1) {
                    this@UiRowLayout.gravity = Gravity.CENTER
                } else {
                    val spaceBetween = when (justifyContent) {
                        3 -> (totalWidth - totalChildrenWidth) / (childCount - 1) // space-between
                        4 -> (totalWidth - totalChildrenWidth) / childCount // space-around
                        5 -> (totalWidth - totalChildrenWidth) / (childCount + 1) // space-evenly
                        else -> 0
                    }
                    var currentX = paddingStart
                    if (justifyContent == 5) currentX += spaceBetween // Adjust for space-evenly
                    for (i in 0 until childCount) {
                        val child = getChildAt(i)
                        val childWidth = child.measuredWidth
                        child.layout(currentX, child.top, currentX + childWidth, child.bottom)
                        currentX += childWidth + spaceBetween
                    }
                }
            }
        }
    }

    fun setJustifyContent(justify: Int) {
        justifyContent = justify
        updateJustifyContent()
        requestLayout()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = minOf(w / 2, h / 2, cornerRadius)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL

            if (isGradient) {
                val colors = if (bgGradientCenter != Color.TRANSPARENT) {
                    intArrayOf(bgGradientStart, bgGradientCenter, bgGradientEnd)
                } else {
                    intArrayOf(bgGradientStart, bgGradientEnd)
                }

                val (x0, y0, x1, y1) = bgGradientOrientation.toCoordinates(w, h)
                shader = LinearGradient(
                    x0, y0, x1, y1,
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )
            } else {
                color = if (isDarkMode()) bgColorDark else bgColorLight
            }
        }
        canvas.drawRoundRect(0f, 0f, w, h, radius, radius, bgPaint)
        canvas.withClip(Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), radius, radius, Path.Direction.CW)
        }) {
            canvas.save()
            canvas.restore()
            super.dispatchDraw(canvas)
        }

        if (stWidth > 0f) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stWidth
                strokeJoin = Paint.Join.ROUND

                if (isDashed) {
                    pathEffect = DashPathEffect(floatArrayOf(dashSpace, dashSpace), 0f)
                }

                if ((strokeGradient?.size ?: 0) > 1) {
                    val (x0, y0, x1, y1) = strokeGradientOrientation.toCoordinates(w, h)
                    shader = LinearGradient(
                        x0,
                        y0,
                        x1,
                        y1,
                        strokeGradient!!,
                        null,
                        Shader.TileMode.CLAMP
                    )
                } else {
                    color = if (isDarkMode()) stColorDark else stColorLight
                }
            }
            val inset = stWidth / 2
            strokeRectF.set(inset, inset, w - inset, h - inset)
            canvas.drawRoundRect(strokeRectF, radius, radius, strokePaint)
        }
    }

    private fun isDarkMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    enum class TextGradientOrientation {
        TOP_TO_BOTTOM, TR_BL, RIGHT_TO_LEFT, BR_TL,
        BOTTOM_TO_TOP, BL_TR, LEFT_TO_RIGHT, TL_BR
    }

    enum class GradientOrientation {
        TOP_TO_BOTTOM, TR_BL, RIGHT_TO_LEFT, BR_TL,
        BOTTOM_TO_TOP, BL_TR, LEFT_TO_RIGHT, TL_BR
    }

    private fun Int.toTextOrientation() = TextGradientOrientation.entries.toTypedArray()
        .getOrElse(this) { TextGradientOrientation.LEFT_TO_RIGHT }

    private fun Int.toStrokeOrientation() =
        UiRowLayout.GradientOrientation.entries.toTypedArray()
            .getOrElse(this) { GradientOrientation.LEFT_TO_RIGHT }

    private fun Int.toGradientOrientation() =
        UiRowLayout.GradientOrientation.entries.toTypedArray()
            .getOrElse(this) { GradientOrientation.TOP_TO_BOTTOM }

    private fun TextGradientOrientation.toCoordinates(w: Float, h: Float) = when (this) {
        TextGradientOrientation.TOP_TO_BOTTOM -> Quad(0f, 0f, 0f, h)
        TextGradientOrientation.BOTTOM_TO_TOP -> Quad(0f, h, 0f, 0f)
        TextGradientOrientation.LEFT_TO_RIGHT -> Quad(0f, 0f, w, 0f)
        TextGradientOrientation.RIGHT_TO_LEFT -> Quad(w, 0f, 0f, 0f)
        TextGradientOrientation.TL_BR -> Quad(0f, 0f, w, h)
        TextGradientOrientation.TR_BL -> Quad(w, 0f, 0f, h)
        TextGradientOrientation.BL_TR -> Quad(0f, h, w, 0f)
        TextGradientOrientation.BR_TL -> Quad(w, h, 0f, 0f)
    }

    private fun GradientOrientation.toCoordinates(w: Float, h: Float) = when (this) {
        GradientOrientation.TOP_TO_BOTTOM -> Quad(0f, 0f, 0f, h)
        GradientOrientation.BOTTOM_TO_TOP -> Quad(0f, h, 0f, 0f)
        GradientOrientation.LEFT_TO_RIGHT -> Quad(0f, 0f, w, 0f)
        GradientOrientation.RIGHT_TO_LEFT -> Quad(w, 0f, 0f, 0f)
        GradientOrientation.TL_BR -> Quad(0f, 0f, w, h)
        GradientOrientation.TR_BL -> Quad(w, 0f, 0f, h)
        GradientOrientation.BL_TR -> Quad(0f, h, w, 0f)
        GradientOrientation.BR_TL -> Quad(w, h, 0f, 0f)
    }

    private data class Quad(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

    private fun String.isValidHexColor(): Boolean {
        return this.matches(Regex("^#?[0-9a-fA-F]{6,8}$"))
    }

    fun cornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }

    fun backgroundLight(color: Int) {
        isGradient = false
        bgColorLight = color
        invalidate()
    }

    fun backgroundDark(color: Int) {
        isGradient = false
        bgColorDark = color
        invalidate()
    }

    fun backgroundAll(color: Int) {
        isGradient = false
        bgColorLight = color
        bgColorDark = color
        invalidate()
    }

    fun backgroundGradientStart(color: Int) {
        isGradient = true
        bgGradientStart = color
        invalidate()
    }

    fun backgroundGradientCenter(color: Int) {
        isGradient = true
        bgGradientCenter = color
        invalidate()
    }

    fun backgroundGradientEnd(color: Int) {
        isGradient = true
        bgGradientEnd = color
        invalidate()
    }

    fun backgroundOrientation(orientation: GradientOrientation) {
        isGradient = true
        bgGradientOrientation = orientation
        invalidate()
    }

    fun strokeWidth(width: Float) {
        stWidth = width
        invalidate()
    }

    fun strokeLight(color: Int) {
        strokeGradient = null
        stColorLight = color
        invalidate()
    }

    fun strokeDark(color: Int) {
        strokeGradient = null
        stColorDark = color
        invalidate()
    }

    fun strokeColor(color: Int) {
        strokeGradient = null
        stColorDark = color
        stColorLight = color
        invalidate()
    }

    fun strokeDashed(dashed: Boolean, spacing: Float = 10f) {
        isDashed = dashed
        dashSpace = spacing
        invalidate()
    }

    fun strokeGradientColors(colors: IntArray) {
        strokeGradient = colors
        invalidate()
    }

    fun strokeOrientation(orientation: GradientOrientation) {
        strokeGradientOrientation = orientation
        invalidate()
    }
}
