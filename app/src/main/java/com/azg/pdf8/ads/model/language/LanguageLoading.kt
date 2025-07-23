package com.azg.pdf8.ads.model.language

import com.google.gson.annotations.SerializedName

data class LanguageLoading(
    @SerializedName("enable_screen")
    val enableScreen: Boolean,
    @SerializedName("show_time_ms")
    val showTimeMs: Long,
) {
    companion object {
        fun default(): LanguageLoading {
            return LanguageLoading(
                enableScreen = true,
                showTimeMs = 2000,
            )
        }
    }
}