package com.azg.pdf8.dialog

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
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
     fun attachActivity(activity: AppCompatActivity): DialogCreatePdf {

        return this@DialogCreatePdf
    }
}