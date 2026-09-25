package com.azg.pdf8.viewmodel

import android.content.Context
import com.azg.pdf8.R
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.model.FolderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AppDataRepo(val context: Context) {
    val _listDoc = MutableStateFlow(mutableListOf<RecentDocument>())
    val _listXls = MutableStateFlow(mutableListOf<RecentDocument>())
    val _listPpt = MutableStateFlow(mutableListOf<RecentDocument>())
    val _listPdf = MutableStateFlow(mutableListOf<RecentDocument>())
    val _listTxt = MutableStateFlow(mutableListOf<RecentDocument>())
    val documentListPdf = _listPdf.asStateFlow()
    val documentListDoc = _listDoc.asStateFlow()
    val documentListXls = _listXls.asStateFlow()
    val documentListPpt = _listPpt.asStateFlow()
    val documentListTxt = _listTxt.asStateFlow()
    val _listAllData = MutableStateFlow(mutableListOf<RecentDocument>())
    val listAllData = _listAllData.asStateFlow()
    val _listAllImage = MutableStateFlow(mutableListOf<RecentDocument>())
    val listAllImage = _listAllImage.asStateFlow()

    fun removeItemInList(path: String) {
        _listAllData.value = _listAllData.value.filter { it.path != path }.toMutableList()
    }

    fun groupToFolderList(list: List<RecentDocument>): MutableList<FolderItem> {
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
            folderName = context.getString(R.string.all_photo),
            previewPath = list.first().path,
            listData = list.toMutableList(),
            itemCount = list.size
        )

        return (mutableListOf(allFolder) + groupedFolders) as MutableList<FolderItem>
    }

    fun renameAndGetPath(oldFile: File, newName: String): String? {
        val parent = oldFile.parentFile ?: return null
        val newFile = File(parent, newName)
        return newFile.absolutePath
    }
}