package com.azg.pdf8.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ads.control.admob.AppOpenManager
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.az.inappupdate.AppUpdate
import com.az.inappupdate.AppUpdateManager
import com.azg.pdf8.ads.ads.banner.BannerPlacement
import com.azg.pdf8.ads.ads.interstitial.InterstitialAdManager
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.firstOpenApp
import com.azg.pdf8.app.isUfo
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.databinding.ActivityMainBinding
import com.azg.pdf8.dialog.DialogPermission
import com.azg.pdf8.dialog.DialogRequestFullscreen
import com.azg.pdf8.dialog.QuitAppDialog
import com.azg.pdf8.firebase.Analytics
import com.azg.pdf8.notilock.ReminderUtils
import com.azg.pdf8.ui.main.document.DocumentFragment
import com.azg.pdf8.ui.main.favorite.FavoriteFragment
import com.azg.pdf8.ui.main.setting.SettingFragment
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.widget.NavigationBar
import com.dong.baselib.base.SystemUtil
import com.dong.baselib.permission.Permission
import com.dong.baselib.permission.getNotificationManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.compareTo

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate, true) {
    companion object {
         var isShowRateFirstView = MutableSharedFlow<Boolean>(1)
    }

    private var isShowRateClickBack = false
    override fun backPressed() {
        if (!SystemUtil.isRatting(this@MainActivity)
            && !isShowRateClickBack
            && remoteConfig.rattingConfig.contains(firstOpenApp)
        ) {
            rattingDialog.setFinishRate {
                finishAffinity()
            }
            rattingDialog.show()
        } else {
            quitActivity.show()
        }
    }

    val documentViewModel: DocumentViewModel by inject()
    private val quitActivity by lazy {
        QuitAppDialog(this@MainActivity) {
            finishAffinity()
        }
    }
    var reminderUtils: ReminderUtils? = null
    @SuppressLint("ScheduleExactAlarm")
    private val launcherFullScreenIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                reminderUtils?.createScheduleLockScreenReminder(this)
            }
        }

    private fun Context.isUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getNotificationManager().canUseFullScreenIntent()
        } else {
            true
        }
    }

    private fun showFullScreenDialog() {
        DialogRequestFullscreen(this@MainActivity) {
            AppOpenManager.getInstance().disableAdResumeByClickAction()
            requestFullScreenIntent()
        }.show()
    }

    private fun requestFullScreenIntent() {
        AppOpenManager.getInstance().disableAdResumeByClickAction()
        val intent = Intent()
        intent.action = Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        launcherFullScreenIntent.launch(intent)
    }

    private fun checkPostNotification() {
        if (permission.checkGrantNotification) {
            if (!isUseFullScreenIntent()) {
                showFullScreenDialog()
            } else {
                reminderUtils?.createScheduleLockScreenReminder(this)
            }
        } else {
            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            checkPostNotification()
        }
    private val manageStorageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkPostNotification()
        }

    fun requestRate() {
        if (!SystemUtil.isRatting(this@MainActivity) && firstOpenApp >= 2) {
            rattingDialog.show()
        }

        rattingDialog.setDismissRate {
            isShowRateClickBack = true
            checkPostNotification()
        }
    }
    @SuppressLint("SetTextI18n")
    override fun initialize() {
        if (reminderUtils == null) {
            reminderUtils = ReminderUtils()
        }
        addFragment(DocumentFragment(), binding.mainContainer.id, false)
        documentViewModel.loadDocuments(this@MainActivity)
        if (!isGrantedPermission()) {
            DialogPermission(this@MainActivity).attachActivity(this@MainActivity).onAllowAccess {
                requestStoragePermission()
            }.dismissRate {
                requestRate()
            }.show()
        } else {
            InterstitialAdManager.isCloseInterSplash.observe(this@MainActivity) {
                if (it) {
                    if (!SystemUtil.isRatting(this@MainActivity) && firstOpenApp >= 2) {
                        requestRate()
                    } else {
                        checkPostNotification()
                    }
                }
            }
        }
        checkUpdate()
        binding.bannerAdView
            .setBannerPlacement(this@MainActivity, BannerPlacement.BANNER_ALL)
            .requestBanner()

        InterstitialAdManager.loadInterAll(this@MainActivity)
        lifecycleScope.launch {
            if (isShowRateFirstView.firstOrNull { it } == true) {
                rattingDialog.show()
            }
        }
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
        if(isUfo()){
            Analytics.track("ufo_home")
        }
    }
    @SuppressLint("UseKtx")
    fun requestStoragePermission() {
        AppOpenManager.getInstance().disableAdResumeByClickAction()
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
        if (isGrantedPermission()) {
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
        AppOpenManager.getInstance().enableAppResume()
    }

    private fun disableAdsResume() {
        AppOpenManager.getInstance().disableAppResume()
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