package com.azg.pdf8.ui.feature

import android.os.Bundle
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.firebase.Analytics

class Feature2Activity : FeatureActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isUfo()) {
            Analytics.track("ufo_feature_2")
        }

    }
}
