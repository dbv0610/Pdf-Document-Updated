package com.azg.pdf8.ads.ads.interstitial

import android.content.Context
import android.util.Log
import com.ads.control.ads.AzAdCallback
import com.ads.control.ads.AzAds
import com.ads.control.ads.wrapper.ApAdError
import com.ads.control.ads.wrapper.ApInterstitialAd
import com.ads.control.billing.AppPurchase
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.app.remoteConfig

class InterstitialAdsWrapper(
    private val config: InterstitialPlacement,
) {
    private var lastTimeInterstitialShow = 0L
    private var isRequestAds = false
    private var interAd: ApInterstitialAd? = null
    private var currentStepInter = 1
    private var interstitialAdList: MutableList<String> = mutableListOf()
    private val showType by lazy {
        when {
            config.timeInterval() > 0 -> InterShowType.INTERVAL
            else -> InterShowType.NONE
        }
    }

    private fun isAdsReady(): Boolean {
        return interAd?.isReady == true
    }

    fun resetConfig() {
        lastTimeInterstitialShow = 0
        currentStepInter = 1
    }

    fun updateTimeShowInter() {
        lastTimeInterstitialShow = System.currentTimeMillis()
    }

    fun preloadAd(context: Context) {
        if (!isRequestAds
            && !isAdsReady()
            && context.isInternetAvailable()
            && config.canShowAds()
            && !AppPurchase.getInstance().isPurchased
        ) {
            interstitialAdList = config.listId().toMutableList()
            isRequestAds = true
            loadNextAdInter(context)
        }
    }

    private fun loadNextAdInter(context: Context) {
        val adUnitId = interstitialAdList.removeFirstOrNull() ?: return run {
            isRequestAds = false
        }
        AzAds.getInstance().getInterstitialAds(
            context,
            adUnitId,
            object : AzAdCallback() {
                override fun onInterstitialLoad(interstitialAd: ApInterstitialAd?) {
                    super.onInterstitialLoad(interstitialAd)
                    interAd = interstitialAd
                    isRequestAds = false
                }

                override fun onAdFailedToLoad(adError: ApAdError?) {
                    super.onAdFailedToLoad(adError)
                    loadNextAdInter(context)
                }
            }
        )
    }

    fun showAd(
        context: Context,
        shouldReloadAds: Boolean = false,
        onNextAction: () -> Unit = {},
    ) {
        when (showType) {
            InterShowType.INTERVAL -> {
                showInterInterval(context, shouldReloadAds, onNextAction)
            }

            InterShowType.NONE -> {
                showInterNone(context, shouldReloadAds, onNextAction)
            }
        }
    }

    private fun showInterInterval(
        context: Context,
        shouldReloadAds: Boolean = false,
        onNextAction: () -> Unit = {},
    ) {
        if (canShowAdsInterval() || canShowAdsStepInterval()) {
            AzAds.getInstance().forceShowInterstitial(
                context,
                interAd,
                object : AzAdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        onNextAction()
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        lastTimeInterstitialShow = System.currentTimeMillis()
                        interAd = null
                        if (shouldReloadAds) {
                            preloadAd(context)
                        }
                    }

                    override fun onAdFailedToShow(adError: ApAdError?) {
                        super.onAdFailedToShow(adError)
                        Log.d("ErrorAdInter","Ads onAdFailedToShow: ${adError?.message}")
                    }

                    override fun onAdPriorityFailedToShow(adError: ApAdError?) {
                        super.onAdPriorityFailedToShow(adError)
                        Log.d("ErrorAdInter","Ads onAdPriorityFailedToShow: ${adError?.message}")
                    }
                },
                false,
            )
        } else {
            onNextAction()
        }
        currentStepInter++
    }


    private fun showInterNone(
        context: Context,
        shouldReloadAds: Boolean = false,
        onNextAction: () -> Unit = {},
    ) {
        if (canShowAdsStep()) {
            AzAds.getInstance().forceShowInterstitial(
                context,
                interAd,
                object : AzAdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        onNextAction()
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        lastTimeInterstitialShow = System.currentTimeMillis()
                        interAd = null
                        if (shouldReloadAds) {
                            preloadAd(context)
                        }
                    }
                },
                shouldReloadAds,
            )
        } else {
            onNextAction()
        }
        currentStepInter++
    }

    private fun canShowAdsInterval(): Boolean {
        val timeInterval = System.currentTimeMillis() - lastTimeInterstitialShow
        return timeInterval > config.timeInterval()
                && isAdsReady()
    }

    private fun canShowAdsStepInterval(): Boolean {
        return if (config.timeSteps().isEmpty()) {
            false
        } else {
            config.timeSteps().contains(currentStepInter) && isAdsReady()
        }
    }

    private fun canShowAdsStep(): Boolean {
        return if (config.timeSteps().isEmpty() && isAdsReady()) {
            true
        } else {
            config.timeSteps().contains(currentStepInter) && isAdsReady()
        }
    }
}

enum class InterShowType {
    INTERVAL,
    NONE
}

enum class InterstitialPlacement(
    val listId: () -> List<String>,
    val canShowAds: () -> Boolean,
    val timeInterval: () -> Long,
    val timeSteps: () -> List<Int>,
) {
    INTER_ALL(
        listId = {
            remoteConfig.I102Config.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.I102Config.enable && remoteConfig.isAdEnable },
        timeInterval = { remoteConfig.I102Config.timeInterval },
        timeSteps = { remoteConfig.I102Config.timeSteps },
    ),
    ;
}