package com.ag.sampleadsfirstflow.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.ag.sampleadsfirstflow.utils.Language
import com.ag.sampleadsfirstflow.utils.PreferenceHelper
import com.ag.sampleadsfirstflow.utils.extensions.hideSystemBar
import com.ag.sampleadsfirstflow.utils.extensions.setFullScreen
import org.koin.android.ext.android.inject

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    protected val preferenceHelper: PreferenceHelper by inject()
    protected lateinit var binding: VB
        private set

    protected abstract fun inflateBinding(layoutInflater: LayoutInflater): VB
    abstract fun updateUI(savedInstanceState: Bundle?)
    open fun isDisplayCutout(): Boolean = false

    open fun loadAd() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val insetTypes =
                if (!isDisplayCutout()) WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars()
                else WindowInsetsCompat.Type.systemBars()
            val systemBars = insets.getInsets(insetTypes)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.setFullScreen()
        window.hideSystemBar()
        updateUI(savedInstanceState)
        loadAd()
    }

    override fun attachBaseContext(newBase: Context?) {
        if (this.javaClass.simpleName == "Language1Activity" || this.javaClass.simpleName == "Language2Activity") {
            super.attachBaseContext(newBase)
        } else {
            newBase?.let {
                super.attachBaseContext(
                    Language.changeLanguage(newBase, preferenceHelper.languageSelected)
                )
            } ?: run { super.attachBaseContext(newBase) }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.hideSystemBar()
    }

    fun setViewInsets(view: View, isDisplayCutout: Boolean = false) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val insetTypes =
                if (!isDisplayCutout) WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars()
                else WindowInsetsCompat.Type.systemBars()
            val systemBars = insets.getInsets(insetTypes)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
