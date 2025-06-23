package com.ag.sampleadsfirstflow.remoteconfig.config

import com.ag.sampleadsfirstflow.remoteconfig.BaseRemoteConfiguration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class RemoteLogicConfiguration private constructor() : BaseRemoteConfiguration() {
    override fun getPreferencesName(): String {
        return PREFS_NAME
    }

    override fun sync(remoteConfig: FirebaseRemoteConfig) {
        remoteConfig.saveToLocal(InAppUpdate)
        remoteConfig.saveToLocal(TimesShowUpdate)
    }

    private data object InAppUpdate : RemoteKeys.StringKey("update_app", "off_pop_up_update")
    private data object TimesShowUpdate : RemoteKeys.LongKey("update_app_times", 3)

    val inAppUpdate: String get() = InAppUpdate.get()
    val timesShowUpdate: Int get() = TimesShowUpdate.get().toInt()

    companion object {
        private const val PREFS_NAME = "remote_config_logic_prefs"

        @Volatile
        private var _instance: RemoteLogicConfiguration? = null

        fun getInstance(): RemoteLogicConfiguration =
            _instance ?: synchronized(this) {
                _instance ?: RemoteLogicConfiguration().also { _instance = it }
            }
    }
}
