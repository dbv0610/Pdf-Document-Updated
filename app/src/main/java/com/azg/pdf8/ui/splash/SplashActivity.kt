package com.azg.pdf8.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.az.inappupdate.AppUpdateManager
import com.azg.pdf8.app.firstOpenApp
import com.azg.pdf8.app.isFinishFirstFlow
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.ActivitySplashBinding
import com.azg.pdf8.firebase.Analytics
import com.azg.pdf8.ui.language.LanguageOpenActivity
import com.azg.pdf8.ui.language.LanguageScreenType
import com.azg.pdf8.ui.main.MainActivity
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.widget.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {
    private val TIME_OUT = 30000L
    private val TIME_DELAY = 3000L
    private var isNextAction = false
    private var isAcceptUmp = true
    override fun backPressed() {
    }

    override fun initialize() {
    }

    override fun ActivitySplashBinding.onClick() {
    }

    override fun ActivitySplashBinding.setData() {
        lifecycleScope.launch {
            /* kotlinx.coroutines.awaitAll(
                async { requestUmp() },
                async { kotlinx.coroutines.withTimeoutOrNull(TIME_OUT) { RemoteInitializer.setupRemoteConfig() } }
            )*/
            setupInAppUpdate()
//            initAds()
        }

        delay(1000) {
            navigateToNextScreen()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstOpenApp += 1
        if (isUfo()) {
            Analytics.track("ufo_splash")
        }
    }

    private fun setupInAppUpdate() {
        AppUpdateManager.getInstance(this@SplashActivity).setupUpdate(
            remoteConfig.InAppUpdate,
            remoteConfig.TimesShowUpdate,
        )
    }

    private fun navigateToNextScreen() {
        if (isNextAction) return
        isNextAction = true
        if (isFinishFirstFlow) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            LanguageOpenActivity.start(this, LanguageScreenType.Language1)
        }

        finish()
    }
}
