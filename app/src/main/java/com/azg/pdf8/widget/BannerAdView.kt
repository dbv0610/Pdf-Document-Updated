package com.azg.pdf8.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.azg.pdf8.ads.ads.banner.BannerAdWrapper
import com.azg.pdf8.ads.ads.banner.BannerPlacement
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.databinding.LayoutBannerAdBinding
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible

class BannerAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), DefaultLifecycleObserver {
    private val binding = LayoutBannerAdBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private var attachActivity: AppCompatActivity? = null
    private var bannerAdsWrapper: BannerAdWrapper? = null

    fun setBannerPlacement(
        lifecycleOwner: AppCompatActivity,
        placement: BannerPlacement
    ): BannerAdView {
        attachActivity = lifecycleOwner
        lifecycleOwner.lifecycle.addObserver(this)
        bannerAdsWrapper = BannerAdWrapper(
            activity = lifecycleOwner,
            config = placement,
            lifecycleOwner = lifecycleOwner,
            adContainer = { binding.flBanner }
        )
        return this
    }

    override fun onResume(owner: LifecycleOwner) {
        attachActivity?.let { activity ->
            if (!activity.isInternetAvailable()) {
                cancelRequest()
                this@BannerAdView.gone()
            }
        }
    }

    fun requestBanner() {
        bannerAdsWrapper?.setupBannerAd()
        bannerAdsWrapper?.requestAds()
    }

    fun cancelRequest() = bannerAdsWrapper?.cancelRequest()

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelRequest()
        attachActivity?.lifecycle?.removeObserver(this)
    }
}
