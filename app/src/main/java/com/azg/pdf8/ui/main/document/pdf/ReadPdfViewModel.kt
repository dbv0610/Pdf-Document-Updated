package com.azg.pdf8.ui.main.document.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.model.ContentWithPage
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.model.PdfPage
import com.azg.pdf8.viewmodel.AppDataRepo
import com.azg.pdf8.viewmodel.DataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.update

enum class PageViewType {
    PageByPage, Thumbnail
}

class ReadPdfViewModel(private val repository: AppDataRepo) : ViewModel() {
    private val _pageViewState = MutableStateFlow<PageViewType>(PageViewType.PageByPage)
    val pageViewState = _pageViewState.asStateFlow()

    fun setPageState(state: PageViewType) {
        _pageViewState.value = state
    }

    private val _pagesState = MutableStateFlow<List<PdfPage>>(emptyList())
    val pagesState: LiveData<List<PdfPage>> = _pagesState.asLiveData()

    fun renderPdf(context: Context, pdfUri: Uri, thumbnailWidth: Int = 200) {
        viewModelScope.launch {
            try {
                val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                    ?: throw IllegalArgumentException("Cannot open PDF: $pdfUri")

                PdfRenderer(pfd).use { renderer ->
                    _pagesState.value = List(renderer.pageCount) { PdfPage.loading(it) }

                    withContext(Dispatchers.IO) {
                        (0 until renderer.pageCount).forEach { index ->
                            try {
                                val page = renderer.openPage(index)
                                val ratio = page.height.toFloat() / page.width
                                val thumbHeight = (thumbnailWidth * ratio).toInt()
                                val bitmap = try {
                                    createBitmap(thumbnailWidth, thumbHeight).also { bmp ->
                                        page.render(
                                            bmp,
                                            null,
                                            null,
                                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                        )
                                    }
                                } catch (e: OutOfMemoryError) {
                                    createBitmap(
                                        thumbnailWidth / 2,
                                        (thumbHeight / 2).coerceAtLeast(1),
                                        Bitmap.Config.RGB_565
                                    ).also { bmp ->
                                        page.render(
                                            bmp,
                                            null,
                                            null,
                                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                        )
                                    }
                                }

                                page.close()
                                _pagesState.update { current ->
                                    current.map {
                                        if (it.index == index) it.copy(
                                            bitmap = bitmap,
                                            isLoading = false
                                        ) else it
                                    }
                                }
                            } catch (e: Exception) {
                                _pagesState.update { current ->
                                    current.map {
                                        if (it.index == index) PdfPage.error(index, e) else it
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _pagesState.value = listOf(PdfPage.error(0, e))
            }
        }
    }

    override fun onCleared() {
        _pagesState.value.forEach { it.bitmap?.recycle() }
        super.onCleared()
    }

    private val _favorite = MutableStateFlow(false)
    val favorite: StateFlow<Boolean> get() = _favorite
    var pdfPath: String? = null
        private set

    fun setPdfPath(path: String) {
        pdfPath = path
    }

    var fullScreen: Boolean = false
        private set

    fun setStateFullScreen(value: Boolean) {
        fullScreen = value
    }

    var nightMode: Boolean = false
        private set

    fun setNightMode(value: Boolean) {
        nightMode = value
    }

    var page: Int = 1
        private set

    fun setPage(value: Int) {
        page = value
    }

    var zoomLock: Boolean = false
        private set

    fun setZoomLock(value: Boolean) {
        zoomLock = value
    }

    var isHorizontal: Boolean = false
        private set

    fun setIsHorizontal(value: Boolean) {
        isHorizontal = value
    }

    var mediaModel: RecentDocument? = null
        private set

    fun setMediaModel(value: RecentDocument) {
        setPdfPath(value.path)
        mediaModel = value
    }

    var isShowToolbar: Boolean = true
        private set

    fun setShowToolbar(value: Boolean) {
        isShowToolbar = value
    }

    var expend: Boolean = true
        private set

    fun setExpand(value: Boolean) {
        expend = value
    }

    fun insertFavoriteOrUnFavorite(path: String?) {
        path?.let {
            viewModelScope.launch {
            }
        }
    }

    fun stateFavoriteCurrent(path: String?) {
        path?.let {
            viewModelScope.launch {
            }
        }
    }

    private val _textCopy =
        MutableStateFlow<DataResponse<String?>>(DataResponse.DataIdle())
    val textCopy: StateFlow<DataResponse<String?>> get() = _textCopy

    fun getTextCopy(path: String) {
        viewModelScope.launch {
            repository.extractPdfTextFromPath(path).collect { state ->
                _textCopy.value = state
            }
        }
    }

    private val _searchQuery =
        MutableStateFlow<DataResponse<List<ContentWithPage>>>(DataResponse.DataIdle())
    val searchQuery: StateFlow<DataResponse<List<ContentWithPage>>> get() = _searchQuery

    fun getSearchQuery(pdfPath: String, query: String) {
        viewModelScope.launch {
            repository.getTextSearch(pdfPath, query).collect { state ->
                _searchQuery.value = state
            }
        }
    }

    private val _linksQuery =
        MutableStateFlow<DataResponse<List<Pair<Int, String>>>>(DataResponse.DataIdle())
    val linksQuery: StateFlow<DataResponse<List<Pair<Int, String>>>> get() = _linksQuery

    fun getLinks(pdfPath: String) {
        viewModelScope.launch {
        }
    }

    private val _saveScreenShort =
        MutableStateFlow<DataResponse<Pair<Boolean, String>>>(DataResponse.DataIdle())
    val saveScreenShort: StateFlow<DataResponse<Pair<Boolean, String>>> get() = _saveScreenShort

    fun saveToDownloadScreenShort(bitmap: Bitmap) {
        viewModelScope.launch {
        }
    }
}