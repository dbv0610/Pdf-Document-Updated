package com.azg.pdf8.ui.feature

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class FeatureModel(
    var index: Int = 0,
    val name: Int,
    var isSelected: Boolean = false,
)

sealed class FeatureScreenType : Parcelable {
    @Parcelize
    data object Feature1 : FeatureScreenType()
    @Parcelize
    data object Feature2 : FeatureScreenType()
}
