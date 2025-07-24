package com.azg.pdf8.dialog

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.CreatePdfDialogBinding
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.click

interface CreateEventHandle {
    fun createImage()
    fun scanDocument()
}

class DialogCreatePdf(context: Context, val eventHandle: CreateEventHandle) :
    BaseDialog<CreatePdfDialogBinding>(context, CreatePdfDialogBinding::inflate, true){
    override fun CreatePdfDialogBinding.initView() {
        lnCreateScan.click {
            eventHandle.scanDocument()
            dismiss()
        }
        lnCreateImage.click {
            eventHandle.createImage()
            dismiss()
        }
        icClose.click {
            dismiss()
        }
    }
    val isSmallNative = remoteConfig.N110Config1.layout.contains("small")
    fun attachActivity(activity: AppCompatActivity): DialogCreatePdf {
        val nativeAdsWrapper by lazy {
            NativeAdsWrapper(
                activity = activity,
                config = NativePlacement.PERMISSION,
                lifecycleOwner = activity,
                adContainer = { binding.flNativeAd },
                shimmerView = { if (isSmallNative) binding.shimmerAdSmall.shimmerContainerNative else binding.shimmerAdMedium.shimmerContainerNative }
            )
        }
        with(nativeAdsWrapper) {
            setupNativeAd("native_permission")
            requestAds()
        }
        return this@DialogCreatePdf
    }
}