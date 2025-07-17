package com.azg.pdf8.ui.feature

import android.os.*
import com.azg.pdf8.R
import com.azg.pdf8.databinding.*

class Feature1Activity : FeatureActivity() {

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun initList() {
        super.initList()
        placeLabelIds= listFeature()
    }

}
