package com.azg.pdf8.ui.onboarding

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ads.control.admob.AppOpenManager
import com.azg.pdf8.R
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.FragmentOnboardingFullBinding
import com.dong.baselib.api.hideSystemBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.getValue

class OnboardingFullFragment :
    BaseFragment<FragmentOnboardingFullBinding>(FragmentOnboardingFullBinding::inflate, true) {
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
    private val position by lazy {
        runCatching { arguments?.getInt(ARG_POSITION) }.getOrNull() ?: 0
    }
    private val nativeFullConfig by lazy {
        if (position == 0) remoteConfig.n107Config2
        else remoteConfig.n108Config2
    }
    private var nativeAdsWrapper: NativeAdsWrapper? = null

    private fun getNativeAdsWrapper(): NativeAdsWrapper? {
        return (activity as? AppCompatActivity)?.let {
            NativeAdsWrapper(
                activity = it,
                config = if (position == 0) NativePlacement.ONBOARDING_FULL_1 else NativePlacement.ONBOARDING_FULL_2,
                lifecycleOwner = this,
                adContainer = { binding.flNativeAd },
                shimmerView = { binding.shimmerNativeAd.shimmerContainerNative }
            )
        }
    }

    override fun FragmentOnboardingFullBinding.initView() {
        if (nativeAdsWrapper == null) nativeAdsWrapper = getNativeAdsWrapper()
        nativeAdsWrapper?.run {
            setupNativeAd("native_full_$position")
            registerAdCallbacks(onFailed = {
                nextPage()
            }, onImpression = ::initListener)
            requestAds()
        }
        countDownSkip()
    }

    override fun FragmentOnboardingFullBinding.onClick() = Unit
    fun initListener() {
        if (nativeFullConfig.timeDelaySkip == 0L) {
            nextPage()
        }
        runCatching {
            val btnSkip = binding.flNativeAd.findViewById<View>(R.id.tvSkip)
            btnSkip.post {
                if (btnSkip.isAttachedToWindow) {
                    val lp = btnSkip.layoutParams as? ViewGroup.MarginLayoutParams
                    lp?.apply {
                        topMargin = statusBarHeight + 12
                    }?.let {
                        btnSkip.layoutParams = it
                    }


                    btnSkip.postDelayed({
                        btnSkip.isVisible = nativeFullConfig.isShowClose
                    }, nativeFullConfig.delayShowClose)
                    btnSkip?.setOnClickListener { nextPage() }

                    binding.flNativeAd.findViewById<View>(R.id.btnNext)?.let {
                        it.setOnClickListener { nextPage() }
                    }
                }
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
        viewModel.trackScreenView(
            isFullScreen = true,
            position = position
        )
    }

    private fun countDownSkip() {
        job?.cancel()
        val timeDelay = nativeFullConfig.timeDelaySkip
        if (timeDelay == 0L) return
        job = lifecycleScope.launch {
            delay(timeDelay)
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
