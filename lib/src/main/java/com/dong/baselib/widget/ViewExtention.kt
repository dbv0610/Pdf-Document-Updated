package com.dong.baselib.widget

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible

abstract class TapNoHandleListener : View.OnClickListener {
    override fun onClick(v: View?) {
        onTap(v)
    }

    abstract fun onTap(v: View?)
}

abstract class TapListener : View.OnClickListener {
    companion object {
        private const val TIME_WAIT = 500L
    }

    private var lastClick: Long = 0
    private var tracking: String? = null
    fun withTracking(tracking: String): TapListener {
        this.tracking = tracking
        return this
    }

    override fun onClick(v: View?) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClick > TIME_WAIT) {
            onTap(v)
            tracking?.let {
                Log.d("Tracking", "onClick: ${it}")
            }
            lastClick = now
        }
    }

    abstract fun onTap(v: View?)
}

fun View.click(action: (view: View?) -> Unit): TapListener {
    val tapListener = object : TapListener() {
        override fun onTap(v: View?) {
            action(v)
        }
    }
    setOnClickListener(tapListener)
    return tapListener
}

fun View.clickNoHandle(action: (view: View?) -> Unit) {
    setOnClickListener(object : TapNoHandleListener() {
        override fun onTap(v: View?) {
            action(v)
        }
    })
}

fun View.setAllEnabled(enabled: Boolean) {
    isEnabled = enabled
    if (this is ViewGroup) children.forEach { child -> child.setAllEnabled(enabled) }
}

var TextView.listenText: MutableLiveData<String>?
    get() = throw UnsupportedOperationException("Getter is not supported for listenText")
    set(value) {
        value?.observe(context as LifecycleOwner) { text ->
            this.text = text.toString()
        }
    }

fun TextView.afterTextChanged(callback: (String) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null

    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            runnable?.let { handler.removeCallbacks(it) }
            runnable = Runnable { callback(s.toString()) }
            handler.postDelayed(runnable!!, 150)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun EditText.listenNumberFormat(value: (EditText, String) -> Unit) {
    val edt = this
    edt.addTextChangedListener(object : TextWatcher {
        private var isEditing: Boolean = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            if (isEditing) return
            isEditing = true

            try {
                val input = editable.toString()

                if (input.isNotEmpty()) {
                    val cleanInput = input.replace(",", "")
                    if (cleanInput.matches(Regex("^[0-9]*\\.?[0-9]*$"))) {
                        if (cleanInput.contains(".")) {
                            val firstNum = cleanInput.substringBefore(".").toBigIntegerOrNull()
                            val secondNum = cleanInput.substringAfter(".")
                            if (firstNum != null) {
                                val formattedFirstPart = String.format("%,d", firstNum)
                                val totalText = "$formattedFirstPart.$secondNum"
                                editable?.replace(0, editable.length, totalText)
                                value?.invoke(edt, firstNum.toString().replace(",", ""))
                            }
                        } else {
                            val firstNum = cleanInput.toBigIntegerOrNull()
                            if (firstNum != null) {
                                val formattedFirstPart = String.format("%,d", firstNum)
                                editable?.replace(0, editable.length, formattedFirstPart)
                                value?.invoke(edt, firstNum.toString().replace(",", ""))
                            }
                        }
                    } else {
                        editable?.clear()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            isEditing = false
        }
    })
}

fun TextView.onFocusChange(callback: (Boolean, String) -> Unit) {
    this.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        callback(hasFocus, this.text.toString())
    }
}

fun TextView.keyDown(callback: (Int) -> Unit) {
    this.setOnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_DOWN) {
            callback.invoke(keyCode)
            true
        } else {
            false
        }
    }
}

fun TextView.beforeTextChanged(callback: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            callback(s.toString())
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun TextView.textChanged(callback: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            callback(s.toString())
        }
    })
}

fun View.fadeIn(duration: Long = 300) {
    this.alpha = 0f
    this.visibility = View.VISIBLE
    ObjectAnimator.ofFloat(this, "alpha", 1f).setDuration(duration).start()
}

fun View.fadeOut(duration: Long = 300) {
    ObjectAnimator.ofFloat(this, "alpha", 0f).setDuration(duration).start()
    this.postDelayed({ this.visibility = View.GONE }, duration)
}

fun ImageView.loadImage(url: String) {
    Glide.with(this.context).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).into(this)
}

