package com.azg.pdf8.dialog

import android.content.Context
import com.azg.pdf8.databinding.DialogOpenFileErrorBinding
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.click

/** Uses the same app dialog base and visual pattern as DeleteDialog. */
class OpenFileErrorDialog(context: Context, private val message: String) :
    BaseDialog<DialogOpenFileErrorBinding>(context, DialogOpenFileErrorBinding::inflate, true) {
    override fun DialogOpenFileErrorBinding.initView() {
        txtMessage.text = message
        btnAgree.click { dismiss() }
    }
}
