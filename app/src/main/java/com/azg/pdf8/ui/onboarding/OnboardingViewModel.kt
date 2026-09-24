package com.azg.pdf8.ui.onboarding

import androidx.lifecycle.ViewModel
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.firebase.Analytics

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
    fun trackScreenView(isFullScreen: Boolean, position: Int) {
        if (!isUfo()) return
        val trackingMap = if (isFullScreen) listTrackingFullScreen else listTrackingScreen
        if (trackingMap[position] == true) return
        trackingMap[position] = true
        val eventName = if (isFullScreen) {
            "ufo_native_full_screen_${position + 1}"
        } else {
            "ufo_onboarding_${position + 1}"
        }

        Analytics.track(eventName)
    }
}
