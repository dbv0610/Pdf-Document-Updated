package com.azg.pdf8.ui.main.document;

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.R
import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.ads.ads.interstitial.InterstitialAdManager
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.base.BaseFragment
import com.azg.pdf8.databinding.FragmentDocumentBinding
import com.azg.pdf8.databinding.PopupMenuActionBinding
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DeleteDialog
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.dialog.DialogPermission
import com.azg.pdf8.dialog.RenameDialog
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.create.ChooseImageActivity
import com.azg.pdf8.ui.main.document.other.OtherFileActivity
import com.azg.pdf8.ui.main.document.other.ReadDocumentActivity
import com.azg.pdf8.ui.main.document.pdf.PdfActivity
import com.azg.pdf8.ui.main.document.pdf.ReadPdfActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.dong.baselib.base.PopupDataHelper
import com.dong.baselib.file.shareFileWithPath
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.permission.Permission
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dimenSdp
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.paddingTop
import com.dong.baselib.widget.visible
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class DocumentFragment :
    BaseFragment<FragmentDocumentBinding>(FragmentDocumentBinding::inflate, true) {
    val documentViewModel: DocumentViewModel by activityViewModel()
    private val createDialog by lazy {
        DialogCreatePdf(this@DocumentFragment.appActivity, object : CreateEventHandle {
            override fun createImage() {
                NativeAdPreloadManager.preloadAd(appActivity, NativePlacement.PERMISSION, 2, false)
                InterstitialAdManager.showInterAll(appActivity) {
                    launchActivity<ChooseImageActivity>(hashMapOf(Constant.KEY_ACTION to "createNew"))
                }
            }

            override fun scanDocument() {
                if (permission.checkGrantedCamera) {
                    InterstitialAdManager.showInterAll(appActivity) {
                        launchActivity<CameraActivity>(hashMapOf(Constant.SCREEN_ACTION to "mainSc"))
                    }
                } else {
                    fragmentAttach?.fragmentAction("requestCameraPer")
                }
            }
        }).attachActivity(appActivity)
    }

    override fun backPress() {
        super.backPress()
        fragmentAttach?.fragmentOnBack()
    }

    private var currentIndex = 0
    var popupHerper: PopupDataHelper<PopupMenuActionBinding, RecentDocument>? = null
    private val pdfAdapter by lazy {
        RecentAdapter(onFavoriteClick = { doc, index ->
            currentIndex = 0
            documentViewModel.toggleFavoriteRecent(doc)
        }, onMenuClick = { view, doc, pos ->
            popupHerper?.show(view, doc)
        }) {
            if (it.type == DocumentType.Pdf) {
                launchActivity<ReadPdfActivity>(hashMapOf(Constant.ARG_MEDIA_MODEL to it))
            } else {
                val docKey = when (it.type) {
                    DocumentType.Doc -> Constant.Doc
                    DocumentType.Excel -> Constant.Xls
                    else -> Constant.Ppt
                }
                launchActivity<ReadDocumentActivity>(
                    hashMapOf(
                        Constant.DOCUMENT_TYPE to docKey,
                        Constant.ARG_MEDIA_MODEL to it
                    )
                )
            }
            documentViewModel.addToRecent(it.apply { it.lastTimeView = System.currentTimeMillis() })
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
        popupHerper = PopupDataHelper.with(
            appActivity,
            PopupMenuActionBinding::inflate
        )
        popupHerper?.onBindData { binding, popup, model ->
            binding.lnShare.click {
                shareFileWithPath(this@DocumentFragment.appActivity, model?.path ?: "")
                popup.dismiss()
            }
            binding.lnRename.click {
                popup.dismiss()
                RenameDialog(appActivity) { newN ->
                    model?.let {
                        documentViewModel.renameFile(model, newN)
                    }
                }.showRename(model?.path?.fileName() ?: "")
            }
            binding.lnDelete.click {
                popup.dismiss()
                DeleteDialog(appActivity) {
                    model?.let {
                        documentViewModel.removeRecent(model)
                    }
                }.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val isGrantPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isStorageAccess()
        } else permission.checkGrantedStorage_24_33
        binding.lnGrantPer.isVisible = isGrantPermission
        binding.lnNoPer.isVisible = !isGrantPermission
    }

    override fun FragmentDocumentBinding.onClick() {
        btnCreate.click {
            if (isStorageAccess()) {
                createDialog.show()
            } else {
                DialogPermission(appActivity).attachActivity(appActivity).onAllowAccess {
                    fragmentAttach?.fragmentAction("requestPermission")
                }.show()
            }
        }
        lnPdf.click {
            if(isStorageAccess()){
            InterstitialAdManager.showInterAll(appActivity) {
                launchActivity<PdfActivity>()
            }
        } else {
            DialogPermission(appActivity).attachActivity(appActivity).onAllowAccess {
                fragmentAttach?.fragmentAction("requestPermission")
            }.show()
        }
        }
        tvShowAll.click {
            if (isStorageAccess()) {
                launchActivity<RecentActivity>()
            } else {
                DialogPermission(appActivity).attachActivity(appActivity).onAllowAccess {
                    fragmentAttach?.fragmentAction("requestPermission")
                }.show()
            }
        }
        btnGranted.click {
            fragmentAttach?.fragmentAction("requestPermission")
        }
        lnDocx.click {
            if (isStorageAccess()) {
                InterstitialAdManager.showInterAll(appActivity) {
                    launchActivity<OtherFileActivity>(hashMapOf(Constant.DOCUMENT_TYPE to Constant.Doc))
                }
            } else {
                DialogPermission(appActivity).attachActivity(appActivity).onAllowAccess {
                    fragmentAttach?.fragmentAction("requestPermission")
                }.show()
            }
        }
        lnXls.click {
            if (isStorageAccess()) {
                InterstitialAdManager.showInterAll(appActivity) {
                    launchActivity<OtherFileActivity>(hashMapOf(Constant.DOCUMENT_TYPE to Constant.Xls))
                }
            } else {
                DialogPermission(appActivity).attachActivity(appActivity).onAllowAccess {
                    fragmentAttach?.fragmentAction("requestPermission")
                }.show()
            }
        }
        lnPpt.click {
            if (isStorageAccess()) {
                InterstitialAdManager.showInterAll(appActivity) {
                    launchActivity<OtherFileActivity>(hashMapOf(Constant.DOCUMENT_TYPE to Constant.Ppt))
                }
            } else {
                DialogPermission(appActivity).attachActivity(appActivity).onAllowAccess {
                    fragmentAttach?.fragmentAction("requestPermission")
                }.show()
            }
        }
    }
}