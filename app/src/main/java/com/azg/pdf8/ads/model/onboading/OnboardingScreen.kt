package com.azg.pdf8.ads.model.onboading

import com.google.gson.annotations.SerializedName

data class OnboardingScreen(
    @SerializedName("screen_1")
    val isEnableScreen1: Boolean,
    @SerializedName("screen_2")
    val isEnableScreen2: Boolean,
    @SerializedName("screen_3")
    val isEnableScreen3: Boolean,
    @SerializedName("screen_4")
    val isEnableScreen4: Boolean,
) {
    companion object {
        val defaultConfig = OnboardingScreen(
            isEnableScreen1 = true,
            isEnableScreen2 = true,
            isEnableScreen3 = true,
            isEnableScreen4 = true,
        )
    }
}