package com.azg.pdf8.ui.onboarding

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.ads.control.billing.AppPurchase
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.R
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isFinishFirstFlow
import com.azg.pdf8.app.isInternetAvailable
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
    private val isSmallNative get() = remoteConfig.n104Config1.layout.contains("small")
    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this,
            config = NativePlacement.ONBOARDING,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { if (isSmallNative) binding.shimmerAdSmall.shimmerContainerNative else binding.shimmerAdMedium.shimmerContainerNative }
        )
    }
    private val listFragment by lazy {
        mutableListOf<Fragment>(
            OnboardingFragment.newInstance(0),
            OnboardingFragment.newInstance(1),
            OnboardingFragment.newInstance(2),
            OnboardingFragment.newInstance(3)
        ).apply {
            if (isShowNativeFullScreen2()) add(3, OnboardingFullFragment.newInstance(1))
            if (isShowNativeFullScreen1()) add(2, OnboardingFullFragment.newInstance(0))
        }
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
        0 to { remoteConfig.onboardingConfig.isEnableScreen1 },
        1 to { remoteConfig.onboardingConfig.isEnableScreen2 },
        2 to { !isShowNativeFullScreen1() && remoteConfig.onboardingConfig.isEnableScreen3 },
        3 to {
            if (isShowNativeFullScreen1()) remoteConfig.onboardingConfig.isEnableScreen3
            else !isShowNativeFullScreen2() && remoteConfig.onboardingConfig.isEnableScreen4
        },
        4 to { !isShowNativeFullScreen1() && !isShowNativeFullScreen2() && remoteConfig.onboardingConfig.isEnableScreen4 },
        5 to { remoteConfig.onboardingConfig.isEnableScreen4 }
    )
    private val adapter by lazy { OnboardingViewPagerAdapter(this, listFragment) }

    override fun initialize() = Unit
    override fun ActivityOnboardingBinding.onClick() = Unit

    override fun ActivityOnboardingBinding.setData() {
        nativeAdsWrapper.setupNativeAd("native_onboarding")
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
            NativeAdPreloadManager.preloadAd(
                this,
                NativePlacement.ONBOARDING_FULL_1,
                isForce = true
            )
            NativeAdPreloadManager.preloadAd(
                this,
                NativePlacement.ONBOARDING_FULL_2,
                isForce = true
            )
        } else if (position == listFragment.lastIndex && !viewModel.isPreloadNativeFeature) {
            if (remoteConfig.wellComeEnable) {
                viewModel.isPreloadNativeFeature = true
                NativeAdPreloadManager.preloadAd(this, NativePlacement.FEATURE, buffer = 2, false)
            }
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
        if (show) requestAds() else cancelAds()
    }

    private fun requestAds() {
        nativeAdsWrapper.requestAds()
        val missing = 2 - nativeAdsWrapper.getAvailableAdCount()
        if (missing > 0) nativeAdsWrapper.preloadAd(missing, isForce = true)
    }

    private fun cancelAds() {
        nativeAdsWrapper.cancelRequest()
    }

    private fun isShowNativeFullScreen1() = remoteConfig.N107Config1.enable
            && remoteConfig.isAdEnable
            && isInternetAvailable()
            && !AppPurchase.getInstance().isPurchased

    private fun isShowNativeFullScreen2() = remoteConfig.n108Config1.enable
            && remoteConfig.isAdEnable
            && isInternetAvailable()
            && !AppPurchase.getInstance().isPurchased

    override fun fragmentOnBack() {
        previousPage()
    }

    override fun backPressed() = Unit

    private fun previousPage() {
        if (binding.viewPager.currentItem == 0) finishAffinity()
        else binding.viewPager.setCurrentItem(binding.viewPager.currentItem - 1, false)
    }
}
