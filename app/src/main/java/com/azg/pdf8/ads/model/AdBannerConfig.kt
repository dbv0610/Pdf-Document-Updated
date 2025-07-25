package com.azg.pdf8.ads.model

import com.azg.pdf8.app.isAppDebug
import com.google.gson.annotations.SerializedName

data class AdBannerConfig(
    @SerializedName("enable")
    var enable: Boolean,
    @SerializedName("is_collapsible")
    val isCollapsible: Boolean,
    @SerializedName("list_ads")
    val listAds: List<AdConfig>,
) {
    companion object {
        val defaultAll = AdBannerConfig(
            enable = true,
            isCollapsible = false,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5417263955398589/7245266242"
                )
            )
        )
    }
}