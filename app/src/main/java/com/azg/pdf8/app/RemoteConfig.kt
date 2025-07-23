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
        AdNativeConfig.N101_NativeSplash
    )
        private set

    var N103Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N103_config_1",
        AdNativeConfig.N103_NativeLanguage
    )
        private set
    var N104Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N104_config_1",
        AdNativeConfig.N104_NativeLanguageDup
    )
        private set
    var N105Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N105_config_1",
        AdNativeConfig.N104_NativeLanguageDup
    )
        private set
    var OnboardingConfig by sharedPreference.dataObject<OnboardingScreen>(
        "onboarding_config",
        OnboardingScreen.defaultConfig
    )
        private set
    var N107Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N107_config_1",
        AdNativeConfig.N107_1_ObFull
    )
        private set
    var N107Config2 by sharedPreference.dataObject<NativeFullConfig>(
        "N107_config_2",
        NativeFullConfig.defaultFull1()
    )
        private set
    var N108Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N108_config_1",
        AdNativeConfig.N108_1_ObFull
    )
        private set
    var N108Config2 by sharedPreference.dataObject<NativeFullConfig>(
        "N108_config_2",
        NativeFullConfig.defaultFull2()
    )
        private set
    var N109Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N109_config_1",
        AdNativeConfig.N109_NativeProfile
    )
        private set
    var N120Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N120_config_1",
        AdNativeConfig.N120_NativeHome
    )
        private set
    var N121Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N121_config_1",
        AdNativeConfig.N121_NativeAll
    )
        private set
    var N122Config1 by sharedPreference.dataObject<AdNativeConfig>(
        "N122_config_1",
        AdNativeConfig.N122_NativePermission
    )
        private set
    var TimeOutSplash by sharedPreference.dataObject<SplashTimeout>(
        " splash_timeout",
        SplashTimeout.defaultTimeout
    )
    var InAppUpdate by sharedPreference.string("update_app", "off_pop_up_update")
    var TimesShowUpdate by sharedPreference.int("update_app_times", 3)

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

        val stringN001 = remoteConfig.getString("N101_config_1")
        rattingConfig = listFromCsv(
            remoteConfig.getString("ratting_config")
        ) { it.toIntOrNull() }
            .ifEmpty { listOf(1, 2, 3, 4, 5) }
        A001Config = getAndApply<AppOpenConfig>(remoteConfig.getString("A101_config"),AppOpenConfig.defaultAll){
            enable = enable && isAdEnable
        }
        B100Config = getAndApply<AdBannerConfig>(
            remoteConfig.getString("B100_config"),
            AdBannerConfig.defaultAll){
            enable = enable && isAdEnable
        }
        I101Config = gson.fromJson(
            remoteConfig.getString("I101_config"),
            AdSplashConfig::class.java
        )
        I102Config = gson.fromJson(
            remoteConfig.getString("I102_config"),
            AdInterConfig::class.java
        )
        N101Config1 = gson.fromJson(
            stringN001,
            AdNativeConfig::class.java
        )
        N103Config1 = gson.fromJson(
            remoteConfig.getString("N103_config_1"),
            AdNativeConfig::class.java
        )
        N104Config1 = gson.fromJson(
            remoteConfig.getString("N104_config_1"),
            AdNativeConfig::class.java
        )
        N105Config1 = gson.fromJson(
            remoteConfig.getString("N105_config_1"),
            AdNativeConfig::class.java
        )
        OnboardingConfig = gson.fromJson(
            remoteConfig.getString("onboarding_config"),
            OnboardingScreen::class.java
        )
        N107Config1 = gson.fromJson(
            remoteConfig.getString("N107_config_1"),
            AdNativeConfig::class.java
        )
        N107Config2 = gson.fromJson(
            remoteConfig.getString("N107_config_2"),
            NativeFullConfig::class.java
        )
        N108Config1 = gson.fromJson(
            remoteConfig.getString("N108_config_1"),
            AdNativeConfig::class.java
        )
        N108Config2 = gson.fromJson(
            remoteConfig.getString("N108_config_2"),
            NativeFullConfig::class.java
        )
        N109Config1 = gson.fromJson(
            remoteConfig.getString("N109_config_1"),
            AdNativeConfig::class.java
        )
        N120Config1 = gson.fromJson(
            remoteConfig.getString("N120_config_1"),
            AdNativeConfig::class.java
        )
        N121Config1 = gson.fromJson(
            remoteConfig.getString("N121_config_1"),
            AdNativeConfig::class.java
        )

        N122Config1 = gson.fromJson(
            remoteConfig.getString("N122_config_1"),
            AdNativeConfig::class.java
        )
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
            firebaseRemoteConfig.fetchAndActivate().await()

        }.getOrElse {
            false
        }
        Log.e("FetchRemote","Fetch Status:$isSuccess")
        if (isSuccess) { syncRemote(firebaseRemoteConfig) }
    }

}