package com.azg.pdf8.ui.language

import android.os.Bundle
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.ActivityLanguageOpenBinding
import com.azg.pdf8.firebase.Analytics

class Language2Activity : LanguageOpenActivity() {
    val isSmallNative = remoteConfig.n104Config1.layout.contains("small")
    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this,
            config = NativePlacement.LANGUAGE_2,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { if (isSmallNative) binding.shimmerAdSmall.shimmerContainerNative else binding.shimmerAdMedium.shimmerContainerNative }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isUfo()) {
            Analytics.track("ufo_language_dup")
        }
        with(nativeAdsWrapper) {
            setupNativeAd("native_language_2")
            requestAds()
        }
    }

    override fun ActivityLanguageOpenBinding.setData() {
        currentLang.value?.let {
            languageAdapter.selectItem(it)
        }
    }
}
