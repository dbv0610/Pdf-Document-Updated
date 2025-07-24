package com.azg.pdf8.ads.ads.native

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.control.ads.AzAdCallback
import com.ads.control.ads.AzAds
import com.ads.control.ads.wrapper.ApAdError
import com.ads.control.ads.wrapper.ApNativeAd
import com.ads.control.config.AzAdConfig
import com.ads.control.helper.AdOptionVisibility
import com.ads.control.helper.adnative.NativeAdConfig
import com.ads.control.helper.adnative.NativeAdHelper
import com.ads.control.helper.adnative.params.AdNativeMediation
import com.ads.control.helper.adnative.params.AdNativeState
import com.ads.control.helper.adnative.params.NativeAdParam
import com.ads.control.helper.adnative.params.NativeLayoutMediation
import com.ads.control.helper.adnative.preload.NativeAdPreload
import com.ads.control.helper.adnative.preload.NativePreloadState
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.R
import com.azg.pdf8.ads.model.type.LayoutNativeType
import com.azg.pdf8.app.remoteConfig
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.nativead.NativeAdView
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
                        nativeAd.layoutCustomNative =
                            config.displayLayoutId ?: config.preloadLayoutId()
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

    fun setupNativeAd(tag: String? = null, layoutId: Int, block: NativeAdView.() -> Unit = {}) {
        nativeAdHelper.apply {
            setNativeContentView(adContainer.invoke())
            setShimmerLayoutView(shimmerView.invoke())
            tag?.let { setTagForDebug(it) }
            activityRef.get()?.let { activity ->
                setCustomContentView { nativeAd ->
                    nativeAd.layoutCustomNative = layoutId
                    populateNativeAdView(
                        activity,
                        nativeAd,
                        adContainer(),
                        shimmerView(), block
                    )
                }
            }
        }
    }

    private fun populateUnifiedNativeAdView(
        nativeAd: com.google.android.gms.ads.nativead.NativeAd,
        adView: NativeAdView, block: NativeAdView.() -> Unit = {}
    ) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.body?.let {
            (adView.bodyView as TextView).apply {
                visibility = View.VISIBLE
                text = it
            }
        } ?: run { adView.bodyView?.visibility = View.GONE }

        nativeAd.callToAction?.let {
            (adView.callToActionView as Button).apply {
                visibility = View.VISIBLE
                text = it
            }
        } ?: run { adView.callToActionView?.visibility = View.GONE }

        nativeAd.icon?.let {
            (adView.iconView as ImageView).apply {
                visibility = View.VISIBLE
                setImageDrawable(it.drawable)
            }
        } ?: run { adView.iconView?.visibility = View.GONE }
        adView.setNativeAd(nativeAd)
        adView.block()
    }
    @SuppressLint("InflateParams")
    fun populateNativeAdView(
        activity: Activity,
        apNativeAd: ApNativeAd,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout, block: NativeAdView.() -> Unit = {}
    ) {
        if (apNativeAd.admobNativeAd == null && apNativeAd.nativeView == null) {
            containerShimmerLoading.visibility = View.GONE
            Log.e("AzAds", "populateNativeAdView failed : native is not loaded ")
            return
        }
        val mediationProvider = AzAds.getInstance().mediationProvider
        when (mediationProvider) {
            AzAdConfig.PROVIDER_ADMOB -> {
                val adView = LayoutInflater.from(activity)
                    .inflate(apNativeAd.layoutCustomNative, null) as NativeAdView

                containerShimmerLoading.stopShimmer()
                containerShimmerLoading.visibility = View.GONE
                adPlaceHolder.visibility = View.VISIBLE
                apNativeAd.admobNativeAd?.let { admobAd ->
                    populateUnifiedNativeAdView(
                        admobAd,
                        adView,
                    ) { block() }
                }
                adPlaceHolder.removeAllViews()
                adPlaceHolder.addView(adView)
            }
            AzAdConfig.PROVIDER_MAX -> {
                containerShimmerLoading.stopShimmer()
                containerShimmerLoading.visibility = View.GONE
                adPlaceHolder.visibility = View.VISIBLE

                adPlaceHolder.removeAllViews()
                apNativeAd.nativeView?.parent
                    ?.let { (it as? ViewGroup)?.removeAllViews() }
                apNativeAd.nativeView?.let { adPlaceHolder.addView(it) }
            }
            else -> {
                Log.w("AzAds", "Unknown mediation provider: $mediationProvider")
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

    fun requestAdsFragment() {
        nativeAdHelper.nativeAd?.let {
            nativeAdHelper.requestAds(NativeAdParam.Ready(it))
        } ?: run {
            nativeAdHelper.requestAds(NativeAdParam.Request.create())
        }
    }

    fun pollAdNative(): ApNativeAd? {
        if (!config.canShowAds()) return null
        return NativeAdPreload.getInstance().pollAdNative(
            listId = config.listId(),
            layoutId = config.preloadLayoutId()
        )
    }
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
            createNativeAdConfig(placement, canShowAds, placement.preloadLayoutId())
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
            idAds = placement.listId().lastOrNull() ?: "",
            canShowAds = canShowAds,
            canReloadAds = true,
            layoutId = finalLayoutId,
        ).apply {
            setListId(placement.listId())
            if (placement.layoutMeta != null) {
                setLayoutMediation(
                    NativeLayoutMediation(AdNativeMediation.FACEBOOK, placement.layoutMeta.invoke())
                )
            }
        }
    }
}
/**
 * Enum defining different ad placements with their configurations
 */
