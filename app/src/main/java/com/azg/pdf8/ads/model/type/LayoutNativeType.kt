package com.azg.pdf8.ads.model.type

import androidx.annotation.LayoutRes
import com.azg.pdf8.R

sealed class LayoutNativeType(val type: String, @LayoutRes val resLayout: Int = 0) {
    data object NativeSmallCtaBottom :
        LayoutNativeType("native_small_cta_bottom", R.layout.layout_native_small_cta_bot)

    data object NativeSmallCtaTop :
        LayoutNativeType("native_small_cta_top", R.layout.layout_native_small_cta_top)

    data object NativeSmallCtaRight :
        LayoutNativeType("native_small_cta_right", R.layout.layout_native_small_cta_right)

    data object NativeMediumCtaBottom :
        LayoutNativeType("native_medium_cta_bottom", R.layout.layout_native_medium_cta_bot)

    data object NativeMediumCtaTop :
        LayoutNativeType("native_medium_cta_top", R.layout.layout_native_medium_cta_top)

    data object MediumCtaRightBottom :
        LayoutNativeType("medium_cta_right_bottom", R.layout.layout_native_medium_cta_bot_right)

    data object MediumCtaRightTop :
        LayoutNativeType("medium_cta_right_top", R.layout.layout_native_medium_cta_top_right)

    data object Other : LayoutNativeType("other")
}