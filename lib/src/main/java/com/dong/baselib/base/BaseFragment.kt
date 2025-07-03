package com.dong.baselib.base

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.io.ByteArrayOutputStream
import java.io.Serializable
import kotlin.collections.iterator

abstract class BaseFragment<VB : ViewBinding>(
    open val bindingFactory: (LayoutInflater) -> VB,
    open var isFullSc: Boolean = false
) : Fragment() {
    lateinit var appContext: Context
    lateinit var appActivity: AppCompatActivity
    var fragmentAttach: FragmentAttachEvent? = null

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

    fun appContext(): Context {
        return context ?: appActivity
    }

    open fun initialize(context: Context) {}

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            fragmentAttach = context as FragmentAttachEvent?
        } catch (e: Exception) {
            Log.e("BaseFragment", "Class cast exception: ${e.message}")
        }
        this@BaseFragment.appContext = context
        initialize(context)
        (activity as? AppCompatActivity)?.let { this.appActivity = it }
    }

    val binding: VB by lazy { bindingFactory(layoutInflater) }
    fun Boolean?.orFalse() = this == true
    inline fun <reified T : Any> Fragment.getData(key: String?): T? {
        val bundle = arguments
        if (key == null || bundle?.containsKey(key) != true) return null

        return when (T::class) {
            Int::class -> bundle.getInt(key, 0) as T
            Boolean::class -> bundle.getBoolean(key, false) as T
            String::class -> bundle.getString(key) as T
            Float::class -> bundle.getFloat(key, 0f) as T
            Long::class -> bundle.getLong(key, 0L) as T
            Double::class -> bundle.getDouble(key, 0.0) as T
            Char::class -> bundle.getChar(key, Char.MIN_VALUE) as T
            CharSequence::class -> bundle.getCharSequence(key) as T
            else -> {
                @Suppress("DEPRECATION")
                when {
                    Parcelable::class.java.isAssignableFrom(T::class.java) ->
                        bundle.getParcelable(key) as? T
                    Serializable::class.java.isAssignableFrom(T::class.java) ->
                        bundle.getSerializable(key) as? T
                    else -> throw IllegalArgumentException("Unsupported type ${T::class.java}")
                }
            }
        }
    }

    inline fun <reified T : Any> ActivityResult.getResultData(key: String): T? {
        val intent = this.data ?: return null
        if (!intent.extras?.containsKey(key).orFalse()) return null

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        return when {
            T::class == Int::class -> intent.getIntExtra(key, 0) as T
            T::class == Boolean::class -> intent.getBooleanExtra(key, false) as T
            T::class == String::class -> intent.getStringExtra(key) as T
            T::class == Float::class -> intent.getFloatExtra(key, 0f) as T
            T::class == Long::class -> intent.getLongExtra(key, 0L) as T
            T::class == Double::class -> intent.getDoubleExtra(key, 0.0) as T
            T::class == Char::class -> intent.getCharExtra(key, Char.MIN_VALUE) as T
            T::class == CharSequence::class -> intent.getCharSequenceExtra(key) as T
            intent.extras?.get(key) is ArrayList<*> -> {
                (intent.extras!!.get(key) as ArrayList<*>) as T
            }
            Parcelable::class.java.isAssignableFrom(T::class.java) ->
                intent.getParcelableExtra(key) as? T
            Serializable::class.java.isAssignableFrom(T::class.java) ->
                intent.getSerializableExtra(key) as? T
            else -> throw IllegalArgumentException("Unsupported type ${T::class.java}")
        }
    }

    inline fun <reified T : Any> newIntent(context: Context): Intent =
        Intent(context, T::class.java)

    inline fun <reified T : Any> launchActivity(
        params: HashMap<String, Any>? = null,
    ) {
        val intent = newIntent<T>(appContext())
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

    inline fun <reified T : Any> Activity.getData(key: String?): T? {
        if (key == null || intent?.extras?.containsKey(key) != true) return null

        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        return when {
            T::class == Int::class -> intent.getIntExtra(key, 0) as T
            T::class == Boolean::class -> intent.getBooleanExtra(key, false) as T
            T::class == String::class -> intent.getStringExtra(key) as T
            T::class == Float::class -> intent.getFloatExtra(key, 0f) as T
            T::class == Long::class -> intent.getLongExtra(key, 0L) as T
            T::class == Double::class -> intent.getDoubleExtra(key, 0.0) as T
            T::class == Char::class -> intent.getCharExtra(key, Char.MIN_VALUE) as T
            T::class == CharSequence::class -> intent.getCharSequenceExtra(key) as T
            intent.extras?.get(key) is ArrayList<*> -> {
                (intent.extras!!.get(key) as ArrayList<*>) as T
            }
            Parcelable::class.java.isAssignableFrom(T::class.java) ->
                intent.getParcelableExtra(key) as? T
            Serializable::class.java.isAssignableFrom(T::class.java) ->
                intent.getSerializableExtra(key) as? T
            else -> throw IllegalArgumentException("Unsupported type ${T::class.java}")
        }
    }

    inline fun <reified T : Any> Context.launchActivity(
        params: Map<String, Any?> = emptyMap()
    ) {
        val intent = Intent(this, T::class.java).apply {
            params.forEach { (key, value) ->
                when (value) {
                    null -> putExtra(key, null as Serializable?)
                    is Int -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    is Float -> putExtra(key, value)
                    is Double -> putExtra(key, value)
                    is String -> putExtra(key, value)
                    is Char -> putExtra(key, value)
                    is CharSequence -> putExtra(key, value)
                    is Parcelable -> putExtra(key, value)
                    is Serializable -> putExtra(key, value)
                    is List<*> -> {
                        when {
                            value.all { it is Parcelable } -> {
                                @Suppress("UNCHECKED_CAST")
                                putParcelableArrayListExtra(
                                    key,
                                    ArrayList(value as List<Parcelable>)
                                )
                            }
                            value.all { it is Serializable } -> {
                                @Suppress("UNCHECKED_CAST")
                                putParcelableArrayListExtra(
                                    key,
                                    ArrayList(value as List<Parcelable>)
                                )
                            }
                            value.all { it is String } -> {
                                @Suppress("UNCHECKED_CAST")
                                putStringArrayListExtra(
                                    key,
                                    ArrayList(value as List<String>)
                                )
                            }
                            else -> throw IllegalArgumentException(
                                "Unsupported List element type for key=\"$key\": " +
                                        value.firstOrNull()?.javaClass
                            )
                        }
                    }
                    else -> throw IllegalArgumentException(
                        "Unsupported extra type for key=\"$key\": ${value.javaClass}"
                    )
                }
            }
        }
        if (this is Activity) {
            startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
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
        val intent = Intent(this.requireActivity(), T::class.java).apply {
            params?.forEach { (key, value) ->
                when (value) {
                    null -> putExtra(key, null as Serializable?)
                    is Int -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    is Float -> putExtra(key, value)
                    is Double -> putExtra(key, value)
                    is String -> putExtra(key, value)
                    is Char -> putExtra(key, value)
                    is CharSequence -> putExtra(key, value)
                    is Parcelable -> putExtra(key, value)
                    is Serializable -> putExtra(key, value)
                    is List<*> -> {
                        when {
                            value.all { it is Parcelable } -> {
                                @Suppress("UNCHECKED_CAST")
                                putParcelableArrayListExtra(
                                    key,
                                    ArrayList(value as List<Parcelable>)
                                )
                            }
                            value.all { it is Serializable } -> {
                                @Suppress("UNCHECKED_CAST")
                                putParcelableArrayListExtra(
                                    key,
                                    ArrayList(value as List<Parcelable>)
                                )
                            }
                            value.all { it is String } -> {
                                @Suppress("UNCHECKED_CAST")
                                putStringArrayListExtra(
                                    key,
                                    ArrayList(value as List<String>)
                                )
                            }
                            else -> throw IllegalArgumentException(
                                "Unsupported List element type for key=\"$key\": " +
                                        value.firstOrNull()?.javaClass
                            )
                        }
                    }
                    else -> throw IllegalArgumentException(
                        "Unsupported extra type for key=\"$key\": ${value.javaClass}"
                    )
                }
            }
        }
        resultLauncher.launch(intent)
    }

    fun View.isBackgroundTransparent(): Boolean {
        val background = this.background
        return background is ColorDrawable && background.color == Color.TRANSPARENT
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (binding.root.isBackgroundTransparent()) {
            binding.root.setBackgroundColor(Color.WHITE)
        }
        binding.root.isClickable = true

        return binding.root
    }

    val statusBarHeight: Int
        @SuppressLint("DiscouragedApi", "InternalInsetResource")
        get() {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        }

    open fun backPress() {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isFullSc) {
            notFullView()
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPress()
                }
            })
        binding.initView()
        binding.onClick()
    }

    abstract fun VB.initView()
    abstract fun VB.onClick()

    fun notFullView() {
        val paddingTop = binding.root.paddingTop + statusBarHeight
        binding.root.setPadding(
            binding.root.paddingLeft,
            paddingTop,
            binding.root.paddingRight,
            binding.root.paddingBottom
        )
    }

    open fun showKeyboard(view: View?) {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    open fun showKeyboard() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.root, InputMethodManager.SHOW_IMPLICIT)
    }

    open fun hideKeyboard() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().window.decorView.rootView.windowToken, 0)
    }

    fun addFragment(
        fragment: Fragment,
        id: Int = R.id.content,
        addToBackStack: Boolean = false
    ) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.add(id, fragment)
        if (addToBackStack) {
            transaction?.addToBackStack(fragment.javaClass.simpleName)
        }
        transaction?.commitAllowingStateLoss()
    }

    fun replaceFullViewFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(id, fragment)
        if (addToBackStack) {
            transaction?.addToBackStack(fragment.javaClass.simpleName)
        }
        transaction?.commitAllowingStateLoss()
    }

    fun replaceFragment(
        fragment: Fragment,
        id: Int = R.id.content,
        addToBackStack: Boolean = true
    ) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(id, fragment)
        if (addToBackStack) {
            transaction?.addToBackStack(fragment.javaClass.simpleName)
        }
        transaction?.commitAllowingStateLoss()
    }

    open fun closeFragment(fragment: Fragment) {
        val activity = fragment.activity
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            activity.supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    com.dong.baselib.R.anim.enter_from_right,
                    com.dong.baselib.R.anim.exit_to_left
                )
                .remove(fragment)
                .commitNowAllowingStateLoss()
        }
    }

    fun Fragment.closeSelf() {
        activity?.let {
            if (!it.isFinishing && !it.isDestroyed) {
                it.supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        com.dong.baselib.R.anim.enter_from_right,
                        com.dong.baselib.R.anim.exit_to_left
                    )
                    .remove(this)
                    .commitNowAllowingStateLoss()
            }
        }
    }
}