enum class NativePlacement(
    val listId: () -> List<String>,
    val canShowAds: () -> Boolean,
    val adVisibility: AdOptionVisibility = AdOptionVisibility.GONE,
    @LayoutRes val preloadLayoutId: () -> Int,
    @LayoutRes var displayLayoutId: Int? = null,
    val layoutMeta: (() -> Int)? = null,
) {
    SPLASH(
        listId = {
            remoteConfig.n101Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.n101Config1.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            LayoutSelector.getLayout(remoteConfig.n101Config1.layout)
        },
        layoutMeta = { LayoutSelector.getMetaLayout(type = remoteConfig.n101Config1.layout) },
    ),

    LANGUAGE_1(
        listId = {
            remoteConfig.n103Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.n103Config1.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            LayoutSelector.getLayout(remoteConfig.n103Config1.layout)
        },
        layoutMeta = { LayoutSelector.getMetaLayout(type = remoteConfig.n103Config1.layout) },
    ),
    LANGUAGE_2(
        listId = {
            remoteConfig.n104Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.n104Config1.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            LayoutSelector.getLayout(remoteConfig.n104Config1.layout)
        },
        layoutMeta = { LayoutSelector.getMetaLayout(type = remoteConfig.n104Config1.layout) },
    ),

    ONBOARDING(
        listId = {
            remoteConfig.n105Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.n105Config1.enable && remoteConfig.isAdEnable },
        adVisibility = AdOptionVisibility.INVISIBLE,
        preloadLayoutId = {
            LayoutSelector.getLayout(remoteConfig.n105Config1.layout)
        },
        layoutMeta = { LayoutSelector.getMetaLayout(type = remoteConfig.n105Config1.layout) },
    ),

    ONBOARDING_FULL_1(
        listId = {
            remoteConfig.N107Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.N107Config1.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            R.layout.layout_native_full_screen
        },
        layoutMeta = { LayoutSelector.getMetaLayout(isFullScreen = true) },
    ),

    ONBOARDING_FULL_2(
        listId = {
            remoteConfig.n108Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.N107Config1.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            R.layout.layout_native_full_screen
        },
        layoutMeta = { LayoutSelector.getMetaLayout(isFullScreen = true) },
    ),

    FEATURE(
        listId = {
            remoteConfig.n109Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.n109Config1.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            LayoutSelector.getLayout(remoteConfig.n109Config1.layout)
        },
        layoutMeta = { LayoutSelector.getMetaLayout(type = remoteConfig.n109Config1.layout) },
    ),

    PERMISSION(
        listId = {
            remoteConfig.n110Config1.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.n110Config1.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            LayoutSelector.getLayout(remoteConfig.n110Config1.layout)
        }
    ),
    NATIVE_DOC(
        listId = {
            remoteConfig.n110Config2.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.n110Config2.enable && remoteConfig.isAdEnable },
        preloadLayoutId = {
            LayoutSelector.getLayout(remoteConfig.n110Config2.layout)
        },
    ),
    ;
    /**
     * Helper object for layout selection with memory-efficient caching
     */
}

