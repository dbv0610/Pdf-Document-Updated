package com.azg.pdf8.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.adapter.DocumentAdapter
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityMainBinding
import com.azg.pdf8.dialog.QuitAppDialog
import com.azg.pdf8.ui.main.document.DocumentFragment
import com.azg.pdf8.ui.main.favorite.FavoriteFragment
import com.azg.pdf8.ui.main.setting.SettingFragment
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.widget.NavigationBar
import com.dong.baselib.permission.Permission
import com.dong.baselib.widget.transparent
import com.dong.baselib.widget.white
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate, true) {
    private val permission: Permission by inject()
    override fun backPressed() {
        quitActivity.show()
    }

    val documentViewModel: DocumentViewModel by inject()
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

    val adapter by lazy {
        DocumentAdapter()
    }
    @SuppressLint("SetTextI18n")
    override fun initialize() {
        addFragment(DocumentFragment(), binding.mainContainer.id, false)
        documentViewModel.loadDocuments(this@MainActivity)
    }

    override fun ActivityMainBinding.setData() {
        lifecycleScope.launch {
            documentViewModel.repo.listAllData.collect {
                adapter.submitListCustom(it)
            }
        }

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