package com.azg.pdf8.ui.main.document;

import android.annotation.SuppressLint
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.lifecycleScope
import com.azg.pdf8.R;
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.FragmentDocumentBinding
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.create.ChooseImageActivity
import com.azg.pdf8.ui.main.create.CreateActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dimenSdp
import com.dong.baselib.widget.paddingTop
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
    }

    override fun FragmentDocumentBinding.onClick() {
        btnCreate.click {
            createDialog.show()
        }
    }
}