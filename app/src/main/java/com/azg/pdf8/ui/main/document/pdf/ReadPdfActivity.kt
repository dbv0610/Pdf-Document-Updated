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
            autoAdjustScrollViewHeight()
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
            binding.lnToolFullScreen.gone()
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
        }
        viewModel.pagesState.observe(this) { pages ->
            adapter.submitList(pages)
        }
    }

//    private fun searchAndHighlightInPdf(pdfView: PDFView, searchQuery: String) {
//        try {
//            // 1) Load tài liệu trực tiếp từ Uri/File đã được pdfView mở
//            val uri = pdfView.
//                ?: throw IllegalArgumentException("Không lấy được Uri từ pdfView")
//            val descriptor = contentResolver.openFileDescriptor(uri, "r")
//                ?: throw IllegalArgumentException("Không mở được file descriptor")
//            val document = PDDocument.load(descriptor.fileDescriptor)
//
//            // 2) Chuẩn bị lấy TextPosition chỉ trên trang hiện tại
//            val stripper = object : PDFTextStripper() {
//                val pagePositions = mutableListOf<TextPosition>()
//                override fun writeString(text: String, textPositions: List<TextPosition>) {
//                    pagePositions += textPositions
//                    super.writeString(text, textPositions)
//                }
//            }
//            stripper.sortByPosition = true
//            // startPage/endPage là 1‐based index của PDFBox, trong khi pdfView.currentPage là 0‐based
//            val page = pdfView.currentPage + 1
//            stripper.startPage = page
//            stripper.endPage   = page
//            stripper.getText(document)
//
//            val positions = stripper.pagePositions
//            val query = searchQuery.trim()
//            val qLen = query.length
//
//            val highlightRects = mutableListOf<RectF>()
//            // 3) Sliding window trên từng vị trí
//            for (i in 0..positions.size - qLen) {
//                // nối qLen ký tự từ i
//                val sb = StringBuilder()
//                for (j in 0 until qLen) {
//                    sb.append(positions[i + j].unicode)
//                }
//                if (sb.toString().equals(query, ignoreCase = true)) {
//                    // tính bounding box của đúng những ký tự này
//                    var minX = Float.MAX_VALUE
//                    var minY = Float.MAX_VALUE
//                    var maxX = Float.MIN_VALUE
//                    var maxY = Float.MIN_VALUE
//
//                    for (j in 0 until qLen) {
//                        val pos = positions[i + j]
//                        minX = min(minX, pos.x)
//                        minY = min(minY, pos.y)
//                        maxX = max(maxX, pos.x + pos.width)
//                        maxY = max(maxY, pos.y + pos.height)
//                    }
//
//                    val pdfRect = RectF(minX, minY, maxX, maxY)
//                    highlightRects += convertPdfRectToViewRect(pdfView, pdfRect)
//                }
//            }
//
//            document.close()
//            descriptor.close()
//
//            if (highlightRects.isNotEmpty()) {
//                showHighlights(pdfView, highlightRects)
//            } else {
//                Toast.makeText(this, "Không tìm thấy “$searchQuery” trên trang này", Toast.LENGTH_SHORT).show()
//            }
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Lỗi khi tìm kiếm trên PDF: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun convertPdfRectToViewRect(pdfView: PDFView, pdfRect: RectF): RectF {
        // 1) Lấy tỷ lệ zoom và offset hiện tại
        val zoom = pdfView.zoom
        val offsetX = pdfView.currentXOffset
        val offsetY = pdfView.currentYOffset

        // 2) PDFBox trả về hệ tọa độ ở bottom‐left, trong khi Android gốc bắt đầu từ top‐left
        val pageHeight = pdfView.getPageSize(pdfView.currentPage).height
        // nghĩa là y_android = (pageHeight - y_pdf) * zoom + offsetY

        return RectF(
            offsetX + pdfRect.left   * zoom,
            offsetY + (pageHeight - pdfRect.top)    * zoom,
            offsetX + pdfRect.right  * zoom,
            offsetY + (pageHeight - pdfRect.bottom) * zoom
        )
    }

    private fun showHighlights(pdfView: PDFView, highlightRects: List<RectF>) {
        val parent = pdfView.parent as ViewGroup
        // gỡ bỏ view highlight cũ nếu có
        parent.findViewWithTag<View>("highlightView")?.let { parent.removeView(it) }

        PdfHighlightView(this).apply {
            tag = "highlightView"
            setHighlightRects(highlightRects)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }.also { parent.addView(it) }
    }
    override fun ActivityReadPdfBinding.setData() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.favorite.collect {
                binding.btnFavorite.setImageResource(if (it) R.drawable.ic_favorite_select else R.drawable.ic_favorite_no)
            }
        }
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
        }

        lifecycleScope.launch(Dispatchers.Main) {
        }

        lifecycleScope.launch(Dispatchers.Main) {
        }

        lifecycleScope.launch(Dispatchers.Main) {
        }
    }

    override fun ActivityReadPdfBinding.onClick() {
        binding.btnMode.setOnClickListener {
            viewModel.setNightMode(!viewModel.nightMode)
            loadPdf()
        }
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

        binding.btnCopy.setOnClickListener {
            binding.layoutLoading.visible()
            binding.txtContentLoading.text = getString(R.string.copying_)
            viewModel.pdfPath?.let {
                viewModel.getTextCopy(it)
            }
        }

        icOpenTools.click {
            popupHerper.show(icOpenTools)
        }

        binding.btnGoToPage.setOnClickListener {
//            DialogHelper.showGoToPageDialog(
//                supportFragmentManager,
//                binding.txtNumberPage.text.toString()
//            ) { page ->
//                val pageNumber = page.minus(1)
//                if (pageNumber in 0 until binding.pdfRead.pageCount) {
//                    viewModel.setPage(page)
//                    jumpToPageWhenReady(pageNumber)
//                } else {
//                    toastShort(getString(R.string.invalid_page_number))
//                }
//            }
        }

        binding.btnEdit.setOnClickListener {
        }

        binding.btnLink.setOnClickListener {
            viewModel.pdfPath?.let {
                binding.layoutLoading.visible()

                viewModel.getLinks(it)
            }
        }

        binding.btnSearch.setOnClickListener {
            binding.lnSearch.visible()
            binding.lnTool.gone()
            binding.ctHeader.gone()
        }



        binding.btnBackSearch.setOnClickListener {
            binding.lnSearch.gone()
            hideAndShowToolbar()
            binding.ctHeader.visible()
            binding.edtSearch.setText("")
        }

        binding.btnBack.setOnClickListener {
//            showInterFileClick(this) {
//                finish()
//            }
        }



        binding.edtSearch.onActionSearch(actionSuccess = { query ->
            viewModel.pdfPath?.let { path ->
                binding.layoutLoading.visible()
                binding.txtContentLoading.text = getString(R.string.searching)
                viewModel.getSearchQuery(path, query)
            }
        }, actionFail = {
            toastShort(getString(R.string.please_enter_a_search))
        })

        binding.btnFavorite.setOnClickListener {
            viewModel.insertFavoriteOrUnFavorite(viewModel.pdfPath)
        }

        binding.btnFullScreen.setOnClickListener {
            binding.lnToolFullScreen.visible()
            binding.viewHeader.visible()
            binding.lnTool.gone()
            binding.ctHeader.gone()
            viewModel.setStateFullScreen(true)
        }

        binding.btnHideTools.setOnClickListener {
            binding.imgIconShowTool.setImageResource(if (viewModel.expend) R.drawable.ic_hide_tools else R.drawable.ic_hide_tools)
            viewModel.setExpand(!viewModel.expend)
            expand()
        }



        binding.btnRotate.setOnClickListener {
            val currentOrientation = resources.configuration.orientation
            requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_PORTRAIT)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        binding.btnBrightness.setOnClickListener {
            viewModel.setNightMode(!viewModel.nightMode)
            loadPdf()
        }

        binding.btnAutoScroll.setOnClickListener {
            if (binding.ctAutoScroll.isVisible) {
                binding.ctAutoScroll.gone()
            } else binding.ctAutoScroll.visible()
        }

        binding.btnHzVtLock.setOnClickListener {
            viewModel.setIsHorizontal(!viewModel.isHorizontal)
            loadPdf()
        }

        binding.btnZoomLock.setOnClickListener {
            viewModel.setZoomLock(!viewModel.zoomLock)
            setZoomLock()
        }

        binding.btnScreenShort.setOnClickListener {
            binding.layoutLoading.visible()
            // viewModel.saveToDownloadScreenShort(bitmap)
        }

        binding.btnPlayPause.setOnClickListener {
        }

        binding.btnChangeDirection.setOnClickListener {
        }

        binding.btnPlus.setOnClickListener {
        }

        binding.btnMinus.setOnClickListener {
        }
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

    private fun autoAdjustScrollViewHeight() {
        val scrollView = binding.lnToolFullScreen
        val innerLayout = binding.lnInnerTools
        scrollView.post {
            val availableHeight = binding.root.height - binding.pdfRead.top
            val contentHeight = innerLayout.measuredHeight
            val layoutParams = scrollView.layoutParams as ConstraintLayout.LayoutParams
            if (contentHeight > availableHeight) {
                layoutParams.height = 0
                layoutParams.topToTop = binding.pdfRead.id
                layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            } else {
                layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                layoutParams.topToTop = binding.pdfRead.id
                layoutParams.bottomToBottom =
                    ConstraintLayout.LayoutParams.UNSET
            }

            scrollView.layoutParams = layoutParams
        }
    }

    fun toggleCardExpand(context: Context, card: CardView, expand: Boolean) {
        val targetWidth = if (expand) {
            context.dimenSdp(140).toInt()
        } else {
            card.height
        }
        val anim = ValueAnimator.ofInt(card.width, targetWidth)
        anim.addUpdateListener {
            val params = card.layoutParams
            params.width = it.animatedValue as Int
            card.layoutParams = params
        }
        anim.duration = 200
        anim.start()
    }

    private fun expand() {
        toggleCardExpand(this, binding.btnExit, viewModel.expend)
        toggleCardExpand(this, binding.btnRotate, viewModel.expend)
        toggleCardExpand(this, binding.btnBrightness, viewModel.expend)
        toggleCardExpand(this, binding.btnAutoScroll, viewModel.expend)
        toggleCardExpand(this, binding.btnHzVtLock, viewModel.expend)
        toggleCardExpand(this, binding.btnZoomLock, viewModel.expend)
        toggleCardExpand(this, binding.btnScreenShort, viewModel.expend)
        toggleCardExpand(this, binding.btnHideTools, viewModel.expend)
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

    private fun setZoomLock() {
        if (viewModel.zoomLock) {
            binding.pdfRead.minZoom = 1f
            binding.pdfRead.maxZoom = 1f
        } else {
            binding.pdfRead.minZoom = 1f
            binding.pdfRead.maxZoom = 6f
        }
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

    private fun printPdf(file: File) {
        newContext?.let {
            val printManager = it.getSystemService(PRINT_SERVICE) as PrintManager
            val adapter = PdfDocumentAdapter(file)
            printManager.print(CONTENT_PRINT, adapter, null)
        }
    }

    private fun hideAndShowToolbar() {
        binding.lnTool.visibility = if (viewModel.isShowToolbar) View.VISIBLE else View.GONE
    }

    private fun exitFullScreen() {
        binding.viewHeader.gone()
        binding.lnToolFullScreen.gone()
        binding.lnTool.visible()
        binding.ctHeader.visible()
        binding.ctAutoScroll.gone()
        viewModel.setStateFullScreen(false)
        val currentOrientation = resources.configuration.orientation
        if (currentOrientation != Configuration.ORIENTATION_PORTRAIT)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun backPressed() {
        if (this@ReadPdfActivity.isFinishing || this@ReadPdfActivity.isDestroyed) {
            this@ReadPdfActivity.finishAffinity()
            return
        }
        if (binding.lnSearch.isVisible) {
            binding.lnSearch.gone()
            hideAndShowToolbar()
            binding.ctHeader.visible()
            binding.edtSearch.setText("")
            return
        } else if (binding.lnToolFullScreen.isVisible) {
            exitFullScreen()
            return
        }
        finish()
    }
}

