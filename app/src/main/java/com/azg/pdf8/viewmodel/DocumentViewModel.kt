package com.azg.pdf8.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.database.FavoriteDao
import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.dialog.SortDateType
import com.azg.pdf8.dialog.SortSizeType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.FavoriteDocument
import com.azg.pdf8.model.FavoriteUi
import com.azg.pdf8.model.RecentUi
import com.azg.pdf8.utils.AppUtils
import com.azg.pdf8.utils.formatDateByMillis
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
import kotlinx.coroutines.launch
import java.io.File

class DocumentViewModel(val repo: AppDataRepo, val recentDao: RecentDao, val favoriteDao: FavoriteDao) :
    ViewModel() {
    private val _recentDocument = recentDao.getAll()
        .map { list ->
            list.map { doc ->
                doc to (favoriteDao.isExistInFavorite(doc.mediaId.toInt()) > 0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recentDocument: StateFlow<List<RecentUi>> = _recentDocument.map { pairs ->
        pairs.map { (doc, isFav) ->
            RecentUi(doc, isFav)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())




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



    private val _searchKey  = MutableStateFlow("")
    private val _searchType = MutableStateFlow<DocumentType?>(null)
    fun setSearchCriteria(key: String, type: DocumentType?) {
        _searchKey.value  = key
        _searchType.value = type
    }
    private val allDocsFlow: Flow<List<RecentDocument>> =
        combine(
            repo.documentListPdf,
            repo.documentListDoc,
            repo.documentListXls,
            repo.documentListPpt
        ) { pdfs, docs, xls, ppts ->
            (pdfs + docs + xls + ppts)
        }

    val listDocumentSearch: StateFlow<List<RecentUi>> = combine(
        allDocsFlow,
        favoriteDocument,
        _searchKey,
        _searchType
    ) { docs, favUiList, key, type ->
        val k = key.trim().lowercase()
        docs
            .map { doc ->
                val isFav = favUiList.any { it.document.mediaId == doc.mediaId }
                RecentUi(doc, isFav)
            }
            .let { list ->
                if (type != null) list.filter { it.document.type == type }
                else list
            }
            .filter { ui ->
                if (k.isEmpty()) true
                else {
                    val name = File(ui.document.path).name.lowercase()
                    val date = formatDateByMillis(ui.document.lastModified).lowercase()
                    name.contains(k) || date.contains(k)
                }
            }
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())




    init {
        viewModelScope.launch {
            combine(
                favoriteDocument,
                sortByDateFlow,
                sortBySizeFlow,
                filterTypesFlow
            ) { original, byDate, bySize, types ->
                val filtered = if (types.isEmpty()) {
                    mutableListOf()
                } else {
                    original.filter { doc -> types.contains(doc.document.type) }
                }
                if (byDate == SortDateType.NoSelect && bySize == SortSizeType.NoSelect) {
                    filtered
                } else {
                    if (byDate != SortDateType.NoSelect) {
                        val sorted = when (byDate) {
                            SortDateType.NewToOld -> filtered.sortedByDescending { it.document.lastModified }
                            SortDateType.OldToNew -> filtered.sortedBy { it.document.lastModified }
                            else -> filtered
                        }
                        sorted
                    } else {
                        val sorted = when (bySize) {
                            SortSizeType.BigToSmall -> filtered.sortedByDescending { it.document.size }
                            SortSizeType.SmallToBig -> filtered.sortedBy { it.document.size }
                            else -> filtered
                        }
                        sorted
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
    private val favoriteIds = emptySet<Long>()

    fun searchByKey(key: String, type: DocumentType) {
        val normalizedKey = key.trim().lowercase()
        val srcDocs: List<RecentDocument> = when (type) {
            DocumentType.Doc   -> repo.documentListDoc.value
            DocumentType.Pdf   -> repo.documentListPdf.value
            DocumentType.Ppt   -> repo.documentListPpt.value
            DocumentType.Excel -> repo.documentListXls.value
            else               -> emptyList()
        }
        val srcUiList = srcDocs.map { doc ->
            RecentUi(
                document   = doc,
                isFavorite = favoriteIds.contains(doc.mediaId)
            )
        }
        if (normalizedKey.isEmpty()) {
          //  _listDocumentSearch.value = srcUiList.toMutableList()
            return
        }
        val filtered = srcUiList.filter { ui ->
            val filename = File(ui.document.path).name.lowercase()
            val dateStr  = formatDateByMillis(ui.document.lastModified)
                .lowercase()

            filename.contains(normalizedKey)
                    || dateStr.contains(normalizedKey)
        }
       // _listDocumentSearch.value = filtered.toMutableList()
    }

    init {
        viewModelScope.launch {
            combine(
                repo.documentListPdf,
                repo.documentListDoc,
                repo.documentListXls,
                repo.documentListPpt,
            ) { pdf, doc, xls, ppt ->
                listOf(
                    "PDF: ${pdf.size}",
                    "DOC: ${doc.size}",
                    "XLS: ${xls.size}",
                    "PPT: ${ppt.size}"
                )
            }.collect { logList ->
                logList.forEach { Log.d("DocumentViewModel", it) }
            }
        }
    }

    fun loadDocuments(context: Context) {
        viewModelScope.launch {
            AppUtils.scanAllDocumentsFlow(context)
                .onEach { delay(2) }
                .catch { e -> Log.e("DocumentViewModel", "Error scanning docs: $e") }
                .collect { documents ->
                    repo._listAllData.value = documents.toMutableList()
                    Log.e("DocumentViewModel", "Error scanning docs: $documents.")
                    repo._listPdf.value =
                        documents.filter { it.type == DocumentType.Pdf }.toMutableList()
                    repo._listDoc.value =
                        documents.filter { it.type == DocumentType.Doc }.toMutableList()
                    repo._listXls.value =
                        documents.filter { it.type == DocumentType.Excel }.toMutableList()
                    repo._listPpt.value =
                        documents.filter { it.type == DocumentType.Ppt }.toMutableList()
                }
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
}