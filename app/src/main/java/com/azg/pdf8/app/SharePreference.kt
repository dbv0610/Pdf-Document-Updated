package com.azg.pdf8.app

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface OnPreferenceChangeListener {
    fun onPreferenceChanged(key: String)
}

class PreferenceProperty<T : Any>(
    private val prefs: SharedPreference,
    val key: String,
    val default: T
) {
    var value: T by prefs.preference(key, default)
    val liveData: LiveData<T> = prefs.liveData(key, default)
    val flow: Flow<T> = prefs.flow(key, default)
}

fun <T : Any> SharedPreference.preferenceOf(
    key: String,
    default: T
): PreferenceProperty<T> = PreferenceProperty(this, key, default)

class SharedPreference(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: SharedPreference? = null
        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = SharedPreference(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): SharedPreference {
            return INSTANCE
                ?: throw IllegalStateException("SharedPreference not initialized. Call SharedPreference.init(context) first.")
        }
    }

    private var onValueChange: OnPreferenceChangeListener? = null
    fun registerListener(listener: OnPreferenceChangeListener) {
        this.onValueChange = listener
    }

    fun unRegisterListener() {
        this.onValueChange = null
    }

    private var sharedPreferences =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    private var lastKey = ""
    fun edit(block: SharedPreferences.Editor.() -> Unit) {
        with(sharedPreferences!!.edit()) {
            block()
            apply()
        }
    }

    fun getString(key: String, default: String? = ""): String? {
        return sharedPreferences?.getString(key, default)
    }

    fun putString(key: String, value: String) {
        edit {
            onValueChange?.onPreferenceChanged(key)
            putString(key, value)
        }
    }

    inline fun <reified T> dataObject(
        key: String,
        default: T
    ): ReadWriteProperty<Any?, T> {
        val gson = Gson()
        val type = object : TypeToken<T>() {}.type

        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                val json = getString(key, default.toString())
                if (json.isNullOrBlank()) {
                    return default
                }
                val parsed: T? = runCatching {
                    gson.fromJson<T>(json, type)
                }.getOrNull()
                return parsed ?: default
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                val json = gson.toJson(value, type)
                putString(key, json)
            }
        }
    }

    fun boolean(key: String, default: Boolean = false): ReadWriteProperty<Any?, Boolean> {
        return object : ReadWriteProperty<Any?, Boolean> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
                return sharedPreferences?.getBoolean(key, default) ?: default
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
                edit {
                    onValueChange?.onPreferenceChanged(key)
                    putBoolean(key, value)
                }
            }
        }
    }

    fun string(key: String, default: String = ""): ReadWriteProperty<Any?, String> {
        return object : ReadWriteProperty<Any?, String> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): String {
                return sharedPreferences?.getString(key, default) ?: default
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
                edit {
                    onValueChange?.onPreferenceChanged(key)
                    putString(key, value)
                }
            }
        }
    }

    fun int(key: String, default: Int = 0): ReadWriteProperty<Any?, Int> {
        return object : ReadWriteProperty<Any?, Int> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                return sharedPreferences?.getInt(key, default) ?: default
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                edit {
                    onValueChange?.onPreferenceChanged(key)
                    putInt(key, value)
                }
            }
        }
    }

    inline fun <reified T> list(
        key: String,
        default: List<T> = emptyList()
    ): ReadWriteProperty<Any?, List<T>> {
        val gson = Gson()

        return object : ReadWriteProperty<Any?, List<T>> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
                val json = getString(key, null) ?: return default
                val type = object : TypeToken<List<T>>() {}.type
                return runCatching { gson.fromJson<List<T>>(json, type) }.getOrDefault(default)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
                val json = gson.toJson(value)
                putString(key, json)
            }
        }
    }
    @Suppress("UNCHECKED_CAST")
    fun float(key: String, default: Float = 0f): ReadWriteProperty<Any?, Float> =
        object : ReadWriteProperty<Any?, Float> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) =
                sharedPreferences.getFloat(key, default) ?: default

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) = edit {
                onValueChange?.onPreferenceChanged(key)
                putLong(key, java.lang.Double.doubleToRawLongBits(value.toDouble()))
            }
        }

    fun long(key: String, default: Long = 0L): ReadWriteProperty<Any?, Long> =
        object : ReadWriteProperty<Any?, Long> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) =
                sharedPreferences.getLong(key, default) ?: default

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) = edit {
                onValueChange?.onPreferenceChanged(key)
                putLong(key, value)
            }
        }

    fun double(key: String, default: Double = 0.0): ReadWriteProperty<Any?, Double> =
        object : ReadWriteProperty<Any?, Double> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences!!.getString(key, default.toString())?.toDoubleOrNull() ?:0.0

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) = edit {
                onValueChange?.onPreferenceChanged(key)
                putString(key, value.toString())
            }
        }
    private fun <T> getValueFor(key: String, default: T): T = when (default) {
        is String -> sharedPreferences.getString(key, default)!!
        is Int -> sharedPreferences.getInt(key, default)
        is Boolean -> sharedPreferences.getBoolean(key, default)
        is Float -> sharedPreferences.getFloat(key, default)
        is Long -> sharedPreferences.getLong(key, default)
        else -> throw IllegalArgumentException("Unsupported type")
    } as T

    fun <T : Any> liveData(
        key: String,
        default: T
    ): LiveData<T> = object : LiveData<T>() {
        private val listener =
            OnSharedPreferenceChangeListener { _, changedKey ->
                if (changedKey == key) {
                    value = getValueFor(key, default)
                }
            }

        override fun onActive() {
            super.onActive()
            value = getValueFor(key, default)
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onInactive() {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            super.onInactive()
        }
    }

    fun <T : Any> preference(
        key: String,
        default: T
    ): ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T =
            getValueFor(key, default)

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            sharedPreferences.edit().apply {
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Long -> putLong(key, value)
                    else -> error("Unsupported type ${value::class}")
                }
            }.apply()
            onValueChange?.onPreferenceChanged(key)
        }
    }

    fun <T : Any> flow(
        key: String,
        default: T
    ): Flow<T> = callbackFlow {
        trySend(getValueFor(key, default))
        val listener = OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) trySend(getValueFor(key, default))
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
    fun clear() {
        edit {
            clear()
        }
    }
}






















