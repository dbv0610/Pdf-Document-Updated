package com.azg.pdf8.ui.main.document.other

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.azg.pdf8.R
import com.azg.pdf8.adapter.PdfPreviewAdapter
import com.azg.pdf8.app.isInternetAvailable
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.base.BaseActivity
import com.azg.pdf8.databinding.ActivityReadFileBinding
import com.azg.pdf8.databinding.PopupMoreActionBinding
import com.azg.pdf8.dialog.DialogProcess
import com.azg.pdf8.dialog.DocumentPasswordDialog
import com.azg.pdf8.dialog.OpenFileErrorDialog
import com.azg.pdf8.dialog.PdfNameDialog
import com.azg.pdf8.model.DocumentPage
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.ui.main.MainActivity
import com.azg.pdf8.ui.main.document.pdf.PageViewType
import com.azg.pdf8.ui.main.document.pdf.ReadPdfActivity
import com.azg.pdf8.utils.Constant
import com.azg.pdf8.utils.Constant.ARG_MEDIA_MODEL
import com.azg.pdf8.viewmodel.DocumentViewModel
import com.azg.pdf8.viewmodel.ConvertViewModel
import com.azg.pdf8.viewmodel.StateLoadData
import com.azg.pdf8.widget.docColor
import com.azg.pdf8.widget.pptColor
import com.azg.pdf8.widget.txtColor
import com.azg.pdf8.widget.xlsColor
import com.dong.baselib.api.parcelable
import com.dong.baselib.base.PopupHelper
import com.dong.baselib.lifecycle.combine
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.click
import com.dong.baselib.widget.gone
import com.dong.baselib.widget.gray
import com.dong.baselib.widget.navigationBarHeight
import com.dong.baselib.widget.paddingRight
import com.dong.baselib.widget.visible
import com.wxiwei.office.constant.EventConstant
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.constant.wp.WPViewConstant
import com.wxiwei.office.officereader.AppFrame
import com.wxiwei.office.pg.control.Presentation
import com.wxiwei.office.res.ResKit
import com.wxiwei.office.ss.control.ExcelView
import com.wxiwei.office.ss.view.SheetView
import com.wxiwei.office.system.IMainFrame
import com.wxiwei.office.system.MainControl
import com.wxiwei.office.system.OpenTrace
import com.wxiwei.office.system.DocumentPasswords
import com.wxiwei.office.system.OpenFileException
import com.wxiwei.office.system.OnOpenFileListener
import com.wxiwei.office.system.view
import com.wxiwei.office.system.find
import com.wxiwei.office.system.beans.CalloutView.drawingMode
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener
import com.wxiwei.office.wp.control.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import com.wxiwei.office.system.search.DocumentSearch
import org.koin.android.ext.android.inject
import java.io.File

