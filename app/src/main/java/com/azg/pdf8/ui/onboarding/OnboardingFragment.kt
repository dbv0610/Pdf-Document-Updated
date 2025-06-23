package com.azg.pdf8.ui.onboarding

import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.azg.pdf8.R
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.FragmentOnboardingBinding
import com.bumptech.glide.Glide
import org.koin.android.ext.android.inject

class OnboardingFragment :
    BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate, true) {
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

    override fun backPress() {
        super.backPress()
        fragmentAttach?.fragmentOnBack()
    }

    override fun FragmentOnboardingBinding.initView() {
        if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            binding.lavSwipe.scaleX = -1f
        } else {
            binding.lavSwipe.scaleX = 1f
        }
        when (position) {
            0 -> {
                Glide.with(appContext)
                    .load(R.drawable.img_intro_1)
                    .into(binding.imgOnbBackground)
                fragmentAttach?.fragmentSendData("post", 0)
            }
            1 -> {
                Glide.with(appContext)
                    .load(R.drawable.img_intro_2)
                    .into(binding.imgOnbBackground)
                fragmentAttach?.fragmentSendData("post", 1)
            }
            2 -> {
                Glide.with(appContext)
                    .load(R.drawable.img_intro_3)
                    .into(binding.imgOnbBackground)
                fragmentAttach?.fragmentSendData("post", 2)
            }
            else -> {
                Glide.with(appContext)
                    .load(R.drawable.img_intro_4)
                    .into(binding.imgOnbBackground)
                fragmentAttach?.fragmentSendData("post", 3)
            }
        }

        binding.lavSwipe.isVisible = position == 1
        binding.tvSwipe.isVisible = position == 1
    }

    override fun FragmentOnboardingBinding.onClick() {
    }
}
