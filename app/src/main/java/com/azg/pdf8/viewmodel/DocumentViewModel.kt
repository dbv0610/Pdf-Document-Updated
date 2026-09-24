package com.azg.pdf8.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.database.FavoriteDao
import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.dialog.SortByData
import com.azg.pdf8.dialog.SortDateType
import com.azg.pdf8.dialog.SortSizeType
import com.azg.pdf8.model.DocumentPage
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.FavoriteDocument
import com.azg.pdf8.model.FavoriteUi
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.model.RecentUi
import com.azg.pdf8.ui.main.document.pdf.PageViewType
import com.azg.pdf8.utils.AppUtils
import com.azg.pdf8.utils.formatDateByMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocumentViewModel(
    val repo: AppDataRepo,
    val recentDao: RecentDao,
    val favoriteDao: FavoriteDao
) : ViewModel() {
    private val recentFlow = recentDao.getAll()
    private val favIdsFlow = favoriteDao.favoriteIds()
    private var _sortByData = MutableStateFlow(SortByData.None)
    val sortByData = _sortByData.asStateFlow()
    private val _pageViewState = MutableStateFlow<PageViewType>(PageViewType.PageByPage)
    val pageViewState = _pageViewState.asStateFlow()

    fun setPageState(state: PageViewType) {
        _pageViewState.value = state
    }

    val recentDocument: StateFlow<List<RecentUi>> =
        combine(recentFlow, favIdsFlow) { recents, favIds ->
            val favSet = favIds.map { it.toInt() }.toSet()
            recents.map { doc ->
                RecentUi(doc, doc.mediaId.toInt() in favSet)
            }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _listFavoriteSearch = MutableStateFlow(mutableListOf<FavoriteUi>())
    val listFavoriteSearch = _listFavoriteSearch.asStateFlow()
    private val _favorites = favoriteDao.getAll()
        .map { list ->
            list.map { doc ->
                doc to (favoriteDao.isExistInFavorite(doc.mediaId.toInt()) > 0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val favoriteDocument: StateFlow<List<FavoriteUi>> = _favorites.map { pairs ->
        pairs.map { (doc, isFav) ->
            FavoriteUi(doc, isFav)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sortByDateFlow = MutableStateFlow(SortDateType.NoSelect)
    val sortBySizeFlow = MutableStateFlow(SortSizeType.NoSelect)
    val filterTypesFlow = MutableStateFlow(
        listOf(
            DocumentType.Doc,
            DocumentType.Ppt,
            DocumentType.Pdf,
            DocumentType.Excel,
            DocumentType.Image
        )
    )
    private val _searchKey = MutableStateFlow("")
    private val _searchType = MutableStateFlow<DocumentType?>(null)

    fun setSortByData(data: SortByData) {
        _sortByData.value = data
    }

    fun setSearchCriteria(key: String, type: DocumentType?) {
        _searchKey.value = key
        _searchType.value = type
    }

    private val _pagesState = MutableStateFlow<List<DocumentPage>>(emptyList())
    val pagesState: LiveData<List<DocumentPage>> = _pagesState.asLiveData()

    fun renderPdf(context: Context, pdfUri: Uri, thumbnailWidth: Int = 200) {
        viewModelScope.launch {
            try {
                val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                    ?: throw IllegalArgumentException("Cannot open PDF: $pdfUri")

                PdfRenderer(pfd).use { renderer ->
                    _pagesState.value = List(renderer.pageCount) { DocumentPage.loading(it) }

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
                                        if (it.index == index) DocumentPage.error(index, e) else it
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _pagesState.value = listOf(DocumentPage.error(0, e))
            }
        }
    }


    val listRecentSearch: StateFlow<List<RecentUi>> = combine(
        recentDocument,
        _searchKey,
        sortByDateFlow,
        sortBySizeFlow,
        filterTypesFlow
    ) { docs, rawKey, sortDateType, sortSizeType, filterByTypes ->
        val byType = if (filterByTypes.isEmpty()) {
            mutableListOf()
        } else {
            docs.filter { it.document.type in filterByTypes }
        }
        val key = rawKey.trim().lowercase()
        val byKey = if (key.isEmpty()) {
            byType
        } else {
            byType.filter { ui ->
                val name = File(ui.document.path).name.lowercase()
                val date = formatDateByMillis(ui.document.lastModified).lowercase()
                name.contains(key) || date.contains(key)
            }
        }
        val sorted = when (sortDateType) {
            SortDateType.NewToOld -> byKey.sortedBy { it.document.lastModified }
            SortDateType.OldToNew -> byKey.sortedByDescending { it.document.lastModified }
            SortDateType.NoSelect -> when (sortSizeType) {
                SortSizeType.BigToSmall -> byKey.sortedBy { it.document.size }
                SortSizeType.SmallToBig -> byKey.sortedByDescending { it.document.size }
                SortSizeType.NoSelect -> byKey
            }
        }
        sorted
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    val listDocumentSearch: StateFlow<List<RecentUi>> = combine(
        repo.listAllData,
        favoriteDocument,
        _searchKey,
        _searchType,
        _sortByData
    ) { docs, favUiList, key, type, sortType ->
        val k = key.trim().lowercase()
        val baseList = docs.map { doc ->
            val isFav = favUiList.any { it.document.mediaId == doc.mediaId }
            RecentUi(doc, isFav)
        }
        val filteredByType = type?.let { t ->
            baseList.filter { it.document.type == t }
        } ?: baseList
        val filteredByKey = if (k.isEmpty()) {
            filteredByType
        } else {
            filteredByType.filter { ui ->
                val name = File(ui.document.path).name.lowercase()
                val date = formatDateByMillis(ui.document.lastModified).lowercase()
                name.contains(k) || date.contains(k)
            }
        }
        val sorted = when (sortType) {
            SortByData.None -> filteredByKey
            SortByData.SortByName ->
                filteredByKey.sortedBy { File(it.document.path).name.lowercase() }
            SortByData.SortBySize ->
                filteredByKey.sortedBy { it.document.size }
            SortByData.SortByDate ->
                filteredByKey.sortedBy { it.document.lastModified }
        }
        sorted.reversed()
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            combine(
                favoriteDocument,
                sortByDateFlow,
                sortBySizeFlow,
                filterTypesFlow,
                _searchKey,
            ) { original, byDate, bySize, types, key ->
                val filtered = if (types.isEmpty()) {
                    mutableListOf()
                } else {
                    original.filter { doc -> types.contains(doc.document.type) }
                }
                if (byDate == SortDateType.NoSelect && bySize == SortSizeType.NoSelect) {
                    if (key.isEmpty()) {
                        filtered
                    } else {
                        filtered.filter { ui ->
                            val filename = File(ui.document.path).name.lowercase()
                            val dateStr = formatDateByMillis(ui.document.lastModified)
                                .lowercase()
                            filename.contains(key.lowercase())
                                    || dateStr.contains(key.lowercase())
                        }.toMutableList()
                    }
                } else {
                    if (byDate != SortDateType.NoSelect) {
                        val sorted = when (byDate) {
                            SortDateType.NewToOld -> filtered.sortedByDescending { it.document.lastModified }
                            SortDateType.OldToNew -> filtered.sortedBy { it.document.lastModified }
                            else -> filtered
                        }
                        if (key.isEmpty()) {
                            sorted
                        } else {
                            sorted.filter { ui ->
                                val filename = File(ui.document.path).name.lowercase()
                                val dateStr = formatDateByMillis(ui.document.lastModified)
                                    .lowercase()
                                filename.contains(key.lowercase())
                                        || dateStr.contains(key.lowercase())
                            }.toMutableList()
                        }
                    } else {
                        val sorted = when (bySize) {
                            SortSizeType.BigToSmall -> filtered.sortedByDescending { it.document.size }
                            SortSizeType.SmallToBig -> filtered.sortedBy { it.document.size }
                            else -> filtered
                        }
                        if (key.isEmpty()) {
                            sorted
                        } else {
                            sorted.filter { ui ->
                                val filename = File(ui.document.path).name.lowercase()
                                val dateStr = formatDateByMillis(ui.document.lastModified)
                                    .lowercase()
                                filename.contains(key.lowercase())
                                        || dateStr.contains(key.lowercase())
                            }.toMutableList()
                        }
                    }
                }
            }.collect { result ->
                _listFavoriteSearch.value = result.toMutableList()
            }
        }
    }

    fun filterSortFavorite(
        byDate: SortDateType,
        bySize: SortSizeType,
        types: List<DocumentType>
    ) {
        sortByDateFlow.value = byDate
        sortBySizeFlow.value = bySize
        filterTypesFlow.value = types
    }

    fun loadDocuments(context: Context) {
        viewModelScope.launch {
            val allDocs = AppUtils
                .scanAllDocumentsFlow(context, recentDao, favoriteDao)
                .onEach { delay(2) }
                .catch { e -> Log.e("DocumentViewModel", "Error scanning docs: $e") }
                .toList()
            repo._listAllData.value = allDocs.toMutableList()
            repo._listPdf.value =
                allDocs.filter { it.type == DocumentType.Pdf }.toMutableList().reversed()
                    .toMutableList()
            repo._listDoc.value =
                allDocs.filter { it.type == DocumentType.Doc }.toMutableList().reversed()
                    .toMutableList()
            repo._listXls.value =
                allDocs.filter { it.type == DocumentType.Excel }.toMutableList().reversed()
                    .toMutableList()
            repo._listPpt.value =
                allDocs.filter { it.type == DocumentType.Ppt }.toMutableList().reversed()
                    .toMutableList()
        }
    }

    fun addToRecent(it: RecentDocument) {
        viewModelScope.launch {
            recentDao.insert(it)
        }
    }

    fun addToRecent(it: FavoriteDocument) {
        viewModelScope.launch {
            favoriteDao.insert(it)
        }
    }

    fun toggleFavorite(doc: FavoriteDocument) {
        viewModelScope.launch {
            val isExit = favoriteDao.isExistInFavorite(doc.mediaId.toInt()) > 0
            favoriteDao.toggleFavorite(doc, !isExit)
        }
    }

    fun toggleFavoriteRecent(doc: RecentDocument) {
        viewModelScope.launch {
            val isExit = favoriteDao.isExistInFavorite(doc.mediaId.toInt()) > 0
            favoriteDao.toggleFavorite(doc, !isExit)
        }
    }

    fun removeFavorite(model: FavoriteDocument) {
        viewModelScope.launch {
            favoriteDao.deleteById(model.mediaId)
        }
    }

    fun removeRecent(model: RecentDocument) {
        viewModelScope.launch {
            repo.removeItemInList(model.path)
            recentDao.deleteById(model.mediaId)
        }
    }

    fun renameFile(
        model: RecentDocument,
        newBaseName: String
    ) {
        val oldFile = File(model.path)
        val extension = oldFile.extension
        val newName = "$newBaseName.$extension"
        val newFile = File(oldFile.parentFile, newName)

        if (newFile.exists()) return
        else {
            if (!oldFile.renameTo(newFile)) {
                Log.e("TAG_Rename", "Failed to rename file on disk: ${oldFile.path}")
                return
            }
            Log.d("TAG_Rename", "Renamed on disk to: ${newFile.absolutePath}")
            val updatedPath = newFile.absolutePath


            viewModelScope.launch {
                repo._listAllData.value = repo._listAllData.value
                    .map { data ->
                        if (data.mediaId == model.mediaId) data.copy(path = updatedPath)
                        else data
                    }
                    .toMutableList()

                recentDao.updatePath(model.mediaId.toInt(), updatedPath)
                favoriteDao.updateName(
                    documentId = model.mediaId.toInt(),
                    newPath = updatedPath
                )
            }
        }
    }

    fun renameFile(
        model: FavoriteDocument,
        newBaseName: String
    ) {
        val oldFile = File(model.path)
        val extension = oldFile.extension
        val newName = "$newBaseName.$extension"
        val newFile = File(oldFile.parentFile, newName)

        if (newFile.exists()) return
        else {
            if (!oldFile.renameTo(newFile)) {
                Log.e("TAG_Rename", "Failed to rename file on disk: ${oldFile.path}")
                return
            }
            Log.d("TAG_Rename", "Renamed on disk to: ${newFile.absolutePath}")
            val updatedPath = newFile.absolutePath


            viewModelScope.launch {
                repo._listPdf.value = repo._listPdf.value
                    .map { data ->
                        if (data.mediaId == model.mediaId) data.copy(path = updatedPath)
                        else data
                    }
                    .toMutableList()

                recentDao.updatePath(model.mediaId.toInt(), updatedPath)
                favoriteDao.updateName(
                    documentId = model.mediaId.toInt(),
                    newPath = updatedPath
                )
            }
        }
    }
}