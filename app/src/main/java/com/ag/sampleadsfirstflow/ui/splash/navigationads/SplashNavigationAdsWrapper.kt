package com.ag.sampleadsfirstflow.ui.splash.navigationads

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.control.admob.AppOpenManager
import com.ads.control.ads.AzAdCallback
import com.ads.control.ads.AzAds
import com.ads.control.ads.wrapper.ApAdError
import com.ads.control.funtion.AdCallback
import com.ag.sampleadsfirstflow.BuildConfig
import com.ag.sampleadsfirstflow.ads.InterstitialAdHelper
import com.ag.sampleadsfirstflow.remoteconfig.remoteAds
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

sealed class AdsState {
    data object Idle: AdsState()

    data object NavigateNext: AdsState()
}

class SplashNavigationAdsWrapper(
    private val activity: AppCompatActivity
): DefaultLifecycleObserver {

    init {
        activity.lifecycle.addObserver(this)
    }
    companion object {
        const val TIME_OUT = 30_000L
        const val TIME_DELAY = 3000L
    }

    private var _adsState: MutableStateFlow<AdsState> = MutableStateFlow(AdsState.Idle)
    val adsState: StateFlow<AdsState> get() = _adsState

    private val azAdCallback by lazy {
        object : AzAdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                // TODO: navigateToNextScreen()
                _adsState.update { AdsState.NavigateNext }
            }

            override fun onAdClosed() {
                super.onAdClosed()
                InterstitialAdHelper.isCloseInterSplash.postValue(true)
                InterstitialAdHelper.updateTimeShowInterSplash()
            }

            override fun onAdFailedToShow(adError: ApAdError?) {
                super.onAdFailedToShow(adError)
                InterstitialAdHelper.isCloseInterSplash.postValue(true)
            }
        }
    }

    fun loadAds() {
        loadInterSplash()
    }

    private fun loadInterSplash() {
        if (remoteAds.isShowI001) {
            if (remoteAds.isShowI002) {
                loadInterSplashHf()
            } else {
                loadInterSplashNormal()
            }
        } else {
            InterstitialAdHelper.isCloseInterSplash.postValue(true)
            // TODO: navigateToNextScreen()
            _adsState.update { AdsState.NavigateNext }
        }
    }

    private fun loadInterSplashHf() {
        AzAds.getInstance().loadSplashInterPriorityAlternate(
            activity,
            BuildConfig.I002,
            BuildConfig.I001,
            TIME_OUT,
            TIME_DELAY,
            false,
            object : AzAdCallback() {
                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    AzAds.getInstance().onShowSplashPriority(activity, azAdCallback)
                }

                override fun onNextAction() {
                    super.onNextAction()
                    InterstitialAdHelper.isCloseInterSplash.postValue(true)
                    // TODO: navigateToNextScreen()
                    _adsState.update { AdsState.NavigateNext }
                }
            }
        )
    }

    private fun loadInterSplashNormal() {
        AzAds.getInstance().loadSplashInterstitialAds(
            activity,
            BuildConfig.I001,
            TIME_OUT,
            TIME_DELAY,
            false,
            object : AzAdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    InterstitialAdHelper.isCloseInterSplash.postValue(true)
                    // TODO: navigateToNextScreen()
                    _adsState.update { AdsState.NavigateNext }

                }

                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    AzAds.getInstance().onShowSplash(activity, azAdCallback)
                }
            }
        )
    }

    private val openAdCallback by lazy {
        object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                InterstitialAdHelper.isCloseInterSplash.postValue(true)
                _adsState.update { AdsState.NavigateNext }
            }
        }
    }

    private fun loadOpenSplashHf() {
        AppOpenManager.getInstance().setSplashAdId(BuildConfig.A001)
        if (remoteAds.isShowA002) {
            AppOpenManager.getInstance().setSplashAdIdHf(BuildConfig.A002)
        }
        AppOpenManager.getInstance().loadOpenAppAdSplash(
            activity,
            TIME_DELAY,
            TIME_OUT,
            object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    _adsState.update { AdsState.NavigateNext }
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    _adsState.update { AdsState.NavigateNext }
                }

                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    AppOpenManager.getInstance().showAppOpenSplash(
                        activity,
                        openAdCallback
                    )
                }
            }
        )
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (!remoteAds.isShowI001 && !remoteAds.isShowA001) return
        if (remoteAds.isShowI002) {
            AzAds.getInstance().onCheckShowSplashPriorityWhenFail(activity, azAdCallback, 1000)
        } else {
            AzAds.getInstance().onCheckShowSplashWhenFail(activity, azAdCallback, 1000)
        }

        if (remoteAds.isShowA001) {
            AppOpenManager.getInstance()
                .onCheckShowAppOpenSplashWhenFail(activity, openAdCallback, 1000)
        }

    }

    override fun onDestroy(owner: LifecycleOwner) {
        activity.lifecycle.removeObserver(this)
        super.onDestroy(owner)
    }
}