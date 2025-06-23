package com.ag.sampleadsfirstflow.ads.native

import android.app.Activity
import com.ads.control.ads.wrapper.ApNativeAd
import com.ads.control.helper.adnative.preload.NativeAdPreload
import com.ads.control.helper.adnative.preload.NativePreloadState
import com.ag.sampleadsfirstflow.BuildConfig
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

/**
 * Manager class for handling native ad preloading operations with optimized memory management
 */
internal object NativeAdPreloadManager {
    private val preloadInstance: NativeAdPreload by lazy {
        NativeAdPreload.getInstance()
    }

    // Cache for activity references to prevent memory leaks
    private var activityRef: WeakReference<Activity>? = null

    /**
     * Get the number of available ads for a specific placement
     * @return The number of available ads in the buffer
     */
    fun getAvailableAdCount(placement: NativePlacement): Int {
        return if (placement.canShowAds()) {
            preloadInstance.getNativeAdBuffer(
                listId = placement.listId,
                layoutId = placement.preloadLayoutId
            ).size
        } else {
            0
        }
    }

    /**
     * Get the ad preload state flow for monitoring loading status
     * @return StateFlow emitting the current preload state
     */
    fun getAdPreloadState(placement: NativePlacement): StateFlow<NativePreloadState> {
        return preloadInstance.getAdPreloadState(
            listId = placement.listId,
            layoutId = placement.preloadLayoutId
        )
    }

    /**
     * Poll an ad from the preload buffer
     * @return ApNativeAd if available, null otherwise
     */
    fun pollAdNative(placement: NativePlacement): ApNativeAd? {
        if (!placement.canShowAds()) return null
        return preloadInstance.pollAdNative(
            listId = placement.listId,
            layoutId = placement.preloadLayoutId
        )
    }

    /**
     * Preload ads for a specific placement with memory-safe activity reference
     * @param isForce if true, preload ads even if there are available ads in the buffer
     */
    fun preloadAd(
        activity: Activity,
        placement: NativePlacement,
        buffer: Int = 1,
        isForce: Boolean = false
    ) {
        if (!placement.canShowAds()) return
        activityRef = WeakReference(activity)
        if (isForce) {
            handleInternalPreload(
                placement = placement,
                buffer = buffer
            )
        } else {
            handleSafeInternalPreload(placement, buffer)
        }
    }

    /**
     * Checks if preloading of native ads can start for the specified placement.
     *
     * Preloading will only proceed when ALL of the following conditions are met:
     * 1. The app is not running in debug mode (`BuildConfig.build_debug == false`).
     * 2. The result of [isPreloadAvailable] is false.
     *
     * @param placement the [NativePlacement] to preload ads for.
     * @param buffer the number of ads to buffer in advance.
     */
    private fun handleSafeInternalPreload(
        placement: NativePlacement,
        buffer: Int
    ) {
        val isDebug = BuildConfig.build_debug

        val canPreload = this.canPreload(placement = placement)
        if (!isDebug && !canPreload) return

        handleInternalPreload(
            placement = placement,
            buffer = buffer
        )
    }

    /**
     * @return true if we should preload a native ad for this placement
     * (i.e. no preload in progress and the preload queue is empty).
     */
    private fun canPreload(
        placement: NativePlacement
    ): Boolean {
        return preloadInstance.isPreloadAvailable(
            listId = placement.listId,
            layoutId = placement.preloadLayoutId
        ).not()
    }

    private fun handleInternalPreload(
        placement: NativePlacement,
        buffer: Int
    ) {
        activityRef?.get()?.let { activity ->
            preloadInstance.preload(
                activity = activity,
                listId = placement.listId,
                layoutId = placement.preloadLayoutId,
                buffer = buffer
            )
        }
    }

    /**
     * Clear any cached references and resources
     */
    fun cleanup() {
        activityRef?.clear()
        activityRef = null
    }
}
