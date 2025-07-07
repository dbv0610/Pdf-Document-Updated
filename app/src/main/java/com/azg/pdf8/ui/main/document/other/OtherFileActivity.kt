package com.azg.pdf8.ui.main.document.other

import com.azg.pdf8.R
import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.database.FavoriteDao
import com.azg.pdf8.databinding.ActivityOtherBinding
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.AppDataRepo
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.widget.docColor
import com.azg.pdf8.widget.pptColor
import com.azg.pdf8.widget.xlsColor
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.widget.afterTextChanged
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import org.koin.android.ext.android.inject

class OtherFileActivity : BaseActivity<ActivityOtherBinding>(ActivityOtherBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    val appRepo: AppDataRepo by inject()
    val viewModel: DocumentViewModel by inject()
    val documentDao: FavoriteDao by inject()
    private val pdfAdapter by lazy {
        RecentAdapter(documentDao) {
            viewModel.addToRecent(it)
        }.attachLifecycle(this@OtherFileActivity)
    }
    var documentType = DocumentType.Doc

    override fun initialize() {

        documentType = when (getData<String>(Constant.DOCUMENT_TYPE).toString()) {
            Constant.Doc -> DocumentType.Doc
            Constant.Xls -> DocumentType.Excel
            else -> DocumentType.Ppt
        }
        viewModel.searchByKey("", documentType)
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
                viewModel.searchByKey(edtSearch.text.toString(), documentType)
            }
        }

    }

    override fun ActivityOtherBinding.onClick() {
        icBack.click {
            backPressed()
        }
        icSearch.click {
            viewModel.searchByKey(edtSearch.text.toString(), documentType)
        }
    }
}