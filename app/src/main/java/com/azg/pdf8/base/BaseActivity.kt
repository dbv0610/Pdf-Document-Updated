package com.azg.pdf8.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import androidx.viewbinding.ViewBinding
import com.azg.pdf8.R
import com.azg.pdf8.app.countGrantedCamera
import com.azg.pdf8.app.countGrantedRecognize
import com.azg.pdf8.app.countGrantedLocation
import com.azg.pdf8.app.countGrantedNotification
import com.azg.pdf8.app.toastShort
import com.dong.baselib.api.isApi33orHigher
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.permission.Permission
import com.dong.baselib.widget.delay
import org.koin.android.ext.android.inject
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

abstract class BaseActivity<VB : ViewBinding>(
    override val bindingFactory: (LayoutInflater) -> VB,
    private var fullStatus: Boolean = false,
) : BaseActivity<VB>(bindingFactory, fullStatus) {
    val permission by inject<Permission>()
    private var yDown = 0f
    private var isMove = false
    var requestLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            handlePermissionDenied(
                Manifest.permission.ACCESS_FINE_LOCATION,
                ::countGrantedLocation,
            )
        }
    }
    var requestRecognizeLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            if (Build.VERSION.SDK_INT > 29) {
                handlePermissionDenied(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    ::countGrantedRecognize,
                )
            }
        }
    }
    var requestCameraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            if (Build.VERSION.SDK_INT > 29) {
                handlePermissionDenied(
                    Manifest.permission.CAMERA,
                    ::countGrantedCamera,
                )
            }
        }
    }
    var requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions ->
        if (!permissions) {
            if (isApi33orHigher) {
                handlePermissionDenied(
                    Manifest.permission.POST_NOTIFICATIONS,
                    ::countGrantedNotification,
                )
            }
        }
    }

    fun handlePermissionDenied(
        permission: String,
        counter: KMutableProperty0<Int>,
    ) {
        if (!shouldShowRequestPermissionRationale(permission)) {
            counter.set(counter.get() + 1)
            if (counter.get() > 1) {
                toastShort(getString(R.string.request_permission_need_to_use_fun))
                delay(1500){
                    goToSetting()
                }
            }
        }
    }

    open fun goToSetting() {
        val intent = Intent()
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.setData(uri)
        startActivity(intent)
    }
    @SuppressLint("ClickableViewAccessibility")
    fun hideKeyboardScrollView(scrollView: NestedScrollView, action: (() -> Unit)? = {}) {
        scrollView.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    yDown = motionEvent.y
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMove) {
                        hideKeyboard()
                        action?.invoke()
                    }
                    isMove = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val yMove = motionEvent.y
                    val distY: Float = yMove - yDown
                    if (abs(distY) >= 10) {
                        isMove = true
                    }
                }
            }
            false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (currentFocus is EditText) {
            val rect = Rect()
            currentFocus!!.getGlobalVisibleRect(rect)
            if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                hideKeyboard()
                currentFocus!!.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }
    @SuppressLint("ClickableViewAccessibility")
    fun hideKeyboardByView(scrollView: View, action: (() -> Unit)? = {}) {
        scrollView.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    yDown = motionEvent.y
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMove) {
                        hideKeyboard()
                        action?.invoke()
                    }
                    isMove = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val yMove = motionEvent.y
                    val distY: Float = yMove - yDown
                    if (abs(distY) >= 10) {
                        isMove = true
                    }
                }
            }
            false
        }
    }

    fun scrollKeyboardShow() {
        binding.root.post {
            binding.root.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15
                binding.root.setPadding(
                    binding.root.paddingLeft,
                    binding.root.paddingTop,
                    binding.root.paddingRight,
                    if (isKeyboardVisible) keypadHeight else 12
                )
            }
        }
    }
}