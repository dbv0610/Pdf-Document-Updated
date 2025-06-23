package com.ag.sampleadsfirstflow.remoteconfig

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ag.sampleadsfirstflow.remoteconfig.config.RemoteEnumString
import com.ag.sampleadsfirstflow.remoteconfig.config.RemoteKeys
import com.google.firebase.remoteconfig.FirebaseRemoteConfig


abstract class BaseRemoteConfiguration {
    private lateinit var application: Application

    internal abstract fun getPreferencesName(): String

    abstract fun sync(remoteConfig: FirebaseRemoteConfig)

    private fun getPreferences(): SharedPreferences {
        return application.getSharedPreferences(getPreferencesName(), Context.MODE_PRIVATE)
    }

    fun init(application: Application) {
        this.application = application
    }

    internal fun FirebaseRemoteConfig.saveToLocal(keyType: RemoteKeys) {
        val remoteConfig = this
        getPreferences().edit {
            val key = keyType.remoteKey
            when(keyType) {
                is RemoteKeys.BooleanKey -> {
                    putBoolean(
                        key,
                        kotlin.runCatching {
                            remoteConfig.getBoolean(key)
                        }.getOrElse { keyType.defaultValue }
                    )
                }

                is RemoteKeys.StringKey -> {
                    putString(
                        key,
                        kotlin.runCatching {
                            remoteConfig.getString(key)
                        }.getOrElse { keyType.defaultValue }
                    )
                }

                is RemoteKeys.LongKey -> {
                    putLong(
                        key,
                        kotlin.runCatching {
                            remoteConfig.getLong(key)
                        }.getOrElse { keyType.defaultValue })
                }

                is RemoteKeys.DoubleKey -> {
                    putFloat(
                        key,
                        kotlin.runCatching {
                            remoteConfig.getDouble(key)
                        }.getOrElse { keyType.defaultValue }.toFloat()
                    )
                }

                is RemoteKeys.ListIntegerKey -> {
                    putString(key, kotlin.runCatching {
                        remoteConfig.getString(key)
                    }.getOrElse { keyType.defaultValue.joinToString(",") })
                }

                is RemoteKeys.StringEnumKey<*> -> {
                    putString(
                        key,
                        kotlin.runCatching {
                            remoteConfig.getString(key)
                        }.getOrElse { keyType.defaultValue.remoteValue })
                }
            }
        }
    }

    internal fun RemoteKeys.BooleanKey.get(): Boolean {
        return kotlin.runCatching {
            getPreferences().getBoolean(remoteKey, defaultValue)
        }.getOrDefault(defaultValue)
    }

    internal fun RemoteKeys.StringKey.get(): String {
        return kotlin.runCatching {
            getPreferences().getString(remoteKey, defaultValue).takeUnless { it.isNullOrBlank() }
        }.getOrNull() ?: defaultValue
    }

    internal fun RemoteKeys.LongKey.get(): Long {
        return kotlin.runCatching {
            getPreferences().getLong(remoteKey, defaultValue)
        }.getOrDefault(defaultValue)
    }

    internal fun RemoteKeys.DoubleKey.get(): Double {
        return kotlin.runCatching {
            getPreferences().getFloat(remoteKey, defaultValue.toFloat())
        }.getOrDefault(defaultValue).toDouble()
    }

    internal fun RemoteKeys.ListIntegerKey.get(): List<Int> {
        return kotlin.runCatching {
            getPreferences().getString(remoteKey, defaultValue.joinToString(","))
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
        }.getOrNull() ?: defaultValue
    }

    internal inline fun <reified T> RemoteKeys.StringEnumKey<T>.get(): T where T : RemoteEnumString, T : Enum<T> {
        return kotlin.runCatching {
            val stringValue = getPreferences().getString(remoteKey, defaultValue.remoteValue)
                .takeUnless { it.isNullOrBlank() } ?: defaultValue.remoteValue
            enumValues<T>().find { it.remoteValue == stringValue } ?: defaultValue
        }.getOrDefault(defaultValue)
    }
}
