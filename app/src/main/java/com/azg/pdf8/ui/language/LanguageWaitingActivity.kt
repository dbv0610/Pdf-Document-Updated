package com.azg.pdf8.ui.language

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityLanguageWaitingBinding
import com.dong.baselib.widget.moveItemToPosition
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LanguageWaitingActivity :
    BaseActivity<ActivityLanguageWaitingBinding>(ActivityLanguageWaitingBinding::inflate) {
    private var job: Job? = null
    private val languageAdapter by lazy { LfoAdapter() }

    companion object {
        var isShowAdInWaiting = false
    }

    override fun backPressed() = Unit
    override fun initialize() = Unit
    override fun ActivityLanguageWaitingBinding.setData() {
        countDownSkip()
        setupListLanguage()
    }

    fun nextAction() {
        LanguageOpenActivity.start(this@LanguageWaitingActivity, LanguageScreenType.Language1)
        overridePendingTransition(0, 0)
        finish()
    }

    override fun ActivityLanguageWaitingBinding.onClick() = Unit

    private fun countDownSkip() {
        job?.cancel()
        job = lifecycleScope.launch {
            val totalDuration = 2000L
            val maxProgress = 100
            for (progress in 0..maxProgress) {
                if (!isActive) return@launch
                binding.progressIndicator.progress = progress
                delay(totalDuration / maxProgress)
            }
            binding.progressIndicator.progress = maxProgress

            nextAction()
        }
    }

    private fun setupListLanguage() {
        languageAdapter.setEnable(false)
        languageAdapter.submitList(getListLanguageLfo())
        binding.rcvLanguage.adapter = languageAdapter
    }

    private fun getListLanguageLfo(): List<LanguageItem> {
        val deviceLanguage = Resources.getSystem().configuration.locales[0].language
        val indexLanguageDevice = listLanguage.indexOfFirst { it.code == deviceLanguage }
        val listLfo = if (indexLanguageDevice != -1) {
            listLanguage[indexLanguageDevice].isDefault = true
            listLanguage.moveItemToPosition(3) { it.code == deviceLanguage }
        } else {
            listLanguage[0].isDefault = true
            listLanguage
        }
        return listLfo
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}
