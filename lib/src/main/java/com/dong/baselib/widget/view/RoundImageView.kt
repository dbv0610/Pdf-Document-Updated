package  com.dong.baselib.widget.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import com.dong.baselib.R
import com.dong.baselib.widget.GradientOrientation
import com.dong.baselib.widget.fromColor
import com.dong.baselib.widget.gradientIcon
import com.dong.baselib.widget.isValidHexColor
import kotlin.math.min
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import com.dong.baselib.widget.layout.UiRowLayout

@SuppressLint("CustomViewStyleable")
class RoundImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var cornerRadius = 0f
    private var stWidth = 0f
    private var stColorDark = Color.BLACK
    private var stColorLight = Color.BLACK
    private var bgColorDark = Color.TRANSPARENT
    private var bgColorLight = Color.TRANSPARENT
    private var bgGradientStart = Color.TRANSPARENT
    private var bgGradientEnd = Color.TRANSPARENT
    private var bgGradientCenter = Color.TRANSPARENT
    private var isGradient = false
    private var strokeGradient: IntArray? = null
    private var strokeGradientOrientation = GradientOrientation.LEFT_TO_RIGHT
    private var gradientOrientation = GradientOrientation.TOP_TO_BOTTOM
    private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isDither = true
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        isDither = true
    }
    private val path = Path()
    private val rectF = RectF()
    private val isDarkMode: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private var gradientIconList = intArrayOf()
    private var gdOrientationIcon = GradientOrientation.LEFT_TO_RIGHT

    init {
        background = null
        context.theme.obtainStyledAttributes(attrs, R.styleable.RoundImageView, 0, 0).apply {
            try {
                cornerRadius = getDimension(R.styleable.RoundImageView_cornerRadius, 0f)
                stWidth = getDimension(R.styleable.RoundImageView_strokeWidth, 0f)
                stColorDark = getColor(R.styleable.RoundImageView_stColorDark, Color.BLACK)
                stColorLight = getColor(R.styleable.RoundImageView_stColorLight, Color.BLACK)

                val stColorAll = getColor(R.styleable.RoundImageView_stColorAll, Color.TRANSPARENT)
                if (stColorAll != Color.TRANSPARENT) {
                    stColorLight = stColorAll
                    stColorDark = stColorAll
                }
                bgColorDark = getColor(R.styleable.RoundImageView_bgColorDark, Color.TRANSPARENT)
                bgColorLight = getColor(R.styleable.RoundImageView_bgColorLight, Color.TRANSPARENT)
                bgGradientStart =
                    getColor(R.styleable.RoundImageView_bgGradientStart, Color.TRANSPARENT)
                bgGradientCenter =
                    getColor(R.styleable.RoundImageView_bgGradientCenter, Color.TRANSPARENT)
                bgGradientEnd =
                    getColor(R.styleable.RoundImageView_bgGradientEnd, Color.TRANSPARENT)

                isGradient =
                    bgGradientStart != Color.TRANSPARENT && bgGradientEnd != Color.TRANSPARENT

                gradientOrientation = GradientOrientation.entries.getOrElse(
                    getInt(R.styleable.RoundImageView_bgGdOrientation, 0)
                ) { GradientOrientation.TOP_TO_BOTTOM }
                val bgColorAll = getColor(R.styleable.RoundImageView_bgColorAll, Color.TRANSPARENT)
                if (bgColorAll != Color.TRANSPARENT) {
                    bgColorLight = bgColorAll
                    bgColorDark = bgColorAll
                }
                strokeGradientOrientation = GradientOrientation.entries.getOrElse(
                    getInt(R.styleable.RoundImageView_strokeGdOrientation, 0)
                ) { GradientOrientation.LEFT_TO_RIGHT }
                val gradientImage = getString(R.styleable.RoundImageView_gradientIcons)
                if (!gradientImage.isNullOrEmpty()) {
                    val validColors = gradientImage.split(" ")
                        .mapNotNull { if (it.isValidHexColor()) Color.parseColor(it) else null }

                    if (validColors.isNotEmpty()) {
                        gradientIconList = validColors.toIntArray()
                    }
                }
                gdOrientationIcon =
                    when (getInt(R.styleable.RoundImageView_imageGdOrientation, 6)) {
                        0 -> GradientOrientation.TOP_TO_BOTTOM
                        1 -> GradientOrientation.TR_BL
                        2 -> GradientOrientation.RIGHT_TO_LEFT
                        3 -> GradientOrientation.BR_TL
                        4 -> GradientOrientation.BOTTOM_TO_TOP
                        5 -> GradientOrientation.BL_TR
                        6 -> GradientOrientation.LEFT_TO_RIGHT
                        7 -> GradientOrientation.TL_BR
                        else -> GradientOrientation.TOP_TO_BOTTOM
                    }
                val strokeGd = getString(R.styleable.RoundImageView_strokeGradient)
                strokeGradient = strokeGd?.split(" ")
                    ?.mapNotNull {
                        it.takeIf { it.matches(Regex("^#?[0-9a-fA-F]{6,8}$")) }
                            ?.let(Color::parseColor)
                    }
                    ?.toIntArray()
            } finally {
                recycle()
            }
        }
        if (gradientIconList.isNotEmpty()) {
            post {
                gradientIcon(
                    *gradientIconList,
                    orientation = gdOrientationIcon
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val radius = min(cornerRadius, min(w, h) / 2)
        rectF.set(0f, 0f, w, h)

        path.reset()
        path.addRoundRect(rectF, radius, radius, Path.Direction.CW)
        canvas.clipPath(path)

        drawBackground(canvas, w, h, radius)
        super.onDraw(canvas)
        drawStroke(canvas, w, h, radius)
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float, radius: Float) {
        paintBackground.shader = if (isGradient) {
            val (x0, y0, x1, y1) = gradientOrientation.toCoordinates(w, h)
            val colors = if (bgGradientCenter != Color.TRANSPARENT) {
                intArrayOf(bgGradientStart, bgGradientCenter, bgGradientEnd)
            } else {
                intArrayOf(bgGradientStart, bgGradientEnd)
            }
            LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP)
        } else null

        if (!isGradient) {
            paintBackground.color = if (isDarkMode) bgColorDark else bgColorLight
        }

        canvas.drawRoundRect(rectF, radius, radius, paintBackground)
    }

    private fun drawStroke(canvas: Canvas, w: Float, h: Float, radius: Float) {
        if (stWidth <= 0f) return
        val inset = stWidth / 2
        rectF.set(inset, inset, w - inset, h - inset)

        paintBorder.strokeWidth = stWidth
        paintBorder.shader = strokeGradient?.takeIf { it.size > 1 }?.let {
            val (x0, y0, x1, y1) = strokeGradientOrientation.toCoordinates(w, h)
            LinearGradient(x0, y0, x1, y1, it, null, Shader.TileMode.CLAMP)
        }
        if (paintBorder.shader == null) {
            paintBorder.color = if (isDarkMode) stColorDark else stColorLight
        }

        canvas.drawRoundRect(rectF, radius, radius, paintBorder)
    }

    private fun GradientOrientation.toCoordinates(w: Float, h: Float): FloatArray {
        return when (this) {
            GradientOrientation.TOP_TO_BOTTOM -> floatArrayOf(0f, 0f, 0f, h)
            GradientOrientation.BOTTOM_TO_TOP -> floatArrayOf(0f, h, 0f, 0f)
            GradientOrientation.LEFT_TO_RIGHT -> floatArrayOf(0f, 0f, w, 0f)
            GradientOrientation.RIGHT_TO_LEFT -> floatArrayOf(w, 0f, 0f, 0f)
            GradientOrientation.TL_BR -> floatArrayOf(0f, 0f, w, h)
            GradientOrientation.TR_BL -> floatArrayOf(w, 0f, 0f, h)
            GradientOrientation.BL_TR -> floatArrayOf(0f, h, w, 0f)
            GradientOrientation.BR_TL -> floatArrayOf(w, h, 0f, 0f)
        }
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
        gradientOrientation = orientation
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

    fun strokeGradientColors(colors: IntArray) {
        strokeGradient = colors
        invalidate()
    }

    fun strokeOrientation(orientation: GradientOrientation) {
        strokeGradientOrientation = orientation
        invalidate()
    }
}



