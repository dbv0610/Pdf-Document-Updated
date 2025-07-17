package com.azg.pdf8.widget

import android.content.Context
import android.view.View
import android.widget.TextView
import com.azg.pdf8.R
import com.google.android.material.snackbar.Snackbar

fun Context.showSnackBar(rootView: View, anchorView: View, message: String) {
    val snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
    snackBar.setTextColor(resources.getColor(R.color.black, null))
        .setBackgroundTint(resources.getColor(R.color.white, null))
        .setAnchorView(anchorView)
        .setAnimationMode(
            Snackbar.ANIMATION_MODE_SLIDE
        ).apply {
            view.elevation = 70f
        }
        .show()
}

fun Context.getSnackBar(
    rootView: View,
    anchorView: View,
    message: String,
    isCenterContent: Boolean = false
): Snackbar {
    val snackBar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        .setTextColor(resources.getColor(R.color.black, null))
        .setBackgroundTint(resources.getColor(R.color.white, null))
        .setAnimationMode(
            Snackbar.ANIMATION_MODE_SLIDE
        )
        .setAnchorView(anchorView)

    if (isCenterContent) {
        val textView =
            snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    return snackBar
}