package com.ag.sampleadsfirstflow.ads

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.ads.control.helper.banner.BannerAdConfig
import com.ads.control.helper.banner.BannerAdHelper

fun AppCompatActivity.bannerAdProvider(
    idAdNormal: String,
    idAdHf: String? = null,
    isShowNormal: Boolean,
    lifecycleOwner: LifecycleOwner? = null,
    collapGravity: String? = null,
    isShowHf: Boolean = false,
): BannerAdHelper {
    return BannerAdHelper(
        this,
        lifecycleOwner ?: this,
        BannerAdConfig(
            idAdNormal,
            isShowNormal,
            true
        ).apply {
            if (isShowHf && !idAdHf.isNullOrEmpty()) {
                setIdHighFloor(idAdHf)
            }
            collapGravity?.let {
                this.collapsibleGravity = it
            }
        }
    ).apply {
        setEnableHighFloor(isShowHf)
    }
}