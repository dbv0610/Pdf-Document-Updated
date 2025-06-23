package com.ag.sampleadsfirstflow.utils

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PreferenceHelper(private val sharePref: SharedPreferences) {
    companion object {
        const val PREFS_NAME = "SampleAdsFirstFlow"
    }

    fun getPreferences(): SharedPreferences {
        return sharePref
    }

    var isFinishFirstFlow by BooleanPreference("finish_first_flow", false)
    var languageSelected by StringPreference("language_selected", "en")
    var countSessionApp by IntPreference("count_session_app", 0)

    fun isUfo(): Boolean {
        return countSessionApp == 1
    }

    open class BooleanPreference(
        private val key: String,
        private val defaultValue: Boolean = true,
    ) : ReadWriteProperty<PreferenceHelper, Boolean> {
        override fun getValue(thisRef: PreferenceHelper, property: KProperty<*>): Boolean {
            return thisRef.getValue(key, defaultValue)
        }

        override fun setValue(thisRef: PreferenceHelper, property: KProperty<*>, value: Boolean) {
            thisRef.saveValue(key, value)
        }
    }

    class StringPreference(
        private val key: String,
        private val defaultValue: String = "",
    ) : ReadWriteProperty<PreferenceHelper, String> {
        override fun getValue(thisRef: PreferenceHelper, property: KProperty<*>): String {
            return thisRef.getValue(key, defaultValue)
        }

        override fun setValue(thisRef: PreferenceHelper, property: KProperty<*>, value: String) {
            thisRef.saveValue(key, value)
        }
    }

    class IntPreference(
        private val key: String,
        private val defaultValue: Int = 0,
    ) : ReadWriteProperty<PreferenceHelper, Int> {
        override fun getValue(thisRef: PreferenceHelper, property: KProperty<*>): Int {
            return thisRef.getValue(key, defaultValue)
        }

        override fun setValue(thisRef: PreferenceHelper, property: KProperty<*>, value: Int) {
            thisRef.saveValue(key, value)
        }
    }

    class LongPreference(
        private val key: String,
        private val defaultValue: Long = 0,
    ) : ReadWriteProperty<PreferenceHelper, Long> {
        override fun getValue(thisRef: PreferenceHelper, property: KProperty<*>): Long {
            return thisRef.getValue(key, defaultValue)
        }

        override fun setValue(thisRef: PreferenceHelper, property: KProperty<*>, value: Long) {
            thisRef.saveValue(key, value)
        }
    }
}

fun <T> PreferenceHelper.saveValue(key: String, value: T) {
    val editor = this.getPreferences().edit()
    when (value) {
        is String -> editor.putString(key, value)
        is Int -> editor.putInt(key, value)
        is Long -> editor.putLong(key, value)
        is Float -> editor.putFloat(key, value)
        is Boolean -> editor.putBoolean(key, value)
        else -> throw IllegalArgumentException("Unsupported data type")
    }
    editor.apply()
}

inline fun <reified T> PreferenceHelper.getValue(key: String, defaultValue: T): T {
    return when (T::class) {
        String::class -> getPreferences().getString(key, defaultValue as? String) as T?
            ?: defaultValue

        Int::class -> getPreferences().getInt(key, defaultValue as? Int ?: 0) as T
        Long::class -> getPreferences().getLong(key, defaultValue as? Long ?: 0L) as T
        Float::class -> getPreferences().getFloat(key, defaultValue as? Float ?: 0.0f) as T
        Boolean::class -> getPreferences().getBoolean(key, defaultValue as? Boolean ?: false) as T
        else -> throw IllegalArgumentException("Unsupported data type")
    }
}
