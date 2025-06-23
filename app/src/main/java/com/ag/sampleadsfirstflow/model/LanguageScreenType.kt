package com.ag.sampleadsfirstflow.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LanguageScreenType : Parcelable {
    sealed class Lfo : LanguageScreenType() {
        @Parcelize
        data object Lfo1 : Lfo()

        @Parcelize
        data object Lfo2 : Lfo()
    }
}
