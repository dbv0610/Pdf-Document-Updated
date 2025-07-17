package com.azg.pdf8.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.azg.pdf8.R
import com.azg.pdf8.databinding.ActivityOnboardingBinding
import com.azg.pdf8.ui.feature.FeatureActivity
import com.azg.pdf8.ui.feature.FeatureScreenType
import com.azg.pdf8.ui.language.LanguageScreenType
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.widget.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnboardingActivity :
    BaseActivity<ActivityOnboardingBinding>(ActivityOnboardingBinding::inflate, true) {
    private var isFirstRequestNativeOnb = true

    fun changeData(data: Int) {
        val post = data
        binding.tvTitle.text = if (post == 0) {
            getString(R.string.title_onboarding_1)
        } else if (post == 1) {
            getString(R.string.title_onboarding_2)
        } else if (post == 2) {
            getString(R.string.title_onboarding_3)
        } else {
            getString(R.string.title_onboarding_4)
        }
        binding.tvContent.text = if (post == 0) {
            getString(R.string.content_onboarding_1)
        } else if (post == 1) {
            getString(R.string.content_onboarding_2)
        } else if (post == 2) {
            getString(R.string.content_onboarding_3)
        } else {
            getString(R.string.content_onboarding_4)
        }
    }

    private val listFragment by lazy {
        mutableListOf<Fragment>(
            OnboardingFragment.newInstance(0),
            OnboardingFragment.newInstance(1),
            OnboardingFragment.newInstance(2),
            OnboardingFragment.newInstance(3),
        )
    }
    private val adapter by lazy {
        OnboardingViewPagerAdapter(this, listFragment)
    }

    private fun updateTextNext() {
        if (getCurrentItem() >= listFragment.size - 1) {
            binding.tvNext.text = getString(R.string.continues)
        } else {
            binding.tvNext.text = getString(R.string.next)
        }
    }

    private fun setActiveIndicator() {
        var indexActive = getCurrentItem()
        changeData(indexActive)
        binding.indicatorView.setIndicatorActive(indexActive)
    }

    private fun getCurrentItem(): Int {
        return binding.viewPager.currentItem
    }

    override fun fragmentOnBack() {
        super.fragmentOnBack()
        previousPage()
    }

    fun nextPage() {
        if (getCurrentItem() >= listFragment.size - 1) {
            FeatureActivity.start(this@OnboardingActivity, FeatureScreenType.Feature1)
            finish()
        } else {
            binding.viewPager.setCurrentItem(getCurrentItem() + 1, false)
        }
    }

    private fun previousPage() {
        if (getCurrentItem() == 0) {
            finishAffinity()
        } else {
            binding.viewPager.setCurrentItem(getCurrentItem() - 1, false)
        }
    }

    override fun backPressed() {
    }

    override fun initialize() {}

    override fun ActivityOnboardingBinding.onClick() {
    }

    override fun ActivityOnboardingBinding.setData() {
        binding.tvNext.setOnClickListener { nextPage() }
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTextNext()
                setActiveIndicator()
                lifecycleScope.launch(Dispatchers.Main) {
                    checkLoadAds()
                }
            }
        })
    }

    private fun checkLoadAds() {
        when (getCurrentItem()) {
            0 -> {
                binding.llTabLayout.visible()
                binding.tvTitle.visible()
//                if (isFirstRequestNativeOnb) {
//                    firstRequestNative()
//                } else {
//                    requestAds()
//                }
            }
            2 -> {
//                if (showNativeFullScreen()) {
//                    binding.llTabLayout.gone()
//                    binding.tvTitle.gone()
//                    nativeAdsWrapper.cancelRequest()
//                } else {
                binding.llTabLayout.visible()
                binding.tvTitle.visible()
//                    requestAds()
//                }
            }
            4 -> {
//                if (showNativeFullScreen()) {
//                    binding.llTabLayout.gone()
//                    binding.tvTitle.gone()
//                    nativeAdsWrapper.cancelRequest()
//                } else {
                binding.llTabLayout.visible()
                binding.tvTitle.visible()
                //  requestAds()
                // }
            }
            else -> {
                binding.llTabLayout.visible()
                binding.tvTitle.visible()
                requestAds()
            }
        }
    }

    private fun firstRequestNative() {
        //  nativeAdsWrapper.requestPreloadAds()
    }

    private fun requestAds() {
//        with(nativeAdsWrapper) {
//            requestAds()
//            val sizeQueue = getAvailableAdCount()
//            if (sizeQueue < 2) {
//                preloadAd(2 - sizeQueue)
//            }
//        }
    }
}
