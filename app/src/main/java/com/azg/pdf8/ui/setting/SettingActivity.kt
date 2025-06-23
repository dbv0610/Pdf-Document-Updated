package com.azg.pdf8.ui.setting

import android.annotation.SuppressLint
import com.azg.pdf8.app.openUrl
import com.azg.pdf8.app.shareApp
import com.azg.pdf8.databinding.ActivitySettingBinding
import com.azg.pdf8.dialog.RatingDialog
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.base.SystemUtil
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone

class SettingActivity : BaseActivity<ActivitySettingBinding>(ActivitySettingBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    override fun initialize() = Unit

    override fun ActivitySettingBinding.onClick() {
        llLanguage.click {
            addFragment(LanguageFragment())
        }
        llRateApp.click {
            RatingDialog(this@SettingActivity).setFinishRate {
                SystemUtil.forceRated(this@SettingActivity)
                llRateApp.gone()
            }.show()
        }
        llShareApp.click {
            this@SettingActivity.shareApp()
        }
        llPolicy.click {
            this@SettingActivity.openUrl("https://docs.google.com/document/d/e/2PACX-1vSSIuHM73h7or06HGpZankMDHi7QFU5pz3r8W6DNdSPWKUmCeq1iWLU_2ZpOorbe3ed7HkwpH-AQr2d/pub")
        }
        llTeamOfService.click {
            openUrl(
                "https://docs.google.com/document/d/e/2PACX-1vS7rh0nkXh12B8N42Ca9dorIN_UktOcti9Hl1rzmAkGPNAb4jttD46544X405EQvsULB1Z07DXk-zQA/pub"
            )
        }
        icBack.click {
            backPressed()
        }
    }
    @SuppressLint("SetTextI18n")
    override fun ActivitySettingBinding.setData() {
        if (SystemUtil.isRatting(this@SettingActivity)) {
            llRateApp.gone()
        }

    }
}