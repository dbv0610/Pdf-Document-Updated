package com.azg.pdf8.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.model.CreatePdf
import com.azg.pdf8.model.RecentDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

interface OnCreateFile {
    fun onStartCreate() {}
    fun onFinish(file: File?) {}
    fun onError() {}
}

class CreateViewModel : ViewModel() {
    private val _listPhotoSelect = MutableStateFlow(mutableListOf<CreatePdf>())
    val listPhotoSelect = _listPhotoSelect.asStateFlow()

    fun setListPhotSelect(listData: MutableList<CreatePdf>) {
        _listPhotoSelect.value = listData
    }

    fun updateDataInList(newItem: CreatePdf) {
        _listPhotoSelect.update { currentList ->
            val updated = currentList.map { existing ->
                if (existing.defId == newItem.defId) newItem else existing
            }.toMutableList()

            if (updated.none { it.defId == newItem.defId }) {
                updated.add(newItem)
            }
            updated
        }
    }

    fun addData(pdf: CreatePdf) {
        val newList = _listPhotoSelect.value.toMutableList()
        newList.add(pdf)
        _listPhotoSelect.value = newList
    }

    fun addDataToList(dataRepo: MutableList<RecentDocument>) {
        viewModelScope.launch(Dispatchers.IO) {
            val created = dataRepo.mapIndexed { index, doc ->
                val thumbnail = BitmapFactory.decodeFile(doc.path)
                CreatePdf(
                    defId = index,
                    picture = thumbnail,
                    indexOfList = index
                )
            }
            withContext(Dispatchers.Main) {
                val totalList = listPhotoSelect.value.toMutableList()
                totalList.addAll(created)
                _listPhotoSelect.value = totalList
            }
        }
    }

    fun initListData(docs: List<RecentDocument>) {
        viewModelScope.launch(Dispatchers.IO) {
            val created = docs.mapIndexed { index, doc ->
                val thumbnail = BitmapFactory.decodeFile(doc.path)
                CreatePdf(
                    defId = index,
                    picture = thumbnail,
                    indexOfList = index
                )
            }
            withContext(Dispatchers.Main) {
                _listPhotoSelect.value = created.toMutableList()
            }
        }
    }
    @Throws(IOException::class)
    fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFile: File,
        pageWidthPts: Int = 595,
        pageHeightPts: Int = 842,
        marginPts: Int = 20,
        event: OnCreateFile
    ) {
        val pdf = PdfDocument()
        event.onStartCreate()
        try {
            bitmaps.forEachIndexed { index, bitmap ->
                val contentW = pageWidthPts - marginPts * 2
                val contentH = pageHeightPts - marginPts * 2
                val scale = minOf(
                    contentW.toFloat() / bitmap.width,
                    contentH.toFloat() / bitmap.height
                )
                val scaledW = (bitmap.width * scale).toInt()
                val scaledH = (bitmap.height * scale).toInt()
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidthPts,
                    pageHeightPts,
                    index + 1
                ).create()
                val page = pdf.startPage(pageInfo)
                val dx = marginPts + (contentW - scaledW) / 2f
                val dy = marginPts + (contentH - scaledH) / 2f
                val matrix = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(dx, dy)
                }
                page.canvas.drawBitmap(bitmap, matrix, null)

                pdf.finishPage(page)
            }
            FileOutputStream(outputFile).use { out ->
                pdf.writeTo(out)
            }
            event.onFinish(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
            event.onError()
        } finally {
            pdf.close()
        }
    }
}