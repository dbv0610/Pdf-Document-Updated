package com.ag.sampleadsfirstflow.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LanguageItem(
    var code: String,
    var name: String,
    var flagId: Int,
    var isChoose: Boolean = false,
    var isDefault: Boolean = false,
): Parcelable
