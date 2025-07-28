package com.dong.baselib.widget

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.*
import android.util.TypedValue
import android.view.View
import android.widget.TextView
fun textStyle(init: TextStyle.() -> Unit): CharSequence {
    val style = TextStyle()
    style.init()
    return style.build()
}
class TextStyle {
    private val builder = SpannableStringBuilder()

    fun bold(
        text: String,
        textColor: Int = Color.BLACK,
        underline: Boolean = false,
        bgColor: Int? = null,
        fontSizeSp: Float? = null,
        onClick: (() -> Unit)? = null
    ): TextStyle {
        applyStyle(text, Typeface.BOLD, textColor, underline, bgColor, fontSizeSp, onClick)
        return this
    }

    fun italic(
        text: String,
        textColor: Int = Color.BLACK,
        underline: Boolean = false,
        bgColor: Int? = null,
        fontSizeSp: Float? = null,
        onClick: (() -> Unit)? = null
    ): TextStyle {
        applyStyle(text, Typeface.ITALIC, textColor, underline, bgColor, fontSizeSp, onClick)
        return this
    }

    fun boldItalic(
        text: String,
        textColor: Int = Color.BLACK,
        underline: Boolean = false,
        bgColor: Int? = null,
        fontSizeSp: Float? = null,
        onClick: (() -> Unit)? = null
    ): TextStyle {
        applyStyle(text, Typeface.BOLD_ITALIC, textColor, underline, bgColor, fontSizeSp, onClick)
        return this
    }

    fun normal(
        text: String,
        textColor: Int = Color.BLACK,
        underline: Boolean = false,
        bgColor: Int? = null,
        fontSizeSp: Float? = null,
        onClick: (() -> Unit)? = null
    ): TextStyle {
        applyStyle(text, Typeface.NORMAL, textColor, underline, bgColor, fontSizeSp, onClick)
        return this
    }

    private fun applyStyle(
        text: String,
        style: Int,
        textColor: Int,
        underline: Boolean,
        bgColor: Int?,
        fontSizeSp: Float?,
        onClick: (() -> Unit)?
    ) {
        val start = builder.length
        builder.append(text)
        val end = builder.length

        builder.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(textColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (underline) {
            builder.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (bgColor != null) {
            builder.setSpan(BackgroundColorSpan(bgColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (fontSizeSp != null) {
            builder.setSpan(AbsoluteSizeSpan(fontSizeSp.toInt(), true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (onClick != null) {
            builder.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = underline
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun build(): CharSequence = builder
}
