package com.azg.pdf8.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.ads.control.admob.AdsConsentManager2
import com.ads.control.admob.AppOpenManager
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.az.inappupdate.AppUpdateManager
import com.azg.pdf8.ads.ads.interstitial.InterstitialAdManager
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.ads.ads.splash.AdSplashCompleteListener
import com.azg.pdf8.ads.ads.splash.AdSplashHelper
import com.azg.pdf8.ads.ads.splash.AdSplashManager
import com.azg.pdf8.ads.ads.splash.AdState
import com.azg.pdf8.ads.ads.splash.NativeSplashManager
import com.azg.pdf8.app.firstOpenApp
import com.azg.pdf8.app.isAppDebug
import com.azg.pdf8.app.isFinishFirstFlow
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivitySplashBinding
import com.azg.pdf8.firebase.Analytics
import com.azg.pdf8.ui.language.LanguageWaitingActivity
import com.azg.pdf8.ui.main.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate),
    AdSplashCompleteListener {
    override fun backPressed() = Unit
    override fun initialize() = Unit

    override fun ActivitySplashBinding.onClick() = Unit

    private var isNextAction = false
    private var isAcceptUmp = true
    private var adSplashManager: AdSplashManager? = null
    private var nativeSplashManager: NativeSplashManager? = null
    private var isFinishAdSplash = false
    private var isFinishAdNative = false
    private var jobAdSplash: Job? = null
    private var jobNativeSplash: Job? = null

    private fun observeAds() {
        lifecycleScope.launch {
            adSplashManager?.adState?.collectLatest {
                when (it) {
                    AdState.Idle -> Unit
                    AdState.NavigateNext -> navigateToNextScreen()
                    AdState.NativeFullScr -> navigateToNativeFullScreen()
                }
            }
        }
    }

    private fun setupAdManager() {
        nativeSplashManager = NativeSplashManager(
            this,
            binding.flNativeAd,
            if (remoteConfig.n101Config1.layout.contains("small"))
                binding.shimmerAdSmall.shimmerContainerNative
            else binding.shimmerAdMedium.shimmerContainerNative,
            this@SplashActivity
        )
        adSplashManager = AdSplashManager(this@SplashActivity)
        adSplashManager?.bind(this)
        AdSplashHelper.adManager = adSplashManager
    }

    private suspend fun requestUmp() {
        val consentManager = AdsConsentManager2(this@SplashActivity)
        consentManager.requestUMP()
        isAcceptUmp = consentManager.getCanRequestAd()
        if (!isAcceptUmp) finish()
    }

    private suspend fun initAds() {
        InterstitialAdManager.resetInterAllConfig()
        val manager = AppOpenManager.getInstance()
        remoteConfig.a001Config
            .takeIf { it.enable && isEnableAds() }
            ?.listAds
            ?.let { ads ->
                ads.getOrNull(if (ads.size > 1) 1 else 0)
                    ?.takeIf { it.enableAd }
                    ?.let { manager.init(application, it.adUnit) }
                if (ads.size > 1) {
                    ads.firstOrNull()
                        ?.takeIf { it.enableAd }
                        ?.also { manager.appResumeAdIdHf = it.adUnit }
                }
            }
        InterstitialAdManager.isCloseInterSplash.postValue(false)
        if (isEnableAds()) {
            setupAdManager()
            nativeSplashManager?.loadNative()
            adSplashManager?.loadAds()
            observeAds()
            if (!isFinishFirstFlow) {
                NativeAdPreloadManager.preloadAd(
                    this,
                    NativePlacement.LANGUAGE_1,
                    2,
                    isForce = true
                )
            } else {
                if (!isGrantedPermission()) {
                    NativeAdPreloadManager.preloadAd(
                        this@SplashActivity,
                        NativePlacement.PERMISSION,
                        1,
                        false
                    )
                }
            }
        } else {
            InterstitialAdManager.isCloseInterSplash.postValue(true)
            kotlinx.coroutines.delay(3000)
            navigateToNextScreen()
        }
    }

    private fun isEnableAds(): Boolean {
        return remoteConfig.isAdEnable && isInternetAvailable() && isAcceptUmp
    }

    private fun navigateToNextScreen() {
        if (isNextAction) return
        isNextAction = true
        if (isFinishFirstFlow) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            launchActivity<LanguageWaitingActivity>()
        }

        finish()
    }

    private fun navigateToNativeFullScreen() {
        startActivity(Intent(this, NativeSplashActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        nativeSplashManager?.cleanup()
        super.onDestroy()
    }

    override fun onAdComplete(adName: String) {
        when (adName) {
            AdSplashManager.TAG -> {
                AdSplashHelper.isLoadAdsFullDone = true
                isFinishAdSplash = true
                if (remoteConfig.newFlowApp){
                    navigateToNextScreen()
                }else{
                    if (isFinishAdNative) {
                        adSplashManager?.showCurrentAd()
                    } else {
                        handleWaitingAdSplash()
                    }
                }
            }
            NativeSplashManager.TAG -> {
                handleDelayAdNative()
            }
        }
    }

    private fun handleWaitingAdSplash() {
        jobAdSplash?.cancel()
        jobAdSplash = lifecycleScope.launch {
            kotlinx.coroutines.delay(remoteConfig.timeOutSplash.waitingMs)
            adSplashManager?.showCurrentAd()
            jobNativeSplash?.cancel()
        }
    }

    private fun handleDelayAdNative() {
        jobNativeSplash?.cancel()
        jobNativeSplash = lifecycleScope.launch {
            kotlinx.coroutines.delay(remoteConfig.timeOutSplash.delayMs)
            isFinishAdNative = true
            if (isFinishAdSplash) {
                adSplashManager?.showCurrentAd()
                jobAdSplash?.cancel()
            }
        }
    }

    private var isFirstResume = true
    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        if (remoteConfig.n101Config1.enable && isEnableAds()) {
            if (isFinishAdSplash && isFinishAdNative) {
                adSplashManager?.onCheckShowAdsWhenFail()
            }
        }
    }

    override fun ActivitySplashBinding.setData() {
        lifecycleScope.launch {
            awaitAll(
                async { requestUmp() },
                async { withTimeoutOrNull(30000) { remoteConfig.setupRemoteConfig(isAppDebug) } }
            )
            initAds()
            setupInAppUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstOpenApp += 1
        if (isUfo()) {
            Analytics.track("ufo_splash")
        }
    }

    private fun setupInAppUpdate() {
        AppUpdateManager.getInstance(this@SplashActivity).setupUpdate(
            remoteConfig.inAppUpdate,
            remoteConfig.timesShowUpdate,
        )
    }
}
