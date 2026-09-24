package com.azg.pdf8.base

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.NestedScrollView
import androidx.viewbinding.R
import androidx.viewbinding.ViewBinding
import com.dong.baselib.base.BaseFragment
import com.dong.baselib.permission.Permission
import org.koin.android.ext.android.inject
import kotlin.getValue
import kotlin.math.abs

abstract class BaseFragment<VB : ViewBinding>(
    override val bindingFactory: (LayoutInflater) -> VB,
    override var isFullSc: Boolean = false
) : BaseFragment<VB>(bindingFactory, isFullSc) {
    val permission by inject<Permission>()
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.setOnTouchListener { _, ev ->
            handleTouchOutsideEditText(ev)
            false
        }
    }
    private fun handleTouchOutsideEditText(ev: MotionEvent) {
        val focusedView = appActivity.currentFocus
        if (focusedView is EditText) {
            val rect = Rect()
            focusedView.getGlobalVisibleRect(rect)
            if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                hideKeyboard()
                focusedView.clearFocus()
            }
        }
    }
    fun isStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            permission.arePermissionsGranted(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}
