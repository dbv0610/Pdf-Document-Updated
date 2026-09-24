package com.azg.pdf8.ui.feature

import android.content.Context
import android.content.Intent
import com.azg.pdf8.R
import com.azg.pdf8.app.isFinishFirstFlow
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityFeatureBinding
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.widget.color_86909c
import com.azg.pdf8.widget.mainColor
import com.dong.baselib.api.parcelable

abstract class FeatureActivity :
    BaseActivity<ActivityFeatureBinding>(ActivityFeatureBinding::inflate) {
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

         var placeLabelIds = mutableListOf<FeatureModel>()


        fun listFeature(): MutableList<FeatureModel>{
           return mutableListOf(
                FeatureModel(0, R.string.btn_home, false),
                FeatureModel(1, R.string.btn_work, false),
                FeatureModel(2, R.string.btn_library, false),
                FeatureModel(3, R.string.btn_restaurant, false),
                FeatureModel(4, R.string.btn_airport, false),
                FeatureModel(5, R.string.btn_coffee_shop, false),
                FeatureModel(6, R.string.btn_hotel, false),
                FeatureModel(7, R.string.btn_train, false),
                FeatureModel(8, R.string.btn_school, false),
                FeatureModel(9, R.string.btn_shopping_mall, false),
                FeatureModel(10, R.string.btn_living_room, false),
                FeatureModel(11, R.string.btn_park, false),
                FeatureModel(12, R.string.btn_bus, false),
                FeatureModel(13, R.string.btn_hospital, false),
                FeatureModel(14, R.string.btn_university, false)
            )
        }
    }

    private val screenType by lazy {
        runCatching {
            intent.parcelable<FeatureScreenType>(ARG_SCREEN_TYPE)
        }.getOrNull() ?: FeatureScreenType.Feature1
    }

    override fun ActivityFeatureBinding.onClick() = Unit
    override fun backPressed() {
        finishAffinity()
    }
    open fun initList(){}
    var countSelect = 0
    override fun ActivityFeatureBinding.setData() {
        initList()
        if(screenType is FeatureScreenType.Feature2){
            countSelect = placeLabelIds.count { it.isSelected }
            checkCountSelect()
        }
        val featureAdapter = FeatureAdapter(placeLabelIds) { clicked ->
            if (screenType is FeatureScreenType.Feature1) onClickFeature()
            countSelect = placeLabelIds.count { it.isSelected }
            checkCountSelect()
        }
        binding.rcvItemData.adapter = featureAdapter
    }

    private fun checkCountSelect() {
        binding.txtContinue.setTextColor(if(countSelect==0) color_86909c else mainColor)
    }

    override fun initialize() {
        binding.txtContinue.setOnClickListener {
            if(countSelect!=0){
                isFinishFirstFlow = true
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else toastShort(getString(R.string.please_choose_feature))
        }
    }

    private fun onClickFeature() {
        if (screenType is FeatureScreenType.Feature1) {
            navigateToScreenDup()
        }
    }

    private fun navigateToScreenDup() {
        start(this, FeatureScreenType.Feature2)
        overridePendingTransition(0, 0)
        finish()
    }
}
