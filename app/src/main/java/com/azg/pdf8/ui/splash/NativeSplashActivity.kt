package com.azg.pdf8.ui.splash

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.control.ads.AzAdCallback
import com.ads.control.ads.wrapper.ApAdError
import com.ads.control.helper.adnative.NativeAdConfig
import com.ads.control.helper.adnative.NativeAdHelper
import com.ads.control.helper.adnative.params.AdNativeMediation
import com.ads.control.helper.adnative.params.NativeAdParam
import com.ads.control.helper.adnative.params.NativeLayoutMediation
import com.azg.pdf8.R
import com.azg.pdf8.ads.model.native_full.NativeFullConfig
import com.azg.pdf8.ads.model.splash.SplashConfig
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityNativeFullScreenBinding
import com.dong.baselib.widget.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class NativeSplashActivity :
    BaseActivity<ActivityNativeFullScreenBinding>(ActivityNativeFullScreenBinding::inflate) {
    companion object {
        var nativeAdSplash: SplashConfig? = null
    }

    private var job: Job? = null
    private var jobLoading: Job? = null
    private var isFinishLoading = false
    private val nativeAdHelper by lazy {
        nativeAdSplash?.let {
            nativeAdProvider(
                adUnit = it.adUnit,
                isShowAd = remoteConfig.isAdEnable,
                layout = R.layout.layout_native_full_screen,
                layoutMeta = if (remoteConfig.metaCtrLow) R.layout.layout_native_full_screen_meta_low else R.layout.layout_native_full_screen_meta_high,
            )
        }
    }

    private fun loadAds() {
        nativeAdHelper
            ?.setNativeContentView(binding.flNativeAd)
            ?.setShimmerLayoutView(binding.adContent.shimmerContainerNative)
        nativeAdHelper?.requestAds(NativeAdParam.Request.create())
        nativeAdHelper?.registerAdListener(object : AzAdCallback() {
            override fun onAdImpression() {
                super.onAdImpression()
                initListener()
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                super.onAdFailedToLoad(adError)
                navigateToNextScreen()
            }
        })
        initListener()
    }

    private fun initListener() {
        runCatching {
            binding.flNativeAd.findViewById<View>(R.id.btnNext)?.let {
                it.setOnClickListener {
                    navigateToNextScreen()
                }
            }
            val btnSkip = binding.flNativeAd.findViewById<View>(R.id.tvSkip)
            btnSkip.postDelayed({
                btnSkip.isVisible = NativeFullConfig.defaultSplash().isShowClose
            }, NativeFullConfig.defaultSplash().delayShowClose)
            btnSkip?.let {
                it.setOnClickListener {
                    navigateToNextScreen()
                }
            }
        }
    }

    private fun navigateToNextScreen() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        countDownSkip()
    }

    private fun countDownSkip() {
        job?.cancel()
        val remoteDelay = NativeFullConfig.defaultSplash().timeDelaySkip
        val delayTime = if (isFinishLoading) {
            (remoteDelay - 1_000).coerceAtLeast(0L)
        } else {
            remoteDelay
        }

        if (delayTime == 0L) return
        job = lifecycleScope.launch {
            delay(delayTime)
            if (isActive) {
                navigateToNextScreen()
            }
        }
    }

    private fun countDownLoading() {
        isFinishLoading = false
        jobLoading?.cancel()
        jobLoading = lifecycleScope.launch {
            delay(1_000)
            if (isActive) {
                isFinishLoading = true
                binding.flNativeAd.visible()
                loadAds()
            }
        }
    }

    override fun backPressed() = Unit

    override fun initialize() {
        countDownLoading()
    }

    override fun ActivityNativeFullScreenBinding.setData() = Unit
    override fun ActivityNativeFullScreenBinding.onClick() = Unit

    override fun onPause() {
        super.onPause()
        job?.cancel()
    }
}

fun AppCompatActivity.nativeAdProvider(
    adUnit: String,
    isShowAd: Boolean,
    @LayoutRes layout: Int,
    lifecycleOwner: LifecycleOwner? = null,
    @LayoutRes layoutMeta: Int? = null,
): NativeAdHelper {
    val config = NativeAdConfig(
        adUnit,
        isShowAd,
        true,
        layout,
    )
    layoutMeta?.let {
        config.setLayoutMediation(
            NativeLayoutMediation(
                AdNativeMediation.FACEBOOK, it
            )
        )
    }
    return NativeAdHelper(
        this,
        lifecycleOwner ?: this,
        config
    ).apply {
        setEnablePreload(true)
    }
}