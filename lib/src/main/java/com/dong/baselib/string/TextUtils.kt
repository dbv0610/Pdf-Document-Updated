package com.dong.baselib.string

import android.text.TextUtils
import android.util.Patterns
import android.webkit.URLUtil
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

fun String.checkURL(): Boolean {
    if (TextUtils.isEmpty(this)) {
        return false
    }
    val pattern: Pattern = Patterns.WEB_URL
    var isURL: Boolean = pattern.matcher(this).matches()
    if (!isURL) {
        val urlString = this + ""
        if (URLUtil.isNetworkUrl(urlString)) {
            try {
                URL(urlString)
                isURL = true
            } catch (e: Exception) {
                isURL = false
            }
        }
    }
    return isURL
}

fun currentTimeFormatted(): String {
    val currentTimeMillis = System.currentTimeMillis()
    val dateFormat = SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault())
    return dateFormat.format(Date(currentTimeMillis))
}

fun String.fileExtension(): String {
    return try {
        File(this).extension
    } catch (e: FileNotFoundException) {
        e.message.toString()
    }
}
fun String.fileName(): String {
    return try {
        File(this).name
    } catch (e: FileNotFoundException) {
        e.message.toString()
    }
}
fun String.format(): String {
    return this.replace("\n", "").trim()
}

fun String.checkValue(): Boolean {
    return this.replace(" ", "").replace("\n", "").isNotEmpty()
}

fun String.maxLength(length: Int): Boolean {
    return this.replace("\n", "").length <= (length)
}

fun String.minLength(length: Int): Boolean {
    return this.replace("\n", "").length >= (length)
}

fun String.isEmail(): Boolean {
    return this.isNotEmpty() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}
