package com.ag.sampleadsfirstflow.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class FeatureScreenType : Parcelable {
    @Parcelize
    data object Feature1 : FeatureScreenType()

    @Parcelize
    data object Feature2 : FeatureScreenType()
}
