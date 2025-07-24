package com.azg.pdf8.app

import android.util.Log
import com.azg.pdf8.ads.model.native_full.NativeFullConfig
import com.azg.pdf8.ads.model.onboading.OnboardingScreen
import com.azg.pdf8.ads.model.AdNativeConfig
import com.azg.pdf8.BuildConfig
import com.azg.pdf8.ads.model.AdBannerConfig
import com.azg.pdf8.ads.model.AdInterConfig
import com.azg.pdf8.ads.model.AppOpenConfig
import com.azg.pdf8.ads.model.splash.AdSplashConfig
import com.azg.pdf8.ads.model.splash.SplashTimeout
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import kotlin.jvm.java

class RemoteConfig(
    private val sharedPreference: SharedPreference
) {
    companion object {
        @Volatile
        private var INSTANCE: RemoteConfig? = null

        fun getInstance(): RemoteConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RemoteConfig(SharedPreference.Companion.getInstance()).also {
                    INSTANCE = it
                }
            }
        }

        private val gson = Gson()
    }

    var isAdEnable by sharedPreference.boolean("ad_enable", true)
        private set
    var rattingConfig by sharedPreference.list(
        "ratting_config",
        listOf<Int>(1, 2, 3, 4, 5)
    )
        private set
    var metaCtrLow by sharedPreference.boolean("meta_ctr_low", false)
        private set
    var timeNotiLockReminder by sharedPreference.list(
        "timeNotiLockReminder",
        listOf(3, 8, 13, 18, 23)
    )
    var A001Config by sharedPreference.dataObject<AppOpenConfig>(
        "A101_config",
        AppOpenConfig.defaultAll
    )
        private set
    var B100Config by sharedPreference.dataObject<AdBannerConfig>(
        "B100_config",
        AdBannerConfig.defaultAll
    )
        private set
    var I101Config by sharedPreference.dataObject<AdSplashConfig>(
        "I101_config",
        AdSplashConfig.defaultSplash
    )
        private set
    var I102Config by sharedPreference.dataObject<AdInterConfig>(
        "I102_config",
        AdInterConfig.I102_InterHome
    )
        private set
    var N101Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N101_config_1",
        AdNativeConfig.N101_Config1
    )
        private set
    var N103Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N103_config_1",
        AdNativeConfig.N103_Config1
    )
        private set
    var N104Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N104_config_1",
        AdNativeConfig.N104_Config1
    )
        private set
    var N105Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N105_config_1",
        AdNativeConfig.N105_Config1
    )
        private set
    var OnboardingConfig by sharedPreference.dataObject<OnboardingScreen>(
        "onboarding_config",
        OnboardingScreen.defaultConfig
    )
        private set
    var N107Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N107_config_1",
        AdNativeConfig.N107_Config1
    )
        private set
    var N107Config2 by sharedPreference.dataObject<NativeFullConfig>(
        "N107_config_2",
        NativeFullConfig.defaultFull1()
    )
        private set
    var N108Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N108_config_1",
        AdNativeConfig.N108_Config1
    )
        private set
    var N108Config2 by sharedPreference.dataObject<NativeFullConfig>(
        "N108_config_2",
        NativeFullConfig.defaultFull2()
    )
        private set
    var N109Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N109_config_1",
        AdNativeConfig.N109_Config1
    )
        private set
    var N110Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N110_config_1",
        AdNativeConfig.N110_Config1
    )
        private set
    var N110Config2 by sharedPreference.dataObject<AdNativeConfig>(
        "N110_config_2",
        AdNativeConfig.N110_Config2
    )
        private set
    var InAppUpdate by sharedPreference.string("update_app", "off_pop_up_update")
    var TimesShowUpdate by sharedPreference.int("update_app_times", 3)
    var WellComeEnable by sharedPreference.boolean("onboarding_enable", true)
    var OnboardingEnable by sharedPreference.boolean("onboarding_enable", true)
    var TimeOutSplash by sharedPreference.dataObject("splash_timeout", SplashTimeout.defaultTimeout)

    inline fun <reified T> listFromCsv(
        value: String,
        defaultList: List<T> = emptyList(),
        transform: (String) -> T?
    ): List<T> {
        if (value.isBlank()) return defaultList
        return value.split(",")
            .mapNotNull { transform(it.trim()) }
            .ifEmpty { defaultList }
    }

    private inline fun <reified T> getAndApply(
        config: String,
        default: T,
        block: T.() -> Unit = {}
    ): T {
        val parsed: T? = try {
            gson.fromJson(config, T::class.java)
        } catch (e: Exception) {
            null
        }
        return (parsed ?: default).apply(block)
    }

    fun syncRemote(remoteConfig: FirebaseRemoteConfig) {
        InAppUpdate = remoteConfig.getString("update_app")
        TimesShowUpdate = remoteConfig.getLong("update_app_times").toInt()
        isAdEnable = remoteConfig.getBoolean("ad_enable")

        rattingConfig = listFromCsv(
            remoteConfig.getString("ratting_config")
        ) { it.toIntOrNull() }
            .ifEmpty { listOf(1, 2, 3, 4, 5) }
        A001Config = getAndApply<AppOpenConfig>(
            remoteConfig.getString("A101_config"),
            AppOpenConfig.defaultAll
        ) {
            enable = enable && isAdEnable
        }
        A001Config = getAndApply(
            remoteConfig.getString("A101_config"),
            AppOpenConfig.defaultAll
        ) {
            enable = enable && isAdEnable
        }

        B100Config = getAndApply(
            remoteConfig.getString("B100_config"),
            AdBannerConfig.defaultAll
        ) {
            enable = enable && isAdEnable
        }

        I101Config = getAndApply(
            remoteConfig.getString("I101_config"),
            AdSplashConfig.defaultSplash
        ) {
            enable = enable && isAdEnable
        }

        I102Config = getAndApply(
            remoteConfig.getString("I102_config"),
            AdInterConfig.I102_InterHome
        ) {
            enable = enable && isAdEnable
        }

        N101Config1 = getAndApply(
            remoteConfig.getString("N101_config_1"),
            AdNativeConfig.N101_Config1
        ) {
            enable = enable && isAdEnable
        }

        N103Config1 = getAndApply(
            remoteConfig.getString("N103_config_1"),
            AdNativeConfig.N103_Config1
        ) {
            enable = enable && isAdEnable
        }

        N104Config1 = getAndApply(
            remoteConfig.getString("N104_config_1"),
            AdNativeConfig.N104_Config1
        ) {
            enable = enable && isAdEnable
        }

        N105Config1 = getAndApply(
            remoteConfig.getString("N105_config_1"),
            AdNativeConfig.N105_Config1
        ) {
            enable = enable && isAdEnable
        }

        OnboardingConfig = getAndApply(
            remoteConfig.getString("onboarding_config"),
            OnboardingScreen.defaultConfig
        )

        N107Config1 = getAndApply(
            remoteConfig.getString("N107_config_1"),
            AdNativeConfig.N107_Config1
        ) {
            enable = enable && isAdEnable
        }

        N107Config2 = getAndApply(
            remoteConfig.getString("N107_config_2"),
            NativeFullConfig.defaultFull1()
        )

        N108Config1 = getAndApply(
            remoteConfig.getString("N108_config_1"),
            AdNativeConfig.N108_Config1
        ) {
            enable = enable && isAdEnable
        }

        N108Config2 = getAndApply(
            remoteConfig.getString("N108_config_2"),
            NativeFullConfig.defaultFull2()
        )

        N109Config1 = getAndApply(
            remoteConfig.getString("N109_config_1"),
            AdNativeConfig.N109_Config1
        ) {
            enable = enable && isAdEnable
        }
        N110Config1 = getAndApply(
            remoteConfig.getString("N110_config_1"),
            AdNativeConfig.N110_Config1
        ) {
            enable = enable && isAdEnable
        }

        N110Config2 = getAndApply(
            remoteConfig.getString("N110_config_2"),
            AdNativeConfig.N110_Config2
        ) {
            enable = enable && isAdEnable
        }
        WellComeEnable = remoteConfig.getBoolean("onboarding_enable")
        OnboardingEnable = remoteConfig.getBoolean("onboarding_enable")
        TimeOutSplash =
            getAndApply(remoteConfig.getString("splash_timeout"), SplashTimeout.defaultTimeout)
    }

    private var remoteConfig: FirebaseRemoteConfig

    init {
        val configSettings = remoteConfigSettings {
            setFetchTimeoutInSeconds(30L)
            setMinimumFetchIntervalInSeconds(if (BuildConfig.build_debug) 0L else 3600L)
        }
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
        }
        this.remoteConfig = Firebase.remoteConfig
    }

    suspend fun setupRemoteConfig(isDebug: Boolean) {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setFetchTimeoutInSeconds(30L)
            .setMinimumFetchIntervalInSeconds(if (isDebug) 0L else 3600L)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        val isSuccess = runCatching {
            Log.e("FetchRemote", "Fetch Data Success")
            firebaseRemoteConfig.fetchAndActivate().await()
            true
        }.getOrElse {
            Log.e("FetchRemote", "Fetch Data:${it.message}")
            false
        }
        Log.e("FetchRemote", "Fetch Status:$isSuccess")
        if (isSuccess) {
            syncRemote(firebaseRemoteConfig)
        }
    }
}