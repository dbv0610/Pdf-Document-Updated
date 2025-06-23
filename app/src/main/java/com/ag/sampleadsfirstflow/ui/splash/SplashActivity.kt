package com.ag.sampleadsfirstflow.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ads.control.admob.AdsConsentManager2
import com.ads.control.admob.AppOpenManager
import com.ads.control.helper.banner.params.BannerAdParam
import com.ag.sampleadsfirstflow.BuildConfig
import com.ag.sampleadsfirstflow.ads.InterstitialAdHelper
import com.ag.sampleadsfirstflow.ads.bannerAdProvider
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.ag.sampleadsfirstflow.ads.native.NativePlacement
import com.ag.sampleadsfirstflow.base.BaseActivity
import com.ag.sampleadsfirstflow.databinding.ActivitySplashBinding
import com.ag.sampleadsfirstflow.model.LanguageScreenType
import com.ag.sampleadsfirstflow.remoteconfig.RemoteInitializer
import com.ag.sampleadsfirstflow.remoteconfig.analytics.Analytics
import com.ag.sampleadsfirstflow.remoteconfig.remoteAds
import com.ag.sampleadsfirstflow.remoteconfig.remoteLogic
import com.ag.sampleadsfirstflow.ui.home.MainActivity
import com.ag.sampleadsfirstflow.ui.language.LanguageActivity
import com.ag.sampleadsfirstflow.ui.splash.navigationads.AdsState
import com.ag.sampleadsfirstflow.ui.splash.navigationads.SplashNavigationAdsWrapper
import com.ag.sampleadsfirstflow.utils.Language.listLanguage
import com.ag.sampleadsfirstflow.utils.extensions.isInternetAvailable
import com.az.inappupdate.AppUpdateManager
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private var isNextAction = false
    private var isAcceptUmp = true

    private val bannerAdHelper by lazy {
        bannerAdProvider(
            idAdNormal = BuildConfig.B001,
            idAdHf = BuildConfig.B002,
            isShowNormal = remoteAds.isShowB001,
            isShowHf = remoteAds.isShowB002,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceHelper.countSessionApp += 1
        if (preferenceHelper.isUfo()) {
            Analytics.track("ufo_splash")
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun isDisplayCutout(): Boolean = true


    private var splashNavigationAdsWrapper: SplashNavigationAdsWrapper? = null
    override fun updateUI(savedInstanceState: Bundle?) {
        splashNavigationAdsWrapper = SplashNavigationAdsWrapper(this@SplashActivity)

        lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { requestUmp() },
                async { kotlinx.coroutines.withTimeoutOrNull(30000) { RemoteInitializer.setupRemoteConfig() } }
            )
            initAds()
            setupInAppUpdate()
            splashNavigationAdsWrapper?.adsState?.collectLatest {
                Log.d("TAG", "updateUI: $it")
                when (it) {
                    AdsState.Idle -> Unit
                    AdsState.NavigateNext -> navigateToNextScreen()
                }
            }
        }
    }

    private suspend fun requestUmp() {
        val consentManager = AdsConsentManager2(this@SplashActivity)
        consentManager.requestUMP()
        isAcceptUmp = consentManager.getCanRequestAd()
        if (!isAcceptUmp) finish()
    }

    private fun setupInAppUpdate() {
        AppUpdateManager.getInstance(this@SplashActivity).setupUpdate(
            remoteLogic.inAppUpdate,
            remoteLogic.timesShowUpdate,
        )
    }

    private suspend fun initAds() {
        InterstitialAdHelper.resetInterConfig()
        if (remoteAds.isShowA001 && isAcceptUmp) {
            AppOpenManager.getInstance().init(application, BuildConfig.A001)
            if (remoteAds.isShowA002) AppOpenManager.getInstance().appResumeAdIdHf =
                BuildConfig.A002
        }
        InterstitialAdHelper.isCloseInterSplash.postValue(false)
        if (remoteAds.isAdsEnable && isInternetAvailable() && isAcceptUmp) {
            loadBannerSplash()
            splashNavigationAdsWrapper?.loadAds()
            if (!preferenceHelper.isFinishFirstFlow) {
                NativeAdPreloadManager.preloadAd(this, NativePlacement.LANGUAGE_1)
                NativeAdPreloadManager.preloadAd(this, NativePlacement.LANGUAGE_2)
            }
        } else {
            InterstitialAdHelper.isCloseInterSplash.postValue(true)
            delay(3000)
            navigateToNextScreen()
        }
    }

    private fun loadBannerSplash() {
        binding.flBanner.isVisible = remoteAds.isShowB001
        bannerAdHelper.setBannerContentView(binding.flBanner)
        bannerAdHelper.requestAds(BannerAdParam.Request.create())
    }

    private fun navigateToNextScreen() {
        if (isNextAction) return
        isNextAction = true
        if (preferenceHelper.isFinishFirstFlow) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            listLanguage.forEach {
                it.isDefault = false
                it.isChoose = false
            }
            LanguageActivity.start(this, LanguageScreenType.Lfo.Lfo1)
        }
        finish()
    }
}
