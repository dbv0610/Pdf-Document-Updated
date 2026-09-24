package com.azg.pdf8.ui.setting

import android.content.Intent
import com.dong.baselib.base.BaseFragment
import com.azg.pdf8.databinding.FragmentLanguageBinding
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.ui.language.LanguageItem
import com.azg.pdf8.ui.language.listLanguage
import com.dong.baselib.base.SystemUtil
import com.dong.baselib.lifecycle.mutableLiveData
import com.dong.baselib.widget.click
import kotlin.code

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {
    override fun backPress() {
        super.backPress()
        closeSelf()
    }

    var currentLang = mutableLiveData<LanguageItem?>(null)
    private val languageAdapter by lazy {
        LanguageSettingAdapter {
            currentLang.value = it
        }.attachLifecycle(viewLifecycleOwner)
    }

    override fun FragmentLanguageBinding.onClick() {
        selectLanguage.click {
            SystemUtil.saveLocale(appContext, currentLang.value?.code ?: "en")
            appContext.startActivity(Intent(appContext, MainActivity::class.java))
            requireActivity().finishAffinity()
        }
        icBack.click {
            closeSelf()
        }
    }

    override fun FragmentLanguageBinding.initView() {
        currentLang.value = listLanguage.find {
            it.code == (SystemUtil.getPreLanguage(appContext) ?: "en")
        }
        val post = listLanguage.find { it.code == SystemUtil.getPreLanguage(appContext) }?.let {
            listLanguage.indexOf(it)
        }
        languageAdapter.currentPosition.value = post

        rcvLanguage.adapter = languageAdapter
        languageAdapter.submitList(listLanguage)
    }
}
