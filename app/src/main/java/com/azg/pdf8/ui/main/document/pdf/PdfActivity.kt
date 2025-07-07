package com.azg.pdf8.ui.main.document.pdf

import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityPdfBinding
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.create.ChooseImageActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.widget.afterTextChanged
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import org.koin.android.ext.android.inject

class PdfActivity : BaseActivity<ActivityPdfBinding>(ActivityPdfBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    val viewModel: DocumentViewModel by inject()
    private val recentAdapter by lazy {
        RecentAdapter(onFavoriteClick = { doc, index ->
            viewModel.toggleFavoriteRecent(doc)
        }) {
            launchActivity<ReadPdfActivity>(hashMapOf(Constant.ARG_MEDIA_MODEL to it))
            viewModel.addToRecent(it)
        }.attachLifecycle(this@PdfActivity)
    }

    override fun initialize() {
        viewModel.setSearchCriteria("", DocumentType.Pdf)
    }

    override fun ActivityPdfBinding.setData() {
        lifecycleLaunch {
            viewModel.listDocumentSearch.collect {
                if (it.isEmpty()) {
                    rcvListData.gone()
                    lnNoData.visible()
                } else {
                    rcvListData.visible()
                    lnNoData.gone()
                    recentAdapter.submitList(it)
                }
            }
        }
        rcvListData.adapter = recentAdapter
        edtSearch.afterTextChanged {
            if (it.isEmpty()) {
                viewModel.setSearchCriteria(edtSearch.text.toString(), DocumentType.Pdf)
            }
        }
    }

    override fun ActivityPdfBinding.onClick() {
        icBack.click {
            backPressed()
        }
        icSearch.click {
            viewModel.setSearchCriteria(edtSearch.text.toString(), DocumentType.Pdf)
        }
        btnCreate.click {
            DialogCreatePdf(this@PdfActivity, object : CreateEventHandle {
                override fun createImage() {
                    launchActivity<ChooseImageActivity>(hashMapOf("key" to "createNew"))
                }

                override fun scanDocument() {
                    launchActivity<CameraActivity>(hashMapOf(Constant.SCREEN_ACTION to "mainSc"))
                }
            }).show()
        }
    }
}