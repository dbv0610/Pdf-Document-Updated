package com.azg.pdf8.viewmodel

import com.azg.pdf8.model.DocumentModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppDataRepo {
    val _listPdf = MutableStateFlow(mutableListOf<DocumentModel>())
    val _listDoc = MutableStateFlow(mutableListOf<DocumentModel>())
    val _listXls = MutableStateFlow(mutableListOf<DocumentModel>())
    val _listPpt = MutableStateFlow(mutableListOf<DocumentModel>())
    val documentListPdf = _listPdf.asStateFlow()
    val documentListDoc = _listDoc.asStateFlow()
    val documentListXls = _listXls.asStateFlow()
    val documentListPpt = _listPpt.asStateFlow()
    val _listAllData = MutableStateFlow(mutableListOf<DocumentModel>())
    val listAllData = _listAllData.asStateFlow()
}