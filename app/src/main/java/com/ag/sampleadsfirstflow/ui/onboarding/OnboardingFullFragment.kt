package com.ag.sampleadsfirstflow.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ads.control.admob.AppOpenManager
import com.ag.sampleadsfirstflow.R
import com.ag.sampleadsfirstflow.ads.native.NativeAdsWrapper
import com.ag.sampleadsfirstflow.ads.native.NativePlacement
import com.ag.sampleadsfirstflow.base.BaseFragment
import com.ag.sampleadsfirstflow.databinding.FragmentOnboardingFullBinding
import com.ag.sampleadsfirstflow.remoteconfig.analytics.Analytics
import com.ag.sampleadsfirstflow.utils.extensions.hideSystemBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class OnboardingFullFragment : BaseFragment<FragmentOnboardingFullBinding>() {
    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun newInstance(position: Int): OnboardingFullFragment {
            val fragment = OnboardingFullFragment()
            fragment.arguments = bundleOf(ARG_POSITION to position)
            return fragment
        }
    }

    private var job: Job? = null
    private val viewModel: OnboardingViewModel by inject()

    private var nativeAdsWrapper: NativeAdsWrapper? = null

    private fun getNativeAdsWrapper(): NativeAdsWrapper? {
        return (activity as? AppCompatActivity)?.let {
            NativeAdsWrapper(
                activity = it,
                config = NativePlacement.ONBOARDING_FULL,
                lifecycleOwner = this,
                adContainer = { binding.flNativeAd },
                shimmerView = { binding.shimmerNativeAd.shimmerContainerNative }
            )
        }
    }

    private val position by lazy {
        runCatching { arguments?.getInt(ARG_POSITION) }.getOrNull() ?: 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModel.listTrackingFullScreen[position] != true) {
            viewModel.listTrackingFullScreen[position] = true
            if (preferenceHelper.isUfo()) {
                Analytics.track("ufo_native_full_screen_${position + 1}")
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): FragmentOnboardingFullBinding {
        return FragmentOnboardingFullBinding.inflate(inflater)
    }

    override fun isDisplayCutout(): Boolean = true

    override fun updateUI(view: View, savedInstanceState: Bundle?) {
        if (nativeAdsWrapper == null) nativeAdsWrapper = getNativeAdsWrapper()
        nativeAdsWrapper?.run {
            setupNativeAd("native_full_$position")
            registerAdCallbacks(onImpression = ::initListener)
            requestAds()
        }
        if (position == 1) countDownSkip()
    }

    override fun initListener() {
        super.initListener()
        runCatching {
            val btnSkip = binding.flNativeAd.findViewById<View>(R.id.tvSkip)
            btnSkip?.isVisible = position == 1
            btnSkip?.setOnClickListener { nextPage() }

            binding.flNativeAd.findViewById<View>(R.id.btnNext)?.let {
                it.setOnClickListener { nextPage() }
            }
        }
    }

    private fun nextPage() {
        (activity as? OnboardingActivity)?.nextPage()
    }

    override fun onResume() {
        super.onResume()
        AppOpenManager.getInstance().disableAppResume()
        activity?.window?.hideSystemBar()
    }

    private fun countDownSkip() {
        job?.cancel()
        job = lifecycleScope.launch {
            delay(3_000L)
            if (isActive) {
                nextPage()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        job?.cancel()
    }

    override fun onDestroyView() {
        nativeAdsWrapper = null
        super.onDestroyView()
    }
}