class ReadDocumentActivity :
    BaseActivity<ActivityReadFileBinding>(ActivityReadFileBinding::inflate),
    IMainFrame, OnOpenFileListener {
    private var openErrorDialog: OpenFileErrorDialog? = null
    private var passwordDialog: DocumentPasswordDialog? = null

    override fun onOpenFileSuccess() {
        mainControl?.jumpToPage(0)
    }

    override fun onOpenFileFailure(error: OpenFileException): Boolean {
        OpenTrace.e("host open failed reason=${error.reason} path=${error.filePath}", error)
        if (isFinishing || isDestroyed) return true
        processDialog.dismiss()
        mainControl?.dismissProgressDialog()
        if (openErrorDialog?.isShowing == true || passwordDialog?.isShowing == true) return true
        if (showPasswordDialog(error)) return true
        val message = openErrorMessage(error)
        openErrorDialog = OpenFileErrorDialog(this, getString(message)).apply {
            setOnDismissListener {
                openErrorDialog = null
                if (!isFinishing && !isDestroyed) finish()
            }
            show()
        }
        return true
    }

    /** Encrypted .docx/.xlsx/.pptx and .xls can be decrypted; legacy .doc/.ppt encryption cannot. */
    private fun showPasswordDialog(error: OpenFileException): Boolean {
        val incorrect = error.reason == OpenFileException.Reason.PASSWORD_INCORRECT
        if (!incorrect && error.reason != OpenFileException.Reason.PASSWORD_REQUIRED) return false
        val path = dataModel.getOrNull()?.path ?: return false
        if (File(path).extension.lowercase() !in PASSWORD_EXTENSIONS) return false
        var submitted = false
        val message = getString(if (incorrect) R.string.error_file_password_incorrect else R.string.error_file_password)
        passwordDialog = DocumentPasswordDialog(this, message) { password ->
            submitted = true
            DocumentPasswords.set(path, password)
            passwordDialog?.dismiss()
            // reopen from scratch; the reader picks the password up from DocumentPasswords
            recreate()
        }.apply {
            setOnDismissListener {
                passwordDialog = null
                if (!submitted && !isFinishing && !isDestroyed) finish()
            }
            show()
        }
        return true
    }

    private fun openErrorMessage(error: OpenFileException): Int = when (error.reason) {
        OpenFileException.Reason.BAD_FILE -> R.string.error_file_damaged
        OpenFileException.Reason.RTF_DOCUMENT -> R.string.error_file_rtf
        OpenFileException.Reason.OLD_DOCUMENT -> R.string.error_file_old_format
        OpenFileException.Reason.PASSWORD_REQUIRED -> R.string.error_file_password
        OpenFileException.Reason.PASSWORD_INCORRECT -> R.string.error_file_password_incorrect
        OpenFileException.Reason.OUT_OF_MEMORY -> R.string.error_file_too_large_memory
        OpenFileException.Reason.FILE_NOT_FOUND -> R.string.error_file_not_found
        OpenFileException.Reason.STORAGE -> R.string.error_file_storage
        OpenFileException.Reason.UNKNOWN -> R.string.error_file_open_generic
    }

    override fun onDestroy() {
        passwordDialog?.setOnDismissListener(null)
        passwordDialog?.dismiss()
        passwordDialog = null
        if (isFinishing) {
            DocumentPasswords.clear(dataModel.getOrNull()?.path)
            // plaintext copies of decrypted documents
            File(cacheDir, "decrypted").deleteRecursively()
        }
        openErrorDialog?.setOnDismissListener(null)
        openErrorDialog?.dismiss()
        openErrorDialog = null
        processDialog.dismiss()
        dispose()
        super.onDestroy()
    }
    override fun backPressed() {
        lifecycleScope.launch {
            MainActivity.isShowRateFirstView.emit(true)
        }
        finish()
    }

    companion object {
        private val PASSWORD_EXTENSIONS = setOf(
            "docx", "dotx", "dotm", "xlsx", "xltx", "xltm", "xlsm", "xls", "xlt",
            "pptx", "pptm", "potx", "potm"
        )
        val listDataSlideShow = MutableStateFlow(mutableListOf<DocumentPage>())
    }

    data class SearchUiState(
        val countText: String,
        val prevEnabled: Boolean,
        val nextEnabled: Boolean,
        val prevColor: Int,
        val nextColor: Int
    )

    val viewModel: DocumentViewModel by inject()
    private val dataModel by lazy {
        runCatching {
            intent.parcelable<RecentDocument>(ARG_MEDIA_MODEL)
        }
    }
    private val processDialog by lazy {
        DialogProcess(this@ReadDocumentActivity)
    }
    val popupHerper = PopupHelper.with(this@ReadDocumentActivity, PopupMoreActionBinding::inflate)
    private var mainControl: MainControl? = null
    private var appFrame: AppFrame? = null

    override fun initialize() {
        documentType = when (getData<String>(Constant.DOCUMENT_TYPE).toString()) {
            Constant.Doc -> DocumentType.Doc
            Constant.Xls -> DocumentType.Excel
            Constant.Txt -> DocumentType.Txt
            else -> DocumentType.Ppt
        }
        val color = when (documentType) {
            DocumentType.Doc -> docColor
            DocumentType.Excel -> xlsColor
            DocumentType.Txt -> txtColor
            else -> pptColor
        }
        binding.root.setBackgroundColor(color)
        binding.lnHeader.setBackgroundColor(color)
        binding.fileName.text = getString(
            when (documentType) {
                DocumentType.Doc -> R.string.doc
                DocumentType.Excel -> R.string.xls
                DocumentType.Txt -> R.string.txt
                else -> R.string.pptx
            }
        )
        listDataSlideShow.value = mutableListOf()
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

    private var canNextView = false
    private var canPrevView = false
    var documentType = DocumentType.Doc
    override fun ActivityReadFileBinding.setData() {
        dataModel.getOrNull()?.let {
            initReader(it.path, it.type)
            fileName.text = it.path.fileName()
            if (it.type == DocumentType.Excel) {
                txtNumberPage.gone()
            }
        }
        setupSearchCombiner()

        lifecycleScope.launch {
            viewModel.pageViewState
                .map { it == PageViewType.Thumbnail }
                .distinctUntilChanged()
                .collect { show ->
                    TransitionManager.beginDelayedTransition(officeViewer, AutoTransition())
                    rcvFrameData.isVisible = show
                }
        }


        lifecycleScope.launch {
            slideViewModel.uiState.collect {
                progressLoad.isVisible =
                    viewModel.pageViewState.value == PageViewType.Thumbnail && it == StateLoadData.Loading
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

            dataModel.getOrNull()?.let {
                if (it.type == DocumentType.Excel) {
                    binding.lnPageByPage.gone()
                    binding.lnThumbNail.gone()
                } else if (it.type == DocumentType.Ppt) {
                    binding.lnSlideShow.visible()
                }
            }
            binding.lnToPdf.setOnClickListener {
                popup.dismiss()
                dataModel.getOrNull()?.let { doc ->
                    PdfNameDialog(this@ReadDocumentActivity) { newName ->
                        slideViewModel.convertToPdf(
                            newName,
                            model = doc,
                            excelSheetView = (mainControl?.view as? ExcelView)?.getSheetView(),
                            onNameExit = {
                                toastShort(getString(R.string.file_have_exit_file))
                            },
                            onStart = { processDialog.show() },
                            onFinish = { file ->
                                runOnUiThread {
                                    toastShort(getString(R.string.pdf_file_was_create))
                                    processDialog.dismiss()
                                    val document = RecentDocument(
                                        mediaId = file.hashCode().toLong(),
                                        path = file.absolutePath,
                                        lastModified = System.currentTimeMillis(),
                                        lastTimeView = System.currentTimeMillis(),
                                        size = file.length(),
                                        type = DocumentType.Pdf
                                    )
                                    viewModel.addToRecent(document.apply {
                                        lastTimeView = System.currentTimeMillis()
                                    })
                                      launchActivity<ReadPdfActivity>(
                                            hashMapOf(
                                                Constant.ARG_MEDIA_MODEL to document,
                                                Constant.RateWhenCreate to true
                                            )
                                        )

                                    finish()
                                }

                                processDialog.dismiss()
                            },
                            onError = { error ->
                                // The document is already open: report the reason but keep the reader
                                processDialog.dismiss()
                                toastShort(getString(openErrorMessage(error)))
                            }
                        )
                    }.showWith(getString(R.string.convert_to_pdf))
                } ?: run {
                    toastShort(getString(R.string.error_create_pdf_file))
                }
            }
            binding.lnSlideShow.click {
                popup.dismiss()
                val listSlide = slideViewModel.listSlide.value
                if (listSlide.isNotEmpty()) {
                    listDataSlideShow.value = listSlide.toMutableList()
                    launchActivity<SlideShowActivity>()
                } else {
                    toastShort(getString(R.string.can_slide_show_now))
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
                requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
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

    private fun setupSearchCombiner() {
        combine(
            lifecycleOwner = this@ReadDocumentActivity,
            liveData1 = searchCount,
            liveData2 = currentIndexSearch
        ) { count, index ->
            val safeCount = count ?: 0
            val safeIndex = index ?: 0

            SearchUiState(
                countText = if (safeCount == 0) {
                    getString(R.string.no_result_found)
                } else {
                    // safeIndex is 0-based
                    getString(R.string.result) + " ${safeIndex + 1}/${safeCount}"
                },
                prevEnabled = safeCount > 0 && safeIndex > 0,
                nextEnabled = safeCount > 0 && safeIndex < safeCount - 1,
                prevColor = if (safeCount == 0 || safeIndex <= 0) gray else Color.TRANSPARENT,
                nextColor = if (safeCount == 0 || safeIndex >= safeCount - 1) gray else Color.TRANSPARENT
            )
        }.observe(this) { state ->
            binding.tvCountSearch.text = state.countText
            binding.icViewPrev.setColorFilter(state.prevColor)
            binding.icViewNext.setColorFilter(state.nextColor)
            canPrevView = state.prevEnabled
            canNextView = state.nextEnabled
        }
    }

    private var search: DocumentSearch? = null
    val searchCount = MutableLiveData(0)
    val currentIndexSearch = MutableLiveData(0)
    private var adapter: PdfPreviewAdapter =
        PdfPreviewAdapter() {
            try {
                mainControl?.jumpToPage(it.index)
                binding.txtNumberPage.text = "${it.index + 1}/${mainControl?.getPageCount()}"
            } catch (e: Exception) {
                toastShort(getString(R.string.error_go_to_page))
            }
        }.attachLifecycle(this@ReadDocumentActivity)
    private var searchJob: Job? = null

    /** Search off the main thread, then focus the first result. */
    fun performSearch(keyword: String) {
        searchJob?.cancel()
        clearSearchResults()
        val s = DocumentSearch.of(mainControl?.view) ?: return
        processDialog.show()
        searchJob = lifecycleScope.launch {
            val count = try {
                withContext(Dispatchers.Default) { s.search(keyword) { isActive } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d("ErrorNavigate", "Search error: ${e.message}")
                toastShort(getString(R.string.search_error_occurred))
                0
            } finally {
                if (searchJob === coroutineContext[Job]) processDialog.dismiss()
            }
            if (isFinishing || isDestroyed) return@launch
            search = s
            searchCount.value = count
            focusSearchResult(0)
            binding.lnResultSearch.visible()
        }
    }

    /** Show and highlight the result at [index] for every document type. */
    private fun focusSearchResult(index: Int) {
        val count = searchCount.value ?: 0
        if (index !in 0 until count) {
            currentIndexSearch.value = 0
            return
        }
        try {
            search?.focus(index)
        } catch (e: Exception) {
            Log.d("ErrorNavigate", "Focus error: ${e.message}")
        }
        currentIndexSearch.value = index
    }

    private fun clearSearchResults() {
        search?.clear()
        search = null
        searchCount.value = 0
        currentIndexSearch.value = 0
    }

    fun goToNext() {
        val index = currentIndexSearch.value ?: 0
        if (index < (searchCount.value ?: 0) - 1) focusSearchResult(index + 1)
    }

    fun goToPrev() {
        val index = currentIndexSearch.value ?: 0
        if (index > 0) focusSearchResult(index - 1)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                Log.d("ErrorNavigate", "Action Cancel")
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    val slideViewModel: ConvertViewModel by inject()

    override fun ActivityReadFileBinding.onClick() {
        icBackApp.click {
            backPressed()
        }
        icBackSearch.click {
            lnDefault.visible()
            lnSearch.gone()
            lnResultSearch.gone()
            edtSearch.setText("")
            searchJob?.cancel()
            clearSearchResults()
        }
        icSearchApp.click {
            lnDefault.gone()
            lnSearch.visible()
        }
        icOpenTools.click {
            popupHerper.show(icOpenTools)
        }

        icSearch.setOnClickListener {
            val key = edtSearch.text.toString().trim()
            if (key.isNotEmpty()) {
                performSearch(key)
            } else toastShort(getString(R.string.please_enter_a_search))
        }

        icViewNext.setOnClickListener {
            goToNext()
        }

        icViewPrev.setOnClickListener {
            goToPrev()
        }
        edtSearch.onActionSearch({ key ->
            if (key.isNotEmpty()) {
                performSearch(key)
            } else toastShort(getString(R.string.please_enter_a_search))
        }) {
            toastShort(getString(R.string.please_enter_a_search))
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun initReader(filePath: String, type: DocumentType) {
        val start = android.os.SystemClock.uptimeMillis()
        val file = File(filePath)
        OpenTrace.mark("activity.initReader.begin type=$type path=${file.absolutePath}")
        OpenTrace.d("file clicked type=$type path=${file.absolutePath} exists=${file.exists()} isFile=${file.isFile} length=${file.length()}")
        mainControl = MainControl(this@ReadDocumentActivity)
        mainControl?.setOpenFileListener(this)
        appFrame = AppFrame(applicationContext)
        appFrame?.post {
            OpenTrace.d("starting MainControl.openFile path=${file.absolutePath}")
            mainControl?.openFile(file.absolutePath)
            OpenTrace.mark("activity.initReader.openFile.return", start)
        }
        binding.officeViewer.addView(
            appFrame,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        appFrame?.setOnTouchListener { _: View?, _: MotionEvent? -> false }
        OpenTrace.d("reader host view added class=${appFrame?.javaClass?.name} size=${appFrame?.width}x${appFrame?.height}")
        appFrame?.post {
            OpenTrace.d("reader host view laid out size=${appFrame?.width}x${appFrame?.height}")
        }
    }

    override fun getActivity(): Activity {
        return this@ReadDocumentActivity
    }

    override fun doActionEvent(actionID: Int, obj: Any?): Boolean {
        try {
            when (actionID) {
                EventConstant.SYS_RESET_TITLE_ID -> {}
                EventConstant.SYS_ONBACK_ID -> {}
                EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS -> {}
                EventConstant.SYS_HELP_ID -> {}
                EventConstant.APP_FIND_ID -> {}
                EventConstant.APP_SHARE_ID -> {}
                EventConstant.FILE_MARK_STAR_ID -> {}
                EventConstant.APP_FINDING -> {}
                EventConstant.APP_FIND_BACKWARD -> {
                    Log.d("FindAction", "Data is ${obj.toString()}")
                }
                EventConstant.APP_FIND_FORWARD -> {
                    Log.d("FindAction", "Data is ${obj.toString()}")
                }
                EventConstant.SS_CHANGE_SHEET -> {}
                EventConstant.APP_DRAW_ID -> {
                    mainControl?.getSysKit()?.calloutManager?.drawingMode =
                        MainConstant.DRAWMODE_CALLOUTDRAW
                    appFrame?.post {
                        mainControl?.actionEvent(EventConstant.APP_INIT_CALLOUTVIEW_ID, null)
                    }
                }
                EventConstant.APP_BACK_ID -> {
                    mainControl?.getSysKit()?.calloutManager?.drawingMode =
                        MainConstant.DRAWMODE_NORMAL
                }
                EventConstant.APP_PEN_ID -> if (obj as Boolean) {
                    mainControl?.getSysKit()?.calloutManager?.drawingMode =
                        MainConstant.DRAWMODE_CALLOUTDRAW
                    appFrame?.post {
                        mainControl?.actionEvent(EventConstant.APP_INIT_CALLOUTVIEW_ID, null)
                    }
                } else {
                    mainControl?.getSysKit()?.calloutManager?.drawingMode =
                        MainConstant.DRAWMODE_NORMAL
                }
                EventConstant.APP_ERASER_ID -> if (obj as Boolean) {
                    mainControl?.getSysKit()?.calloutManager?.drawingMode =
                        MainConstant.DRAWMODE_CALLOUTERASE
                } else {
                    mainControl?.getSysKit()?.calloutManager?.drawingMode =
                        MainConstant.DRAWMODE_NORMAL
                }
                EventConstant.APP_COLOR_ID -> {}
                else -> return false
            }
        } catch (e: Exception) {
            mainControl?.getSysKit()?.errorKit?.writerLog(e)
        }
        return true
    }

    override fun openFileFinish() {
        val start = android.os.SystemClock.uptimeMillis()
        OpenTrace.mark("activity.openFileFinish.begin")
        appFrame?.addView(
            mainControl?.view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        binding.rcvFrameData.apply {
            layoutManager = LinearLayoutManager(context, VERTICAL, false)
            adapter = this@ReadDocumentActivity.adapter
        }
        lifecycleScope.launch {
            slideViewModel.listSlide.collectLatest {
                adapter.submitList(it)
                binding.txtNumberPage.text =
                    "${mainControl?.getCurrentViewIndex() ?: 0}/${it.size}"
            }
        }

        OpenTrace.mark("activity.openFileFinish.end", start)
    }

    override fun updateToolsbarStatus() = Unit
    override fun setFindBackForwardState(state: Boolean) = Unit
    override fun getBottomBarHeight(): Int = 0
    override fun getTopBarHeight(): Int = 0
    override fun getAppName(): String = getString(R.string.app_name)
    override fun getTemporaryDirectory(): File? {
        val file = getExternalFilesDir(null)
        return file ?: filesDir
    }

    override fun onEventMethod(
        v: View?,
        e1: MotionEvent?,
        e2: MotionEvent?,
        xValue: Float,
        yValue: Float,
        eventMethodType: Byte
    ): Boolean = false

    override fun isDrawPageNumber(): Boolean = false
    override fun isShowZoomingMsg(): Boolean = false
    override fun isPopUpErrorDlg(): Boolean = true
    override fun isShowPasswordDlg(): Boolean = true
    override fun isShowProgressBar(): Boolean = true
    override fun isShowFindDlg(): Boolean = true
    override fun isShowTXTEncodeDlg(): Boolean = true
    override fun getTXTDefaultEncode(): String? = "GBK"
    override fun isTouchZoom(): Boolean = true
    override fun isZoomAfterLayoutForWord(): Boolean = true
    override fun getWordDefaultView(): Byte = WPViewConstant.PAGE_ROOT.toByte()
    override fun getLocalString(resName: String): String? =
        ResKit.instance().getLocalString(resName)

    override fun changeZoom() = Unit
    @SuppressLint("SetTextI18n")
    override fun changePage() {
        OpenTrace.mark("activity.changePage.begin current=${mainControl?.getCurrentViewIndex()} count=${mainControl?.getPageCount()}")
        binding.txtNumberPage.text =
            "${mainControl?.getCurrentViewIndex() ?: 0}/${mainControl?.getPageCount() ?: 0}"
        val page = (mainControl?.getCurrentViewIndex() ?: 0) - 1
        adapter.setCurrentPage(if (page <= 0) 0 else page)
        mainControl?.view?.let { view ->
            when (view) {
                is Presentation -> {
                    OpenTrace.mark("activity.changePage.presentation")
                    if (lastRenderPptFile == 0 || lastRenderPptFile != mainControl?.getPageCount()) {
                        OpenTrace.mark("activity.changePage.startPptThumbnail count=${mainControl?.getPageCount()}")
                        lifecycleScope.launch(Dispatchers.IO) {
                            slideViewModel.initSlideShow(view) {
                                lastRenderPptFile = it
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private var lastRenderPptFile = 0

    override fun completeLayout() {
        mainControl?.view?.let { view ->
            when (view) {
                is Word -> {
                    Log.d("ViewInitData", "DataInit: ${view.javaClass.name}")
                    lifecycleScope.launch(Dispatchers.IO) {
                        slideViewModel.initDocSlide(view)
                    }
                    mainControl?.jumpToPage(0)
                }
                else -> Unit
            }

            binding.txtNumberPage.text =
                "${mainControl?.getCurrentViewIndex() ?: 0}/${mainControl?.getPageCount() ?: 0}"
        }
    }

    override fun error(errorCode: Int) = Unit
    override fun fullScreen(fullscreen: Boolean) = Unit
    override fun showProgressBar(visible: Boolean) = Unit
    override fun updateViewImages(viewList: List<Int?>) = Unit
    override fun isChangePage(): Boolean = true
    override fun setWriteLog(saveLog: Boolean) = Unit
    override fun isWriteLog(): Boolean = false
    override fun setThumbnail(isThumbnail: Boolean) {
        Log.d("DataLogPreview", "Thumnail $isThumbnail")
    }

    override fun isThumbnail(): Boolean {
        return mainControl?.view is ExcelView
    }

    override fun getViewBackground(): Any? =
        ContextCompat.getColor(this, R.color.color_f8f8f8)

    override fun setIgnoreOriginalSize(ignoreOriginalSize: Boolean) = Unit
    override fun isIgnoreOriginalSize(): Boolean = false
    override fun getPageListViewMovingPosition(): Byte =
        IPageListViewListener.Moving_Vertical

    override fun dispose() {
        if (mainControl != null) {
            mainControl?.dispose()
            mainControl = null
        }
        if (appFrame != null) {
            appFrame = null
        }
    }
}
