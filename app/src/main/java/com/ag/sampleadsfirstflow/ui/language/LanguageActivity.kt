package com.ag.sampleadsfirstflow.ui.language

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ag.sampleadsfirstflow.base.BaseActivity
import com.ag.sampleadsfirstflow.databinding.ActivityLanguageBinding
import com.ag.sampleadsfirstflow.model.LanguageItem
import com.ag.sampleadsfirstflow.model.LanguageScreenType
import com.ag.sampleadsfirstflow.ui.onboarding.OnboardingActivity
import com.ag.sampleadsfirstflow.utils.Language
import com.ag.sampleadsfirstflow.utils.Language.listLanguage
import com.ag.sampleadsfirstflow.utils.extensions.invisible
import com.ag.sampleadsfirstflow.utils.extensions.moveItemToPosition
import com.ag.sampleadsfirstflow.utils.extensions.parcelable
import com.ag.sampleadsfirstflow.utils.extensions.visible
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    companion object {
        private var scrollOffsetY: Int = 0
        private const val ARG_SCREEN_TYPE = "ARG_SCREEN_TYPE"
        private const val ARG_LANGUAGE = "arg_language"

        fun start(
            context: Context,
            screenType: LanguageScreenType,
        ) {
            val clazz = when (screenType) {
                is LanguageScreenType.Lfo.Lfo1 -> Language1Activity::class.java
                is LanguageScreenType.Lfo.Lfo2 -> Language2Activity::class.java
            }
            val intent = Intent(context, clazz)
            intent.putExtra(ARG_SCREEN_TYPE, screenType)
            context.startActivity(intent)
        }
    }

    private val screenType by lazy {
        runCatching {
            intent.parcelable<LanguageScreenType>(ARG_SCREEN_TYPE)
        }.getOrNull() ?: LanguageScreenType.Lfo.Lfo1
    }

    private val languageAdapter by lazy { LfoAdapter() }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(layoutInflater)
    }

    override fun updateUI(savedInstanceState: Bundle?) {
        when (screenType) {
            is LanguageScreenType.Lfo.Lfo1 -> {
                binding.btnDone.invisible()
            }

            is LanguageScreenType.Lfo.Lfo2 -> {
                binding.btnDone.visible()
                binding.btnDone.setOnClickListener {
                    languageAdapter.getLanguageSelected()?.let {
                        navigateToNextScreen(it)
                    }
                }
            }
        }
        setupListLanguage()
        runCatching { startTutorial() }
    }

    private fun startTutorial() {
        lifecycleScope.launch {
            delay(1000)
            languageAdapter.startTutorial()
        }
    }

    private fun setupListLanguage() {
        val listLfo = getListLanguageLfo()
        if (screenType is LanguageScreenType.Lfo.Lfo2) {
            listLfo.forEach { it.isDefault = false }
        }
        languageAdapter.submitList(listLfo)
        binding.rcvLanguage.layoutManager = LinearLayoutManager(this).also {
            if (screenType is LanguageScreenType.Lfo.Lfo2) {
                it.scrollToPositionWithOffset(0, -1 * scrollOffsetY)
            }
        }
        binding.rcvLanguage.adapter = languageAdapter
        languageAdapter.setOnItemSelected { item ->
            languageAdapter.selectedItem(item)
            if (screenType is LanguageScreenType.Lfo.Lfo1) {
                scrollOffsetY = binding.rcvLanguage.computeVerticalScrollOffset()
                start(this, LanguageScreenType.Lfo.Lfo2)
                finish()
                overridePendingTransition(0, 0)
            }
        }
    }

    private fun getListLanguageLfo(): List<LanguageItem> {
        val deviceLanguage = Resources.getSystem().configuration.locales[0].language
        val indexLanguageDevice = listLanguage.indexOfFirst { it.code == deviceLanguage }
        val listLfo = if (indexLanguageDevice != -1) {
            listLanguage[indexLanguageDevice].isDefault = true
            listLanguage.moveItemToPosition(3) { it.code == deviceLanguage }
        } else {
            listLanguage[0].isDefault = true
            listLanguage
        }
        return listLfo
    }

    private fun navigateToNextScreen(language: LanguageItem) {
        Language.changeLanguage(this, language.code)
        val intent = Intent(this, OnboardingActivity::class.java)
        intent.putExtra(ARG_LANGUAGE, language)
        startActivity(intent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishAffinity()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }
}
