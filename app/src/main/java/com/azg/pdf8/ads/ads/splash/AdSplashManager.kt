package com.azg.pdf8.ads.ads.splash

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.control.admob.AppOpenManager
import com.ads.control.ads.AzAdCallback
import com.ads.control.ads.AzAds
import com.ads.control.ads.wrapper.ApAdError
import com.ads.control.billing.AppPurchase
import com.ads.control.funtion.AdCallback
import com.ads.control.helper.adnative.preload.NativeAdPreload
import com.ads.control.helper.adnative.preload.NativePreloadState
import com.azg.pdf8.ads.ads.interstitial.InterstitialAdManager
import com.azg.pdf8.ads.model.splash.SplashConfig
import com.azg.pdf8.ads.model.type.SplashType
import com.azg.pdf8.R
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.firebase.Analytics
import com.azg.pdf8.ui.splash.NativeSplashActivity
import com.google.android.gms.ads.AdError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

sealed class AdState {
    data object Idle : AdState()
    data object NavigateNext : AdState()
    data object NativeFullScr : AdState()
}

class AdSplashManager(
    private val listener: AdSplashCompleteListener,
) : DefaultLifecycleObserver {
    companion object {
        private const val TIME_DELAY = 3_000L
        const val TAG = "AdSplashManager"
    }

    private var _adState: MutableStateFlow<AdState> = MutableStateFlow(AdState.Idle)
    val adState: StateFlow<AdState> get() = _adState

    private var splashType: SplashType? = null
    private var splashAdList: MutableList<SplashConfig> = mutableListOf()
    private var isLoadedNativeFullScreen = false
    private var isFinishLoadAd = false

    private var timeoutJob: Job? = null
    private var isTimeOut: Boolean = false

    private var timeoutNativeJob: Job? = null
    private var isTimeoutNative = false

    private var timeDelayNativeJob: Job? = null
    private var isTimeDelayNative = false

    private var isAppOpenBackground = false
    private var isPause = false

    private var activityRef: WeakReference<AppCompatActivity>? = null

    fun bind(activity: AppCompatActivity) {
        activityRef = WeakReference(activity)
        activity.lifecycle.addObserver(this)
    }

    private fun getActivity(): AppCompatActivity? {
        return activityRef?.get()
    }


    /**
     * Callback for interstitial ads
     */
    private val interAdCallback by lazy {
        object : AzAdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                updateState(AdState.NavigateNext)
            }

            override fun onAdClosed() {
                super.onAdClosed()
                InterstitialAdManager.isCloseInterSplash.postValue(true)
                InterstitialAdManager.updateTimeShowInterAll()
            }

            override fun onAdFailedToShow(adError: ApAdError?) {
                super.onAdFailedToShow(adError)
                InterstitialAdManager.isCloseInterSplash.postValue(true)
            }
        }
    }

    /**
     * Callback for open app ads
     */
    private val openAdCallback by lazy {
        object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                if (isAppOpenBackground) return
                InterstitialAdManager.isCloseInterSplash.postValue(true)
                updateState(AdState.NavigateNext)
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                if (adError?.message?.contains("in foreground") == true) {
                    isAppOpenBackground = true
                }
            }
        }
    }

    fun showCurrentAd() {
        when (splashType) {
            SplashType.Inter -> showInterSplash()
            SplashType.AppOpen -> showOpenSplash()
            SplashType.Native -> showNativeFullScrSplash()
            else -> {
                InterstitialAdManager.isCloseInterSplash.postValue(true)
                updateState(AdState.NavigateNext)
            }
        }
    }

    fun loadAds() {
        if (remoteConfig.i101Config.enable
            && remoteConfig.isAdEnable
            && getActivity()?.isInternetAvailable() == true
            && !AppPurchase.getInstance().isPurchased
        ) {
            isTimeOut = false
            startLoadAdsTimeout()
            splashAdList = remoteConfig.i101Config.listAds.toMutableList()
            loadNextAdSplash(true)
        } else {
            splashType = null
            isFinishLoadAd = true
            onAdComplete()
        }
    }

    private fun loadNextAdSplash(isDelay: Boolean = false) {
        if (isTimeOut) return
        val adConfig = splashAdList.removeFirstOrNull() ?: return run {
            splashType = null
            isFinishLoadAd = true
            onAdComplete()
        }
        loadAdSplashConfig(adConfig, isDelay)
    }

    private fun loadAdSplashConfig(splashConfig: SplashConfig, isDelay: Boolean) {
        when (splashConfig.type) {
            SplashType.Inter.type -> loadInterSplash(splashConfig, isDelay)
            SplashType.AppOpen.type -> loadOpenSplash(splashConfig, isDelay)
            SplashType.Native.type -> loadNativeSplash(splashConfig, isDelay)
            else -> {
                loadNextAdSplash()
                Analytics.track("Ad Splash Type Not Valid: ${splashConfig.type}")
            }
        }
    }

    private fun loadInterSplash(splashConfig: SplashConfig, isDelay: Boolean) {
        if (splashConfig.enableAd) {
            AzAds.getInstance().loadSplashInterstitialAds(
                getActivity(),
                splashConfig.adUnit,
                splashConfig.timeout,
                if (isDelay) TIME_DELAY else 0,
                false,
                object : AzAdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        loadNextAdSplash()
                    }

                    override fun onAdSplashReady() {
                        super.onAdSplashReady()
                        if (isTimeOut) return
                        isFinishLoadAd = true
                        splashType = SplashType.Inter
                        timeoutJob?.cancel()
                        onAdComplete()
                    }
                }
            )
        } else {
            loadNextAdSplash()
        }
    }

    private fun loadOpenSplash(splashConfig: SplashConfig, isDelay: Boolean) {
        if (splashConfig.enableAd) {
            AppOpenManager.getInstance().setSplashAdId(splashConfig.adUnit)
            AppOpenManager.getInstance().loadOpenAppAdSplash(
                getActivity(),
                if (isDelay) TIME_DELAY else 0,
                splashConfig.timeout,
                object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        loadNextAdSplash()
                    }

                    override fun onAdSplashReady() {
                        super.onAdSplashReady()
                        if (isTimeOut) return
                        isFinishLoadAd = true
                        splashType = SplashType.AppOpen
                        timeoutJob?.cancel()
                        onAdComplete()
                    }
                }
            )
        } else {
            loadNextAdSplash()
        }
    }

    private fun loadNativeSplash(splashConfig: SplashConfig, isDelay: Boolean) {
        if (splashConfig.enableAd) {
            isTimeoutNative = false
            countTimeoutNative(splashConfig.timeout)
            if (isDelay) {
                countTimeDelayNative()
            } else {
                isTimeDelayNative = true
            }
            NativeSplashActivity.nativeAdSplash = splashConfig
            isLoadedNativeFullScreen = false
            getActivity()?.let {
                NativeAdPreload.getInstance().preload(
                    it,
                    splashConfig.adUnit,
                    R.layout.layout_native_full_screen,
                )
            }
            getActivity()?.lifecycleScope?.let {
                NativeAdPreload.getInstance().getAdPreloadState(
                    splashConfig.adUnit,
                    R.layout.layout_native_full_screen
                ).onEach {
                    when (it) {
                        is NativePreloadState.Consume -> {
                            isLoadedNativeFullScreen = true
                        }

                        is NativePreloadState.Complete -> {
                            if (isTimeOut || isTimeoutNative) return@onEach
                            timeoutNativeJob?.cancel()
                            if (isTimeDelayNative) onAdNativeComplete()
                        }

                        else -> Unit
                    }
                }.launchIn(it)
            }
        } else {
            loadNextAdSplash()
        }
    }

    private fun onAdNativeComplete() {
        if (!isLoadedNativeFullScreen) {
            loadNextAdSplash()
        } else {
            isFinishLoadAd = true
            splashType = SplashType.Native
            timeoutJob?.cancel()
            onAdComplete()
        }
    }

    private fun countTimeDelayNative() {
        timeDelayNativeJob?.cancel()
        timeDelayNativeJob = getActivity()?.lifecycleScope?.launch {
            delay(TIME_DELAY)
            if (isActive) handleTimeDelayNative()
        }
    }

    private fun handleTimeDelayNative() {
        isTimeDelayNative = true
        onAdNativeComplete()
    }

    private fun countTimeoutNative(timeout: Long) {
        timeoutNativeJob?.cancel()
        timeoutNativeJob = getActivity()?.lifecycleScope?.launch {
            delay(timeout)
            if (isActive) handleTimeOutNative()
        }
    }

    private fun handleTimeOutNative() {
        if (isTimeoutNative) return
        if (isLoadedNativeFullScreen) {
            isFinishLoadAd = true
            splashType = SplashType.Native
            timeoutJob?.cancel()
            onAdComplete()
        } else {
            loadNextAdSplash()
        }
        isTimeoutNative = true
    }

    private fun showInterSplash() {
        if (getActivity()?.isFinishing == true || getActivity()?.isDestroyed == true) return
        AzAds.getInstance().onShowSplash(getActivity(), interAdCallback)
    }

    private fun showOpenSplash() {
        if (getActivity()?.isFinishing == true || getActivity()?.isDestroyed == true) return
        AppOpenManager.getInstance().showAppOpenSplash(getActivity(), openAdCallback)
    }

    private fun showNativeFullScrSplash() {
        if (isPause) return
        InterstitialAdManager.isCloseInterSplash.postValue(true)
        updateState(AdState.NativeFullScr)
    }

    private fun startLoadAdsTimeout() {
        timeoutJob?.cancel()
        timeoutJob = getActivity()?.lifecycleScope?.launch {
            delay(remoteConfig.i101Config.totalTimeout)
            if (isActive) handleTimeOut()
        }
    }

    private fun handleTimeOut() {
        if (isTimeOut) return
        onAdComplete()
        isTimeOut = true
    }

    /**
     * Called when the activity is resumed
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isAppOpenBackground = false
        isPause = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        getActivity()?.lifecycle?.removeObserver(this)
        super.onDestroy(owner)
    }

    private fun updateState(state: AdState) {
        _adState.update { state }
    }

    fun onCheckShowAdsWhenFail() {
        when (splashType) {
            SplashType.Inter -> {
                AzAds.getInstance().onCheckShowSplashWhenFail(getActivity(), interAdCallback, 1000)
            }

            SplashType.AppOpen -> {
                AppOpenManager.getInstance()
                    .onCheckShowAppOpenSplashWhenFail(getActivity(), openAdCallback, 1000)
            }

            SplashType.Native -> {
                getActivity()?.lifecycleScope?.launch {
                    delay(1000)
                    InterstitialAdManager.isCloseInterSplash.postValue(true)
                    if (isLoadedNativeFullScreen) {
                        updateState(AdState.NativeFullScr)
                    } else {
                        updateState(AdState.NavigateNext)
                    }
                }
            }

            else -> {
                getActivity()?.lifecycleScope?.launch {
                    delay(1000)
                    InterstitialAdManager.isCloseInterSplash.postValue(true)
                    updateState(AdState.NavigateNext)
                }
            }
        }
    }

    private fun onAdComplete() {
        listener.onAdComplete(TAG)
    }
}