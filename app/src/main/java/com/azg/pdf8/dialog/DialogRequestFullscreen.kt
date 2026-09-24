package com.azg.pdf8.dialog

import android.content.Context
import com.azg.pdf8.R
import com.azg.pdf8.databinding.DialogFullScreenPermissionBinding
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.click
import com.dong.baselib.widget.textStyle

class DialogRequestFullscreen(activity: Context, var callback: () -> Unit = {}) :
    BaseDialog<DialogFullScreenPermissionBinding>(
        activity,
        DialogFullScreenPermissionBinding::inflate
    ) {
    override fun DialogFullScreenPermissionBinding.initView() {
        tvTitle.text = textStyle {
            normal(context.getString(R.string.allow))
            normal(" ")
            boldItalic(context.getString(R.string.app_name))
            normal(" "+context.getString(R.string.to_send_you_notifications))
        }
        btnOk.click {
            callback.invoke()
            dismiss()
        }
        btnLater.click {
            dismiss()
        }
    }
}
