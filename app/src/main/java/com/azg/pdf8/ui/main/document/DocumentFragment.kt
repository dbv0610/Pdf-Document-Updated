package com.azg.pdf8.ui.main.document;

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.R;
import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.FragmentDocumentBinding
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.create.ChooseImageActivity
import com.azg.pdf8.ui.main.document.other.OtherFileActivity
import com.azg.pdf8.ui.main.document.pdf.PdfActivity
import com.azg.pdf8.ui.main.document.pdf.ReadPdfActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dimenSdp
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.paddingTop
import com.dong.baselib.widget.visible
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class DocumentFragment :
    BaseFragment<FragmentDocumentBinding>(FragmentDocumentBinding::inflate, true) {
    val documentViewModel: DocumentViewModel by activityViewModel()
    private val createDialog by lazy {
        DialogCreatePdf(this@DocumentFragment.appActivity, object : CreateEventHandle {
            override fun createImage() {
                launchActivity<ChooseImageActivity>(hashMapOf("key" to "createNew"))
            }

            override fun scanDocument() {
                launchActivity<CameraActivity>(hashMapOf(Constant.SCREEN_ACTION to "mainSc"))
            }
        })
    }
    private val pdfAdapter by lazy {
        RecentAdapter(onFavoriteClick = { doc, index ->
            documentViewModel.toggleFavoriteRecent(doc)
        }) {
            launchActivity<ReadPdfActivity>(hashMapOf(Constant.ARG_MEDIA_MODEL to it))
            documentViewModel.addToRecent(it)
        }.attachLifecycle(viewLifecycleOwner)
    }
    @SuppressLint("SetTextI18n")
    override fun FragmentDocumentBinding.initView() {
        lnHeader.paddingTop(statusBarHeight + appActivity.dimenSdp(6))
        val pairs = listOf(
            documentViewModel.repo.documentListDoc to countFileDocx,
            documentViewModel.repo.documentListPdf to countFilePdf,
            documentViewModel.repo.documentListPpt to countFilePpt,
            documentViewModel.repo.documentListXls to countFileXls
        )
        val filesLabel = getString(R.string.files)
        pairs.forEach { (flow, textView) ->
            lifecycleScope.launch {
                flow.collect { count ->
                    textView.text = "${count.size} $filesLabel"
                }
            }
        }

        lifecycleLaunch {
            documentViewModel.recentDocument.collect {
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
    }

    override fun FragmentDocumentBinding.onClick() {
        btnCreate.click {
            createDialog.show()
        }
        lnPdf.click {
            launchActivity<PdfActivity>()
        }
        lnDocx.click {
            launchActivity<OtherFileActivity>(hashMapOf(Constant.DOCUMENT_TYPE to Constant.Doc))
        }
        lnXls.click {
            launchActivity<OtherFileActivity>(hashMapOf(Constant.DOCUMENT_TYPE to Constant.Xls))
        }
        lnPpt.click {
            launchActivity<OtherFileActivity>(hashMapOf(Constant.DOCUMENT_TYPE to Constant.Ppt))
        }
    }
}