package com.azg.pdf8.ui.main.document.pdf

import com.azg.pdf8.dialog.DocumentPasswordDialog
import com.reader.pdfviewer.pdfium.PdfPasswordException
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.os.Bundle
import android.util.Log
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.azg.pdf8.R
import com.azg.pdf8.adapter.PdfPreviewAdapter
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityReadPdfBinding
import com.azg.pdf8.databinding.PopupMoreActionBinding
import com.azg.pdf8.dialog.DialogProcess
import com.azg.pdf8.model.ContentWithPage
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.utils.Constant.ARG_MEDIA_MODEL
import com.azg.pdf8.utils.Constant.ARG_SEARCH_RESULT_PAGE
import com.azg.pdf8.utils.Constant.ARG_SEARCH_WITH_PAGE
import com.dong.baselib.api.parcelable
import com.dong.baselib.base.PopupHelper
import com.dong.baselib.base.SystemUtil
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.navigationBarHeight
import com.dong.baselib.widget.paddingRight
import com.dong.baselib.widget.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class ReadPdfActivity : BaseActivity<ActivityReadPdfBinding>(ActivityReadPdfBinding::inflate) {
    private val viewModel: ReadPdfViewModel by viewModel()
    private var pdfLoaded = false
    private var pendingPage: Int? = null
    private var readerDisposed = false
    private var lastSearchQuery: String? = null
    private val searchResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val page = result.data?.getIntExtra(ARG_SEARCH_RESULT_PAGE, -1) ?: -1
            val query = lastSearchQuery
            if (result.resultCode != Activity.RESULT_OK || page < 1 || query == null) return@registerForActivityResult
            // Search results are 1-based page numbers
            pendingSearchPage = page - 1
            showPendingSearchResult()
        }
    private var pendingSearchPage: Int? = null

    private fun showPendingSearchResult() {
        val page = pendingSearchPage ?: return
        val query = lastSearchQuery ?: return
        if (!pdfLoaded || readerDisposed) return
        pendingSearchPage = null
        binding.pdfRead.jumpToSearchResult(page, query)
    }
    private val mediaModel by lazy {
        runCatching {
            intent.parcelable<RecentDocument>(ARG_MEDIA_MODEL)
        }
    }
    val popupHerper = PopupHelper.with(this@ReadPdfActivity, PopupMoreActionBinding::inflate)
    private var adapter: PdfPreviewAdapter =
        PdfPreviewAdapter(
            thumbnailScope = lifecycleScope,
            loadThumbnail = { viewModel.thumbnail(it) }
        ) {
            jumpToPageWhenReady(it.index)
        }.attachLifecycle(this@ReadPdfActivity)

    override fun initialize() {
        mediaModel.getOrNull()?.let {
            binding.fileName.text = it.path.fileName()
            viewModel.setMediaModel(it)
            if (!loadPdf()) return
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
            binding.lnDraw.isVisible = pdfLoaded && !this@ReadPdfActivity.binding.pdfRead.isDrawingMode
            binding.lnDraw.click {
                popup.dismiss()
                setInkMode(true)
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

    private val loadingDialog by lazy {
        DialogProcess(this@ReadPdfActivity)
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
        setupInkToolbar()
        icBackApp.click {
            backPressed()
        }

        icBackSearch.click {
            edtSearchData.setText("")
            clearSearchHighlight()
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
                startSearch(query)
            } else {
                toastShort(getString(R.string.please_enter_a_search))
            }
        }

        icOpenTools.click {
            popupHerper.show(icOpenTools)
        }

        binding.edtSearchData.onActionSearch(actionSuccess = { query ->
            startSearch(query)
        }, actionFail = {
            toastShort(getString(R.string.please_enter_a_search))
        })
    }

    private fun startSearch(query: String) {
        if (!pdfLoaded || readerDisposed) return
        loadingDialog.show()
        lastSearchQuery = query
        lifecycleScope.launch {
            val results = runCatching { binding.pdfRead.searchDocument(query) }
                .onFailure { Log.e("ReadPdfActivity", "Search failed", it) }
            loadingDialog.dismiss()
            val matches = results.getOrElse {
                toastShort(getString(R.string.search_error_occurred))
                return@launch
            }
            if (matches.isEmpty()) {
                toastShort(getString(R.string.not_found_search))
                return@launch
            }
            binding.pdfRead.setSearchQuery(query)
            // Search results are 1-based page numbers
            val data = matches.map { ContentWithPage(it.page + 1, it.content) }
            val intent = Intent(this@ReadPdfActivity, SearchResultActivity::class.java)
            intent.putExtra(ARG_SEARCH_WITH_PAGE, ArrayList(data))
            searchResultLauncher.launch(intent)
        }
    }

    private fun clearSearchHighlight() {
        lastSearchQuery = null
        binding.pdfRead.clearSearch()
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
        savedInstanceState?.let {
            viewModel.setPage(it.getInt("pdf_page", 1))
            lastSearchQuery = it.getString("pdf_search_query")
        }
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("pdf_page", viewModel.page)
        outState.putString("pdf_search_query", lastSearchQuery)
        super.onSaveInstanceState(outState)
    }

    private fun jumpToPageWhenReady(page: Int) {
        pendingPage = page
        if (!pdfLoaded || readerDisposed) return
        val count = binding.pdfRead.pageCount
        if (count <= 0) return
        pendingPage = null
        binding.pdfRead.jumpTo(page.coerceIn(0, count - 1), true)
    }

    private var pdfPassword: String? = null
    private var passwordDialog: DocumentPasswordDialog? = null

    private fun isPasswordError(error: Throwable?): Boolean =
        generateSequence(error) { it.cause }.take(8).any { it is PdfPasswordException }

    /** Pdfium reports "required" and "incorrect" the same way; tell them apart by whether we sent one. */
    private fun askPdfPassword() {
        if (isFinishing || readerDisposed || passwordDialog?.isShowing == true) return
        loadingDialog.dismiss()
        var submitted = false
        val message = getString(if (pdfPassword == null) R.string.error_file_password else R.string.error_file_password_incorrect)
        passwordDialog = DocumentPasswordDialog(this, message) { password ->
            submitted = true
            pdfPassword = password
            passwordDialog?.dismiss()
            loadPdf()
        }.apply {
            setOnDismissListener {
                passwordDialog = null
                if (!submitted && !isFinishing) finish()
            }
            show()
        }
    }

    private fun loadPdf(): Boolean {
        val file = viewModel.pdfPath?.let(::File)
        Log.d("OPEN_TRACE", "pdf.open path=${file?.absolutePath} size=${file?.length() ?: 0}")
        if (file == null || !file.isFile || !file.canRead()) {
            showReadError()
            return false
        }
        binding.pdfRead.fromFile(file)
            .password(pdfPassword)
            .enableAnnotationRendering(true)
            .defaultPage(viewModel.page - 1)
            .pageSeparatorSpacing(8)
            .pageFling(true)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(false)
            .nightMode(viewModel.nightMode)
            .enableTextSelection(true)
            .enableMagnifier(true)
            .enableTextMarkup(true)
            .setInkColor(INK_COLORS[selectedInkColor])
            .setInkWidth(INK_WIDTHS[selectedInkWidth])
            .onInkChange { _, _ -> updateInkButtons() }
            .selectionHandleColor(ContextCompat.getColor(this, R.color.mainColor))
            .selectionHighlightColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, R.color.mainColor), 0x40))
            .searchHighlightColor(SEARCH_HIGHLIGHT_COLOR)
            .searchHighlightCornerRadius(resources.displayMetrics.density * 2)
            .onLoad { pageCount ->
                if (!readerDisposed && !isFinishing) {
                    Log.d("OPEN_TRACE", "pdf.loaded pages=$pageCount")
                    pdfLoaded = true
                    binding.txtNumberPage.text = "${viewModel.page}/$pageCount"
                    pendingPage?.let { jumpToPageWhenReady(it) }
                    showPendingSearchResult()
                    val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
                    viewModel.renderPdf(applicationContext, uri)
                }
            }
            .onPageChange { page, pageCount ->
                if (!readerDisposed && !isFinishing) {
                    viewModel.setPage(page + 1)
                    adapter.setCurrentPage(page)
                    binding.txtNumberPage.text = "${page + 1}/$pageCount"
                }
            }
            .onError { error ->
                Log.d("OPEN_TRACE", "pdf.error $error", error)
                if (isPasswordError(error)) askPdfPassword() else showReadError()
            }
            .load()
        return true
    }

    /**
     * Write the underlines/strikethroughs added since the last save into the PDF file.
     * Does nothing when there is no unsaved markup.
     *
     * @param onResult called on the main thread with true if the file was written
     */
    fun saveMarkups(onResult: (Boolean) -> Unit = {}) {
        val pdfView = binding.pdfRead
        val file = viewModel.pdfPath?.let(::File)
        if (file == null || !pdfLoaded || readerDisposed || !pdfView.hasUnsavedChanges) {
            onResult(false)
            return
        }
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { pdfView.saveDocument(file) }
            if (readerDisposed || isFinishing) return@launch
            if (saved) {
                MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), null, null)
            } else {
                toastShort(getString(R.string.some_errors_occurred_please_try_again))
            }
            onResult(saved)
        }
    }

    private var selectedInkColor = 0
    private var selectedInkWidth = 1

    private fun setupInkToolbar() {
        val size = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
        val margin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
        binding.lnInkColors.removeAllViews()
        INK_COLORS.forEachIndexed { index, _ ->
            binding.lnInkColors.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
                setOnClickListener {
                    selectedInkColor = index
                    binding.pdfRead.setInkColor(INK_COLORS[index])
                    updateInkOptions()
                }
            })
        }
        binding.lnInkWidths.removeAllViews()
        INK_WIDTHS.forEachIndexed { index, _ ->
            binding.lnInkWidths.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginStart = margin }
                setOnClickListener {
                    selectedInkWidth = index
                    binding.pdfRead.setInkWidth(INK_WIDTHS[index])
                    updateInkOptions()
                }
            })
        }
        binding.icInkClose.click { setInkMode(false) }
        binding.icInkUndo.setOnClickListener { binding.pdfRead.undoInk() }
        binding.icInkRedo.setOnClickListener { binding.pdfRead.redoInk() }
        binding.icInkSave.setOnClickListener {
            binding.icInkSave.isEnabled = false
            saveMarkups { saved ->
                if (saved) toastShort(getString(R.string.saved_successfully))
                updateInkButtons()
            }
        }
        updateInkOptions()
    }

    private fun setInkMode(enabled: Boolean) {
        if (!pdfLoaded || readerDisposed) return
        binding.pdfRead.setDrawingMode(enabled)
        binding.lnInkToolbar.isVisible = enabled
        binding.txtNumberPage.isVisible = !enabled
        if (enabled) {
            binding.pdfRead.setInkColor(INK_COLORS[selectedInkColor])
            binding.pdfRead.setInkWidth(INK_WIDTHS[selectedInkWidth])
        }
        updateInkButtons()
    }

    /**
     * Swatches show the selected color and width with a ring in the app main color
     */
    private fun updateInkOptions() {
        val ring = ContextCompat.getColor(this, R.color.mainColor)
        val ringWidth = (resources.displayMetrics.density * 2).toInt()
        for (index in INK_COLORS.indices) {
            val view = binding.lnInkColors.getChildAt(index) ?: continue
            view.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(INK_COLORS[index], 0xFF))
                setStroke(ringWidth, if (index == selectedInkColor) ring else Color.LTGRAY)
            }
        }
        val dotColor = ContextCompat.getColor(this, R.color.black)
        for (index in INK_WIDTHS.indices) {
            val view = binding.lnInkWidths.getChildAt(index) ?: continue
            // A dot growing with the width, inside a ring when selected
            val inset = ((3 - index) * resources.displayMetrics.density * 2.5f).toInt()
            val dot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(dotColor)
            }
            val ringDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(ringWidth, if (index == selectedInkWidth) ring else Color.TRANSPARENT)
            }
            view.background = LayerDrawable(arrayOf(ringDrawable, InsetDrawable(dot, inset + ringWidth * 2)))
        }
    }

    private fun updateInkButtons() {
        if (readerDisposed) return
        val pdfView = binding.pdfRead
        setIconEnabled(binding.icInkUndo, pdfView.canUndoInk())
        setIconEnabled(binding.icInkRedo, pdfView.canRedoInk())
        setIconEnabled(binding.icInkSave, pdfView.hasUnsavedChanges)
    }

    private fun setIconEnabled(view: ImageView, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.3f
    }

    private fun showReadError() {
        if (isFinishing || readerDisposed) return
        loadingDialog.dismiss()
        toastShort(getString(R.string.some_errors_occurred_please_try_again))
        finish()
    }

    override fun onDestroy() {
        passwordDialog?.setOnDismissListener(null)
        passwordDialog?.dismiss()
        passwordDialog = null
        readerDisposed = true
        pdfLoaded = false
        pendingPage = null
        loadingDialog.dismiss()
        binding.pdfRead.recycle()
        super.onDestroy()
    }

    override fun backPressed() {
        if (binding.pdfRead.isDrawingMode) {
            setInkMode(false)
            return
        }
        if (binding.lnSearchData.isVisible) {
            binding.lnSearchData.gone()
            binding.lnHeaderDef.visible()
            binding.edtSearchData.setText("")
            clearSearchHighlight()
            return
        }
        lifecycleScope.launch {
            MainActivity.isShowRateFirstView.emit(true)
        }
        finish()
    }

    companion object {
        private const val SEARCH_HIGHLIGHT_COLOR = 0x80FFD600.toInt()

        // Black, red, blue, green and a translucent yellow highlighter
        private val INK_COLORS = intArrayOf(
            0xFF000000.toInt(), 0xFFE53935.toInt(), 0xFF1E88E5.toInt(), 0xFF43A047.toInt(), 0x80FFEB3B.toInt()
        )

        // Stroke widths in PDF points
        private val INK_WIDTHS = floatArrayOf(1.5f, 3f, 6f)
    }
}
