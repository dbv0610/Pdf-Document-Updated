package com.azg.pdf8.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.azg.pdf8.R
import com.azg.pdf8.databinding.DialogFilterDataBinding
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.click

enum class SortByData {
    SortByName, SortBySize, SortByDate, None
}

class SortDataByDialog(context: Context) :
    BaseDialog<DialogFilterDataBinding>(context, DialogFilterDataBinding::inflate, true) {
    private var sortByData = MutableLiveData(SortByData.None)
    private var lifecycleOwner: LifecycleOwner? = null
    private var searchData: (SortByData) -> Unit = {}
    fun attachLifecycle(lifecycleOwner: LifecycleOwner): SortDataByDialog {
        this.lifecycleOwner = lifecycleOwner
        return this@SortDataByDialog
    }

    fun showSortByData(sortType: SortByData): SortDataByDialog {
        this.sortByData.value = sortType
        return this@SortDataByDialog
    }

    fun onSearchEvent(data: (SortByData) -> Unit): SortDataByDialog {
        this.searchData = data
        return this@SortDataByDialog
    }

    override fun DialogFilterDataBinding.initView() {
        lifecycleOwner?.let { lf ->
            sortByData.observe(lf) {
                binding.ivSortByDate.setImageResource(if (it == SortByData.SortByDate) R.drawable.ic_language_selected else R.drawable.ic_language_unselected)
                binding.ivSortBySize.setImageResource(if (it == SortByData.SortBySize) R.drawable.ic_language_selected else R.drawable.ic_language_unselected)
                binding.ivSortByName.setImageResource(if (it == SortByData.SortByName) R.drawable.ic_language_selected else R.drawable.ic_language_unselected)
            }
        }
        icClose.click {
            dismiss()
        }
        binding.ivSortByDate.click {
            if (sortByData.value == SortByData.SortByDate) {
                sortByData.value = SortByData.None
            } else {
                sortByData.value = SortByData.SortByDate
            }
        }
        binding.ivSortByName.click {
            if (sortByData.value == SortByData.SortByName) {
                sortByData.value = SortByData.None
            } else {
                sortByData.value = SortByData.SortByName
            }
        }
        binding.ivSortBySize.click {
            if (sortByData.value == SortByData.SortBySize) {
                sortByData.value == SortByData.None
            } else {
                sortByData.value = SortByData.SortBySize
            }
        }

        binding.btnAgree.click {
            searchData.invoke(sortByData.value!!)
            dismiss()
        }
    }
}