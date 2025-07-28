package com.azg.pdf8.ads.model

import com.azg.pdf8.app.isAppDebug
import com.google.gson.annotations.SerializedName

data class AdInterConfig(
    @SerializedName("enable")
    var enable: Boolean,
    @SerializedName("time_interval_ms")
    val timeInterval: Long,
    @SerializedName("time_steps")
    val timeSteps: List<Int>,
    @SerializedName("list_ads")
    val listAds: List<AdConfig>,
) {
    companion object {
        val I102_InterHome = AdInterConfig(
            enable = true,
               timeInterval = 30000,
            timeSteps = listOf(0),
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if (isAppDebug) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-5417263955398589/8363956545"
                )
            )
        )
    }
}