package com.azg.pdf8.ads.model

import com.azg.pdf8.app.isAppDebug
import com.google.gson.annotations.SerializedName

data class AppOpenConfig(
    @SerializedName("enable")
    var enable: Boolean,
    @SerializedName("list_ads")
    val listAds: List<AdConfig>,
) {
    companion object {
        val defaultAll = AppOpenConfig(
            enable = true,
            listAds = listOf(
                AdConfig(
                    enableAd = false,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/9257395921" else "ca-app-pub-6745384043882937/9426149986"
                ),  AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/9257395921" else "ca-app-pub-6745384043882937/2895179462"
                ),
            )
        )
    }
}