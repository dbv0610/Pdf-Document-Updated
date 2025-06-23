package com.azg.pdf8.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityMainBinding
import com.azg.pdf8.dialog.QuitAppDialog
import com.dong.baselib.permission.Permission
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val permission: Permission by inject()
    override fun backPressed() {
        quitActivity.show()
    }

    private val quitActivity by lazy {
        QuitAppDialog(this@MainActivity) {
            finishAffinity()
        }
    }
    override fun fragmentOnBack() {
        super.fragmentOnBack()
        backPressed()
    }

    override fun fragmentAction(data: Any) {
        super.fragmentAction(data)

    }

    override fun <T> fragmentSendData(key: String, data: T) {
        super.fragmentSendData(key, data)
    }

    fun Context.isPermissionPermanentlyDenied(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this, permission
        ) != PackageManager.PERMISSION_GRANTED && !ActivityCompat.shouldShowRequestPermissionRationale(
            this as Activity,
            permission
        )
    }
    @SuppressLint("SetTextI18n")
    override fun initialize() {}

    override fun ActivityMainBinding.setData()  = Unit



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
    }
}