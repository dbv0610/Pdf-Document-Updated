package com.azg.pdf8.ads.ads.splash

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.ActivitySplashBinding
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible

interface AdSplashCompleteListener {
    fun onAdComplete(adName: String)
}

class NativeSplashManager(
    private val activity: AppCompatActivity,
    private val binding: ActivitySplashBinding,
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

    fun loadNative() {
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