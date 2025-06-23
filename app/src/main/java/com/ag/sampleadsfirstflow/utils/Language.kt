package com.ag.sampleadsfirstflow.utils

import android.content.Context
import android.content.res.Configuration
import com.ag.sampleadsfirstflow.R
import com.ag.sampleadsfirstflow.model.LanguageItem
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

object Language : KoinComponent {
    private val preferenceHelper: PreferenceHelper by inject()

    val listLanguage = listOf(
        LanguageItem(flagId = R.drawable.ic_flag_uk, name = "English", code = "en"),
        LanguageItem(flagId = R.drawable.ic_flag_india, name = "हिंदी", code = "hi"),
        LanguageItem(flagId = R.drawable.ic_flag_france, name = "Français", code = "fr"),
        LanguageItem(flagId = R.drawable.ic_flag_spain, name = "Español", code = "es"),
        LanguageItem(flagId = R.drawable.ic_flag_portugal, name = "Português", code = "pt"),
        LanguageItem(flagId = R.drawable.ic_flag_arab, name = "العربية", code = "ar"),
        LanguageItem(flagId = R.drawable.ic_flag_bulgary, name = "Български", code = "bg"),
        LanguageItem(flagId = R.drawable.ic_flag_czech, name = "Čeština", code = "cs"),
        LanguageItem(flagId = R.drawable.ic_flag_denmark, name = "Dansk", code = "da"),
        LanguageItem(flagId = R.drawable.ic_flag_germany, name = "Deutsch", code = "de"),
        LanguageItem(flagId = R.drawable.ic_flag_greek, name = "Ελληνικά", code = "el"),
        LanguageItem(flagId = R.drawable.ic_flag_iran, name = "فارسی", code = "fa_rIR"),
        LanguageItem(flagId = R.drawable.ic_flag_finland, name = "Suomi", code = "fi"),
        LanguageItem(flagId = R.drawable.ic_flag_filipino, name = "Filipino", code = "fil"),
        LanguageItem(flagId = R.drawable.ic_flag_india, name = "ગુજરાતી", code = "gu"),
        LanguageItem(flagId = R.drawable.ic_flag_hungary, name = "Magyar", code = "hu"),
        LanguageItem(flagId = R.drawable.ic_flag_indonesia, name = "Bahasa Indonesia", code = "in"),
        LanguageItem(flagId = R.drawable.ic_flag_italy, name = "Italiano", code = "it"),
        LanguageItem(flagId = R.drawable.ic_flag_israel, name = "עברית", code = "iw"),
        LanguageItem(flagId = R.drawable.ic_flag_japan, name = "日本語", code = "ja"),
        LanguageItem(flagId = R.drawable.ic_flag_india, name = "ಕನ್ನಡ", code = "kn"),
        LanguageItem(flagId = R.drawable.ic_flag_south_korea, name = "한국어", code = "ko"),
        LanguageItem(flagId = R.drawable.ic_flag_india, name = "മലയാളം", code = "ml"),
        LanguageItem(flagId = R.drawable.ic_flag_india, name = "मराठी", code = "mr"),
        LanguageItem(flagId = R.drawable.ic_flag_malaysia, name = "Melayu", code = "ms"),
        LanguageItem(flagId = R.drawable.ic_flag_netherlands, name = "Nederlands", code = "nl"),
        LanguageItem(flagId = R.drawable.ic_flag_pakistan, name = "ਪੰਜਾਬੀ", code = "pa"),
        LanguageItem(flagId = R.drawable.ic_flag_poland, name = "Polski", code = "pl"),
        LanguageItem(flagId = R.drawable.ic_flag_romania, name = "Română", code = "ro"),
        LanguageItem(flagId = R.drawable.ic_flag_russia, name = "Pусский язык", code = "ru"),
        LanguageItem(flagId = R.drawable.ic_flag_serbia, name = "Српски", code = "sr"),
        LanguageItem(flagId = R.drawable.ic_flag_sweden, name = "Svenska", code = "sv"),
        LanguageItem(flagId = R.drawable.ic_flag_india, name = "தமிழ்", code = "ta"),
        LanguageItem(flagId = R.drawable.ic_flag_india, name = "తెలుగు", code = "te"),
        LanguageItem(flagId = R.drawable.ic_flag_thailand, name = "ไทย", code = "th"),
        LanguageItem(flagId = R.drawable.ic_flag_turkey, name = "Türkçe", code = "tr"),
        LanguageItem(flagId = R.drawable.ic_flag_ukraine, name = "Українська", code = "uk"),
        LanguageItem(flagId = R.drawable.ic_flag_pakistan, name = "اردو", code = "ur"),
        LanguageItem(flagId = R.drawable.ic_flag_china, name = "简体中文", code = "zh"),
        LanguageItem(flagId = R.drawable.ic_flag_taiwan, name = "繁體中文", code = "zh_TW"),
        LanguageItem(flagId = R.drawable.ic_flag_vietnam, name = "Tiếng Việt", code = "vi"),
        LanguageItem(flagId = R.drawable.ic_flag_brazil, name = "Brasil", code = "pt_BR"),
        LanguageItem(flagId = R.drawable.ic_flag_mexico, name = "México", code = "es_MX"),
        LanguageItem(flagId = R.drawable.ic_flag_pakistan, name = "پاکستان", code = "ur_PK"),
        LanguageItem(flagId = R.drawable.ic_flag_uae, name = "الإمارات العربية المتحدة", code = "ar_AE"),
        LanguageItem(flagId = R.drawable.ic_flag_arab, name = "المملكة العربية السعودية", code = "ar_SA"),
    )

    fun changeLanguage(context: Context, language: String): Context {
        if (language.equals("", ignoreCase = true)) return context
        var myLocale = Locale(language)
        if (language.contains("_")) {
            myLocale =
                Locale(
                    language.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0],
                    language.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1],
                )
        }
        preferenceHelper.languageSelected = language
        Locale.setDefault(myLocale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(myLocale)
        configuration.setLayoutDirection(myLocale)
        return context.createConfigurationContext(configuration)
    }

    fun createContextLocale(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        val selectedLanguage = preferenceHelper.languageSelected
        val locale = if (selectedLanguage.contains("_")) {
            Locale(selectedLanguage.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0],
                selectedLanguage.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1])
        } else {
            Locale(selectedLanguage)
        }
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}
