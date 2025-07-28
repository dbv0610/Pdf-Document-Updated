package com.azg.pdf8.ads.model

import com.google.gson.annotations.SerializedName

data class AdConfig(
    @SerializedName("enable_ad")
    val enableAd: Boolean,
    @SerializedName("adunit")
    val adUnit: String,
)