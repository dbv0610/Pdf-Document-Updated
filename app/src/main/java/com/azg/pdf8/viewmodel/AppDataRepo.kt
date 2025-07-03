package com.azg.pdf8.viewmodel

import com.azg.pdf8.model.DocumentModel
import com.azg.pdf8.model.FolderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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


    val _listAllImage = MutableStateFlow(mutableListOf<DocumentModel>())
    val listAllImage = _listAllImage.asStateFlow()



    fun groupToFolderList(list: List<DocumentModel>): MutableList<FolderItem> {
        if (list.isEmpty()) return mutableListOf()
        val groupedFolders = list
            .groupBy { File(it.path).parentFile?.name.orEmpty() }
            .filter { (_, items) -> items.isNotEmpty() }
            .map { (folderName, items) ->
                FolderItem(
                    folderName = folderName,
                    previewPath = items.first().path,
                    listData = items.toMutableList(),
                    itemCount = items.size
                )
            }
        val allFolder = FolderItem(
            folderName = "All",
            previewPath = list.first().path,
            listData = list.toMutableList(),
            itemCount = list.size
        )

        return (mutableListOf(allFolder) + groupedFolders) as MutableList<FolderItem>
    }

}