object LayoutSelector {
    private var cachedFullScreenLayout: Int = 0
    private var cachedCommonLayout: Int = 0
    /**
     * Returns the appropriate meta layout based on CTR
     * @param isFullScreen Whether to return a full screen layout
     * @return Layout resource ID
     */
    fun getMetaLayout(isFullScreen: Boolean = false, type: String = ""): Int {
        return if (isFullScreen) {
            if (remoteConfig.metaCtrLow) {
                R.layout.layout_native_full_screen_meta_low
            } else {
                R.layout.layout_native_full_screen_meta_high
            }
        } else {
            if (remoteConfig.metaCtrLow) {
                when (type) {
                    LayoutNativeType.NativeSmallCtaBottom.type -> R.layout.layout_native_small_meta_cta_bot_low
                    LayoutNativeType.NativeSmallCtaTop.type -> R.layout.layout_native_small_meta_cta_top_low
                    LayoutNativeType.NativeSmallCtaRight.type -> R.layout.layout_native_small_cta_right
                    LayoutNativeType.NativeMediumCtaBottom.type -> R.layout.layout_native_medium_meta_cta_bot_low
                    LayoutNativeType.NativeMediumCtaTop.type -> R.layout.layout_native_medium_meta_cta_top_low
                    LayoutNativeType.MediumCtaRightBottom.type -> R.layout.layout_native_medium_cta_bot_right_meta
                    LayoutNativeType.MediumCtaRightTop.type -> R.layout.layout_native_medium_cta_top_right_meta
                    else -> R.layout.layout_native_small_meta_cta_bot_low
                }
            } else {
                when (type) {
                    LayoutNativeType.NativeSmallCtaBottom.type -> R.layout.layout_native_small_meta_cta_bot_high
                    LayoutNativeType.NativeSmallCtaTop.type -> R.layout.layout_native_small_meta_cta_top_high
                    LayoutNativeType.NativeSmallCtaRight.type -> R.layout.layout_native_small_cta_right
                    LayoutNativeType.NativeMediumCtaBottom.type -> R.layout.layout_native_medium_meta_cta_bot_high
                    LayoutNativeType.NativeMediumCtaTop.type -> R.layout.layout_native_medium_meta_cta_top_high
                    LayoutNativeType.MediumCtaRightBottom.type -> R.layout.layout_native_medium_cta_bot_right_meta
                    LayoutNativeType.MediumCtaRightTop.type -> R.layout.layout_native_medium_cta_top_right_meta
                    else -> R.layout.layout_native_small_meta_cta_bot_high
                }
            }
        }
    }

    fun getLayout(type: String): Int {
        return when (type) {
            LayoutNativeType.NativeSmallCtaBottom.type -> LayoutNativeType.NativeSmallCtaBottom.resLayout
            LayoutNativeType.NativeSmallCtaTop.type -> LayoutNativeType.NativeSmallCtaTop.resLayout
            LayoutNativeType.NativeSmallCtaRight.type -> LayoutNativeType.NativeSmallCtaRight.resLayout
            LayoutNativeType.NativeMediumCtaBottom.type -> LayoutNativeType.NativeMediumCtaBottom.resLayout
            LayoutNativeType.NativeMediumCtaTop.type -> LayoutNativeType.NativeMediumCtaTop.resLayout
            LayoutNativeType.MediumCtaRightBottom.type -> LayoutNativeType.MediumCtaRightBottom.resLayout
            LayoutNativeType.MediumCtaRightTop.type -> LayoutNativeType.MediumCtaRightTop.resLayout
            else -> LayoutNativeType.NativeSmallCtaBottom.resLayout
        }
    }
}