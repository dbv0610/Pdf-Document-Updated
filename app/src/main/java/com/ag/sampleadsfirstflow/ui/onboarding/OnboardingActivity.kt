package com.ag.sampleadsfirstflow.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.ag.sampleadsfirstflow.R
import com.ag.sampleadsfirstflow.ads.native.NativeAdsWrapper
import com.ag.sampleadsfirstflow.ads.native.NativePlacement
import com.ag.sampleadsfirstflow.base.BaseActivity
import com.ag.sampleadsfirstflow.databinding.ActivityOnboardingBinding
import com.ag.sampleadsfirstflow.model.FeatureScreenType
import com.ag.sampleadsfirstflow.remoteconfig.remoteAds
import com.ag.sampleadsfirstflow.ui.feature.FeatureActivity
import com.ag.sampleadsfirstflow.ui.home.MainActivity
import com.ag.sampleadsfirstflow.utils.extensions.gone
import com.ag.sampleadsfirstflow.utils.extensions.isInternetAvailable
import com.ag.sampleadsfirstflow.utils.extensions.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {
    private var isFirstRequestNativeOnb = true

    private val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this,
            config = NativePlacement.ONBOARDING,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { binding.shimmerAd.shimmerContainerNative }
        )
    }

    private val listFragment by lazy {
        mutableListOf<Fragment>(
            OnboardingFragment.newInstance(0),
            OnboardingFragment.newInstance(1),
            OnboardingFragment.newInstance(2),
            OnboardingFragment.newInstance(3),
        ).apply {
            if (showNativeFullScreen()) {
                this.add(3, OnboardingFullFragment.newInstance(1))
                this.add(2, OnboardingFullFragment.newInstance(0))
            }
        }
    }

    private val adapter by lazy {
        OnboardingViewPagerAdapter(this, listFragment)
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityOnboardingBinding {
        return ActivityOnboardingBinding.inflate(layoutInflater)
    }

    override fun isDisplayCutout(): Boolean = true

    override fun updateUI(savedInstanceState: Bundle?) {
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

    private fun updateTextNext() {
        if (getCurrentItem() >= listFragment.size - 1) {
            binding.tvNext.text = getString(R.string.get_started)
        } else {
            binding.tvNext.text = getString(R.string.next)
        }
    }

    private fun setActiveIndicator() {
        var indexActive = getCurrentItem()
        if (showNativeFullScreen()) {
            if (indexActive >= 4) {
                indexActive -= 2
            } else if (indexActive >= 2) {
                indexActive--
            }
        }
        binding.indicatorView.setIndicatorActive(indexActive)
    }

    private fun getCurrentItem(): Int {
        return binding.viewPager.currentItem
    }

    fun nextPage() {
        if (getCurrentItem() >= listFragment.size - 1) {
            if (!preferenceHelper.isFinishFirstFlow) {
                FeatureActivity.start(this, FeatureScreenType.Feature1)
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        } else {
            binding.viewPager.setCurrentItem(getCurrentItem() + 1, false)
        }
    }

    private fun previousPage() {
        if (getCurrentItem() == 0) {
            finish()
        } else {
            binding.viewPager.setCurrentItem(getCurrentItem() - 1, false)
        }
    }

    override fun onResume() {
        super.onResume()
        handleBackPress()
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                previousPage()
            }
        })
    }

    override fun loadAd() {
        super.loadAd()
        nativeAdsWrapper.setupNativeAd("native_onboarding")
    }

    private fun checkLoadAds() {
        when (getCurrentItem()) {
            0 -> {
                binding.llTabLayout.visible()
                if (isFirstRequestNativeOnb) {
                    firstRequestNative()
                } else {
                    requestAds()
                }
            }

            2 -> {
                if (showNativeFullScreen()) {
                    binding.llTabLayout.gone()
                    nativeAdsWrapper.cancelRequest()
                } else {
                    binding.llTabLayout.visible()
                    requestAds()
                }
            }

            4 -> {
                if (showNativeFullScreen()) {
                    binding.llTabLayout.gone()
                    nativeAdsWrapper.cancelRequest()
                } else {
                    binding.llTabLayout.visible()
                    requestAds()
                }
            }

            else -> {
                binding.llTabLayout.visible()
                requestAds()
            }
        }
    }

    private fun firstRequestNative() {
        nativeAdsWrapper.requestAds()
    }

    private fun showNativeFullScreen(): Boolean {
        return remoteAds.isShowN007 && isInternetAvailable()
    }

    private fun requestAds() {
        with(nativeAdsWrapper) {
            requestAds()
            val sizeQueue = getAvailableAdCount()
            if (sizeQueue < 2) {
                preloadAd(2 - sizeQueue, isForce = true)
            }
        }
    }
}
