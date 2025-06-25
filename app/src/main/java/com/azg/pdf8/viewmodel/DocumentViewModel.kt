package com.azg.pdf8.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.database.DocumentDao
import com.azg.pdf8.model.DocumentModel
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.utils.AppUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentViewModel(val repo: AppDataRepo,val dao : DocumentDao) : ViewModel() {


    val allDocuments: StateFlow<List<DocumentModel>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteDocuments: StateFlow<List<DocumentModel>> = dao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


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
}