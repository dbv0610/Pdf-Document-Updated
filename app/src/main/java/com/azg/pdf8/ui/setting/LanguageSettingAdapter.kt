package com.azg.pdf8.ui.setting

import android.view.LayoutInflater
import android.view.ViewGroup
import com.azg.pdf8.R
import com.dong.baselib.base.BaseAdapter
import com.azg.pdf8.databinding.ItemLanguageSettingBinding
import com.azg.pdf8.ui.language.LanguageItem
import com.dong.baselib.lifecycle.change
import com.dong.baselib.widget.click
import com.dong.baselib.widget.fromColor

class LanguageSettingAdapter(var lang: (LanguageItem) -> Unit = { _ -> }) :
    BaseAdapter<LanguageItem, ItemLanguageSettingBinding>() {
    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ) = ItemLanguageSettingBinding.inflate(inflater, parent, false)


    override fun ItemLanguageSettingBinding.bind(item: LanguageItem, position: Int) {
        currentPosition.change {
            if (it == position) {
                llFocusItem.strokeColor(fromColor("#3A65FC"))
                imgItemSelect.setImageResource(R.drawable.ic_language_selected)
            } else {
                llFocusItem.strokeColor(fromColor("#00FF6100"))
                imgItemSelect.setImageResource(R.drawable.ic_language_unselected)
            }
        }
        imgItemFlag.setImageResource(item.flagId)
        tvItemName.text = item.name

        root.click {
            currentPosition.value = position
            lang.invoke(item)
        }
    }
}