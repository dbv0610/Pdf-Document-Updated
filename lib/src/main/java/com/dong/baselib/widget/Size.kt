
@file:Suppress("DEPRECATION")

package com.dong.baselib.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.view.WindowManager

fun delay(time: Int, content: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({
        content.invoke()
    }, time.toLong())
}

fun Int.pxToDp(): Float {
    val densityDpi = Resources.getSystem().displayMetrics.densityDpi
    return this / (densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Int.dpToPx(): Float {
    val density = Resources.getSystem().displayMetrics.density
    return this * density
}

fun Float.dpToPx(): Float {
    val density = Resources.getSystem().displayMetrics.density
    return this * density
}

val Activity.screenWidth: Int
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }
val Activity.screenHeight: Int
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

val Context.screenWidth: Int
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }
val Context.screenHeight: Int
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

val Context.displayMetrics: Int
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.densityDpi
    }

val Context.navigationBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    get() {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
val Context.statusBarHeight: Int
    @SuppressLint("DiscouragedApi", "InternalInsetResource") get() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

@Suppress("DEPRECATION")
fun noTitleBar(activity: Activity) {
    activity.requestWindowFeature(Window.FEATURE_NO_TITLE)
    activity.window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
}


fun Activity.widthPercent(percent: Float): Int {
    return ((screenWidth / 100) * percent).toInt()
}

fun Activity.heightPercent(percent: Float): Int {
    return ((screenHeight / 100) * percent).toInt()
}


fun Context.widthPercent(percent: Float): Int {
    return ((screenWidth / 100) * percent).toInt()
}

fun Context.heightPercent(percent: Float): Int {
    return ((screenHeight / 100) * percent).toInt()
}
