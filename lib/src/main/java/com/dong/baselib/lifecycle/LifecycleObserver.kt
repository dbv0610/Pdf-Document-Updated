package com.dong.baselib.lifecycle

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.isAccessible

fun <T> mutableLiveData(value: T): MutableLiveData<T> =
    MutableLiveData<T>().apply { setValue(value) }

fun <T> MutableLiveData<T>.get(): T = this.value!!
fun <T> MutableLiveData<T>.set(value: T) {
    this.value = value
}

fun <T> MutableLiveData<T>.change(value: (T) -> Unit) {
    this.observeForever {
        value(it)
    }
}


fun <T1, T2, R> combine(
    lifecycleOwner: LifecycleOwner,
    liveData1: LiveData<T1>,
    liveData2: LiveData<T2>,
    transform: (T1?, T2?) -> R
): LiveData<R> = MediatorLiveData<R>().apply {
    var last1: T1? = null
    var last2: T2? = null
    fun update() {
        value = transform(last1, last2)
    }
    val observer1 = Observer<T1> { value ->
        last1 = value
        update()
    }
    val observer2 = Observer<T2> { value ->
        last2 = value
        update()
    }
    addSource(liveData1, observer1)
    addSource(liveData2, observer2)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                removeSource(liveData1)
                removeSource(liveData2)
                source.lifecycle.removeObserver(this)
            }
        }
    })
}fun <T1, T2, T3, R> combine(
    lifecycleOwner: LifecycleOwner,
    liveData1: LiveData<T1>,
    liveData2: LiveData<T2>,
    liveData3: LiveData<T3>,
    transform: (T1?, T2?, T3?) -> R
): LiveData<R> = MediatorLiveData<R>().apply {
    var last1: T1? = null
    var last2: T2? = null
    var last3: T3? = null

    fun update() {
        value = transform(last1, last2, last3)
    }
    val observer1 = Observer<T1> { value ->
        last1 = value
        update()
    }
    val observer2 = Observer<T2> { value ->
        last2 = value
        update()
    }

    val observer3 = Observer<T3> { value ->
        last3 = value
        update()
    }
    addSource(liveData1, observer1)
    addSource(liveData2, observer2)
    addSource(liveData3, observer3)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                removeSource(liveData1)
                removeSource(liveData2)
                removeSource(liveData3)
                source.lifecycle.removeObserver(this)
            }
        }
    })
}

fun <R> combine(
    lifecycleOwner: LifecycleOwner,
    vararg sources: LiveData<*>,
    transform: (Array<Any?>) -> R
): LiveData<R> = MediatorLiveData<R>().apply {
    val latestValues = Array<Any?>(sources.size) { null }
    val observers = mutableListOf<Observer<Any?>>()
    fun update() {
        value = transform(latestValues)
    }
    sources.forEachIndexed { index, source ->
        val observer = Observer<Any?> { value ->
            latestValues[index] = value
            update()
        }
        observers.add(observer)
        addSource(source, observer)
    }
    lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                sources.forEachIndexed { index, src ->
                    removeSource(src)
                }
                source.lifecycle.removeObserver(this)
            }
        }
    })
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

fun AppCompatActivity.lifecycleLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch(context = context, start = start) {
        block()
    }
}

fun Fragment.lifecycleLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch(context = context, start = start) {
        block()
    }
}

fun AppCompatActivity.lifecycleLaunchWhenStarted(
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

fun Fragment.lifecycleLaunchWhenStarted(
    block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }
}

fun AppCompatActivity.lifecycleLaunchWhenResumed(
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            block()
        }
    }
}

fun Fragment.lifecycleLaunchWhenResumed(
    block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            block()
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