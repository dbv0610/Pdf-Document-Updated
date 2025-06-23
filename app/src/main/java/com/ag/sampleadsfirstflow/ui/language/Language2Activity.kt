package com.ag.sampleadsfirstflow.ui.language

import android.os.Bundle
import com.ag.sampleadsfirstflow.ads.native.NativeAdsWrapper
import com.ag.sampleadsfirstflow.ads.native.NativePlacement
import com.ag.sampleadsfirstflow.remoteconfig.analytics.Analytics

class Language2Activity : LanguageActivity() {
    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this,
            config = NativePlacement.LANGUAGE_2,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { binding.shimmerAd.shimmerContainerNative }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (preferenceHelper.isUfo()) {
            Analytics.track("ufo_language_dup")
        }
    }

    override fun loadAd() {
        super.loadAd()
        with(nativeAdsWrapper) {
            setupNativeAd("native_language_2")
            requestAds()
        }
    }
}
