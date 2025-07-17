package com.azg.pdf8.dialog

import android.content.Context
import com.azg.pdf8.R
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.databinding.DialogRenameFileBinding
import com.azg.pdf8.widget.color_8c8c8c
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.afterTextChanged
import com.dong.baselib.widget.black
import com.dong.baselib.widget.click

class RenameDialog(context: Context, val onNewName: (String) -> Unit) :
    BaseDialog<DialogRenameFileBinding>(
        context,
        DialogRenameFileBinding::inflate,
        cancelAble = true
    ) {
    var lastName = ""
    override fun DialogRenameFileBinding.initView() {
        icClose.click {
            edtName.setText("")
            dismiss()
        }
        binding.btnAgree.click {
            val text = edtName.text.toString().trim()

            if (text.isEmpty()) {
                context.toastShort(context.getString(R.string.enter_new_name))
                return@click
            }

            if (text == lastName) {
                return@click
            }

            if (!isValidFileName(text)) {
                context.toastShort(context.getString(R.string.invalid_file_name))
                return@click
            }

            onNewName.invoke(text)
            dismiss()
        }
    }

    fun isValidFileName(name: String): Boolean {
        val illegalChars = "[\\\\/:*?\"<>|]".toRegex()
        return name.isNotEmpty() && !name.contains(illegalChars)
    }

    fun showRename(lastName: String) {
        binding.tvHeader.text = context.getString(R.string.rename)
        val nameWithoutExtension = lastName.substringBeforeLast(".")
        this.lastName = nameWithoutExtension
        binding.edtName.apply {
            setText(nameWithoutExtension)
            setTextColor(color_8c8c8c)
            afterTextChanged {
                setTextColor(black)
            }
        }

        this@RenameDialog.show()
    }

    override fun dismiss() {
        super.dismiss()
        binding.edtName.setText("")
    }
}