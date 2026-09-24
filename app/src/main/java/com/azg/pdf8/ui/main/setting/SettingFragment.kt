package com.azg.pdf8.ui.main.setting

import com.azg.pdf8.app.Aso
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
    BaseFragment<FragmentSettingBinding>(
        FragmentSettingBinding::inflate, false
    ) {
    override fun FragmentSettingBinding.initView() {
        if (SystemUtil.isRatting(appActivity)) {
            llRateApp.gone()
        }
        val langName = listLanguage.find { it.code == SystemUtil.getPreLanguage(appActivity) }?.name
            ?: "English"
        binding.langCurrent.text = langName
    }

    override fun backPress() {
        super.backPress()
        fragmentAttach?.fragmentOnBack()
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
            appActivity.openUrl(Aso.PolicyLink)
        }
        llTeamOfService.click {
            appActivity.openUrl(Aso.TemServiceLink)
        }
        icBack.click {
            fragmentAttach?.fragmentOnBack()
        }
    }
}