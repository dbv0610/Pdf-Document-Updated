package com.azg.pdf8.ui.main.document.other

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.R
import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityOtherBinding
import com.azg.pdf8.databinding.PopupMenuActionBinding
import com.azg.pdf8.dialog.RenameDialog
import com.azg.pdf8.dialog.SortDataByDialog
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.document.pdf.PageViewType
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.widget.docColor
import com.azg.pdf8.widget.pptColor
import com.azg.pdf8.widget.xlsColor
import com.dong.baselib.base.PopupDataHelper
import com.dong.baselib.file.shareFileWithPath
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.afterTextChanged
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class OtherFileActivity : BaseActivity<ActivityOtherBinding>(ActivityOtherBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    val viewModel: DocumentViewModel by inject()
    private val pdfAdapter by lazy {
        RecentAdapter(onFavoriteClick = { file, index ->
            viewModel.toggleFavoriteRecent(file)
        }, onMenuClick = { view, doc, pos ->
            popupHerper?.show(view, doc)
        }) {
            launchActivity<ReadDocumentActivity>(
                hashMapOf(
                    Constant.DOCUMENT_TYPE to docKey,
                    Constant.ARG_MEDIA_MODEL to it
                )
            )
            viewModel.addToRecent(it)
        }.attachLifecycle(this@OtherFileActivity)
    }
    var documentType = DocumentType.Doc
    var docKey = Constant.Doc
    var popupHerper: PopupDataHelper<PopupMenuActionBinding, RecentDocument>? = null

    override fun initialize() {
        docKey = getData<String>(Constant.DOCUMENT_TYPE).toString()
        documentType = when (docKey) {
            Constant.Doc -> DocumentType.Doc
            Constant.Xls -> DocumentType.Excel
            else -> DocumentType.Ppt
        }
        viewModel.setSearchCriteria("", documentType)
        binding.root.setBackgroundColor(
            when (documentType) {
                DocumentType.Doc -> docColor
                DocumentType.Excel -> xlsColor
                else -> pptColor
            }
        )
        binding.folderName.text = getString(
            when (documentType) {
                DocumentType.Doc -> R.string.doc
                DocumentType.Excel -> R.string.xls
                else -> R.string.pptx
            }
        )

        popupHerper = PopupDataHelper.with(
            this@OtherFileActivity,
            PopupMenuActionBinding::inflate
        )
        popupHerper?.onBindData { binding, popup, model ->
            binding.lnShare.click {
                shareFileWithPath(this@OtherFileActivity, model?.path ?: "")
                popup.dismiss()
            }
            binding.lnRename.click {
                popup.dismiss()
                RenameDialog(this@OtherFileActivity) { newN ->
                    model?.let {
                        viewModel.renameFile(model, newN)
                    }
                }.showRename(model?.path?.fileName() ?: "")
            }
            binding.lnDelete.click {
                model?.let {
                    viewModel.removeFavorite(model)
                }
            }
        }
    }

    override fun ActivityOtherBinding.setData() {
        lifecycleLaunch {
            viewModel.listDocumentSearch.collect {
                if (it.isEmpty()) {
                    rcvListData.gone()
                    lnNoData.visible()
                } else {
                    rcvListData.visible()
                    lnNoData.gone()
                    pdfAdapter.submitList(it)
                }
            }
        }
        rcvListData.adapter = pdfAdapter
        edtSearch.afterTextChanged {
            if (it.isEmpty()) {
                viewModel.setSearchCriteria(edtSearch.text.toString(), documentType)
            }
        }
    }

    override fun ActivityOtherBinding.onClick() {
        icBack.click {
            backPressed()
        }
        icSearch.click {
            viewModel.setSearchCriteria(edtSearch.text.toString(), documentType)
        }
        icSortData.click {
            SortDataByDialog(this@OtherFileActivity)
                .attachLifecycle(this@OtherFileActivity)
                .onSearchEvent { viewModel.setSortByData(it) }
                .showSortByData(viewModel.sortByData.value)
                .show()
        }
    }
}