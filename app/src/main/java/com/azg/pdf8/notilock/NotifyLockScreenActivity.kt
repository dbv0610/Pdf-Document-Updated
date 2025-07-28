package com.azg.pdf8.notilock

import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.azg.pdf8.R
import com.azg.pdf8.app.isScreenLockType
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.NotifyLockScreenActivityBinding
import com.azg.pdf8.ui.splash.SplashActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotifyLockScreenActivity :
    BaseActivity<NotifyLockScreenActivityBinding>(NotifyLockScreenActivityBinding::inflate) {
    override fun backPressed() {
    }

    private var reminderUtils: ReminderUtils? = null

    override fun initialize() {
    }

    override fun NotifyLockScreenActivityBinding.onClick() {
    }

    override fun NotifyLockScreenActivityBinding.setData() {
        if (reminderUtils == null) {
            reminderUtils = ReminderUtils()

            when (ReminderType.Companion.valueOfName(isScreenLockType)) {
                ReminderType.LOCK_FIRST_TYPE -> {
                    binding.ivTitle.setImageResource(R.drawable.img_noti_lock_1)
                    binding.tvTitle.text = getString(R.string.des_1)
                    binding.btnOpenApp.text = getString(R.string.cta_1)
                }
                ReminderType.LOCK_SECOND_TYPE -> {
                    binding.ivTitle.setImageResource(R.drawable.img_noti_lock_2)
                    binding.tvTitle.text = getString(R.string.des_2)
                    binding.btnOpenApp.text = getString(R.string.cta_2)
                }
                ReminderType.LOCK_THIRD_TYPE -> {
                    binding.ivTitle.setImageResource(R.drawable.img_noti_lock_3)
                    binding.tvTitle.text = getString(R.string.des_3)
                    binding.btnOpenApp.text = getString(R.string.cta_3)
                }
                ReminderType.LOCK_FOURTH_TYPE -> {
                    binding.ivTitle.setImageResource(R.drawable.img_noti_lock_4)
                    binding.tvTitle.text = getString(R.string.des_4)
                    binding.btnOpenApp.text = getString(R.string.cta_4)
                }
                ReminderType.LOCK_FIFTH_TYPE -> {
                    binding.ivTitle.setImageResource(R.drawable.img_noti_lock_5)
                    binding.tvTitle.text = getString(R.string.des_5)
                    binding.btnOpenApp.text = getString(R.string.cta_5)
                }
            }

            reminderUtils?.createScheduleLockScreenReminder(this@NotifyLockScreenActivity)
            turnScreenOnAndKeyguardOff()
        }
    }

    private fun turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        with(getSystemService(KEYGUARD_SERVICE) as KeyguardManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestDismissKeyguard(this@NotifyLockScreenActivity, null)
            }
        }
    }

    fun Calendar.getTimeFormattedString(simpleDateFormatString: String): String {
        val format = SimpleDateFormat(simpleDateFormatString, Locale.getDefault())

        return format.format(time)
    }

    override fun onResume() {
        super.onResume()
        binding.run {
            ivNotifyClose.setOnClickListener {
                finish()
            }

            constNotifyLockHalfRoot.setOnClickListener {
                openApp()
            }

            btnOpenApp.setOnClickListener {
                openApp()
            }
            binding.root.setOnClickListener {
                openApp()
            }
        }

        hideNavigationBar()
    }

    private fun openApp() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            null,
            this,
            SplashActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        finish()
        startActivity(intent)
    }

    private fun hideNavigationBar() {
        window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window?.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else @Suppress("DEPRECATION") {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.decorView.systemUiVisibility = flags
            window.decorView.setOnSystemUiVisibilityChangeListener {
                if ((it and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    window.decorView.systemUiVisibility = flags
                }
            }
        }
    }

    private fun turnScreenOffAndKeyguardOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        reminderUtils?.isShow = false
        turnScreenOffAndKeyguardOn()
    }
}
