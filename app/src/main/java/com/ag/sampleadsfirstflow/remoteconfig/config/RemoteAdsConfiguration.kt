package com.ag.sampleadsfirstflow.remoteconfig.config

import com.ag.sampleadsfirstflow.remoteconfig.BaseRemoteConfiguration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class RemoteAdsConfiguration private constructor() : BaseRemoteConfiguration() {
    override fun getPreferencesName(): String {
        return PREFS_NAME
    }

    override fun sync(remoteConfig: FirebaseRemoteConfig) {
        remoteConfig.saveToLocal(AdsEnable)
        remoteConfig.saveToLocal(A001)
        remoteConfig.saveToLocal(A002)

        remoteConfig.saveToLocal(B001)
        remoteConfig.saveToLocal(B002)

        remoteConfig.saveToLocal(I001)
        remoteConfig.saveToLocal(I002)

        remoteConfig.saveToLocal(N001)
        remoteConfig.saveToLocal(N002)
        remoteConfig.saveToLocal(N003)
        remoteConfig.saveToLocal(N004)
        remoteConfig.saveToLocal(N005)
        remoteConfig.saveToLocal(N006)
        remoteConfig.saveToLocal(N007)
        remoteConfig.saveToLocal(N008)
        remoteConfig.saveToLocal(N009)
        remoteConfig.saveToLocal(N010)
        remoteConfig.saveToLocal(N011)
        remoteConfig.saveToLocal(N012)
        remoteConfig.saveToLocal(N013)
        remoteConfig.saveToLocal(N014)
        remoteConfig.saveToLocal(N015)
        remoteConfig.saveToLocal(N016)

        remoteConfig.saveToLocal(MetaCtrLow)
    }

    private data object AdsEnable : RemoteKeys.BooleanKey("ad_enable", true)

    private data object A001 : RemoteKeys.BooleanKey("A001", true)
    private data object A002 : RemoteKeys.BooleanKey("A002", false)

    private data object B001 : RemoteKeys.BooleanKey("B001", true)
    private data object B002 : RemoteKeys.BooleanKey("B002", true)

    private data object I001 : RemoteKeys.BooleanKey("I001", true)
    private data object I002 : RemoteKeys.BooleanKey("I002", false)

    private data object N001 : RemoteKeys.BooleanKey("N001", true)
    private data object N002 : RemoteKeys.BooleanKey("N002", false)
    private data object N003 : RemoteKeys.BooleanKey("N003", true)
    private data object N004 : RemoteKeys.BooleanKey("N004", false)
    private data object N005 : RemoteKeys.BooleanKey("N005", true)
    private data object N006 : RemoteKeys.BooleanKey("N006", false)
    private data object N007 : RemoteKeys.BooleanKey("N007", true)
    private data object N008 : RemoteKeys.BooleanKey("N008", false)
    private data object N009 : RemoteKeys.BooleanKey("N009", true)
    private data object N010 : RemoteKeys.BooleanKey("N010", false)
    private data object N011 : RemoteKeys.BooleanKey("N011", true)
    private data object N012 : RemoteKeys.BooleanKey("N012", false)
    private data object N013 : RemoteKeys.BooleanKey("N013", true)
    private data object N014 : RemoteKeys.BooleanKey("N014", false)
    private data object N015 : RemoteKeys.BooleanKey("N015", true)
    private data object N016 : RemoteKeys.BooleanKey("N016", false)

    private data object MetaCtrLow : RemoteKeys.BooleanKey("meta_ctr_low", true)

    val isAdsEnable: Boolean get() = AdsEnable.get()
    val isShowA001: Boolean get() = A001.get() && isAdsEnable
    val isShowA002: Boolean get() = A002.get() && isAdsEnable

    val isShowB001: Boolean get() = B001.get() && isAdsEnable
    val isShowB002: Boolean get() = B002.get() && isAdsEnable

    val isShowI001: Boolean get() = I001.get() && isAdsEnable
    val isShowI002: Boolean get() = I002.get() && isAdsEnable

    val isShowN001: Boolean get() = N001.get() && isAdsEnable
    val isShowN002: Boolean get() = N002.get() && isAdsEnable
    val isShowN003: Boolean get() = N003.get() && isAdsEnable
    val isShowN004: Boolean get() = N004.get() && isAdsEnable
    val isShowN005: Boolean get() = N005.get() && isAdsEnable
    val isShowN006: Boolean get() = N006.get() && isAdsEnable
    val isShowN007: Boolean get() = N007.get() && isAdsEnable
    val isShowN008: Boolean get() = N008.get() && isAdsEnable
    val isShowN009: Boolean get() = N009.get() && isAdsEnable
    val isShowN010: Boolean get() = N010.get() && isAdsEnable
    val isShowN011: Boolean get() = N011.get() && isAdsEnable
    val isShowN012: Boolean get() = N012.get() && isAdsEnable
    val isShowN013: Boolean get() = N013.get() && isAdsEnable
    val isShowN014: Boolean get() = N014.get() && isAdsEnable
    val isShowN015: Boolean get() = N015.get() && isAdsEnable
    val isShowN016: Boolean get() = N016.get() && isAdsEnable

    val metaCtrLow: Boolean get() = MetaCtrLow.get()

    companion object {
        private const val PREFS_NAME = "remote_config_ads_prefs"

        @Volatile
        private var _instance: RemoteAdsConfiguration? = null

        fun getInstance(): RemoteAdsConfiguration =
            _instance ?: synchronized(this) {
                _instance ?: RemoteAdsConfiguration().also { _instance = it }
            }
    }
}
