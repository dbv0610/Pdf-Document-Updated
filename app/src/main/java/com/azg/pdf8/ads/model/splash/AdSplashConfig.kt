package com.azg.pdf8.ads.model.splash

import com.azg.pdf8.ads.model.type.SplashType
import com.azg.pdf8.app.isAppDebug
import com.google.gson.annotations.SerializedName

data class AdSplashConfig(
    @SerializedName("enable")
    val enable: Boolean,
    @SerializedName("total_timeout_ms")
    val totalTimeout: Long,
    @SerializedName("list_ads")
    val listAds: List<SplashConfig>,
) {
    companion object {
        val defaultSplash = AdSplashConfig(
            enable = true,
            totalTimeout = 45000,
            listAds = listOf(
                SplashConfig(
                    enableAd = true,
                    type = SplashType.Inter.type,
                    timeout = 30000,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/1033173712"  else "ca-app-pub-6745384043882937/4077233343"
                ),
            )
        )
    }
}