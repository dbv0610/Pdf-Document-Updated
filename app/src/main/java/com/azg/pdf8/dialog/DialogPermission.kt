package com.azg.pdf8.dialog

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.databinding.DialogGrantPermissionBinding
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.click

class DialogPermission(context: Context) :
    BaseDialog<DialogGrantPermissionBinding>(context, DialogGrantPermissionBinding::inflate, true) {
    private var action: () -> Unit = {}
    fun onAllowAccess(action: () -> Unit): DialogPermission {
        this.action = action
        return this@DialogPermission
    }

    val isSmallNative = remoteConfig.n110Config1.layout.contains("small")
    fun attachActivity(activity: AppCompatActivity): DialogPermission {
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
        return this@DialogPermission
    }

    override fun DialogGrantPermissionBinding.initView() {
        icClose.click {
            dismiss()
        }
        btnGranted.click {
            action.invoke()
            dismiss()
        }
    }

    override fun dismiss() {
        super.dismiss()
        onDismiss.invoke()
    }
    private var onDismiss: ()-> Unit = {}
    fun dismissRate(onDismiss: ()-> Unit = {}): DialogPermission{
        this@DialogPermission.onDismiss = onDismiss
        return this@DialogPermission
    }
}