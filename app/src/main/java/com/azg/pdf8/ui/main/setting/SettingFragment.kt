package com.azg.pdf8.ui.main.setting

import com.azg.pdf8.app.openUrl
import com.azg.pdf8.app.shareApp
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.ActivitySettingBinding
import com.azg.pdf8.databinding.FragmentSettingBinding
import com.azg.pdf8.dialog.RatingDialog
import com.azg.pdf8.ui.language.listLanguage
import com.azg.pdf8.ui.setting.LanguageFragment
import com.dong.baselib.base.SystemUtil
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone

class SettingFragment :
    BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate, false
    ) {
    override fun FragmentSettingBinding.initView() {
        if (SystemUtil.isRatting(appActivity)) {
            llRateApp.gone()
        }
        val langName = listLanguage.find { it.code == SystemUtil.getPreLanguage(appActivity) }?.name
            ?: "English"
        binding.langCurrent.text = langName
    }

    override fun FragmentSettingBinding.onClick() {
        llLanguage.click {
            addFragment(LanguageFragment())
        }
        llRateApp.click {
            RatingDialog(appActivity).setFinishRate {
                SystemUtil.forceRated(appActivity)
                llRateApp.gone()
            }.show()
        }
        llShareApp.click {
            appActivity.shareApp()
        }
        llPolicy.click {
            appActivity.openUrl("https://docs.google.com/document/d/e/2PACX-1vSSIuHM73h7or06HGpZankMDHi7QFU5pz3r8W6DNdSPWKUmCeq1iWLU_2ZpOorbe3ed7HkwpH-AQr2d/pub")
        }
        llTeamOfService.click {
            appActivity.openUrl(
                "https://docs.google.com/document/d/e/2PACX-1vS7rh0nkXh12B8N42Ca9dorIN_UktOcti9Hl1rzmAkGPNAb4jttD46544X405EQvsULB1Z07DXk-zQA/pub"
            )
        }
        icBack.click {
            fragmentAttach?.fragmentOnBack()
        }
    }
}