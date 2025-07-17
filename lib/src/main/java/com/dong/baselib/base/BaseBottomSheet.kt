package com.dong.baselib.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.viewbinding.ViewBinding
import com.dong.baselib.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


abstract class BaseBottomSheet<V : ViewBinding>(private val activity: FragmentActivity) :
    BottomSheetDialogFragment() {

    companion object {
        const val TAG = "BaseBottomSheet"
    }

    open fun showKeyboard(view: View?) {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }


    open fun showKeyboard() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.root, InputMethodManager.SHOW_IMPLICIT)
    }

    open fun hideKeyboard() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().window.decorView.rootView.windowToken, 0)
    }

    lateinit var binding: V
    private var isAttached = false


    abstract fun initView(inflater: LayoutInflater, container: ViewGroup?): V

    abstract fun V.onBind()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = initView(inflater, container)
        binding.onBind()
        return binding.root
    }

    lateinit var appContext: Context

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.appContext = context
        isAttached = true
    }

    override fun onDetach() {
        super.onDetach()
        isAttached = false
    }

    fun show() {
        val existingFragment = activity.supportFragmentManager.findFragmentByTag(TAG)
        if (existingFragment != null && existingFragment.isAdded) {
            return
        }
        val transaction: FragmentTransaction = activity.supportFragmentManager.beginTransaction()
        this.show(transaction, TAG)
    }
}
