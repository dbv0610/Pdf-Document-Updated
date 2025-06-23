package com.ag.sampleadsfirstflow

import com.ads.control.admob.Admob
import com.ads.control.admob.AppOpenManager
import com.ads.control.ads.AzAds
import com.ads.control.application.AdsMultiDexApplication
import com.ads.control.config.AdjustConfig
import com.ads.control.config.AppsflyerConfig
import com.ads.control.config.AzAdConfig
import com.ag.sampleadsfirstflow.di.appModule
import com.ag.sampleadsfirstflow.di.viewModelModule
import com.ag.sampleadsfirstflow.remoteconfig.RemoteInitializer
import com.ag.sampleadsfirstflow.ui.splash.SplashActivity
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApplication : AdsMultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(
                listOf(
                    appModule,
                    viewModelModule,
                )
            )
        }
        FirebaseApp.initializeApp(this)
        RemoteInitializer.init(this)
        initAds()
    }

    private fun initAds() {
        val environment = if (BuildConfig.build_debug) {
            AzAdConfig.ENVIRONMENT_DEVELOP
        } else {
            AzAdConfig.ENVIRONMENT_PRODUCTION
        }
        azAdConfig = AzAdConfig(this, AzAdConfig.PROVIDER_ADMOB, environment)
        azAdConfig.adjustConfig = AdjustConfig("zzzzzzzzzzzzz")
        azAdConfig.appsflyerConfig = AppsflyerConfig(false, "")
        azAdConfig.listDeviceTest = listOf("C01E9C6F78D783B443CEA36BFBCBB212")
        AzAds.getInstance().init(this, azAdConfig, false)
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
    }
}
