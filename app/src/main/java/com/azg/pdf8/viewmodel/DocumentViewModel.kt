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
import com.azg.pdf8.utils.AppUtils
import com.azg.pdf8.utils.formatDateByMillis
import com.dong.baselib.string.fileName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentViewModel(val repo: AppDataRepo, val dao: RecentDao, val favoriteDao: FavoriteDao) :
    ViewModel() {
    val recentDocument: StateFlow<List<RecentDocument>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val favoriteDocument: StateFlow<List<FavoriteDocument>> = favoriteDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _listDocumentSearch = MutableStateFlow(mutableListOf<RecentDocument>())
    val listDocumentSearch = _listDocumentSearch.asStateFlow()
    private val _listFavoriteSearch = MutableStateFlow(mutableListOf<FavoriteDocument>())
    val listFavoriteSearch = _listFavoriteSearch.asStateFlow()
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
                    original.filter { doc -> types.contains(doc.type) }
                }
                if (byDate == SortDateType.NoSelect && bySize == SortSizeType.NoSelect) {
                    filtered
                } else {
                    if (byDate != SortDateType.NoSelect) {
                        val sorted = when (byDate) {
                            SortDateType.NewToOld -> filtered.sortedByDescending { it.lastModified }
                            SortDateType.OldToNew -> filtered.sortedBy { it.lastModified }
                            else -> filtered
                        }
                        sorted
                    } else {
                        val sorted = when (bySize) {
                            SortSizeType.BigToSmall -> filtered.sortedByDescending { it.size }
                            SortSizeType.SmallToBig -> filtered.sortedBy { it.size }
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

    fun searchByKey(key: String, type: DocumentType) {
        val listSrcDoc = when (type) {
            DocumentType.Doc -> repo.documentListDoc.value
            DocumentType.Pdf -> repo.documentListPdf.value
            DocumentType.Ppt -> repo.documentListPpt.value
            DocumentType.Excel -> repo.documentListXls.value
            else -> emptyList()
        }
        if (key == "") {
            _listDocumentSearch.value = listSrcDoc.toMutableList()
        } else {
            _listDocumentSearch.value =
                listSrcDoc.filter {
                    it.path.fileName().lowercase().contains(key.lowercase()) || formatDateByMillis(
                        it.lastModified
                    ).contains(key.lowercase())
                }
                    .toMutableList()
        }
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
            dao.insert(it)
        }
    }

    fun addToRecent(it: FavoriteDocument) {
        viewModelScope.launch {
            favoriteDao.insert(it)
        }
    }
}