package com.azg.pdf8.app

import android.util.Log
import com.azg.pdf8.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import kotlin.jvm.java

class RemoteConfig(
    sharedPreference: SharedPreference
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

    var newFlowApp by sharedPreference.boolean("new_flow_app", false)
        private set
    var timeNotiLockReminder by sharedPreference.list(
        "timeNotiLockReminder",
        listOf(3, 8, 13, 18, 23)
    )

        private set
    var inAppUpdate by sharedPreference.string("update_app", "off_pop_up_update")
    var timesShowUpdate by sharedPreference.int("update_app_times", 3)
    var wellComeEnable by sharedPreference.boolean("onboarding_enable", true)
    var onboardingEnable by sharedPreference.boolean("onboarding_enable", true)

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
        inAppUpdate = remoteConfig.getString("update_app")
        timesShowUpdate = remoteConfig.getLong("update_app_times").toInt()
        isAdEnable = remoteConfig.getBoolean("ad_enable")

        rattingConfig = listFromCsv(
            remoteConfig.getString("ratting_config")
        ) { it.toIntOrNull() }
            .ifEmpty { listOf(1, 2, 3, 4, 5) }

        newFlowApp = getAndApply(
            remoteConfig.getString("new_flow_app"),
            false
        )
        wellComeEnable = remoteConfig.getBoolean("onboarding_enable")
        onboardingEnable = remoteConfig.getBoolean("onboarding_enable")
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