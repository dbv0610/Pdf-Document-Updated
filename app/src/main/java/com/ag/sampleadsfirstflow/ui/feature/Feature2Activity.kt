package com.ag.sampleadsfirstflow.ui.feature

import android.os.Bundle
import com.ag.sampleadsfirstflow.R
import com.ag.sampleadsfirstflow.ads.native.NativeAdsWrapper
import com.ag.sampleadsfirstflow.ads.native.NativePlacement
import com.ag.sampleadsfirstflow.remoteconfig.analytics.Analytics

class Feature2Activity : FeatureActivity() {
    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this,
            config = NativePlacement.FEATURE.apply {
                displayLayoutId = R.layout.layout_native_common_meta_low
            },
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { binding.shimmerAd.shimmerContainerNative }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (preferenceHelper.isUfo()) {
            Analytics.track("ufo_feature_2")
        }
    }

    override fun loadAd() {
        super.loadAd()
        with(nativeAdsWrapper) {
            setupNativeAd("native_feature_2")
            requestAds()
        }
    }
}
