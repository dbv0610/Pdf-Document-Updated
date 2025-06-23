package com.azg.pdf8.ui.feature

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class FeatureModel(
    val icon: Int,
    val packageName: String,
    val name: String,
    var isSelected: Boolean = false,
)

sealed class FeatureScreenType : Parcelable {
    @Parcelize
    data object Feature1 : FeatureScreenType()
    @Parcelize
    data object Feature2 : FeatureScreenType()
}
