package com.azg.pdf8.ui.main.document.pdf

import android.animation.ValueAnimator
import android.content.Context
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
import com.azg.pdf8.adapter.PdfDocumentAdapter
import com.azg.pdf8.adapter.PdfPreviewAdapter
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityReadPdfBinding
import com.azg.pdf8.databinding.PopupMoreActionBinding
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.utils.Constant.ARG_MEDIA_MODEL
import com.azg.pdf8.widget.PdfHighlightView
import com.dong.baselib.api.parcelable
import com.dong.baselib.base.PopupHelper
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.click
import com.dong.baselib.widget.dimenSdp
import com.dong.baselib.widget.doOnVisibilityChange
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.visible
import com.github.barteksc.pdfviewer.PDFView
import com.shockwave.pdfium.util.SizeF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class ReadPdfActivity : BaseActivity<ActivityReadPdfBinding>(ActivityReadPdfBinding::inflate) {
    private val viewModel: ReadPdfViewModel by viewModel()
    private var isSearchAction = false
    private var newContext: Context? = null
    private var totalPage = 1
    val pageSizes = mutableListOf<SizeF>()
    private var pdfFile: File? = null

    companion object {
        const val CONTENT_COPY = "PDF Text"
        const val CONTENT_PRINT = "PrintJob"
    }

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
        binding.lnSearchData.doOnVisibilityChange {
            isSearchAction = it
            binding.fileName.isVisible = !it
        }
        popupHerper.onBind { binding, popup ->
            lifecycleScope.launch {
                viewModel.pageViewState.collect {
                    binding.lnThumbNail.isVisible = it != PageViewType.Thumbnail
                    binding.lnPageByPage.isVisible = it != PageViewType.PageByPage
                }
            }
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
                val currentOrientation = resources.configuration.orientation
                requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        viewModel.pagesState.observe(this) { pages ->
            adapter.submitList(pages)
        }
    }
    override fun ActivityReadPdfBinding.setData() {
        lifecycleScope.launch {
            viewModel.pageViewState
                .map { it == PageViewType.Thumbnail }
                .distinctUntilChanged()
                .collect { show ->
                    TransitionManager.beginDelayedTransition(lnPdfRead, AutoTransition())
                    rcvFrameData.isVisible = show
                }
        }

    }

    override fun ActivityReadPdfBinding.onClick() {
        icBack.click {
            if (isSearchAction) {
                binding.lnSearchData.gone()
                binding.fileName.isVisible = true
            } else {
                finish()
            }
        }

        icSearchData.click {
            if (!isSearchAction) {
                binding.fileName.gone()
                binding.lnSearchData.visible()

            } else {
                val text = binding.edtSearchData.text.toString()
                if (text.isNotEmpty()) {
                    //searchAndHighlightInPdf(binding.pdfRead, text)
                }
            }
        }

        icOpenTools.click {
            popupHerper.show(icOpenTools)
        }

        binding.edtSearchData.onActionSearch(actionSuccess = { query ->
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
            binding.pdfRead.jumpTo(page, true)
        } else {
            jumpPending.set(page)
            binding.pdfRead.apply {
                onLayoutReady {
                    if (jumpPending.get() > -1) {
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
                .enableSwipe(true)
                .swipeHorizontal(viewModel.isHorizontal)
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
        if (this@ReadPdfActivity.isFinishing || this@ReadPdfActivity.isDestroyed) {
            this@ReadPdfActivity.finishAffinity()
            return
        }
        if (binding.lnSearchData.isVisible) {
            binding.lnSearchData.gone()
            binding.lnHeader.visible()
            binding.edtSearchData.setText("")
            return
        }
        finish()
    }
}

