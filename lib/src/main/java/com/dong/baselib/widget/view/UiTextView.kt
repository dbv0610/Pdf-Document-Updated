package com.dong.baselib.widget.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.dong.baselib.R
import com.dong.baselib.widget.GradientOrientation
import com.dong.baselib.widget.fromColor
import com.dong.baselib.widget.isValidHexColor
import kotlin.math.min
import androidx.core.graphics.withClip
import com.dong.baselib.widget.layout.UiRowLayout

enum class TextGradientOrientation {
    TOP_TO_BOTTOM, TR_BL, RIGHT_TO_LEFT, BR_TL,
    BOTTOM_TO_TOP, BL_TR, LEFT_TO_RIGHT, TL_BR
}

fun TextView.setGradient(
    startColor: Int,
    endColor: Int,
    orientation: TextGradientOrientation = TextGradientOrientation.LEFT_TO_RIGHT
) {
    val width = paint.measureText(text.toString())
    val height = textSize * 1.3f
    var x0 = 0f
    var y0 = 0f
    var x1 = 0f
    var y1 = 0f

    when (orientation) {
        TextGradientOrientation.TOP_TO_BOTTOM -> Unit.apply {
            x0 = 0f; y0 = 0f; x1 = 0f; y1 = height
        }
        TextGradientOrientation.BOTTOM_TO_TOP -> Unit.apply {
            x0 = 0f; y0 = height; x1 = 0f; y1 = 0f
        }
        TextGradientOrientation.LEFT_TO_RIGHT -> Unit.apply {
            x0 = 0f; y0 = 0f; x1 = width; y1 = 0f
        }
        TextGradientOrientation.RIGHT_TO_LEFT -> Unit.apply {
            x0 = width; y0 = 0f; x1 = 0f; y1 = 0f
        }
        TextGradientOrientation.TL_BR -> Unit.apply {
            x0 = 0f; y0 = 0f; x1 = width; y1 = height
        }
        TextGradientOrientation.TR_BL -> Unit.apply {
            x0 = width; y0 = 0f; x1 = 0f; y1 = height
        }
        TextGradientOrientation.BL_TR -> Unit.apply {
            x0 = 0f; y0 = height; x1 = width; y1 = 0f
        }
        TextGradientOrientation.BR_TL -> Unit.apply {
            x0 = width; y0 = height; x1 = 0f; y1 = 0f
        }
    }
    val gradient = LinearGradient(
        x0,
        y0,
        x1,
        y1,
        intArrayOf(startColor, endColor),
        null,
        Shader.TileMode.CLAMP
    )
    paint.shader = gradient
    invalidate()
}

class UiTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    // Background
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
    // Text
    private var underlineText = false
    private var tColorLight = currentTextColor
    private var tColorDark = currentTextColor
    private var tColorHint = currentHintTextColor
    private var textGradient = false
    private var textGradientStart = Color.TRANSPARENT
    private var textGradientEnd = Color.TRANSPARENT
    private var textGradientOrientation = TextGradientOrientation.LEFT_TO_RIGHT
    // Drawing helpers
    private val clipPath = Path()
    private val strokeRectF = RectF()

    init {
        context.obtainStyledAttributes(attrs, R.styleable.UiTextView).apply {
            try {
                // Radius
                cornerRadius = getDimension(R.styleable.UiTextView_cornerRadius, 0f)
                // Background
                isGradient = getBoolean(R.styleable.UiTextView_bgIsGradient, false)
                bgGradientStart =
                    getColor(R.styleable.UiTextView_bgGradientStart, Color.TRANSPARENT)
                bgGradientCenter =
                    getColor(R.styleable.UiTextView_bgGradientCenter, Color.TRANSPARENT)
                bgGradientEnd = getColor(R.styleable.UiTextView_bgGradientEnd, Color.TRANSPARENT)
                bgColorLight = getColor(R.styleable.UiTextView_bgColorLight, Color.TRANSPARENT)
                bgColorDark = getColor(R.styleable.UiTextView_bgColorDark, Color.TRANSPARENT)
                bgGradientOrientation =
                    getInt(R.styleable.UiTextView_bgGdOrientation, 0).toGradientOrientation()
                val bgColorAll = getColor(R.styleable.UiTextView_bgColorAll, Color.TRANSPARENT)
                if (bgColorAll != Color.TRANSPARENT) {
                    bgColorLight = bgColorAll
                    bgColorDark = bgColorAll
                }
                // Stroke
                stWidth = getDimension(R.styleable.UiTextView_strokeWidth, 0f)
                stColorLight = getColor(R.styleable.UiTextView_stColorLight, Color.TRANSPARENT)
                stColorDark = getColor(R.styleable.UiTextView_stColorDark, Color.TRANSPARENT)
                val stColorAll = getColor(R.styleable.UiTextView_stColorAll, Color.TRANSPARENT)
                if (stColorAll != Color.TRANSPARENT) {
                    stColorLight = stColorAll
                    stColorDark = stColorAll
                }
                isDashed = getBoolean(R.styleable.UiTextView_strokeDistance, false)
                dashSpace = getDimension(R.styleable.UiTextView_distanceSpace, 10f)
                val strokeGdColors = getString(R.styleable.UiTextView_strokeGradient)
                strokeGradient = strokeGdColors?.split(" ")
                    ?.mapNotNull { if (it.isValidHexColor()) Color.parseColor(it) else null }
                    ?.toIntArray()

                strokeGradientOrientation =
                    getInt(R.styleable.UiTextView_strokeGdOrientation, 6).toStrokeOrientation()

                tColorLight = getColor(R.styleable.UiTextView_tvColorLight, currentTextColor)
                tColorDark = getColor(R.styleable.UiTextView_tvColorDark, currentTextColor)
                tColorHint = getColor(R.styleable.UiTextView_tvColorHint, currentHintTextColor)
                val tvColor = getColor(R.styleable.UiTextView_tvColor, Color.TRANSPARENT)
                if (tvColor != Color.TRANSPARENT) {
                    tColorLight = tvColor
                    tColorDark = tvColor
                }

                underlineText = getBoolean(R.styleable.UiTextView_underLine, false)
                textGradient = getBoolean(R.styleable.UiTextView_textGradient, false)
                textGradientStart =
                    getColor(R.styleable.UiTextView_textGradientStart, Color.TRANSPARENT)
                textGradientEnd =
                    getColor(R.styleable.UiTextView_textGradientEnd, Color.TRANSPARENT)
                textGradientOrientation =
                    getInt(R.styleable.UiTextView_textGdOrientation, 6).toTextOrientation()
            } finally {
                recycle()
            }
        }

        applyTextStyles()
    }

    private fun applyTextStyles() {
        if (textGradient) {
            setGradient(textGradientStart, textGradientEnd, textGradientOrientation)
        } else {
            setTextColor(if (isDarkMode()) tColorDark else tColorLight)
        }

        setHintTextColor(tColorHint)

        if (underlineText) {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }
    }
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) {
            super.onDraw(canvas)
            return
        }
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
        canvas.save()
        canvas.clipPath(Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), radius, radius, Path.Direction.CW)
        })
        super.onDraw(canvas)
        canvas.restore()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = minOf(w / 2, h / 2, cornerRadius)

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

    fun setGradient(startColor: Int, endColor: Int, orientation: TextGradientOrientation) {
        val width = paint.measureText(text.toString())
        val height = textSize * 1.3f
        val (x0, y0, x1, y1) = orientation.toCoordinates(width, height)

        paint.shader = LinearGradient(x0, y0, x1, y1, startColor, endColor, Shader.TileMode.CLAMP)
        invalidate()
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

    private fun Int.toStrokeOrientation() = UiTextView.GradientOrientation.entries.toTypedArray()
        .getOrElse(this) { GradientOrientation.LEFT_TO_RIGHT }

    private fun Int.toGradientOrientation() = UiTextView.GradientOrientation.entries.toTypedArray()
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
