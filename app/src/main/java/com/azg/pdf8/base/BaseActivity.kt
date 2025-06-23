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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import androidx.viewbinding.ViewBinding
import com.azg.pdf8.app.countGrantedRecognize
import com.azg.pdf8.app.countGrantedLocation
import com.azg.pdf8.app.countGrantedNotification
import com.dong.baselib.api.isApi33orHigher
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.permission.Permission
import org.koin.android.ext.android.inject
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

abstract class BaseActivity<VB : ViewBinding>(
    override val bindingFactory: (LayoutInflater) -> VB,
    private var fullStatus: Boolean = false,
) : BaseActivity<VB>(bindingFactory, fullStatus) {
    enum class TypeGoSettings {
        NONE,
        NOTIFICATION,
        ACTIVITY_RECOGNITION,
        LOCATION
    }

    private val permission by inject<Permission>()
    private var yDown = 0f
    private var isMove = false
    var requestLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            handlePermissionDenied(
                Manifest.permission.ACCESS_FINE_LOCATION,
                ::countGrantedLocation,
                TypeGoSettings.LOCATION
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
                    TypeGoSettings.ACTIVITY_RECOGNITION
                )
            }
        }
    }
    private val permissionsLiveData = MutableLiveData<PermissionsState?>(null)

    data class PermissionsState(var state: Boolean = false, var typeGoSettings: TypeGoSettings)

    var requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions ->
        if (!permissions) {
            if (isApi33orHigher) {
                handlePermissionDenied(
                    Manifest.permission.POST_NOTIFICATIONS,
                    ::countGrantedNotification,
                    TypeGoSettings.NOTIFICATION
                )
            }
        }
    }

    fun handlePermissionDenied(
        permission: String,
        counter: KMutableProperty0<Int>,
        type: TypeGoSettings
    ) {
        if (!shouldShowRequestPermissionRationale(permission)) {
            counter.set(counter.get() + 1)
            if (counter.get() > 1) {
                permissionsLiveData.postValue(PermissionsState(true, type))
            }
        }
    }

    private var currentType = TypeGoSettings.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsLiveData.observe(this) {
            if (it != null) {
                currentType = it.typeGoSettings
                if (it.state) {
                    goToSetting(it.typeGoSettings, {})
                    permissionsLiveData.postValue(null)
                }
            }
        }
    }


    open fun goToSetting(typeGoSettings: TypeGoSettings, onDeny: () -> Unit) {
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