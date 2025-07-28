package com.azg.pdf8.ads.model

import com.azg.pdf8.ads.model.type.LayoutNativeType
import com.azg.pdf8.app.isAppDebug
import com.google.gson.annotations.SerializedName

data class AdNativeConfig(
    @SerializedName("enable")
    var enable: Boolean,
    @SerializedName("type_layout")
    val layout: String,
    @SerializedName("list_ads")
    val listAds: List<AdConfig>,
) {
    companion object {
        val N101_Config1 = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = false,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/8372351949"
                ),
            )
        )
        val N103_Config1 = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/4193433782"
                ),
            )
        )
        val N104_Config1 = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/1567270447"
                ),
            )
        )
        val  N105_Config1 = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/5730054347"
                ),
            )
        )
        val N107_Config1 = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.Other.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/7573619056"
                ),
            )
        )

        val N108_Config1 = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.Other.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/7573619056"
                ),
            )
        )


        val N109_Config1  = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug)  "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/5346733359"
                ),
            )
        )

        val N110_Config1  = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeSmallCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug)  "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/3103891002"
                ),
            )
        )
        val N110_Config2  = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeSmallCtaRight.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug)  "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-5417263955398589/3103891002"
                ),
            )
        )


    }
}