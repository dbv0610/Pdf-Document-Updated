package com.azg.pdf8.ads.ads.banner

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.control.funtion.AdCallback
import com.ads.control.helper.banner.BannerAdConfig
import com.ads.control.helper.banner.BannerAdHelper
import com.ads.control.helper.banner.params.BannerAdParam
import com.ads.control.util.AppConstant.CollapsibleGravity
import com.azg.pdf8.app.remoteConfig
import com.google.android.gms.ads.LoadAdError
import java.lang.ref.WeakReference

class BannerAdWrapper(
    private val activity: AppCompatActivity,
    private val config: BannerPlacement,
    private val lifecycleOwner: LifecycleOwner,
    private val adContainer: () -> FrameLayout,
) : DefaultLifecycleObserver {

    private var activityRef: WeakReference<AppCompatActivity> = WeakReference(activity)

    private val bannerAdHelper by lazy {
        BannerAdHelperFactory.create(
            activity = activity,
            placement = config,
            lifecycleOwner = lifecycleOwner
        )
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycleOwner.lifecycle.removeObserver(this)
        super.onDestroy(owner)
    }

    private fun cleanup() {
        bannerAdHelper.cancel()
        activityRef.clear()
    }

    fun setupBannerAd(tag: String? = null) {
        bannerAdHelper.apply {
            setBannerContentView(adContainer.invoke())
            tag?.let { setTagForDebug(it) }
        }
    }

    fun cancelRequest() {
        bannerAdHelper.cancel()
    }

    fun requestAds() {
        bannerAdHelper.requestAds(BannerAdParam.Request.create())
    }

    fun registerAdCallbacks(
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError?) -> Unit)? = null,
        onClicked: (() -> Unit)? = null,
        onImpression: (() -> Unit)? = null
    ) {
        bannerAdHelper.registerAdListener(object : AdCallback() {
            override fun onAdClicked() {
                super.onAdClicked()
                onClicked?.invoke()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                onImpression?.invoke()
            }

            override fun onBannerLoaded(adView: ViewGroup?) {
                super.onBannerLoaded(adView)
                onLoaded?.invoke()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                onFailed?.invoke(i)
            }
        })
    }

    fun setFlagReload(isReload: Boolean) {
        bannerAdHelper.flagUserEnableReload = isReload
    }
}

internal object BannerAdHelperFactory {
    fun create(
        activity: AppCompatActivity,
        placement: BannerPlacement,
        lifecycleOwner: LifecycleOwner? = null,
    ): BannerAdHelper {
        val canShowAds = placement.canShowAds()
        return BannerAdHelper(
            activity = activity,
            lifecycleOwner ?: activity,
            createBannerAdConfig(placement, canShowAds)
        ).apply {
            if (placement.listId().size >= 2) {
                setEnableHighFloor(true)
            }
        }
    }

    private fun createBannerAdConfig(
        placement: BannerPlacement,
        canShowAds: Boolean,
    ): BannerAdConfig {
        val listId = placement.listId()
        return if (listId.size >= 2) {
            BannerAdConfig(
                idAds = listId[0],
                canShowAds = canShowAds,
                canReloadAds = true,
            ).apply {
                setIdHighFloor(listId[1])
                if (placement.isCollapsible()) {
                    this.collapsibleGravity = CollapsibleGravity.BOTTOM
                }
            }
        } else {
            val idAd = if (listId.isEmpty()) "" else listId[0]
            BannerAdConfig(
                idAds = idAd,
                canShowAds = canShowAds,
                canReloadAds = true,
            ).apply {
                if (placement.isCollapsible()) {
                    this.collapsibleGravity = CollapsibleGravity.BOTTOM
                }
            }
        }
    }
}

enum class BannerPlacement(
    val listId: () -> List<String>,
    val canShowAds: () -> Boolean,
    val isCollapsible: () -> Boolean,
) {
    BANNER_ALL(
        listId = {
            remoteConfig.B100Config.listAds.filter { it.enableAd }.map { it.adUnit }
        },
        canShowAds = { remoteConfig.B100Config.enable  && remoteConfig.isAdEnable },
        isCollapsible = { remoteConfig.B100Config.isCollapsible },
    )
}