package com.azg.pdf8.ui.language

import android.os.Bundle
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.firebase.Analytics

class Language1Activity : LanguageOpenActivity() {
    val isSmallNative = remoteConfig.N103Config1.layout.contains("small")
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
            Analytics.track("ufo_language")
        }
        NativeAdPreloadManager.preloadAd(
            this,
            NativePlacement.ONBOARDING,
            isSizeEnableNativeOnb(),
            true
        )
        with(nativeAdsWrapper) {
            setupNativeAd("native_language_1")
            requestAds()
        }
    }

    private fun isSizeEnableNativeOnb(): Int {
        return listOf(
            remoteConfig.OnboardingConfig.isEnableScreen1,
            remoteConfig.OnboardingConfig.isEnableScreen2,
            remoteConfig.OnboardingConfig.isEnableScreen3,
            remoteConfig.OnboardingConfig.isEnableScreen4,
        ).count { it }
    }
}
