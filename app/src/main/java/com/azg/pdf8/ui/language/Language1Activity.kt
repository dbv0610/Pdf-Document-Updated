package com.azg.pdf8.ui.language

import android.os.Bundle
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.firebase.Analytics

class Language1Activity : LanguageOpenActivity() {
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
            Analytics.track("ufo_language")
        }
        if (remoteConfig.onboardingEnable) {
            NativeAdPreloadManager.preloadAd(
                this,
                NativePlacement.ONBOARDING,
                isSizeEnableNativeOnb(),
                true
            )
        } else {
            if (remoteConfig.wellComeEnable) {
                NativeAdPreloadManager.preloadAd(
                    this,
                    NativePlacement.FEATURE,
                    2,
                    true
                )
            } else {
                if (isGrantedPermission()) {
                    NativeAdPreloadManager.preloadAd(
                        this@Language1Activity,
                        NativePlacement.PERMISSION,
                        1,
                        true
                    )
                }
            }
        }

        with(nativeAdsWrapper) {
            setupNativeAd("native_language_1")
            requestAds()
        }
    }

    private fun isSizeEnableNativeOnb(): Int {
        return listOf(
            remoteConfig.onboardingConfig.isEnableScreen1,
            remoteConfig.onboardingConfig.isEnableScreen2,
            remoteConfig.onboardingConfig.isEnableScreen3,
            remoteConfig.onboardingConfig.isEnableScreen4,
        ).count { it }
    }
}
