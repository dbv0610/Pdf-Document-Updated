package com.dong.baselib.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.viewbinding.ViewBinding
import com.dong.baselib.R
import androidx.core.graphics.drawable.toDrawable

abstract class BaseDialog<V : ViewBinding>
    (
    private val context: Context,
    val bindingFactory: (LayoutInflater) -> V,
    var cancelAble: Boolean = false,
    private var isFull: Boolean = false
) :
    Dialog(context, if (!isFull) R.style.BaseDialog else R.style.BaseDialogFull) {
    private val TAG: String = BaseDialog::class.java.name
    val binding: V by lazy { bindingFactory(layoutInflater) }
    protected abstract fun V.initView()

    init {
        initialize()
    }

    private fun initialize() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setGravity(Gravity.CENTER)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    open fun showKeyboard(view: View?) {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    open fun hideKeyboard() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window?.decorView?.rootView?.windowToken, 0)
    }

    open fun showKeyboard() {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.root, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.initView()
        setCancelable(cancelAble)
        this.setCanceledOnTouchOutside(cancelAble)
    }
    @Override
    override fun show() {
        if (isShowing) {
            dismiss()
        }
        super.show()
    }
}