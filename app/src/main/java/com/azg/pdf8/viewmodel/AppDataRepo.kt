package com.azg.pdf8.viewmodel

import android.content.Context
import com.azg.pdf8.R
import com.azg.pdf8.model.ContentWithPage
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.model.FolderItem
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

    fun extractPdfTextFromPath(pdfPath: String): Flow<DataResponse<String?>> = flow {
        try {
            val text = extractPdfTextFromPathHandler(pdfPath)
            emit(DataResponse.DataSuccess(text))
        } catch (e: Exception) {
            emit(DataResponse.DataError(e.localizedMessage ?: "error"))
        }
    }.flowOn(Dispatchers.IO)

    fun extractPdfTextFromPathHandler(pdfPath: String): String? {
        return try {
            val file = File(pdfPath)
            val document = PDDocument.load(file)
            val pdfStripper = PDFTextStripper()
            val text = pdfStripper.getText(document)
            document.close()
            text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTextSearch(pdfPath: String, query: String): Flow<DataResponse<List<ContentWithPage>>> =
        flow {
            try {
                val search = searchTextInPdf(pdfPath, query)
                emit(DataResponse.DataSuccess(search))
            } catch (e: Exception) {
                emit(DataResponse.DataError(e.localizedMessage ?: "error"))
            }
        }.flowOn(Dispatchers.IO)

    fun searchTextInPdf(pdfPath: String, query: String): List<ContentWithPage> {
        val result = mutableListOf<ContentWithPage>()
        try {
            val file = File(pdfPath)
            val document = PDDocument.load(file)
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages
            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                val text = stripper.getText(document)
                val matchingLines = text.lines().filter {
                    it.contains(query, ignoreCase = true)
                }
                matchingLines.forEach { line ->
                    result.add(ContentWithPage(page, line.trim()))
                }
            }

            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    fun extractLinksWithPagesFromPdf(pdfPath: String): List<Pair<Int, String>> {
        val links = mutableListOf<Pair<Int, String>>()
        try {
            val file = File(pdfPath)
            val document = PDDocument.load(file)
            val pages = document.pages

            for (i in 0 until pages.count) {
                val page = pages[i]
                val annotations = page.annotations

                for (annotation in annotations) {
                    if (annotation is PDAnnotationLink) {
                        val action = annotation.action
                        if (action is PDActionURI) {
                            links.add(i + 1 to action.uri)
                        }
                    }
                }
            }

            document.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return links
    }

    fun renameAndGetPath(oldFile: File, newName: String): String? {
        val parent = oldFile.parentFile ?: return null
        val newFile = File(parent, newName)
        return newFile.absolutePath
    }
}