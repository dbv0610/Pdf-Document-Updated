package com.azg.pdf8.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.viewbinding.ViewBinding
import com.azg.pdf8.R
import com.azg.pdf8.app.toastShort
import com.dong.baselib.api.isApi33orHigher
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.permission.Permission
import com.dong.baselib.widget.delay
import org.koin.android.ext.android.inject
import kotlin.math.abs

abstract class BaseActivity<VB : ViewBinding>(
    override val bindingFactory: (LayoutInflater) -> VB,
    private var fullStatus: Boolean = false,
) : BaseActivity<VB>(bindingFactory, fullStatus) {
    val permission by inject<Permission>()

    companion object {
        var isGrantPermission = false
    }

    var requestCameraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            if (Build.VERSION.SDK_INT > 29) {
                handlePermissionDenied(
                    Manifest.permission.CAMERA,
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
                )
            }
        }
    }

    fun handlePermissionDenied(
        permission: String,
    ) {
        if (!shouldShowRequestPermissionRationale(permission)) {
            toastShort(getString(R.string.request_permission_need_to_use_fun))
            delay(1500) {
                goToSetting()
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

    override fun onResume() {
        super.onResume()
        isGrantPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isStorageAccess()
        } else permission.checkGrantedStorage_24_33
    }
}