fun ImageView.loadImage(url: Int) {
    Glide.with(this.context).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).into(this)
}

fun ImageView.loadImage(url: Uri) {
    Glide.with(this.context).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).into(this)
}

fun View.doOnVisibilityChange(onChange: (Boolean) -> Unit) {
    var lastVisible = (isVisible)
    onChange(lastVisible)
    val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val nowVisible = (isVisible)
        if (nowVisible != lastVisible) {
            lastVisible = nowVisible
            onChange(nowVisible)
        }
    }
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) {
            viewTreeObserver.removeOnGlobalLayoutListener(listener)
            removeOnAttachStateChangeListener(this)
        }
    })
}
fun View.gone() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        this.visibility = View.GONE
    } else {
        Handler(Looper.getMainLooper()).post {
            this.visibility = View.GONE
        }
    }
}

fun View.visible() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        this.visibility = View.VISIBLE
    } else {
        Handler(Looper.getMainLooper()).post {
            this.visibility = View.VISIBLE
        }
    }
}

fun View.invisible() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        this.visibility = View.INVISIBLE
    } else {
        Handler(Looper.getMainLooper()).post {
            this.visibility = View.INVISIBLE
        }
    }
}

fun <T> View.showStateGone(mutableLiveData: MutableLiveData<T>, lifecycleOwner: LifecycleOwner) {
    mutableLiveData.observe(lifecycleOwner) { isVisible ->
        if (isVisible == null) {
            this.visibility = View.GONE
        } else {
            if (isVisible is Boolean) {
                this.visibility = if (isVisible) View.VISIBLE else View.GONE
            } else {
                this.visibility = View.VISIBLE
            }
        }
    }
}

fun <T> View.showStateInvisible(
    mutableLiveData: MutableLiveData<T>,
    lifecycleOwner: LifecycleOwner
) {
    mutableLiveData.observe(lifecycleOwner) { isVisible ->
        if (isVisible == null) {
            this.visibility = View.INVISIBLE
        } else {
            if (isVisible is Boolean) {
                this.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            } else {
                this.visibility = View.VISIBLE
            }
        }
    }
}

fun <T> View.showStateGone(mutableLiveData: MutableLiveData<T>) {
    mutableLiveData.observeForever { isVisible ->
        if (isVisible == null) {
            this.visibility = View.GONE
        } else {
            if (isVisible is Boolean) {
                this.visibility = if (isVisible) View.VISIBLE else View.GONE
            } else {
                this.visibility = View.VISIBLE
            }
        }
    }
}

fun <T> View.showStateInvisible(mutableLiveData: MutableLiveData<T>) {
    mutableLiveData.observeForever { isVisible ->
        if (isVisible == null) {
            this.visibility = View.INVISIBLE
        } else {
            if (isVisible is Boolean) {
                this.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            } else {
                this.visibility = View.VISIBLE
            }
        }
    }
}

fun <T> View.showStateGone(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.GONE
}

fun <T> View.showStateInvisible(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
}

inline fun <T> List<T>.moveItemToPosition(position: Int, predicate: (T) -> Boolean): List<T> {
    for (element in this.withIndex()) {
        if (predicate(element.value)) {
            return this.toMutableList().apply {
                removeAt(element.index)
                add(position - 1, element.value)
            }.toList()
        }
    }
    return this
}
@SuppressLint("DefaultLocale")
fun convertMillieToHhMmSs(millis: Long): String {
    val seconds = millis / 1000
    val second = seconds % 60
    val minute = seconds / 60 % 60
    val hour = seconds / (60 * 60) % 24
    return if (hour > 0) {
        String.format("%02d:%02d:%02d", hour, minute, second)
    } else {
        String.format("%02d:%02d", minute, second)
    }
}
@Suppress("DEPRECATION")
fun getScreenDimensions(context: Context): Pair<Int, Int> {
    val displayMetrics = DisplayMetrics()
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    return Pair(screenWidth, screenHeight)
}

fun ImageView.imageBitmap(): Bitmap? {
    try {
        val drawable = this.drawable
        val originalBitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            is VectorDrawable -> {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            else -> null
        }
        return originalBitmap
    } catch (e: Exception) {
        return null
    }
}

