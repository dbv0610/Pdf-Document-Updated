package com.ag.sampleadsfirstflow.ads.native

import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.control.ads.AzAdCallback
import com.ads.control.ads.AzAds
import com.ads.control.ads.wrapper.ApAdError
import com.ads.control.ads.wrapper.ApNativeAd
import com.ads.control.helper.AdOptionVisibility
import com.ads.control.helper.adnative.NativeAdConfig
import com.ads.control.helper.adnative.NativeAdHelper
import com.ads.control.helper.adnative.params.AdNativeMediation
import com.ads.control.helper.adnative.params.AdNativeState
import com.ads.control.helper.adnative.params.NativeAdParam
import com.ads.control.helper.adnative.params.NativeLayoutMediation
import com.ads.control.helper.adnative.preload.NativePreloadState
import com.ag.sampleadsfirstflow.BuildConfig
import com.ag.sampleadsfirstflow.R
import com.ag.sampleadsfirstflow.remoteconfig.remoteAds
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

/**
 * A wrapper for handling native ads, providing an interface for loading,
 * displaying, and managing ad states with proper lifecycle management and memory optimization.
 */
class NativeAdsWrapper(
    activity: AppCompatActivity,
    private val config: NativePlacement,
    private val lifecycleOwner: LifecycleOwner,
    private val adContainer: () -> FrameLayout,
    private val shimmerView: () -> ShimmerFrameLayout
) : DefaultLifecycleObserver {
    private var activityRef: WeakReference<AppCompatActivity> = WeakReference(activity)

    private val nativeAdHelper: NativeAdHelper by lazy {
        NativeAdHelperFactory.create(
            activity = activity,
            placement = config,
            lifecycleOwner = lifecycleOwner
        )
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cleanup()
        super.onDestroy(owner)
    }

    private fun cleanup() {
        nativeAdHelper.cancel()
        activityRef.clear()
    }

    /**
     * Get the number of available ads for a specific placement
     * @return The number of available ads
     */
    fun getAvailableAdCount(): Int = NativeAdPreloadManager.getAvailableAdCount(config)

    /**
     * Get the ad preload state flow for monitoring loading status
     * @return StateFlow emitting the current preload state
     */
    fun getAdPreloadState(): StateFlow<NativePreloadState> =
        NativeAdPreloadManager.getAdPreloadState(config)

    /**
     * Setup the native ad by configuring the container and shimmer views
     * @param tag Optional debug tag for the ad
     */
    fun setupNativeAd(tag: String? = null) {
        nativeAdHelper.apply {
            setNativeContentView(adContainer.invoke())
            setShimmerLayoutView(shimmerView.invoke())
            tag?.let { setTagForDebug(it) }
            activityRef.get()?.let { activity ->
                if (config.displayLayoutId != null) {
                    setCustomContentView { nativeAd ->
                        nativeAd.layoutCustomNative = config.displayLayoutId ?: config.preloadLayoutId
                        AzAds.getInstance().populateNativeAdView(
                            activity,
                            nativeAd,
                            adContainer(),
                            shimmerView()
                        )
                    }
                }
            }
        }
    }

    fun setFlagReload(isReload: Boolean) {
        nativeAdHelper.flagUserEnableReload = isReload
    }

    /**
     * Register callbacks for ad events
     *
     * @param onLoaded Called when an ad is loaded successfully
     * @param onFailed Called when ad loading fails
     * @param onClicked Called when the ad is clicked
     * @param onImpression Called when the ad makes an impression
     */
    fun registerAdCallbacks(
        onLoaded: ((ApNativeAd) -> Unit)? = null,
        onFailed: ((ApAdError?) -> Unit)? = null,
        onClicked: (() -> Unit)? = null,
        onImpression: (() -> Unit)? = null
    ) {
        nativeAdHelper.registerAdListener(object : AzAdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                onLoaded?.invoke(nativeAd)
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                super.onAdFailedToLoad(adError)
                onFailed?.invoke(adError)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                onClicked?.invoke()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                onImpression?.invoke()
            }
        })
    }

    /**
     * Get the ad state flow to monitor loading status
     * @return Flow emitting the current ad state
     */
    fun getAdState(): Flow<AdNativeState> = nativeAdHelper.getAdNativeState()

    /**
     * Cancel the current ad request
     */
    fun cancelRequest() {
        nativeAdHelper.cancel()
    }

    /**
     * Request ads using the configured parameters
     */
    fun requestAds() = nativeAdHelper.requestAds(NativeAdParam.Request.create())

    /**
     * Preload ads for this wrapper's placement
     * @param buffer Number of ads to preload (default: 1)
     */
    fun preloadAd(buffer: Int = 1) {
        activityRef.get()?.let { activity ->
            NativeAdPreloadManager.preloadAd(activity, config, buffer)
        }
    }

    /**
     * Preload ads for this wrapper's placement
     * @param buffer Number of ads to preload (default: 1)
     * @param isForce if true, preload ads even if there are available ads in the buffer
     */
    fun preloadAd(buffer: Int = 1, isForce: Boolean = false) {
        activityRef.get()?.let { activity ->
            NativeAdPreloadManager.preloadAd(activity, config, buffer, isForce)
        }
    }
}

