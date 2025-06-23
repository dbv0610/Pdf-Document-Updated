package com.dong.baselib.lifecycle

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.isAccessible

fun <T> mutableLiveData(value: T): MutableLiveData<T> = MutableLiveData<T>().apply { setValue(value) }
fun <T> MutableLiveData<T>.get(): T = this.value!!
fun <T> MutableLiveData<T>.set(value: T) {
    this.value=value
}




fun <T> MutableLiveData<T>.change(value: (T) -> Unit) {
    this.observeForever {
        value(it)
    }
}



fun <T> MutableLiveData<T>.post(value: T) {
    this.postValue(value)
}

fun <T> mutableListLiveData(initialList: MutableList<T> = mutableListOf()): MutableLiveData<MutableList<T>> {
    return MutableLiveData(initialList)
}

fun <T> MutableLiveData<MutableList<T>>.addItem(item: T) {
    val updatedList = this.value ?: mutableListOf()
    updatedList.add(item)
    this.postValue(updatedList)
}

fun <T> MutableLiveData<MutableList<T>>.removeItem(item: T) {
    val updatedList = this.value ?: mutableListOf()
    updatedList.remove(item)
    this.value = updatedList
}
fun <T> MutableLiveData<MutableList<T>>.changeItemAt(index: Int, newItem: T) {
    val updatedList = this.value ?: mutableListOf()
    
    if (index in updatedList.indices) {
        updatedList[index] = newItem
        this.postValue(updatedList)
    }
}

fun <T> MutableLiveData<MutableList<T>>.clearItems() {
    this.value = mutableListOf()
}

fun AppCompatActivity.LauncherEffect(
    vararg liveData: LiveData<*>,
    block: () -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            liveData.forEach { ld ->
                launch {
                    ld.asFlow().collect {
                        block()
                    }
                }
            }
        }
    }
}

fun Fragment.LauncherEffect(
    vararg liveData: LiveData<*>,
    block: () -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            liveData.forEach { ld ->
                launch {
                    ld.asFlow().collect {
                        block()
                    }
                }
            }
        }
    }
}


fun <T> AppCompatActivity.DisposeEffect(
    key: MutableLiveData<T>,
    block: suspend CoroutineScope.(T) -> Unit
) {
    val observer = Observer<T> { newValue ->
        lifecycleScope.launch {
            block(newValue)
        }
    }
    val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            key.observe(this@DisposeEffect, observer)
        }
        
        override fun onStop(owner: LifecycleOwner) {
            key.removeObserver(observer)
        }
    }
    lifecycle.addObserver(lifecycleObserver)
}



fun <T> AppCompatActivity.LauncherEffect(
    vararg keys: MutableLiveData<T>,
    block: suspend CoroutineScope.(T) -> Unit
) {
    val observer = Observer<T> { newValue ->
        lifecycleScope.launch {
            block(newValue)
        }
    }
    keys.forEach { key ->
        key.observe(this, observer)
    }
    
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            keys.forEach { key ->
                key.removeObserver(observer)
            }
            super.onDestroy(owner)
        }
    })
}

class LifecycleObserver : DefaultLifecycleObserver {
    
    override fun onStart(owner: LifecycleOwner) {
        println("Component has started")
    }
    
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
    }
    
    override fun onPause(owner: LifecycleOwner) {
    }
    
    override fun onStop(owner: LifecycleOwner) {
    }
}