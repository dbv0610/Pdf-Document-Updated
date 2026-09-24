package com.azg.pdf8.widget

import com.azg.pdf8.R

enum class AspectRatio(val x: Float, val y: Float, val drawable: Int, val titleRes: Int) {
    RATIO_ORIGINAL(0f, 0f, R.drawable.ic_aspect_ratio_original, R.string.original ),
    RATIO_1_1(1f, 1f, R.drawable.ic_aspect_ratio_1_1, R.string.aspect_ratio_1_1),
    RATIO_3_4(3f, 4f, R.drawable.ic_aspect_ratio_3_4, R.string.aspect_ratio_3_4),
    RATIO_4_3(4f, 3f, R.drawable.ic_aspect_ratio_4_3, R.string.aspect_ratio_4_3),
    RATIO_2_3(2f, 3f, R.drawable.ic_aspect_ratio_2_3, R.string.aspect_ratio_2_3),
    RATIO_3_2(3f, 2f, R.drawable.ic_aspect_ratio_3_2, R.string.aspect_ratio_3_2),
    RATIO_9_16(9f, 16f, R.drawable.ic_aspect_ratio_9_16, R.string.aspect_ratio_9_16),
    RATIO_16_9(16f, 9f, R.drawable.ic_aspect_ratio_16_9, R.string.aspect_ratio_16_9);
}