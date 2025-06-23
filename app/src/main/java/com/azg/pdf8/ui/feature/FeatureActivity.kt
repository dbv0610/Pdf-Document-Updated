package com.azg.pdf8.ui.feature

import android.content.Context
import android.content.Intent
import com.azg.pdf8.app.isFinishFirstFlow
import com.dong.baselib.base.BaseActivity
import com.azg.pdf8.databinding.ActivityFeatureBinding
import com.azg.pdf8.ui.main.MainActivity
import com.dong.baselib.api.parcelable

abstract class FeatureActivity : BaseActivity<ActivityFeatureBinding>(ActivityFeatureBinding::inflate) {
    companion object {
        private var scrollOffsetY: Int = 0
        private const val ARG_SCREEN_TYPE = "ARG_SCREEN_TYPE"

        fun start(
            context: Context,
            screenType: FeatureScreenType,
        ) {
            val clazz = when (screenType) {
                is FeatureScreenType.Feature1 -> Feature1Activity::class.java
                is FeatureScreenType.Feature2 -> Feature2Activity::class.java
            }
            val intent = Intent(context, clazz)
            intent.putExtra(ARG_SCREEN_TYPE, screenType)
            context.startActivity(intent)
        }
        
    }

 
    private val screenType by lazy {
        runCatching {
            intent.parcelable<FeatureScreenType>(ARG_SCREEN_TYPE)
        }.getOrNull() ?: FeatureScreenType.Feature1
    }

    override fun ActivityFeatureBinding.onClick() {

    }


    override fun backPressed() {
        finishAffinity()
    }

    override fun ActivityFeatureBinding.setData() {}

    override fun initialize() {
        binding.txtContinue.setOnClickListener {
            isFinishFirstFlow = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun onClickFeature() {
        if (screenType is FeatureScreenType.Feature1) {
            navigateToScreenDup()
        }
    }

    private fun navigateToScreenDup() {
      
        start(this, FeatureScreenType.Feature2)
        overridePendingTransition(0, 0)
        finish()
    }
}
