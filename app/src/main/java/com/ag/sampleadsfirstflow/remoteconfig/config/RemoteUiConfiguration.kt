package com.ag.sampleadsfirstflow.remoteconfig.config

import com.ag.sampleadsfirstflow.remoteconfig.BaseRemoteConfiguration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class RemoteUiConfiguration private constructor() : BaseRemoteConfiguration() {
    override fun getPreferencesName(): String {
        return PREFS_NAME
    }

    override fun sync(remoteConfig: FirebaseRemoteConfig) {}

    companion object {
        private const val PREFS_NAME = "remote_config_view_prefs"

        @Volatile
        private var _instance: RemoteUiConfiguration? = null

        fun getInstance(): RemoteUiConfiguration =
            _instance ?: synchronized(this) {
                _instance ?: RemoteUiConfiguration().also { _instance = it }
            }
    }
}
