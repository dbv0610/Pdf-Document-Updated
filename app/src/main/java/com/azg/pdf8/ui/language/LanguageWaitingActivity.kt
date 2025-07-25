package com.azg.pdf8.ui.language

import android.content.res.Resources
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityLanguageWaitingBinding
import com.azg.pdf8.firebase.Analytics
import com.dong.baselib.widget.moveItemToPosition
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LanguageWaitingActivity :
    BaseActivity<ActivityLanguageWaitingBinding>(ActivityLanguageWaitingBinding::inflate) {
    private var job: Job? = null
    private val languageAdapter by lazy { LfoAdapter() }

    companion object {
        var isShowAdInWaiting = false
    }

    override fun backPressed() = Unit
    override fun initialize() = Unit
    override fun ActivityLanguageWaitingBinding.setData() {
        countDownSkip()
        setupListLanguage()
    }

    fun nextAction() {
        LanguageOpenActivity.start(this@LanguageWaitingActivity, LanguageScreenType.Language1)
        overridePendingTransition(0, 0)
        finish()
    }

    override fun ActivityLanguageWaitingBinding.onClick() = Unit

    private fun countDownSkip() {
        job?.cancel()
        job = lifecycleScope.launch {
            val totalDuration = 2000L
            val maxProgress = 100
            for (progress in 0..maxProgress) {
                if (!isActive) return@launch
                binding.progressIndicator.progress = progress
                delay(totalDuration / maxProgress)
            }
            binding.progressIndicator.progress = maxProgress
            nextAction()
        }
    }

    val isSmallNative = remoteConfig.n103Config1.layout.contains("small")
    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this,
            config = NativePlacement.LANGUAGE_1,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { if (isSmallNative) binding.shimmerAdSmall.shimmerContainerNative else binding.shimmerAdMedium.shimmerContainerNative }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isUfo()) {
            Analytics.track("ufo_language_loading")
        }
        with(nativeAdsWrapper) {
            setupNativeAd("native_language_loading")
            requestAds()
        }
        NativeAdPreloadManager.preloadAd(this, NativePlacement.LANGUAGE_2, isForce = true)
    }

    private fun setupListLanguage() {
        languageAdapter.setEnable(false)
        languageAdapter.submitList(getListLanguageLfo())
        binding.rcvLanguage.adapter = languageAdapter
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

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}
