package com.ag.sampleadsfirstflow.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import com.ag.sampleadsfirstflow.base.BaseActivity
import com.ag.sampleadsfirstflow.databinding.ActivityMainBinding
import com.ag.sampleadsfirstflow.remoteconfig.analytics.Analytics

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.track("launcher_home")
        if (preferenceHelper.isUfo()) {
            Analytics.track("ufo_home")
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun updateUI(savedInstanceState: Bundle?) {

    }

}
