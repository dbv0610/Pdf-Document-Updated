package com.azg.pdf8.ui.onboarding

import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {
    var isPreloadNativeFull = false
    var isPreloadNativeFeature = false
    val listTrackingScreen = hashMapOf<Int, Boolean>(
        0 to false,
        1 to false,
        2 to false,
        3 to false
    )
    val listTrackingFullScreen = hashMapOf<Int, Boolean>(
        0 to false,
        1 to false,
    )
}
