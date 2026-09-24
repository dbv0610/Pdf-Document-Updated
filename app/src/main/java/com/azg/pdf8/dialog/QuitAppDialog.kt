package com.azg.pdf8.dialog

import android.app.Activity
import com.dong.baselib.base.BaseDialog
import com.azg.pdf8.databinding.DialogQuitAppBinding
import com.dong.baselib.widget.click

class QuitAppDialog(activity:Activity, var action:()->Unit) : BaseDialog<DialogQuitAppBinding>(activity,DialogQuitAppBinding::inflate, true) {

    override fun DialogQuitAppBinding.initView() {
      btnAgree.click {
          action.invoke()
          dismiss()
      }
        btnCancel.click {
            dismiss()
        }

    }

}