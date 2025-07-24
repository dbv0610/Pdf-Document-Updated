package com.azg.pdf8.ui.feature

import android.os.*
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.R
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.*
import com.azg.pdf8.firebase.Analytics

class Feature1Activity : FeatureActivity() {
    private val isSmallNative get() = remoteConfig.N109Config1.layout.contains("small")
    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this,
            config = NativePlacement.FEATURE,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { if (isSmallNative) binding.shimmerAdSmall.shimmerContainerNative else binding.shimmerAdMedium.shimmerContainerNative }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isUfo()) {
            Analytics.track("ufo_feature_1")
        }
        with(nativeAdsWrapper) {
            setupNativeAd("native_feature_1")
            requestAds()
        }
        if (!BaseActivity.isGrantPermission) {
            NativeAdPreloadManager.preloadAd(
                this@Feature1Activity,
                NativePlacement.PERMISSION,
                1,
                false
            )
        }
    }

    override fun initList() {
        super.initList()
        placeLabelIds = listFeature()
    }
}
