@file:Suppress("DEPRECATION")

package com.dong.baselib.base
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
object SystemUtil {
    var myLocale: Locale? = null
    fun saveLocale(context: Context, lang: String?) { setPreLanguage(context, lang) }
    fun setLocale(context: Context, language: String = "en") {
        if (language.isEmpty()) {
            val config = Configuration()
            val locale = context.resources.configuration.locale
            Locale.setDefault(locale)
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } else { changeLang(language, context) }
    }
    private fun changeLang(lang: String?, context: Context) {
        if (lang.isNullOrEmpty()) return
        val parts = lang.split("_")
        val languageCode = parts[0]
        val countryCode = if (parts.size > 1) parts[1] else ""
        myLocale = if (countryCode.isNotEmpty()) Locale(languageCode, countryCode) else Locale(languageCode)
        saveLocale(context, lang)
        if (myLocale != null) {
            Locale.setDefault(myLocale)
        }
        val config = Configuration()
        config.locale = myLocale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    fun getPreLanguage(mContext: Context): String? = mContext.getSharedPreferences("data", Context.MODE_PRIVATE).getString("KEY_LANGUAGE", "en")
    private fun setPreLanguage(context: Context, language: String?) {
        if (language == null || language == "") {} else {
            context.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("KEY_LANGUAGE", language).apply()
        }
    }
    fun forceRated(context: Context) { context.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putBoolean("rated", true).apply() }
    fun isRatting(context: Context): Boolean = context.getSharedPreferences("data", Context.MODE_PRIVATE).getBoolean("rated", false)
}