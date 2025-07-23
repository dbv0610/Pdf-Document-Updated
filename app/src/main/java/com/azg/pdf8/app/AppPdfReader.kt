package com.azg.pdf8.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.azg.pdf8.BuildConfig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

interface ActivityLifecycleCallbacksImpl : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityResumed(activity: Activity)
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
}

class AppPdfReader : Application() {
    companion object {
        var isAppForeground = false
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AppPdfReader)
            modules(
                listOf(
                    dataModule,
                    viewModelModule,
                )
            )
        }
        SharedPreference.Companion.init(this@AppPdfReader)
        registerLifecycleCallback()
    }

    private fun registerLifecycleCallback() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksImpl {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }
        })

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppForeground = true
                Log.d("AppLifecycle", "App in foreground")
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppForeground = false
                Log.d("AppLifecycle", "App in background")
            }
        })
    }
}
val isAppDebug = BuildConfig.build_debug == true
val sharedPreference get() = SharedPreference.getInstance()
val remoteConfig get() = RemoteConfig.getInstance()