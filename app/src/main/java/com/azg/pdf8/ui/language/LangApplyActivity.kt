package com.azg.pdf8.ui.language

import com.azg.pdf8.R
import com.azg.pdf8.app.remoteConfig
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.base.SystemUtil
import com.azg.pdf8.databinding.ActivityLangApplyBinding
import com.azg.pdf8.ui.feature.FeatureActivity
import com.azg.pdf8.ui.feature.FeatureScreenType
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.ui.onboarding.OnboardingActivity

import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.loadImage
import com.dong.baselib.widget.rotateViewByTime
import com.dong.baselib.widget.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LangApplyActivity :
    BaseActivity<ActivityLangApplyBinding>(ActivityLangApplyBinding::inflate) {
    override fun backPressed() {
        SystemUtil.saveLocale(
            this@LangApplyActivity, "en"
        )
        finish()
    }

    override fun initialize() {
        LanguageOpenActivity.currentLang.observe(this) {
            binding.tvItemName.text = it?.name ?: "English"
            binding.imgItemFlag.loadImage(it?.flagId ?: R.drawable.ic_flag_uk)
        }
    }

    private val jobLauncher = CoroutineScope(Dispatchers.Main)

    override fun ActivityLangApplyBinding.onClick() {
        selectLanguage.click {
            if (jobLauncher.isActive) {
                jobLauncher.cancel()
            }
            SystemUtil.saveLocale(
                this@LangApplyActivity,
                LanguageOpenActivity.currentLang.value?.code ?: "en"
            )
            launchActivity<OnboardingActivity>()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (jobLauncher.isActive) {
            jobLauncher.cancel()
        }
    }

    override fun ActivityLangApplyBinding.setData() {
        jobLauncher.launch {
            ivImgLoad.rotateViewByTime(duration = 1500, isLoop = true)
            delay(2000)
            llLoading.gone()
            llApplySuccess.visible()
            delay(1000)
            runCatching {
                SystemUtil.saveLocale(
                    this@LangApplyActivity,
                    LanguageOpenActivity.currentLang.value?.code ?: "en"
                )
                if(remoteConfig.onboardingEnable){
                    launchActivity<OnboardingActivity>()
                } else {
                    if(remoteConfig.wellComeEnable){
                        FeatureActivity.start(this@LangApplyActivity, FeatureScreenType.Feature1)
                    } else {
                        launchActivity<MainActivity>()
                    }
                }
                finish()
            }
        }
    }
}