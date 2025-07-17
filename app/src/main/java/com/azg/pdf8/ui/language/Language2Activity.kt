package com.azg.pdf8.ui.language

import android.os.Bundle
import android.view.View
import com.azg.pdf8.databinding.ActivityLanguageOpenBinding

class Language2Activity : LanguageOpenActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun ActivityLanguageOpenBinding.setData() {
        currentLang.value?.let {
            languageAdapter.selectItem(it)
        }

    }
}
