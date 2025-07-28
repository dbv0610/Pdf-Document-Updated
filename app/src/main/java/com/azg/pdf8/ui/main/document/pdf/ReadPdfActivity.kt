package com.azg.pdf8.ui.main.document.pdf

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.RectF
import android.print.PrintManager
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.azg.pdf8.R
import com.azg.pdf8.adapter.PdfPreviewAdapter
import com.azg.pdf8.ads.ads.banner.BannerPlacement
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityReadPdfBinding
import com.azg.pdf8.databinding.PopupMoreActionBinding
import com.azg.pdf8.dialog.DialogProcess
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.utils.Constant.ARG_MEDIA_MODEL
import com.azg.pdf8.utils.Constant.ARG_SEARCH_WITH_PAGE
import com.azg.pdf8.viewmodel.DataResponse
import com.azg.pdf8.widget.PdfHighlightView
import com.dong.baselib.api.parcelable
import com.dong.baselib.base.PopupHelper
import com.dong.baselib.base.SystemUtil
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dimenSdp
import com.dong.baselib.widget.doOnVisibilityChange
import com.dong.baselib.widget.dpToPx
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.navigationBarHeight
import com.dong.baselib.widget.paddingRight
import com.dong.baselib.widget.visible
import com.github.barteksc.pdfviewer.PDFView
import com.shockwave.pdfium.util.SizeF
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class ReadPdfActivity : BaseActivity<ActivityReadPdfBinding>(ActivityReadPdfBinding::inflate) {
    private val viewModel: ReadPdfViewModel by viewModel()
    private var newContext: Context? = null
    private var totalPage = 1
    val pageSizes = mutableListOf<SizeF>()
    private var pdfFile: File? = null
    private val mediaModel by lazy {
        runCatching {
            intent.parcelable<RecentDocument>(ARG_MEDIA_MODEL)
        }
    }
    val popupHerper = PopupHelper.with(this@ReadPdfActivity, PopupMoreActionBinding::inflate)
    private var adapter: PdfPreviewAdapter =
        PdfPreviewAdapter() {
            jumpToPageWhenReady(it.index)
        }.attachLifecycle(this@ReadPdfActivity)

    override fun initialize() {
        mediaModel.getOrNull()?.let {
            binding.fileName.text = it.path.fileName()
            viewModel.setMediaModel(it)
            loadPdf()
            viewModel.stateFavoriteCurrent(viewModel.pdfPath)
            val pdfUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                File(it.path)
            )
            viewModel.renderPdf(this, pdfUri)
            binding.rcvFrameData.apply {
                layoutManager = LinearLayoutManager(context, VERTICAL, false)
                adapter = this@ReadPdfActivity.adapter
            }
        }

        getData<Boolean?>(Constant.RateWhenCreate)?.let {
            if(!SystemUtil.isRatting(this@ReadPdfActivity)){
                rattingDialog.show()
            }
        }


        popupHerper.onBind { binding, popup ->
            val currentOrientation = resources.configuration.orientation
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                binding.root.paddingRight(navigationBarHeight * 1.15)
            }
            lifecycleScope.launch {
                viewModel.pageViewState.collect {
                    binding.lnThumbNail.isVisible = it != PageViewType.Thumbnail
                    binding.lnPageByPage.isVisible = it != PageViewType.PageByPage
                }
            }
            binding.lnToPdf.gone()
            binding.lnPageByPage.click {
                viewModel.setPageState(PageViewType.PageByPage)
                popup.dismiss()
            }
            binding.lnThumbNail.click {
                viewModel.setPageState(PageViewType.Thumbnail)
                popup.dismiss()
            }
            binding.lnRotate.click {
                popup.dismiss()

                requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        viewModel.pagesState.observe(this) { pages ->
            adapter.submitList(pages)
        }
        binding.bannerAdView
            .setBannerPlacement(this@ReadPdfActivity, BannerPlacement.BANNER_ALL)
            .requestBanner()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                if (!isInternetAvailable()) {
                    binding.bannerAdView.gone()
                }
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                binding.bannerAdView.gone()
            }
            else -> Unit
        }
    }

    private val loadingDialog by lazy {
        DialogProcess(this@ReadPdfActivity)
    }

    override fun ActivityReadPdfBinding.setData() {
        PDFBoxResourceLoader.init(this@ReadPdfActivity)
        lifecycleScope.launch {
            viewModel.pageViewState
                .map { it == PageViewType.Thumbnail }
                .distinctUntilChanged()
                .collect { show ->
                    TransitionManager.beginDelayedTransition(lnPdfRead, AutoTransition())
                    rcvFrameData.isVisible = show
                }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.searchQuery.collect { state ->
                if (state is DataResponse.DataSuccess) {
                    if (state.data.isNotEmpty()) {
                        loadingDialog.dismiss()
                        val intent = Intent(this@ReadPdfActivity, SearchResultActivity::class.java)
                        intent.putExtra(ARG_SEARCH_WITH_PAGE, ArrayList(state.data))
                        startActivity(intent)
                    } else {
                        loadingDialog.dismiss()
                        toastShort(getString(R.string.not_found_search))
                    }
                }
            }
        }
    }

    override fun ActivityReadPdfBinding.onClick() {
        icBackApp.click {
            backPressed()
        }

        icBackSearch.click {
            edtSearchData.setText("")
            binding.lnSearchData.gone()
            binding.lnHeaderDef.visible()
        }

        icSearchApp.click {
            binding.lnSearchData.visible()
            binding.lnHeaderDef.gone()
        }
        icSearchData.click {
            val query = edtSearchData.text.toString()
            if (query.isNotEmpty()) {
                loadingDialog.show()
                viewModel.pdfPath?.let { path ->
                    viewModel.getSearchQuery(path, query)
                }
            } else {
                toastShort(getString(R.string.please_enter_a_search))
            }
        }

        icOpenTools.click {
            popupHerper.show(icOpenTools)
        }

        binding.edtSearchData.onActionSearch(actionSuccess = { query ->
            loadingDialog.show()
            viewModel.pdfPath?.let { path ->
                viewModel.getSearchQuery(path, query)
            }
        }, actionFail = {
            toastShort(getString(R.string.please_enter_a_search))
        })
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        newContext = newBase
    }

    fun EditText.onActionSearch(actionSuccess: (query: String) -> Unit, actionFail: () -> Unit) {
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = text.toString().trim()
                if (query.isNotEmpty()) {
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(windowToken, 0)
                    actionSuccess.invoke(query)
                } else {
                    actionFail.invoke()
                }
                true
            } else {
                false
            }
        }
    }

    private val jumpPending = AtomicInteger(-1)

    private fun jumpToPageWhenReady(page: Int) {
        if (binding.pdfRead.isLaidOut) {
            adapter.setCurrentPage(page)
            binding.pdfRead.jumpTo(page, true)
        } else {
            jumpPending.set(page)
            binding.pdfRead.apply {
                onLayoutReady {
                    if (jumpPending.get() > -1) {
                        adapter.setCurrentPage(jumpPending.get())
                        jumpTo(jumpPending.get(), true)
                        jumpPending.set(-1)
                    }
                }
            }
        }
    }

    fun View.onLayoutReady(action: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                action()
            }
        })
    }

    private fun loadPdf() {
        viewModel.pdfPath?.let {
            pdfFile = File(it)
            binding.pdfRead.fromFile(pdfFile)
                .enableAnnotationRendering(true)
                .defaultPage(viewModel.page - 1)
                .spacing(8)
                .pageFling(true)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(false)
                .nightMode(viewModel.nightMode)
                .onLoad { pageCount ->
                    pageSizes.clear()
                    for (i in 0 until pageCount) {
                        pageSizes.add(binding.pdfRead.getPageSize(i))
                    }
                    totalPage = pageCount
                    binding.txtNumberPage.text = "${viewModel.page}/$pageCount"
                }
                .onPageChange { page, pageCount ->
                    val content = "${page + 1}/$pageCount"
                    binding.txtNumberPage.text = content
                }.load()
        }
    }

    override fun backPressed() {
        if (binding.lnSearchData.isVisible) {
            binding.lnSearchData.gone()
            binding.lnHeaderDef.visible()
            binding.edtSearchData.setText("")
            return
        }
        lifecycleScope.launch {
            MainActivity.isShowRateFirstView.emit(true)
        }
        finish()
    }
}

