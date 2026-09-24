package com.azg.pdf8.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.app.firstOpenApp
import com.azg.pdf8.app.isAppDebug
import com.azg.pdf8.app.isFinishFirstFlow
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivitySplashBinding
import com.azg.pdf8.firebase.Analytics
import com.azg.pdf8.ui.language.LanguageWaitingActivity
import com.azg.pdf8.ui.main.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.objectweb.asm.Handle

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate){
    override fun backPressed() = Unit
    override fun initialize() = Unit
    override fun ActivitySplashBinding.setData() {
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        },3000)
    }

    override fun ActivitySplashBinding.onClick() = Unit


    private fun navigateToNextScreen() {
   //     if (isFinishFirstFlow) {
            startActivity(Intent(this, MainActivity::class.java))
     /*   } else {
            launchActivity<LanguageWaitingActivity>()
        }
*/
        finish()
    }


    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firstOpenApp += 1
        if (isUfo()) {
            Analytics.track("ufo_splash")
        }
    }


}
