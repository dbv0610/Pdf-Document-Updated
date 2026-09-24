package com.azg.pdf8.ui.main.document.pdf

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import com.wxiwei.office.pdf.PDFView
import com.wxiwei.office.system.IMainFrame
import com.wxiwei.office.system.MainControl
import com.wxiwei.office.system.OnOpenFileListener
import com.wxiwei.office.res.ResKit
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.azg.pdf8.R
import com.azg.pdf8.adapter.PdfPreviewAdapter
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
import com.dong.baselib.api.parcelable
import com.dong.baselib.base.PopupHelper
import com.dong.baselib.base.SystemUtil
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.navigationBarHeight
import com.dong.baselib.widget.paddingRight
import com.dong.baselib.widget.visible
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class ReadPdfActivity : BaseActivity<ActivityReadPdfBinding>(ActivityReadPdfBinding::inflate), IMainFrame {
    private val viewModel: ReadPdfViewModel by viewModel()
    private var mainControl: MainControl? = null
    private var pdfView: PDFView? = null
    private var pendingPage: Int? = null
    private var readerDisposed = false
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
            binding.rcvFrameData.apply {
                layoutManager = LinearLayoutManager(context, VERTICAL, false)
                adapter = this@ReadPdfActivity.adapter
            }
        } ?: run {
            showReadError()
            return
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
            binding.lnThumbNail.isVisible = viewModel.pageViewState.value != PageViewType.Thumbnail
            binding.lnPageByPage.isVisible = viewModel.pageViewState.value != PageViewType.PageByPage
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
                if (state is DataResponse.DataError) {
                    loadingDialog.dismiss()
                    toastShort(getString(R.string.search_error_occurred))
                } else if (state is DataResponse.DataSuccess) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let { viewModel.setPage(it.getInt("pdf_page", 1)) }
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("pdf_page", viewModel.page)
        super.onSaveInstanceState(outState)
    }

    private fun jumpToPageWhenReady(page: Int) {
        pendingPage = page
        val view = pdfView ?: return
        view.doOnLayout {
            if (readerDisposed || pdfView !== view) return@doOnLayout
            val target = pendingPage ?: return@doOnLayout
            val count = view.getPageCount()
            if (count <= 0) return@doOnLayout
            pendingPage = null
            val index = target.coerceIn(0, count - 1)
            view.showPDFPageForIndex(index)
            viewModel.setPage(index + 1)
            adapter.setCurrentPage(index)
            binding.txtNumberPage.text = "${index + 1}/$count"
        }
    }

    private fun loadPdf() {
        val file = viewModel.pdfPath?.let(::File)
        if (file == null || !file.isFile || !file.canRead()) {
            showReadError()
            return
        }
        mainControl = MainControl(this).also { control ->
            control.setOpenFileListener(object : OnOpenFileListener {
                override fun onOpenFileSuccess() = Unit
                override fun onOpenFileFailure() = showReadError()
            })
            control.openFile(file.absolutePath)
        }
    }

    private fun showReadError() {
        if (isFinishing || readerDisposed) return
        loadingDialog.dismiss()
        toastShort(getString(R.string.some_errors_occurred_please_try_again))
        finish()
    }

    override fun openFileFinish() {
        if (isFinishing || readerDisposed) return
        val view = mainControl?.getView() as? PDFView ?: return showReadError()
        if (pdfView === view) return
        pdfView = view
        binding.pdfRead.addView(view, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        jumpToPageWhenReady(pendingPage ?: (viewModel.page - 1))
        viewModel.pdfPath?.let { path ->
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", File(path))
            viewModel.renderPdf(applicationContext, uri)
        }
    }

    override fun changePage() {
        val view = pdfView ?: return
        if (readerDisposed || pendingPage != null) return
        val count = view.getPageCount()
        if (count <= 0) return
        val page = view.getCurrentPageNumber().coerceIn(1, count)
        viewModel.setPage(page)
        binding.txtNumberPage.text = "$page/$count"
        // PDFView calls this while drawing; update the RecyclerView afterwards.
        binding.rcvFrameData.post {
            if (!readerDisposed) adapter.setCurrentPage(viewModel.page - 1)
        }
    }

    override fun getActivity(): Activity = this
    override fun doActionEvent(actionID: Int, obj: Any?): Boolean = false
    override fun updateToolsbarStatus() = Unit
    override fun setFindBackForwardState(state: Boolean) = Unit
    override fun getBottomBarHeight(): Int = 0
    override fun getTopBarHeight(): Int = 0
    override fun getAppName(): String = getString(R.string.app_name)
    override fun getTemporaryDirectory(): File = cacheDir
    override fun onEventMethod(v: View?, e1: MotionEvent?, e2: MotionEvent?,
        xValue: Float, yValue: Float, eventMethodType: Byte): Boolean = false
    override fun isDrawPageNumber(): Boolean = false
    override fun isShowZoomingMsg(): Boolean = false
    override fun isPopUpErrorDlg(): Boolean = true
    override fun isShowPasswordDlg(): Boolean = true
    override fun isShowProgressBar(): Boolean = true
    override fun isShowFindDlg(): Boolean = false
    override fun isShowTXTEncodeDlg(): Boolean = false
    override fun getTXTDefaultEncode(): String = "UTF-8"
    override fun isTouchZoom(): Boolean = !viewModel.zoomLock
    override fun isZoomAfterLayoutForWord(): Boolean = false
    override fun getWordDefaultView(): Byte = 0
    override fun getLocalString(resName: String): String? = ResKit.instance().getLocalString(resName)
    override fun changeZoom() = Unit
    override fun completeLayout() = Unit
    override fun error(errorCode: Int) = showReadError()
    override fun fullScreen(fullscreen: Boolean) = Unit
    override fun showProgressBar(visible: Boolean) = Unit
    override fun updateViewImages(viewList: List<Int?>) = Unit
    override fun isChangePage(): Boolean = true
    override fun setWriteLog(saveLog: Boolean) = Unit
    override fun isWriteLog(): Boolean = false
    override fun setThumbnail(isThumbnail: Boolean) = Unit
    override fun isThumbnail(): Boolean = false
    override fun getViewBackground(): Any = getColor(R.color.gray_bg)
    override fun setIgnoreOriginalSize(ignoreOriginalSize: Boolean) = Unit
    override fun isIgnoreOriginalSize(): Boolean = false
    override fun getPageListViewMovingPosition(): Byte =
        if (viewModel.isHorizontal) 0 else 1

    override fun dispose() {
        if (readerDisposed) return
        readerDisposed = true
        pendingPage = null
        binding.pdfRead.removeAllViews()
        pdfView = null
        mainControl?.setOpenFileListener(null)
        mainControl?.dispose()
        mainControl = null
    }

    override fun onDestroy() {
        loadingDialog.dismiss()
        dispose()
        super.onDestroy()
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

