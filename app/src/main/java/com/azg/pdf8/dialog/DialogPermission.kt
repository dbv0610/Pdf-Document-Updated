package com.azg.pdf8.dialog

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
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

    fun attachActivity(activity: AppCompatActivity): DialogPermission {

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