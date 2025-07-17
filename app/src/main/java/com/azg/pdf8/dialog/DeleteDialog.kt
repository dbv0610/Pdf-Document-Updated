package com.azg.pdf8.dialog

import android.content.Context
import com.dong.baselib.base.BaseDialog
import com.azg.pdf8.databinding.DialogDeleteItemBinding
import com.dong.baselib.widget.click

class DeleteDialog(
    activity: Context,
    val action: () -> Unit = {}
) :
    BaseDialog<DialogDeleteItemBinding>(
        activity,
        DialogDeleteItemBinding::inflate,
        cancelAble = true
    ) {
    override fun DialogDeleteItemBinding.initView() {
        btnAgree.click {
            dismiss()
            action.invoke()
        }
        btnCancel.click {
            dismiss()
        }
    }
}