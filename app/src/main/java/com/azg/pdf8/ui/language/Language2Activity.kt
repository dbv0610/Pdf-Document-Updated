package com.azg.pdf8.ui.language

import android.os.Bundle
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.databinding.ActivityLanguageOpenBinding
import com.azg.pdf8.firebase.Analytics

class Language2Activity : LanguageOpenActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isUfo()) {
            Analytics.track("ufo_language_dup")
        }

    }

    override fun ActivityLanguageOpenBinding.setData() {
        currentLang.value?.let {
            languageAdapter.selectItem(it)
        }
    }
}
