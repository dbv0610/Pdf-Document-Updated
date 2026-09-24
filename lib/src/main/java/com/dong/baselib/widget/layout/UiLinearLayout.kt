package com.dong.baselib.widget.layout

import android.annotation.*
import android.content.*
import android.content.res.*
import android.graphics.*
import android.util.*
import android.widget.*
import androidx.core.graphics.*
import com.dong.baselib.*
import androidx.core.graphics.withClip

class UiLinearLayout @JvmOverloads constructor(
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

    init {
        context.obtainStyledAttributes(attrs, R.styleable.UiLinearLayout).apply {
            try {
                // Radius
                cornerRadius = getDimension(R.styleable.UiLinearLayout_cornerRadius, 0f)
                // Background
                isGradient = getBoolean(R.styleable.UiLinearLayout_bgIsGradient, false)
                bgGradientStart =
                    getColor(R.styleable.UiLinearLayout_bgGradientStart, Color.TRANSPARENT)
                bgGradientCenter =
                    getColor(R.styleable.UiLinearLayout_bgGradientCenter, Color.TRANSPARENT)
                bgGradientEnd =
                    getColor(R.styleable.UiLinearLayout_bgGradientEnd, Color.TRANSPARENT)
                bgColorLight = getColor(R.styleable.UiLinearLayout_bgColorLight, Color.TRANSPARENT)
                bgColorDark = getColor(R.styleable.UiLinearLayout_bgColorDark, Color.TRANSPARENT)
                bgGradientOrientation =
                    getInt(R.styleable.UiLinearLayout_bgGdOrientation, 0).toGradientOrientation()
                val bgColorAll = getColor(R.styleable.UiLinearLayout_bgColorAll,Color.TRANSPARENT)
                if(bgColorAll!= Color.TRANSPARENT){
                    bgColorLight = bgColorAll
                    bgColorDark = bgColorAll
                }
                // Stroke
                stWidth = getDimension(R.styleable.UiLinearLayout_strokeWidth, 0f)
                stColorLight = getColor(R.styleable.UiLinearLayout_stColorLight, Color.TRANSPARENT)
                stColorDark = getColor(R.styleable.UiLinearLayout_stColorDark, Color.TRANSPARENT)
                val stColorAll = getColor(R.styleable.UiLinearLayout_stColorAll, Color.TRANSPARENT)
                if (stColorAll != Color.TRANSPARENT) {
                    stColorLight = stColorAll
                    stColorDark = stColorAll
                }
                isDashed = getBoolean(R.styleable.UiLinearLayout_strokeDistance, false)
                dashSpace = getDimension(R.styleable.UiLinearLayout_distanceSpace, 10f)
                val strokeGdColors = getString(R.styleable.UiLinearLayout_strokeGradient)
                strokeGradient = strokeGdColors?.split(" ")
                    ?.mapNotNull { if (it.isValidHexColor()) Color.parseColor(it) else null }
                    ?.toIntArray()

                strokeGradientOrientation =
                    getInt(R.styleable.UiLinearLayout_strokeGdOrientation, 6).toStrokeOrientation()
            } finally {
                recycle()
            }
        }
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
        UiLinearLayout.GradientOrientation.entries.toTypedArray()
            .getOrElse(this) { GradientOrientation.LEFT_TO_RIGHT }

    private fun Int.toGradientOrientation() =
        UiLinearLayout.GradientOrientation.entries.toTypedArray()
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
