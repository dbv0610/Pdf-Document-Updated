package com.azg.pdf8.ui.onboarding

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.azg.pdf8.R
import com.azg.pdf8.app.isFinishFirstFlow
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.ActivityOnboardingBinding
import com.azg.pdf8.ui.feature.FeatureActivity
import com.azg.pdf8.ui.feature.FeatureScreenType
import com.azg.pdf8.ui.main.MainActivity
import com.dong.baselib.base.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class OnboardingActivity :
    BaseActivity<ActivityOnboardingBinding>(ActivityOnboardingBinding::inflate, true) {
    private val viewModel: OnboardingViewModel by inject()

    private val listFragment by lazy {
        mutableListOf<Fragment>(
            OnboardingFragment.newInstance(0),
            OnboardingFragment.newInstance(1),
            OnboardingFragment.newInstance(2),
            OnboardingFragment.newInstance(3)
        )
    }
    private val titles = arrayOf(
        R.string.title_onboarding_1,
        R.string.title_onboarding_2,
        R.string.title_onboarding_3,
        R.string.title_onboarding_4
    )
    private val contents = arrayOf(
        R.string.content_onboarding_1,
        R.string.content_onboarding_2,
        R.string.content_onboarding_3,
        R.string.content_onboarding_4
    )
    private val showAdConfig: Map<Int, () -> Boolean> = mapOf(
        0 to {false },
        1 to { false},
        2 to { false},
        3 to {
            false
        },
        4 to { false},
        5 to {false }
    )
    private val adapter by lazy { OnboardingViewPagerAdapter(this, listFragment) }

    override fun initialize() = Unit
    override fun ActivityOnboardingBinding.onClick() = Unit

    override fun ActivityOnboardingBinding.setData() {
        tvNext.setOnClickListener { nextPage() }
        viewPager.adapter = adapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTextNext()
                changeData(position)
                setActiveIndicator()
                lifecycleScope.launch(Dispatchers.Main) { checkLoadAds() }
            }
        })
    }

    private fun getCurrentItem(): Int {
        return binding.viewPager.currentItem
    }

    private fun setActiveIndicator() {
        val currentItem = getCurrentItem()
        val currentFragment = listFragment.getOrNull(currentItem)
        if (currentFragment !is OnboardingFragment) return
        val indicatorIndex = listFragment
            .take(currentItem + 1)
            .count { it is OnboardingFragment } - 1

        binding.indicatorView.setIndicatorActive(indicatorIndex)
        binding.tvTitle.text = getString(titles[indicatorIndex])
        binding.tvContent.text = getString(contents[indicatorIndex])
    }

    private fun updateTextNext() {
        val nextText =
            if (binding.viewPager.currentItem == listFragment.lastIndex) R.string.continues else R.string.next
        binding.tvNext.text = getString(nextText)
    }

    fun changeData(position: Int) {
        if (position == 0 && !viewModel.isPreloadNativeFull) {
            viewModel.isPreloadNativeFull = true

        } else if (position == listFragment.lastIndex && !viewModel.isPreloadNativeFeature) {

        }
    }

    fun nextPage() {
        val current = binding.viewPager.currentItem
        if (current == listFragment.lastIndex) {
            if (remoteConfig.wellComeEnable) {
                FeatureActivity.start(this@OnboardingActivity, FeatureScreenType.Feature1)
            } else {
                launchActivity<MainActivity>()
                isFinishFirstFlow = true
            }
            finish()
        } else {
            binding.viewPager.setCurrentItem(current + 1, false)
        }
    }

    private fun checkLoadAds() {
        val idx = binding.viewPager.currentItem
        val show = showAdConfig[idx]?.invoke() == true
        binding.llTabLayout.isVisible = show

    }

    override fun fragmentOnBack() {
        previousPage()
    }

    override fun backPressed() = Unit

    private fun previousPage() {
        if (binding.viewPager.currentItem == 0) finishAffinity()
        else binding.viewPager.setCurrentItem(binding.viewPager.currentItem - 1, false)
    }
}
