package com.dong.baselib.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.viewbinding.ViewBinding
import com.dong.baselib.api.hideSystemBar
import com.dong.baselib.api.setFullScreen
import java.io.Serializable
import kotlin.collections.iterator

abstract class BaseActivity<VB : ViewBinding>(
    open val bindingFactory: (LayoutInflater) -> VB,
    private var fullStatus: Boolean = false,
) : AppCompatActivity(), FragmentAttachEvent {
    val binding: VB by lazy { bindingFactory(layoutInflater) }
    internal val statusBarHeight: Int
        @SuppressLint("DiscouragedApi", "InternalInsetResource") get() {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        }

    abstract fun backPressed()
    abstract fun initialize()
    abstract fun VB.setData()
    abstract fun VB.onClick()

    companion object {
        inline fun <reified T : Fragment> newInstanceWithArgs(args: HashMap<String, Any>): T {
            val fragment = T::class.java.newInstance()
            val bundle = Bundle()
            for ((key, value) in args) {
                when (value) {
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is String -> bundle.putString(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    is Float -> bundle.putFloat(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Serializable -> bundle.putSerializable(key, value)
                    is Parcelable -> bundle.putParcelable(key, value)
                    else -> throw IllegalArgumentException("Unsupported bundle type for key $key")
                }
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    open fun closeFragment(fragment: Fragment) {
        if (fragment.isAdded && fragment.isVisible) {
            supportFragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
        }
    }

    inline fun <reified T : Any> Activity.getData(key: String?): T? {
        if (key == null || intent?.extras?.containsKey(key) != true) return null

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        return when (T::class) {
            Int::class -> intent.getIntExtra(key, 0) as T
            Boolean::class -> intent.getBooleanExtra(key, false) as T
            String::class -> intent.getStringExtra(key) as T
            Float::class -> intent.getFloatExtra(key, 0f) as T
            Long::class -> intent.getLongExtra(key, 0L) as T
            Double::class -> intent.getDoubleExtra(key, 0.0) as T
            Char::class -> intent.getCharExtra(key, Char.MIN_VALUE) as T
            CharSequence::class -> intent.getCharSequenceExtra(key) as T
            else -> {
                when {
                    Parcelable::class.java.isAssignableFrom(T::class.java) ->
                        intent.getParcelableExtra(key) as? T
                    Serializable::class.java.isAssignableFrom(T::class.java) ->
                        intent.getSerializableExtra(key) as? T
                    else -> throw IllegalArgumentException("Unsupported type ${T::class.java}")
                }
            }
        }
    }

    inline fun <reified T : Any> launchActivity(
        params: HashMap<String, Any>? = null,
    ) {
        val intent = newIntent<T>(this)
        val bundle = Bundle()
        params?.let { map ->
            for ((key, value) in map) {
                when (value) {
                    is Int -> bundle.putInt(key, value)
                    is String -> bundle.putString(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    is Float -> bundle.putFloat(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Char -> bundle.putChar(key, value)
                    is CharSequence -> bundle.putCharSequence(key, value)
                    is Parcelable -> bundle.putParcelable(key, value)
                    is Serializable -> bundle.putSerializable(key, value)
                    is Bundle -> bundle.putBundle(key, value)
                    else -> throw IllegalArgumentException("Unsupported bundle component (${value.javaClass})")
                }
            }
            intent.putExtras(bundle)
        }
        startActivity(intent, bundle)
    }

    open fun listenerResult(result: ActivityResult) {}
    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            listenerResult(result)
            activityResultCallback?.invoke(result)
            activityResultCallback = null
        }
    var activityResultCallback: ((ActivityResult) -> Unit)? = null
    inline fun <reified T : Any> launcherForResult(
        params: HashMap<String, Any>? = null,
        noinline dataResult: (ActivityResult) -> Unit = { _ -> }
    ) {
        activityResultCallback = dataResult
        val intent = Intent(this, T::class.java)
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is Int -> bundle.putInt(key, value)
                is String -> bundle.putString(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is Float -> bundle.putFloat(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Char -> bundle.putChar(key, value)
                is CharSequence -> bundle.putCharSequence(key, value)
                is Parcelable -> bundle.putParcelable(key, value)
                is Serializable -> bundle.putSerializable(key, value)
                is Bundle -> bundle.putBundle(key, value)
                else -> throw IllegalArgumentException("Unsupported bundle component (${value.javaClass})")
            }
        }
        intent.putExtras(bundle)
        resultLauncher.launch(intent)
    }

    open fun showKeyboard(view: View?) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    open fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.root, InputMethodManager.SHOW_IMPLICIT)
    }

    open fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.rootView.windowToken, 0)
    }

    inline fun <reified T : Any> newIntent(context: Context): Intent =
        Intent(context, T::class.java)

    fun hideNavigation() {
        val windowInsetsController = if (Build.VERSION.SDK_INT >= 30) {
            ViewCompat.getWindowInsetsController(window.decorView)
        } else {
            WindowInsetsControllerCompat(window, binding.root)
        }

        windowInsetsController?.let {
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsetsCompat.Type.navigationBars())
            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility == 0) {
                    Handler().postDelayed({
                        val controller = if (Build.VERSION.SDK_INT >= 30) {
                            ViewCompat.getWindowInsetsController(window.decorView)
                        } else {
                            WindowInsetsControllerCompat(window, binding.root)
                        }
                        controller?.hide(WindowInsetsCompat.Type.navigationBars())
                    }, 3000)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SystemUtil.setLocale(
            this@BaseActivity, SystemUtil.getPreLanguage(this@BaseActivity) ?: "en"
        )
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(binding.root)
        val paddingTop = binding.root.paddingTop + statusBarHeight
        if (!fullStatus) {
            binding.root.setPadding(
                binding.root.paddingLeft,
                paddingTop,
                binding.root.paddingRight,
                binding.root.paddingBottom
            )
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPressed()
            }
        })
        initialize()

        binding.setData()
        binding.onClick()
        window.hideSystemBar()
        window.setFullScreen()
        hideNavigation()
    }

    fun addFragment(
        fragment: Fragment, containerId: Int = android.R.id.content, addToBackStack: Boolean = false
    ) {
        hideKeyboard()
        val tag = fragment.javaClass.simpleName
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        if (existingFragment != null && existingFragment.isAdded) {
            supportFragmentManager.beginTransaction().setCustomAnimations(
                com.dong.baselib.R.anim.enter_from_right,
                com.dong.baselib.R.anim.exit_to_left,
                com.dong.baselib.R.anim.enter_from_left,
                com.dong.baselib.R.anim.exit_to_right
            ).show(existingFragment).commitAllowingStateLoss()
            return
        }
        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                com.dong.baselib.R.anim.enter_from_right,
                com.dong.baselib.R.anim.exit_to_left,
                com.dong.baselib.R.anim.enter_from_left,
                com.dong.baselib.R.anim.exit_to_right
            )
            supportFragmentManager.fragments.lastOrNull()?.let { hide(it) }
            add(containerId, fragment, tag)

            if (addToBackStack) {
                addToBackStack(tag)
            }
            commitAllowingStateLoss()
        }
    }

    fun changeFragment(
        fragment: Fragment,
        containerId: Int = android.R.id.content,
        addToBackStack: Boolean = false
    ) {
        hideKeyboard()
        val tag = fragment.javaClass.simpleName
        val fm = supportFragmentManager
        val target = fm.findFragmentByTag(tag) ?: fragment
        val current = fm.fragments.firstOrNull { it.isVisible }

        if (current === target) return
        fm.beginTransaction().apply {
            setCustomAnimations(
                com.dong.baselib.R.anim.enter_from_right,
                com.dong.baselib.R.anim.exit_to_left,
                com.dong.baselib.R.anim.enter_from_left,
                com.dong.baselib.R.anim.exit_to_right
            )
            current?.let { hide(it) }
            if (target.isAdded) {
                show(target)
            } else {
                add(containerId, target, tag)
            }
            if (addToBackStack) addToBackStack(tag)

            commitAllowingStateLoss()
        }
    }

    open fun replaceFragment(
        fragment: Fragment, containerId: Int = android.R.id.content, addToBackStack: Boolean = false
    ) {
        hideKeyboard()
        val tag = fragment.javaClass.simpleName

        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                com.dong.baselib.R.anim.enter_from_right,
                com.dong.baselib.R.anim.exit_to_left,
                com.dong.baselib.R.anim.enter_from_left,
                com.dong.baselib.R.anim.exit_to_right
            )

            replace(containerId, fragment, tag)

            if (addToBackStack) {
                addToBackStack(tag)
            }

            commitAllowingStateLoss()
        }
    }

    private fun FragmentTransaction.setDefaultAnimations() = apply {
        setCustomAnimations(
            com.dong.baselib.R.anim.enter_from_right,
            com.dong.baselib.R.anim.exit_to_left,
            com.dong.baselib.R.anim.enter_from_left,
            com.dong.baselib.R.anim.exit_to_right
        )
    }

    fun shouldShowDialog(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onRestart() {
        super.onRestart()
    }
}