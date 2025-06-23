package com.ag.sampleadsfirstflow.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.ads.control.admob.AppOpenManager
import com.ag.sampleadsfirstflow.R
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.ag.sampleadsfirstflow.ads.native.NativePlacement
import com.ag.sampleadsfirstflow.base.BaseFragment
import com.ag.sampleadsfirstflow.databinding.FragmentOnboardingBinding
import com.ag.sampleadsfirstflow.remoteconfig.analytics.Analytics
import com.ag.sampleadsfirstflow.remoteconfig.remoteAds
import com.bumptech.glide.Glide
import org.koin.android.ext.android.inject

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>() {
    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun newInstance(position: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            fragment.arguments = bundleOf(ARG_POSITION to position)
            return fragment
        }
    }

    private val viewModel: OnboardingViewModel by inject()
    private val position by lazy {
        runCatching { arguments?.getInt(ARG_POSITION) }.getOrNull() ?: 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModel.listTrackingScreen[position] != true) {
            viewModel.listTrackingScreen[position] = true
            if (preferenceHelper.isUfo()) {
                Analytics.track("ufo_onboarding_${position + 1}")
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): FragmentOnboardingBinding {
        return FragmentOnboardingBinding.inflate(inflater)
    }

    override fun isDisplayCutout(): Boolean = true

    override fun updateUI(view: View, savedInstanceState: Bundle?) {
        if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            binding.lavSwipe.scaleX = -1f
        } else {
            binding.lavSwipe.scaleX = 1f
        }
        when (position) {
            0 -> {
                Glide.with(getContextF())
                    .load(R.drawable.img_onboarding_1)
                    .into(binding.imgOnbBackground)
                binding.tvTitle.text = getString(R.string.title_onboarding_1)
                if (!viewModel.isPreloadNativeFull) {
                    viewModel.isPreloadNativeFull = true
                    activity?.let { NativeAdPreloadManager.preloadAd(activity = it, placement = NativePlacement.ONBOARDING_FULL, buffer = 2) }
                }
            }

            1 -> {
                Glide.with(getContextF())
                    .load(R.drawable.img_onboarding_2)
                    .into(binding.imgOnbBackground)
                binding.tvTitle.text = getString(R.string.title_onboarding_2)
            }

            2 -> {
                Glide.with(getContextF())
                    .load(R.drawable.img_onboarding_3)
                    .into(binding.imgOnbBackground)
                binding.tvTitle.text = getString(R.string.title_onboarding_3)
            }

            else -> {
                Glide.with(getContextF())
                    .load(R.drawable.img_onboarding_4)
                    .into(binding.imgOnbBackground)
                binding.tvTitle.text = getString(R.string.title_onboarding_4)
                if (!preferenceHelper.isFinishFirstFlow && !viewModel.isPreloadNativeFeature) {
                    viewModel.isPreloadNativeFeature = true
                    activity?.let {
                        NativeAdPreloadManager.preloadAd(activity = it, placement = NativePlacement.FEATURE, 2)
                    }
                }
            }
        }

        binding.lavSwipe.isVisible = position == 1
        binding.tvSwipe.isVisible = position == 1
    }

    override fun onResume() {
        super.onResume()
        if (remoteAds.isShowA001) AppOpenManager.getInstance().enableAppResume()
    }
}