enum class GradientOrientation {
    TOP_TO_BOTTOM,
    TR_BL,
    RIGHT_TO_LEFT,
    BR_TL,
    BOTTOM_TO_TOP,
    BL_TR,
    LEFT_TO_RIGHT,
    TL_BR
}

fun ImageView.gradientIcon(
    vararg colors: Int,
    orientation: GradientOrientation = GradientOrientation.TOP_TO_BOTTOM
) {
    if (colors.size < 2) return
    val drawable = this.drawable ?: return
    val originalBitmap = when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is VectorDrawable -> {
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        else -> return
    }
    val width = originalBitmap.width
    val height = originalBitmap.height
    val updatedBitmap = createBitmap(width, height)
    val canvas = Canvas(updatedBitmap)
    canvas.drawBitmap(originalBitmap, 0f, 0f, null)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = getGradientShader(width, height, colors, orientation)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    this.setImageDrawable(updatedBitmap.toDrawable(resources))
}

private fun getGradientShader(
    width: Int,
    height: Int,
    colors: IntArray,
    orientation: GradientOrientation
): Shader {
    return when (orientation) {
        GradientOrientation.TOP_TO_BOTTOM -> LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        GradientOrientation.BOTTOM_TO_TOP -> LinearGradient(
            0f,
            height.toFloat(),
            0f,
            0f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        GradientOrientation.LEFT_TO_RIGHT -> LinearGradient(
            0f,
            0f,
            width.toFloat(),
            0f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        GradientOrientation.RIGHT_TO_LEFT -> LinearGradient(
            width.toFloat(),
            0f,
            0f,
            0f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        GradientOrientation.TL_BR -> LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        GradientOrientation.TR_BL -> LinearGradient(
            width.toFloat(),
            0f,
            0f,
            height.toFloat(),
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        GradientOrientation.BL_TR -> LinearGradient(
            0f,
            height.toFloat(),
            width.toFloat(),
            0f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        GradientOrientation.BR_TL -> LinearGradient(
            width.toFloat(),
            height.toFloat(),
            0f,
            0f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
    }
}

fun View.rotateViewByTime(
    fromDegree: Float = 0f,
    toDegree: Float = 360f,
    duration: Long = 3000,
    isLoop: Boolean = false
) {
    val rotation = ObjectAnimator.ofFloat(this, "rotation", fromDegree, toDegree)
    rotation.duration = duration // Set the duration of the animation
    if (isLoop) {
        rotation.repeatCount = ObjectAnimator.INFINITE
        rotation.repeatMode =
            ObjectAnimator.RESTART  // Optionally set the mode to restart the animation after each cycle
    }
    rotation.start() // Start the animation
}

fun View.rotateViewWithTime(
    fromDegree: Float = 0f,
    toDegree: Float = 360f,
    duration: Long = 3000,
    isLoop: Boolean = false
): ObjectAnimator {
    return ObjectAnimator.ofFloat(this, "rotation", fromDegree, toDegree).apply {
        this.duration = duration
        if (isLoop) {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }
        start()
    }
}

fun ViewGroup.swapChildren(index1: Int, index2: Int) {
    if (index1 < 0 || index1 >= this.childCount || index2 < 0 || index2 >= this.childCount) {
        return
    }
    val view1 = getChildAt(index1)
    val view2 = getChildAt(index2)
    this.removeViewAt(index1)
    this.removeViewAt(index2 - 1)
    this.addView(view1, index2)
    this.addView(view2, index1)
    this.requestLayout()
    this.invalidate()
}

fun View.paddingTop(number: Number) {
    this.setPadding(this.paddingStart, number.toInt(), this.paddingEnd, this.paddingBottom)
}

fun View.paddingLeft(number: Number) {
    this.setPadding(number.toInt(), this.paddingTop, this.paddingRight, this.paddingBottom)
}

fun View.paddingRight(number: Number) {
    this.setPadding(this.paddingStart, this.paddingTop, number.toInt(), this.paddingBottom)
}

fun View.paddingBottom(number: Number) {
    this.setPadding(this.paddingStart, this.paddingTop, this.paddingRight, number.toInt())
}

fun View.paddingVertical(number: Number) {
    this.setPadding(this.paddingStart, number.toInt(), this.paddingTop, number.toInt())
}

fun View.paddingHorizontal(number: Number) {
    this.setPadding(number.toInt(), this.paddingTop, number.toInt(), this.paddingBottom)
}
