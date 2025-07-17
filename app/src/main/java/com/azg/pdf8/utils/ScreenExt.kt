package com.azg.pdf8.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

fun Context.dpToPx(number: Number): Float {
    return number.toFloat() * (this.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}
fun Context.dpToPxInt(number: Number): Int {
    return number.toInt() * (this.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}

fun Context.pxToDp(number: Number): Float {
    return number.toFloat() / (this.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Context.clearFocus(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(messageRes: Int) {
    Toast.makeText(this, this.getString(messageRes), Toast.LENGTH_SHORT).show()
}

fun Activity.hideKeyboard() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
}
