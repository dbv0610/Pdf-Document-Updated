package com.dong.baselib.permission

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.requestMultiplePermissions(multiRequest:Array<String>,permissionsState: (Boolean)->Unit){
    var requestLauncher = this.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allPermissionsGranted = true
        permissions.entries.forEach { entry ->
            if (!entry.value) {
                allPermissionsGranted = false
            }
        }
        permissionsState.invoke(allPermissionsGranted)
    }
    requestLauncher.launch(multiRequest)
}

fun AppCompatActivity.requestSinglePermissions(multiRequest:String, permissionsState: (Boolean)->Unit){
    val requestLauncher = this.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions ->
       permissionsState.invoke(permissions)
    }
    requestLauncher.launch(multiRequest)
}


fun Context.getIntentSettingsPermission(): Intent {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts(
        "package",
        packageName, null
    )
    intent.setData(uri)
    return intent
}



fun Context.getNotificationManager() =
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
