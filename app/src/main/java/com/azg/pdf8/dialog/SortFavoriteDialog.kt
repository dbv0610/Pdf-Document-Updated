package com.azg.pdf8.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.R
import com.azg.pdf8.databinding.DialogSortDataBinding
import com.azg.pdf8.model.DocumentType
import com.dong.baselib.base.BaseDialog
import com.dong.baselib.widget.click
import kotlinx.coroutines.launch

enum class SortDateType {
    NewToOld, OldToNew, NoSelect
}

enum class SortSizeType {
    BigToSmall, SmallToBig, NoSelect
}

class SortFavoriteDialog(context: Context) :
    BaseDialog<DialogSortDataBinding>(context, DialogSortDataBinding::inflate, true) {
    private var sortDate = MutableLiveData(SortDateType.NoSelect)
    private var sortSize = MutableLiveData(SortSizeType.NoSelect)
    private var listFileType = MutableLiveData(
        mutableListOf(
            DocumentType.Doc, DocumentType.Ppt, DocumentType.Pdf,
            DocumentType.Excel
        )
    )
    private var lifecycleOwner: LifecycleOwner? = null
    private var searchData: (SortDateType, SortSizeType, MutableList<DocumentType>) -> Unit =
        { _, _, _ -> }

    fun attachLifecycle(lifecycleOwner: LifecycleOwner): SortFavoriteDialog {
        this.lifecycleOwner = lifecycleOwner
        return this@SortFavoriteDialog
    }

    fun showSortDate(sortType: SortDateType): SortFavoriteDialog {
        this.sortDate.value = sortType
        return this@SortFavoriteDialog
    }

    fun showSortSize(sortType: SortSizeType): SortFavoriteDialog {
        this.sortSize.value = sortType

        return this@SortFavoriteDialog
    }

    fun showFileType(sortType: MutableList<DocumentType>): SortFavoriteDialog {
        this.listFileType.value = sortType
        return this@SortFavoriteDialog
    }

    fun onSearchEvent(data: (SortDateType, SortSizeType, MutableList<DocumentType>) -> Unit): SortFavoriteDialog {
        this.searchData = data
        return this@SortFavoriteDialog
    }

    override fun DialogSortDataBinding.initView() {
        lifecycleOwner?.let { lf ->
            sortDate.observe(lf) {
                binding.ivLastFirst.setImageResource(if (it == SortDateType.NewToOld) R.drawable.ic_language_selected else R.drawable.ic_language_unselected)
                binding.ivOldFirst.setImageResource(if (it == SortDateType.OldToNew) R.drawable.ic_language_selected else R.drawable.ic_language_unselected)
            }
            sortSize.observe(lf) {
                binding.ivLargetFirst.setImageResource(if (it == SortSizeType.BigToSmall) R.drawable.ic_language_selected else R.drawable.ic_language_unselected)
                binding.ivSmallFirst.setImageResource(if (it == SortSizeType.SmallToBig) R.drawable.ic_language_selected else R.drawable.ic_language_unselected)
            }
            listFileType.observe(lf) { list ->
                val isFilterPdf = list.contains(DocumentType.Pdf)
                val isFilterDoc = list.contains(DocumentType.Doc)
                val isFilterXls = list.contains(DocumentType.Excel)
                val isFilterPpt = list.contains(DocumentType.Ppt)
                binding.ivPdf.setImageResource(if (!isFilterPdf) R.drawable.ic_language_unselected else R.drawable.ic_language_selected)
                binding.ivDocx.setImageResource(if (!isFilterDoc) R.drawable.ic_language_unselected else R.drawable.ic_language_selected)
                binding.ivPpt.setImageResource(if (!isFilterPpt) R.drawable.ic_language_unselected else R.drawable.ic_language_selected)
                binding.ivXlsx.setImageResource(if (!isFilterXls) R.drawable.ic_language_unselected else R.drawable.ic_language_selected)
            }
        }
        icClose.click {
            dismiss()
        }
        binding.ivLastFirst.click {
            sortDate.value = SortDateType.NewToOld
            sortSize.value = SortSizeType.NoSelect
        }
        binding.ivOldFirst.click {
            sortDate.value = SortDateType.OldToNew
            sortSize.value = SortSizeType.NoSelect
        }
        binding.ivLargetFirst.click {
            sortSize.value =  SortSizeType.BigToSmall
            sortDate.value = SortDateType.NoSelect
        }
        binding.ivSmallFirst.click {
            sortSize.value = SortSizeType.SmallToBig
            sortDate.value = SortDateType.NoSelect
        }
        binding.ivPdf.click {
            val isExit = listFileType.value!!.contains(DocumentType.Pdf)
            if (isExit) {
                listFileType.value =
                    listFileType.value!!.filter { it != DocumentType.Pdf }.toMutableList()
            } else {
                val defList = listFileType.value
                defList!!.add(DocumentType.Pdf)
                listFileType.value = defList
            }
        }
        binding.ivDocx.click {
            val isExit = listFileType.value!!.contains(DocumentType.Doc)
            if (isExit) {
                listFileType.value =
                    listFileType.value!!.filter { it != DocumentType.Doc }.toMutableList()
            } else {
                val defList = listFileType.value
                defList!!.add(DocumentType.Doc)
                listFileType.value = defList
            }
        }
        binding.ivPpt.click {
            val isExit = listFileType.value!!.contains(DocumentType.Ppt)
            if (isExit) {
                listFileType.value =
                    listFileType.value!!.filter { it != DocumentType.Ppt }.toMutableList()
            } else {
                val defList = listFileType.value
                defList!!.add(DocumentType.Ppt)
                listFileType.value = defList
            }
        }
        binding.ivXlsx.click {
            val isExit = listFileType.value!!.contains(DocumentType.Excel)
            if (isExit) {
                listFileType.value =
                    listFileType.value!!.filter { it != DocumentType.Excel }.toMutableList()
            } else {
                val defList = listFileType.value
                defList!!.add(DocumentType.Excel)
                listFileType.value = defList
            }
        }
        binding.btnAgree.click {
            searchData.invoke(sortDate.value!!, sortSize.value!!, listFileType.value!!)
            dismiss()
        }
    }
}