package com.azg.pdf8.ui.main.document.pdf

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.widget.Button
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ag.sampleadsfirstflow.ads.native.NativeAdPreloadManager
import com.azg.pdf8.adapter.RecentAdapter
import com.azg.pdf8.ads.ads.banner.BannerPlacement
import com.azg.pdf8.ads.ads.native.NativeAdsWrapper
import com.azg.pdf8.ads.ads.native.NativePlacement
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.app.remoteConfig
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityPdfBinding
import com.azg.pdf8.databinding.PopupMenuActionBinding
import com.azg.pdf8.dialog.CreateEventHandle
import com.azg.pdf8.dialog.DeleteDialog
import com.azg.pdf8.dialog.DialogCreatePdf
import com.azg.pdf8.dialog.RenameDialog
import com.azg.pdf8.dialog.SortDataByDialog
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.camera.CameraActivity
import com.azg.pdf8.ui.main.create.ChooseImageActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.widget.pdfColor
import com.dong.baselib.base.PopupDataHelper
import com.dong.baselib.file.shareFileWithPath
import com.dong.baselib.lifecycle.lifecycleLaunch
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.afterTextChanged
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import org.koin.android.ext.android.inject
import java.io.File

class PdfActivity : BaseActivity<ActivityPdfBinding>(ActivityPdfBinding::inflate) {
    override fun backPressed() {
        finish()
    }

    val viewModel: DocumentViewModel by inject()
    private var currentPos = 0
    private lateinit var layoutManager: LinearLayoutManager
    private var lastScrollPosition = 0
    private var isClickInFavorite = false
    private val recentAdapter by lazy {
        RecentAdapter(onFavoriteClick = { doc, index ->
            isClickInFavorite = true
            viewModel.toggleFavoriteRecent(doc)
        }, onMenuClick = { view, doc, pos ->
            currentPos = pos
            popupHerper?.show(view, doc)
        }) {
            launchActivity<ReadPdfActivity>(hashMapOf(Constant.ARG_MEDIA_MODEL to it))
            viewModel.addToRecent(it.apply { it.lastTimeView = System.currentTimeMillis() })
        }.attachLifecycle(this@PdfActivity)
    }
    val nativeAdsWrapper by lazy {
        NativeAdsWrapper(
            activity = this@PdfActivity,
            config = NativePlacement.NATIVE_DOC,
            lifecycleOwner = this,
            adContainer = { binding.flNativeAd },
            shimmerView = { binding.shimmerNativeAd.shimmerContainerNative }
        )
    }

    fun requestAds() {
        with(nativeAdsWrapper) {
            setupNativeAd(
                "native_choose_image", NativePlacement.LayoutSelector.getLayout(
                    remoteConfig.N110Config2.layout
                )
            ) {
                (callToActionView as? Button)?.let { btn ->
                    val wrapped = DrawableCompat.wrap(btn.background.mutate())
                    DrawableCompat.setTintList(wrapped, ColorStateList.valueOf(pdfColor))
                    DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.SRC_IN)
                    btn.background = wrapped
                }
            }
            requestAds()
        }
    }

    var popupHerper: PopupDataHelper<PopupMenuActionBinding, RecentDocument>? = null
    override fun initialize() {
        viewModel.setSearchCriteria("", DocumentType.Pdf)
        popupHerper = PopupDataHelper.with(
            this@PdfActivity,
            PopupMenuActionBinding::inflate
        )

        popupHerper?.onBindData { binding, popup, model ->
            binding.lnShare.click {
                shareFileWithPath(this@PdfActivity, model?.path ?: "")
                popup.dismiss()
            }
            binding.lnRename.click {
                popup.dismiss()
                RenameDialog(this@PdfActivity) { newN ->
                    model?.let {
                        viewModel.renameFile(model, newN)
                    }
                }.showRename(model?.path?.fileName() ?: "")
            }
            binding.lnDelete.click {
                popup.dismiss()
                DeleteDialog(this@PdfActivity) {
                    model?.let {
                        val file = File(it.path)
                        if (file.delete()) {
                            recentAdapter.removeItem(currentPos)
                            viewModel.removeRecent(model)
                        }
                    }
                }.show()
            }
        }
        requestAds()
    }

    override fun ActivityPdfBinding.setData() {
        layoutManager = LinearLayoutManager(this@PdfActivity)
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
                    recentAdapter.submitList(it) {
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

        rcvListData.adapter = recentAdapter
        edtSearch.afterTextChanged {
            viewModel.setSearchCriteria(edtSearch.text.toString(), DocumentType.Pdf)
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
                    NativeAdPreloadManager.preloadAd(
                        this@PdfActivity,
                        NativePlacement.PERMISSION,
                        2,
                        false
                    )
                    launchActivity<ChooseImageActivity>(hashMapOf(Constant.KEY_ACTION to "createNew"))
                }

                override fun scanDocument() {
                    if (permission.checkGrantedCamera) {
                        launchActivity<CameraActivity>(hashMapOf(Constant.SCREEN_ACTION to "mainSc"))
                    } else {
                        requestCameraLauncher.launch(permission.cameraRequest)
                    }
                }
            }).attachActivity(this@PdfActivity).show()
        }
        icSortData.click {
            SortDataByDialog(this@PdfActivity)
                .attachLifecycle(this@PdfActivity)
                .onSearchEvent { viewModel.setSortByData(it) }
                .showSortByData(viewModel.sortByData.value)
                .show()
        }
    }
}