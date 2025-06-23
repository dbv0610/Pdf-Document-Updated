package com.ag.sampleadsfirstflow.ui.onboarding

import androidx.lifecycle.ViewModel

class OnboardingViewModel: ViewModel() {
    var isPreloadNativeFull = false
    var isPreloadNativeFeature = false
    val listTrackingScreen = HashMap<Int, Boolean>()
    val listTrackingFullScreen = HashMap<Int, Boolean>()
}
