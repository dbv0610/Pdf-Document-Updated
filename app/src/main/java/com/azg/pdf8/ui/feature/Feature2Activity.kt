package com.azg.pdf8.ui.feature

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.R
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.ActivityFeatureBinding
import com.azg.pdf8.firebase.Analytics
import com.dong.baselib.widget.fromColor
import com.dong.baselib.widget.layout.UiLinearLayout
import kotlinx.coroutines.launch

class Feature2Activity : FeatureActivity() {
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
            Analytics.track("ufo_feature_2")
        }
        with(nativeAdsWrapper) {
            setupNativeAd("native_feature_2")
            requestAds()
        }
    }
}
