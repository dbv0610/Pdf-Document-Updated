package com.azg.pdf8.ui.language

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.azg.pdf8.ads.ads.splash.AdSplashCompleteListener
import com.azg.pdf8.ads.ads.splash.AdSplashHelper
import com.azg.pdf8.ads.ads.splash.AdState
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.dong.baselib.base.SystemUtil
import com.azg.pdf8.databinding.ActivityLanguageOpenBinding
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.ui.splash.NativeSplashActivity
import com.azg.pdf8.utils.Constant
import com.dong.baselib.api.parcelable
import com.dong.baselib.lifecycle.set
import com.dong.baselib.widget.invisible
import com.dong.baselib.widget.moveItemToPosition
import com.dong.baselib.widget.visible
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class LanguageOpenActivity :
    BaseActivity<ActivityLanguageOpenBinding>(ActivityLanguageOpenBinding::inflate){

    companion object {
        private var scrollOffsetY: Int = 0
        private const val ARG_SCREEN_TYPE = "ARG_SCREEN_TYPE"
        private const val ARG_LANGUAGE = "arg_language"

        fun start(
            context: Context,
            screenType: LanguageScreenType,
        ) {
            val clazz = when (screenType) {
                is LanguageScreenType.Language1 -> Language1Activity::class.java
                is LanguageScreenType.Language2 -> Language2Activity::class.java
            }
            val intent = Intent(context, clazz)
            intent.putExtra(ARG_SCREEN_TYPE, screenType)
            val anim = ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
            context.startActivity(intent, anim)
        }

        val currentLang = MutableLiveData<LanguageItem>()
        val languageAdapter by lazy { LfoAdapter() }
    }

    private val screenType by lazy {
        runCatching {
            intent.parcelable<LanguageScreenType>(ARG_SCREEN_TYPE)
        }.getOrNull() ?: LanguageScreenType.Language1
    }

    override fun ActivityLanguageOpenBinding.onClick() = Unit

    override fun initialize() {
        updateUI()
    }

    override fun backPressed() {
        finishAffinity()
    }

    override fun ActivityLanguageOpenBinding.setData() = Unit

    fun updateUI() {
        when (screenType) {
            is LanguageScreenType.Language1 -> {
                binding.selectLanguage.invisible()
            }
            is LanguageScreenType.Language2 -> {
                binding.selectLanguage.visibility = View.VISIBLE
                currentLang.value?.let { languageAdapter.selectItem(it) }
                binding.selectLanguage.visible()
                binding.selectLanguage.setOnClickListener {
                    if (AdSplashHelper.isLoadAdsFullDone && remoteConfig.newFlowApp) {
                        AdSplashHelper.adManager?.showCurrentAd()
                    }else{
                        languageAdapter.getSelectedLanguage()?.let {
                            navigateToNextScreen(it)
                        }
                    }
                }
            }
        }
        setupListLanguage()
        runCatching { startTutorial() }
        preloadAndObserverAdsFullScreen()
    }

    private fun preloadAndObserverAdsFullScreen() {
        if (remoteConfig.newFlowApp) {
            AdSplashHelper.adManager?.bind(this)
            lifecycleScope.launch {
                AdSplashHelper.adManager?.adState?.collectLatest {
                    when (it) {
                        AdState.Idle -> Unit
                        AdState.NavigateNext -> navigateToHome()
                        AdState.NativeFullScr -> navigateToNativeFullScreen()
                    }
                }
            }
        }
    }

    private fun startTutorial() {
        lifecycleScope.launch {
            delay(1000)
            languageAdapter.enableTutorialMode()
        }
    }

    private fun setupListLanguage() {
        val listLfo = getListLanguageLfo()
        if (screenType is LanguageScreenType.Language2) {
            listLfo.forEach { it.isDefault = false }
        }
        languageAdapter.submitList(listLfo)
        binding.rcvLanguage.layoutManager = LinearLayoutManager(this).also {
            if (screenType is LanguageScreenType.Language2) {
                it.scrollToPositionWithOffset(0, -1 * scrollOffsetY)
            }
        }
        binding.rcvLanguage.adapter = languageAdapter
        languageAdapter.setOnItemSelected { item ->
            languageAdapter.selectItem(item)
            currentLang.set(item)
            if (screenType is LanguageScreenType.Language1) {
                scrollOffsetY = binding.rcvLanguage.computeVerticalScrollOffset()
                start(this, LanguageScreenType.Language2)
                finish()
            }
        }
    }

    private fun getListLanguageLfo(): List<LanguageItem> {
        val langCode = Resources.getSystem().configuration.locales[0].language
        return listLanguage.toMutableList().let { list ->
            list.indexOfFirst { it.code.startsWith(langCode, ignoreCase = true) }
                .takeIf { it != -1 }
                ?.let { idx ->
                    list.mapIndexed { i, item ->
                        item.copy(isDefault = i == idx)
                    }.moveItemToPosition(3) { it.code.startsWith(langCode, ignoreCase = true) }
                } ?: list.mapIndexed { i, item ->
                item.copy(isDefault = i == 0)
            }
        }
    }

    private fun navigateToNextScreen(language: LanguageItem) {
        SystemUtil.setLocale(this, language.code)
        val intent = Intent(this, LangApplyActivity::class.java)
        intent.putExtra(ARG_LANGUAGE, language)
        startActivity(intent)
        finish()
    }

    val launcherResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                navigateToHome()
            }
        }

    private fun navigateToNativeFullScreen() {
        val intent = Intent(this, NativeSplashActivity::class.java).apply {
            putExtra(Constant.SCREEN, Constant.LANGUAGE)
        }
        launcherResult.launch(intent)
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
