package com.azg.pdf8.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.az.inappupdate.AppUpdate
import com.az.inappupdate.AppUpdateManager
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.databinding.ActivityMainBinding
import com.azg.pdf8.dialog.DialogPermission
import com.azg.pdf8.dialog.QuitAppDialog
import com.azg.pdf8.ui.main.document.DocumentFragment
import com.azg.pdf8.ui.main.favorite.FavoriteFragment
import com.azg.pdf8.ui.main.setting.SettingFragment
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.widget.NavigationBar
import com.dong.baselib.permission.Permission
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate, true) {
    override fun backPressed() {
        quitActivity.show()
    }

    val documentViewModel: DocumentViewModel by inject()
    private val quitActivity by lazy {
        QuitAppDialog(this@MainActivity) {
            finishAffinity()
        }
    }

    override fun fragmentAction(data: Any) {
        super.fragmentAction(data)
        if (data is String) {
            if (data == "requestPermission") {
                requestStoragePermission()
            } else if (data == "requestCameraPer") {
                requestCameraLauncher.launch(permission.cameraRequest)
            }
        }
    }

    override fun fragmentOnBack() {
        super.fragmentOnBack()
        backPressed()
    }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
            isGrantPermission = readGranted && writeGranted
        }
    private val manageStorageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isGrantPermission = isStorageAccess()
        }
    @SuppressLint("SetTextI18n")
    override fun initialize() {
        addFragment(DocumentFragment(), binding.mainContainer.id, false)
        documentViewModel.loadDocuments(this@MainActivity)
        isGrantPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isStorageAccess()
        } else permission.checkGrantedStorage_24_33
        if (!isGrantPermission) {
            DialogPermission(this@MainActivity).attachActivity(this@MainActivity).onAllowAccess {
                requestStoragePermission()
            }.show()
        }
        checkUpdate()
    }

    override fun ActivityMainBinding.setData() {
        navigationBar.onMenuItemChange(object : NavigationBar.MenuActionChange {
            override fun onHome() {
                replaceFragment(DocumentFragment(), mainContainer.id, false)
            }

            override fun onFavorite() {
                replaceFragment(FavoriteFragment(), mainContainer.id, false)
            }

            override fun onSetting() {
                replaceFragment(SettingFragment(), mainContainer.id, false)
            }
        })
    }
    @SuppressLint("UseKtx")
    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData("package:$packageName".toUri())
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        } else {
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    override fun replaceFragment(
        fragment: Fragment,
        containerId: Int,
        addToBackStack: Boolean
    ) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(containerId, fragment)

        if (addToBackStack) transaction.addToBackStack(fragment::class.java.simpleName)
        transaction.commitAllowingStateLoss()
    }

    override fun ActivityMainBinding.onClick() = Unit

    override fun onResume() {
        super.onResume()
        if (isGrantPermission) {
            lifecycleScope.launch {
                documentViewModel.loadDocuments(this@MainActivity)
            }
        } else {
            NativeAdPreloadManager.preloadAd(
                this@MainActivity,
                NativePlacement.PERMISSION,
                1,
                false
            )
        }
    }

    private fun enableAdsResume() {
//        AppOpenManager.getInstance().enableAppResume()
    }

    private fun disableAdsResume() {
//        AppOpenManager.getInstance().disableAppResume()
    }

    private var isCheckedUpdate = false
    private fun checkUpdate() {
        if (!isCheckedUpdate) {
            isCheckedUpdate = true
            AppUpdateManager.getInstance(this).checkUpdateApp(this) {
                if (it) {
                    disableAdsResume()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppUpdate.REQ_CODE_VERSION_UPDATE) {
            if (resultCode == Activity.RESULT_OK) {
                disableAdsResume()
            } else {
                if (AppUpdateManager.getInstance(this)
                        .getStyleUpdate() == AppUpdateManager.STYLE_FORCE_UPDATE
                ) {
                    disableAdsResume()
                } else {
                    enableAdsResume()
                }
            }
            AppUpdateManager.getInstance(this).onCheckResultUpdate(requestCode, resultCode) {
                if (it) {
                    disableAdsResume()
                }
            }
        }
    }
}