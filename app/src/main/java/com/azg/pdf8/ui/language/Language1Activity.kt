package com.azg.pdf8.ui.language

import android.os.Bundle
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.firebase.Analytics
import com.azg.pdf8.ui.feature.FeatureActivity
import com.azg.pdf8.ui.feature.FeatureScreenType
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.ui.onboarding.OnboardingActivity

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
        if (remoteConfig.OnboardingEnable) {
            NativeAdPreloadManager.preloadAd(
                this,
                NativePlacement.ONBOARDING,
                isSizeEnableNativeOnb(),
                true
            )
        } else {
            if (remoteConfig.WellComeEnable) {
                NativeAdPreloadManager.preloadAd(
                    this,
                    NativePlacement.FEATURE,
                    2,
                    true
                )
            }
        }

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
