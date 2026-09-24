package com.azg.pdf8.widget

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

fun View.setBackgroundTintCompat(
    @ColorInt tint: Int,
    mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
) {
    val original: Drawable = background ?: return
    val wrapped = DrawableCompat.wrap(original.mutate())
    DrawableCompat.setTintList(wrapped, ColorStateList.valueOf(tint))
    DrawableCompat.setTintMode(wrapped, mode)
    background = wrapped
}
fun ViewGroup.setBackgroundTintCompat(
    @ColorInt tint: Int,
    mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
) {
    val original: Drawable = background ?: return
    val wrapped = DrawableCompat.wrap(original.mutate())
    DrawableCompat.setTintList(wrapped, ColorStateList.valueOf(tint))
    DrawableCompat.setTintMode(wrapped, mode)
    background = wrapped
}