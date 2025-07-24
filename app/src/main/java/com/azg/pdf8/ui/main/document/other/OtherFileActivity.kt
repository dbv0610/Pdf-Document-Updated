package com.azg.pdf8.ui.main.document.other

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.widget.Button
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.azg.pdf8.R
import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityOtherBinding
import com.azg.pdf8.databinding.PopupMenuActionBinding
import com.azg.pdf8.dialog.DeleteDialog
import com.azg.pdf8.dialog.RenameDialog
import com.azg.pdf8.dialog.SortDataByDialog
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.widget.docColor
import com.azg.pdf8.widget.pdfColor
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
import com.wxiwei.office.java.awt.Color
import org.koin.android.ext.android.inject
import java.io.File

class OtherFileActivity : BaseActivity<ActivityOtherBinding>(ActivityOtherBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    val viewModel: DocumentViewModel by inject()
    private lateinit var layoutManager: LinearLayoutManager
    private var lastScrollPosition = 0
    private var currentPos = 0
    private var isClickInFavorite = false
    private val documentAdapter by lazy {
        RecentAdapter(onFavoriteClick = { file, index ->
            isClickInFavorite = true
            viewModel.toggleFavoriteRecent(file)
        }, onMenuClick = { view, doc, pos ->
            popupHerper?.show(view, doc)
            currentPos = pos
        }) {
            launchActivity<ReadDocumentActivity>(
                hashMapOf(
                    Constant.DOCUMENT_TYPE to docKey,
                    Constant.ARG_MEDIA_MODEL to it
                )
            )
            viewModel.addToRecent(it.apply { it.lastTimeView = System.currentTimeMillis() })
        }.attachLifecycle(this@OtherFileActivity)
    }
    var documentType = DocumentType.Doc
    var docKey = Constant.Doc
    var popupHerper: PopupDataHelper<PopupMenuActionBinding, RecentDocument>? = null
    val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this@OtherFileActivity,
            config = NativePlacement.NATIVE_DOC.apply {
                NativePlacement.LayoutSelector.getLayout(
                    remoteConfig.N110Config1.layout
                )
            },
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { binding.shimmerNativeAd.shimmerContainerNative }
        )
    }

    fun requestAds(color: Int) {
        with(nativeAdsWrapper) {
            setupNativeAd(
                "native_choose_image", NativePlacement.LayoutSelector.getLayout(
                    remoteConfig.N110Config2.layout
                )
            ) {
                (callToActionView as? Button)?.let { btn ->
                    val wrapped = DrawableCompat.wrap(btn.background.mutate())
                    DrawableCompat.setTintList(wrapped, ColorStateList.valueOf(color))
                    DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.SRC_IN)
                    btn.background = wrapped
                }
            }
            requestAds()
        }
    }

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
                popup.dismiss()
                DeleteDialog(this@OtherFileActivity) {
                    model?.let {
                        val file = File(it.path)
                        if (file.delete()) {
                            viewModel.removeRecent(model)
                            documentAdapter.removeItem(currentPos)
                        }
                    }
                }.show()
            }
        }
        requestAds(
            when (documentType) {
                DocumentType.Doc -> docColor
                DocumentType.Excel -> xlsColor
                else -> pptColor
            }
        )
    }

    override fun ActivityOtherBinding.setData() {
        layoutManager = LinearLayoutManager(this@OtherFileActivity)
        rcvListData.layoutManager = layoutManager
        lifecycleLaunch {
            viewModel.listDocumentSearch.collect {
                if (it.isEmpty()) {
                    rcvListData.gone()
                    lnNoData.visible()
                } else {
                    rcvListData.visible()
                    lnNoData.gone()
                    lastScrollPosition = layoutManager.findFirstVisibleItemPosition()
                    documentAdapter.submitList(it) {
                        if (isClickInFavorite) {
                            layoutManager.scrollToPositionWithOffset(lastScrollPosition, 0)
                            isClickInFavorite = false
                        } else {
                            layoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    }
                }
            }
        }
        rcvListData.adapter = documentAdapter
        edtSearch.afterTextChanged {
            viewModel.setSearchCriteria(edtSearch.text.toString(), documentType)
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