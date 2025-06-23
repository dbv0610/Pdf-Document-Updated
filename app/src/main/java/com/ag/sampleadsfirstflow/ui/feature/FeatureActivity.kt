package com.ag.sampleadsfirstflow.ui.feature

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.recyclerview.widget.GridLayoutManager
import com.ag.sampleadsfirstflow.base.BaseActivity
import com.ag.sampleadsfirstflow.databinding.ActivityFeatureBinding
import com.ag.sampleadsfirstflow.model.FeatureScreenType
import com.ag.sampleadsfirstflow.ui.home.MainActivity
import com.ag.sampleadsfirstflow.utils.Resources
import com.ag.sampleadsfirstflow.utils.extensions.gone
import com.ag.sampleadsfirstflow.utils.extensions.parcelable
import com.ag.sampleadsfirstflow.utils.extensions.visible
import com.ag.sampleadsfirstflow.utils.view.GridSpacingItemDecoration
import com.intuit.sdp.R

abstract class FeatureActivity : BaseActivity<ActivityFeatureBinding>() {
    companion object {
        private var scrollOffsetY: Int = 0
        private const val ARG_SCREEN_TYPE = "ARG_SCREEN_TYPE"

        fun start(
            context: Context,
            screenType: FeatureScreenType,
        ) {
            val clazz = when (screenType) {
                is FeatureScreenType.Feature1 -> Feature1Activity::class.java
                is FeatureScreenType.Feature2 -> Feature2Activity::class.java
            }
            val intent = Intent(context, clazz)
            intent.putExtra(ARG_SCREEN_TYPE, screenType)
            context.startActivity(intent)
        }
    }

    private val featureAdapter by lazy {
        FeatureAdapter { onClickFeature() }
    }

    private val screenType by lazy {
        runCatching {
            intent.parcelable<FeatureScreenType>(ARG_SCREEN_TYPE)
        }.getOrNull() ?: FeatureScreenType.Feature1
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityFeatureBinding {
        return ActivityFeatureBinding.inflate(layoutInflater)
    }

    override fun updateUI(savedInstanceState: Bundle?) {
        initListener()
        binding.rcvFeature.layoutManager = GridLayoutManager(this, 3).also {
            if (screenType is FeatureScreenType.Feature2) {
                it.scrollToPositionWithOffset(0, -1 * scrollOffsetY)
            }
        }
        binding.rcvFeature.adapter = featureAdapter
        binding.rcvFeature.addItemDecoration(
            GridSpacingItemDecoration(
                3,
                resources.getDimensionPixelSize(R.dimen._6sdp)
            )
        )
        if (screenType is FeatureScreenType.Feature1) {
            Resources.listFeature.forEach {
                it.isSelected = false
            }
            binding.btnDone.gone()
        } else {
            binding.btnDone.visible()
        }
        featureAdapter.submitList(Resources.listFeature)
    }

    private fun initListener() {
        binding.btnDone.setOnClickListener {
            preferenceHelper.isFinishFirstFlow = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun onClickFeature() {
        if (screenType is FeatureScreenType.Feature1) {
            navigateToScreenDup()
        }
    }

    private fun navigateToScreenDup() {
        scrollOffsetY = binding.rcvFeature.computeVerticalScrollOffset()
        start(this, FeatureScreenType.Feature2)
        overridePendingTransition(0, 0)
        finish()
    }
}
