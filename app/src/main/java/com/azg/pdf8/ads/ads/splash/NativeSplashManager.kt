package com.azg.pdf8.ads.ads.splash

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.ActivitySplashBinding
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import com.facebook.shimmer.ShimmerFrameLayout


interface AdSplashCompleteListener {
    fun onAdComplete(adName: String)
}

class NativeSplashManager(
    private val activity: AppCompatActivity,
    private val showLayout: FrameLayout,
    private val shimmerFrameLayout: ShimmerFrameLayout,
    private val listener: AdSplashCompleteListener,

) : DefaultLifecycleObserver {
    companion object {
        const val TAG = "NativeSplashManager"
    }

    private var isCleanedUp = false
    private var isCallAdComplete = false

    init {
        activity.lifecycle.addObserver(this)
    }



    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = activity,
            config = NativePlacement.SPLASH,
            lifecycleOwner = activity,
            adContainer = { showLayout },
            shimmerView = { shimmerFrameLayout },
        )
    }

    fun loadNative() {
        if (remoteConfig.N101Config1.enable) {
            shimmerFrameLayout.visible()
            nativeAdsWrapper.apply {
                setupNativeAd(TAG)
                registerAdCallbacks(
                    onImpression = {
                        if (isCleanedUp) return@registerAdCallbacks
                        onAdComplete()
                    },
                    onFailed = {
                        if (isCleanedUp) return@registerAdCallbacks
                        onAdComplete()
                    },
                )
                requestAds()
            }
        } else {
            showLayout.gone()
            listener.onAdComplete(TAG)
        }
    }

    fun cleanup() {
        isCleanedUp = true
        activity.lifecycle.removeObserver(this)
    }

    private fun onAdComplete() {
        if (isCallAdComplete) return
        isCallAdComplete = true
        listener.onAdComplete(TAG)
    }
}