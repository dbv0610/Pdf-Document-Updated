package com.azg.pdf8.ads.model

import com.azg.pdf8.ads.model.type.LayoutNativeType
import com.azg.pdf8.app.isAppDebug
import com.google.gson.annotations.SerializedName

data class AdNativeConfig(
    @SerializedName("enable")
    val enable: Boolean,
    @SerializedName("type_layout")
    val layout: String,
    @SerializedName("list_ads")
    val listAds: List<AdConfig>,
) {
    companion object {
        val N101_NativeSplash = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = false,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/7276760688"
                ),
                AdConfig(
                    enableAd = true ,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/4202755227"
                ),
            )
        )
        val N103_NativeLanguage = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/1451070007"
                ),
            )
        )

        val N104_NativeLanguageDup = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/2165385877"
                ),
            )
        )
        val  N105_NativeOnboarding = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/7245488017"
                ),
            )
        )
        val N107_1_ObFull = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.Other.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/8000166482"
                ),
            )
        )

        val N108_1_ObFull = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.Other.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit = if(isAppDebug) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/8000166482"
                ),
            )
        )


        val N109_NativeProfile  = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug)  "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/2889673557"
                ),
            )
        )

        val N120_NativeHome  = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeSmallCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug)  "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/2552398683"
                ),
            )
        )

        val N121_NativeAll  = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeMediumCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug)  "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/9587847800"
                ),
            )
        )

        val N122_NativePermission  = AdNativeConfig(
            enable = true,
            layout = LayoutNativeType.NativeSmallCtaBottom.type,
            listAds = listOf(
                AdConfig(
                    enableAd = true,
                    adUnit =  if(isAppDebug)  "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-6745384043882937/9587847800"
                ),
            )
        )

    }
}