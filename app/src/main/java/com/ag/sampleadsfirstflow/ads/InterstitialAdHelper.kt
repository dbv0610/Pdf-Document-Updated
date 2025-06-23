package com.ag.sampleadsfirstflow.ads

import androidx.lifecycle.MutableLiveData

object InterstitialAdHelper {
    var isCloseInterSplash = MutableLiveData(false)
    private var lastTimeInterstitialShow = 0L

    fun resetInterConfig() {
        lastTimeInterstitialShow = 0L
    }

    fun updateTimeShowInterSplash() {
        lastTimeInterstitialShow = System.currentTimeMillis()
    }
}