/**
 * Factory for creating NativeAdHelper instances with optimized configuration
 */
internal object NativeAdHelperFactory {
    /**
     * Creates a NativeAdHelper instance configured for the specified placement
     */
    fun create(
        activity: AppCompatActivity,
        placement: NativePlacement,
        lifecycleOwner: LifecycleOwner? = null,
    ): NativeAdHelper {
        val canShowAds = placement.canShowAds()

        return NativeAdHelper(
            activity,
            lifecycleOwner ?: activity,
            createNativeAdConfig(placement, canShowAds, placement.preloadLayoutId)
        ).apply {
            setEnableListNative(true)
            setEnablePreload(true)
            adVisibility = placement.adVisibility
        }
    }

    /**
     * Creates the ad configuration for the placement
     */
    private fun createNativeAdConfig(
        placement: NativePlacement,
        canShowAds: Boolean,
        finalLayoutId: Int,
    ): NativeAdConfig {
        return NativeAdConfig(
            idAds = placement.listId.lastOrNull() ?: "",
            canShowAds = canShowAds,
            canReloadAds = true,
            layoutId = finalLayoutId,
        ).apply {
            setListId(placement.listId)
            setLayoutMediation(
                NativeLayoutMediation(AdNativeMediation.FACEBOOK, placement.layoutMeta())
            )
        }
    }
}

/**
 * Enum defining different ad placements with their configurations
 */
enum class NativePlacement(
    val listId: List<String>,
    val canShowAds: () -> Boolean,
    val adVisibility: AdOptionVisibility = AdOptionVisibility.GONE,
    @LayoutRes val preloadLayoutId: Int,
    @LayoutRes var displayLayoutId: Int? = null,
    val layoutMeta: () -> Int,
) {
    LANGUAGE_1(
        listId = listOfNotNull(
            BuildConfig.N002.takeIf { remoteAds.isShowN002 },
            BuildConfig.N001.takeIf { remoteAds.isShowN001 },
        ),
        canShowAds = { remoteAds.isShowN001 },
        preloadLayoutId = R.layout.layout_native_common,
        layoutMeta = { LayoutSelector.getMetaLayout() },
    ),

    LANGUAGE_2(
        listId = listOfNotNull(
            BuildConfig.N004.takeIf { remoteAds.isShowN004 },
            BuildConfig.N003.takeIf { remoteAds.isShowN003 },
        ),
        canShowAds = { remoteAds.isShowN003 },
        preloadLayoutId = R.layout.layout_native_common,
        displayLayoutId = R.layout.layout_native_common_meta_low,
        layoutMeta = { LayoutSelector.getMetaLayout() }
    ),

    ONBOARDING(
        listId = listOfNotNull(
            BuildConfig.N006.takeIf { remoteAds.isShowN006 },
            BuildConfig.N005.takeIf { remoteAds.isShowN005 },
        ),
        canShowAds = { remoteAds.isShowN005 },
        adVisibility = AdOptionVisibility.INVISIBLE,
        preloadLayoutId = R.layout.layout_native_common_meta_low,
        layoutMeta = { LayoutSelector.getMetaLayout() }
    ),

    ONBOARDING_FULL(
        listId = listOfNotNull(
            BuildConfig.N008.takeIf { remoteAds.isShowN008 },
            BuildConfig.N007.takeIf { remoteAds.isShowN007 },
        ),
        canShowAds = { remoteAds.isShowN007 },
        preloadLayoutId = R.layout.layout_native_full_screen_onb,
        layoutMeta = { LayoutSelector.getMetaLayout(isFullScreen = true) }
    ),

    FEATURE(
        listId = listOfNotNull(
            BuildConfig.N012.takeIf { remoteAds.isShowN012 },
            BuildConfig.N011.takeIf { remoteAds.isShowN011 },
        ),
        canShowAds = { remoteAds.isShowN011 },
        preloadLayoutId = R.layout.layout_native_common_meta_high,
        layoutMeta = { LayoutSelector.getMetaLayout() }
    );

    /**
     * Helper object for layout selection with memory-efficient caching
     */
    private object LayoutSelector {
        private var cachedFullScreenLayout: Int = 0
        private var cachedCommonLayout: Int = 0

        /**
         * Returns the appropriate meta layout based on CTR
         * @param isFullScreen Whether to return a full screen layout
         * @return Layout resource ID
         */
        fun getMetaLayout(isFullScreen: Boolean = false): Int {
            return if (isFullScreen) {
                if (cachedFullScreenLayout == 0) {
                    cachedFullScreenLayout = if (remoteAds.metaCtrLow) {
                        R.layout.layout_native_full_screen_meta_low
                    } else {
                        R.layout.layout_native_full_screen_meta_high
                    }
                }
                cachedFullScreenLayout
            } else {
                if (cachedCommonLayout == 0) {
                    cachedCommonLayout = if (remoteAds.metaCtrLow) {
                        R.layout.layout_native_common_meta_low
                    } else {
                        R.layout.layout_native_common_meta_high
                    }
                }
                cachedCommonLayout
            }
        }
    }
}