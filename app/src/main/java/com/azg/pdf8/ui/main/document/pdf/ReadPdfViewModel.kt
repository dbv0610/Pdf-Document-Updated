package com.azg.pdf8.ui.main.document.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.model.DocumentPage
import com.azg.pdf8.viewmodel.AppDataRepo
import com.azg.pdf8.viewmodel.DataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData

enum class PageViewType {
    PageByPage, Thumbnail
}

class ReadPdfViewModel(private val repository: AppDataRepo) : ViewModel() {
    private val _pageViewState = MutableStateFlow<PageViewType>(PageViewType.PageByPage)
    val pageViewState = _pageViewState.asStateFlow()

    fun setPageState(state: PageViewType) {
        _pageViewState.value = state
    }

    private val _pagesState = MutableStateFlow<List<DocumentPage>>(emptyList())
    val pagesState: LiveData<List<DocumentPage>> = _pagesState.asLiveData()

    private var thumbnailLoader: PdfThumbnailLoader? = null
    private var renderJob: Job? = null
    private var thumbnailUri: Uri? = null

    fun renderPdf(context: Context, pdfUri: Uri, thumbnailWidth: Int = 200) {
        if (thumbnailUri == pdfUri && thumbnailLoader != null) return
        renderJob?.cancel()
        thumbnailLoader?.close()
        thumbnailUri = pdfUri
        val loader = PdfThumbnailLoader(context.applicationContext, pdfUri, thumbnailWidth)
        thumbnailLoader = loader
        renderJob = viewModelScope.launch {
            val count = loader.pageCount()
            _pagesState.value = List(count) { DocumentPage(it) }
        }
    }

    suspend fun thumbnail(index: Int): Bitmap? = thumbnailLoader?.thumbnail(index)

    override fun onCleared() {
        thumbnailLoader?.close()
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