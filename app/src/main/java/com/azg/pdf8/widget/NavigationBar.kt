package com.azg.pdf8.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import com.azg.pdf8.R
import com.azg.pdf8.databinding.LayoutItemNavBinding
import com.azg.pdf8.databinding.LayoutNavigationBinding
import com.dong.baselib.widget.white
import kotlin.apply
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.runCatching

class NavigationItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = LayoutItemNavBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    var index: Int = 1

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.NavigationItem)
        val iconResId = ta.getResourceId(
            R.styleable.NavigationItem_navIcon,
            R.drawable.nav_dis_1
        )
        val text = ta.getString(R.styleable.NavigationItem_navText)
        val isActive = ta.getBoolean(R.styleable.NavigationItem_isActive, false)
        ta.recycle()
        binding.tvContent.setTextColor(if(isActive) mainColor else color_8c8c8c)
        binding.tvContent.text = text
        binding.iconResource.setImageResource(iconResId)
    }

    fun updateEnableState(isActive: Boolean) {
        @DrawableRes
        val resId = when (index) {
            1 -> if (isActive) R.drawable.nav_enb_1 else R.drawable.nav_dis_1
            2 -> if (isActive) R.drawable.nav_enb_2 else R.drawable.nav_dis_2
            else -> if (isActive) R.drawable.nav_enb_3 else R.drawable.nav_dis_3
        }
        binding.iconResource.apply {
            setImageResource(resId)
        }
        binding.tvContent.setTextColor(if(isActive) mainColor else color_8c8c8c)
    }
}

class NavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = LayoutNavigationBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private var currentMenu = 1

    interface MenuActionChange {
        fun onHome()
        fun onFavorite()
        fun onSetting()
    }

    private var menuAction: MenuActionChange? = null
    fun onMenuItemChange(menu: MenuActionChange) {
        this.menuAction = menu
    }

    init {
        binding.navItem1.index = 1
        binding.navItem2.index = 2
        binding.navItem3.index = 3

        listOf(
            binding.navItem1,
            binding.navItem2,
            binding.navItem3
        ).forEach { item ->
            item.setOnClickListener {
                if (currentMenu != item.index) {
                    currentMenu = item.index
                    updateAllItems { idx -> idx == currentMenu }
                    runCatching {
                        when (item) {
                            binding.navItem1 -> menuAction?.onHome()
                            binding.navItem2 -> menuAction?.onFavorite()
                            binding.navItem3 -> menuAction?.onSetting()
                            else -> menuAction?.onSetting()
                        }
                    }
                }
            }
        }

        updateAllItems { idx -> idx == currentMenu }
    }

    fun updateAllItems(isEnabled: (Int) -> Boolean) {
        listOf(
            binding.navItem1,
            binding.navItem2,
            binding.navItem3,
        ).forEach { item ->
            item.updateEnableState(isEnabled(item.index))
        }
    }

    fun setCurrentItem(i: Int) {
        currentMenu = i
        listOf(
            binding.navItem1,
            binding.navItem2,
            binding.navItem3,
        ).forEachIndexed { index, item ->
            updateAllItems { idx -> idx == currentMenu }
            when (item) {
                binding.navItem1 -> menuAction?.onHome()
                binding.navItem2 -> menuAction?.onFavorite()
                binding.navItem3 -> menuAction?.onSetting()
                else -> menuAction?.onSetting()
            }
        }
    }
}
