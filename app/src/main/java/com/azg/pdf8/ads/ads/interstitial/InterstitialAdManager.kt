package com.azg.pdf8.ads.ads.interstitial

import android.content.Context
import androidx.lifecycle.MutableLiveData

object InterstitialAdManager {
    var isCloseInterSplash = MutableLiveData(false)
    private var interAll = InterstitialAdsWrapper(InterstitialPlacement.INTER_ALL)

    fun resetInterAllConfig() {
        interAll.resetConfig()
    }

    fun updateTimeShowInterAll() {
        interAll.updateTimeShowInter()
    }

    fun loadInterAll(context: Context) {
        interAll.preloadAd(context)
    }

    fun showInterAll(context: Context, onNextAction: () -> Unit) {
        interAll.showAd(context, true, onNextAction)
    }
}
