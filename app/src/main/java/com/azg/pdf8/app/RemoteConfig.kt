package com.azg.pdf8.app

import com.azg.pdf8.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class RemoteConfig(
                   private val sharedPreference: SharedPreference
) {
    companion object {
        private const val TAG = "CompositeRemoteRepository"
        private const val TIMEOUT_DURATION = 30_000L

        @Volatile
        private var INSTANCE: RemoteConfig? = null

        fun getInstance(): RemoteConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RemoteConfig(SharedPreference.Companion.getInstance()).also {
                    INSTANCE = it
                }
            }
        }
    }

    var InAppUpdate by sharedPreference.string("update_app", "off_pop_up_update")
    var TimesShowUpdate by sharedPreference.int("update_app_times", 3)
    private var isShowAds by sharedPreference.boolean("isShowAds", false)
    fun SharedPreference.syncRemote(remoteConfig: FirebaseRemoteConfig) {
        InAppUpdate = remoteConfig.getString("update_app")
        TimesShowUpdate = remoteConfig.getLong("update_app_times").toString().toInt()

        isShowAds = remoteConfig.getBoolean("isShowAss")
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

    suspend fun fetchRemoteData(): Boolean = withTimeoutOrNull(TIMEOUT_DURATION) {
        runCatching {
            val fetchResult = Firebase.remoteConfig.fetchAndActivate().await()
            if (fetchResult) {
                syncData()
            }
            true
        }.getOrElse { true }
    } ?: true

    private fun syncData() = try {
        sharedPreference.syncRemote(this@RemoteConfig.remoteConfig)
    } catch (e: Exception) {
    }
}