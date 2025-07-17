package com.azg.pdf8.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.model.DocumentPage
import com.azg.pdf8.model.DocumentType
import com.azg.pdf8.model.RecentDocument
import com.azg.pdf8.utils.AppUtils
import com.dong.baselib.string.fileName
import com.dong.baselib.widget.pink
import com.wxiwei.office.pg.control.Presentation
import com.wxiwei.office.ss.control.ExcelView
import com.wxiwei.office.ss.model.baseModel.Row
import com.wxiwei.office.wp.control.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

enum class StateLoadData {
    Loading,Success
}

class ConvertViewModel : ViewModel() {
    private val TAG = "ConvertViewModel"
    private val _listSlide = MutableStateFlow<List<DocumentPage>>(listOf())
    var excelBitmap = MutableStateFlow<Bitmap?>(null)
    val listSlide = _listSlide.asStateFlow()

    private val _uiState = MutableStateFlow(StateLoadData.Loading)
    val uiState = _uiState.asStateFlow()

    fun initSlideShow(presentation: Presentation,dataSize: (Int)-> Unit) {
        _uiState.value = StateLoadData.Loading
        val listSlide = mutableListOf<DocumentPage>()
        val runtime = Runtime.getRuntime()

        // Log memory before processing
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        Log.e(TAG, "Initial memory usage: ${initialMemory / 1024 / 1024}MB")

        val count = presentation.pgModel.slideCount
        val realCount = presentation.pgModel.getRealSlideCount()
        Log.e(TAG, "SlideShowViewModel - initSlideShow: total slides: $count")
        Log.e(TAG, "SlideShowViewModel - initSlideShow: real slides: $realCount")

        // Process slides in smaller batches to manage memory better
        val batchSize = 3 // Reduced batch size
        var processedCount = 0
        var successfulCount = 0

        for (batchStart in 0 until count step batchSize) {
            val batchEnd = minOf(batchStart + batchSize, count)
            Log.i(TAG, "Processing batch from $batchStart to ${batchEnd - 1}")

            // Clear memory before each batch
            System.gc()
            Thread.sleep(100) // Give GC time to work

            for (i in batchStart until batchEnd) {
                processedCount++
                try {
                    val pgSlide = presentation.pgModel.getSlide(i)
                    if (pgSlide != null) {
                        Log.i(TAG, "Slide $i: pgSlide type: ${pgSlide.javaClass.name}")

                        // Create bitmap with reduced size if possible
                        val bitmap = presentation.slideToImage(i + 1)
                        if (bitmap != null && !bitmap.isRecycled) {
                            try {
                                // Scale down the bitmap to reduce memory usage
                                val scaledWidth = bitmap.width / 2
                                val scaledHeight = bitmap.height / 2
                                val scaledBitmap = bitmap.scale(scaledWidth, scaledHeight)

                                if (scaledBitmap != null && !scaledBitmap.isRecycled) {
                                    val slide = DocumentPage((i+1), bitmap = scaledBitmap)
                                    listSlide.add(slide)
                                    successfulCount++
                                    Log.i(TAG, "Successfully added scaled slide $i to list")
                                }

                                // Recycle original bitmap immediately
                                bitmap.recycle()
                            } catch (e: OutOfMemoryError) {
                                Log.e(TAG, "OutOfMemoryError for slide $i: ${e.message}")
                                System.gc()
                                Thread.sleep(200)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing slide $i: ${e.message}")
                }
            }

            // Log memory usage after each batch
            val currentMemory = runtime.totalMemory() - runtime.freeMemory()
            Log.e(TAG, "Memory usage after batch ${batchStart/batchSize}: ${currentMemory / 1024 / 1024}MB")

            // Update UI with current progress
            if (listSlide.isNotEmpty()) {
                _listSlide.update { listSlide.toList() }
            }
            dataSize.invoke(listSlide.size)
        }
        _uiState.value= StateLoadData.Success
        // Final memory check
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        Log.e(TAG, "Final memory usage: ${finalMemory / 1024 / 1024}MB")
        Log.e(TAG, "Slide processing summary:")
        Log.e(TAG, "Total slides to process: $count")
        Log.e(TAG, "Total slides processed: $processedCount")
        Log.e(TAG, "Successfully created slides: $successfulCount")
        Log.e(TAG, "Final list size: ${listSlide.size}")
    }


    fun initDocSlide(
        view: Word,
        batchSize: Int = 3,
        scaleFactor: Int = 2,
    ) {
        _uiState.value = StateLoadData.Loading
        viewModelScope.launch(Dispatchers.Default) {
            val runtime = Runtime.getRuntime()
            val initialMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            Log.e(TAG, "initDocSlide: Initial memory usage: ${initialMem}MB")
            val totalPages = view.pageCount
            if (totalPages <= 10) {
                val accumulator = mutableListOf<DocumentPage>()
                for (i in 0..totalPages) {
                    try {
                        val raw = view.pageToImage(i)
                        if (raw != null && !raw.isRecycled) {
                            try {
                                val scaled =
                                    raw.scale(raw.width / scaleFactor, raw.height / scaleFactor)
                                if (scaled != null && !scaled.isRecycled) {
                                    accumulator += DocumentPage(i, bitmap = scaled)
                                }
                            } catch (oom: OutOfMemoryError) {
                                Log.e(TAG, "OOM at page $i: ${oom.message}")
                                raw.recycle()
                                System.gc()
                                Thread.sleep(200)
                            } finally {
                                if (!raw.isRecycled) raw.recycle()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "initDocSlide: error at page $i: ${e.message}")
                    }
                }
                withContext(Dispatchers.Main) {
                    _listSlide.value = (accumulator.toList())
                    _uiState.value= StateLoadData.Success
                }
            } else {
                Log.i(TAG, "initDocSlide: total pages to process: $totalPages")
                var processed = 0
                var successCount = 0
                val accumulator = mutableListOf<DocumentPage>()

                for (batchStart in 0 until totalPages step batchSize) {
                    val batchEnd = minOf(batchStart + batchSize, totalPages)
                    Log.i(TAG, "initDocSlide: processing pages $batchStart..${batchEnd - 1}")
                    System.gc()
                    Thread.sleep(100)

                    for (i in batchStart until batchEnd) {
                        processed++
                        try {
                            val raw = view.pageToImage(i)
                            if (raw != null && !raw.isRecycled) {
                                try {
                                    val scaled =
                                        raw.scale(raw.width / scaleFactor, raw.height / scaleFactor)
                                    if (scaled != null && !scaled.isRecycled) {
                                        accumulator += DocumentPage(i, bitmap = scaled)
                                        successCount++
                                    }
                                } catch (oom: OutOfMemoryError) {
                                    Log.e(TAG, "OOM at page $i: ${oom.message}")
                                    raw.recycle()
                                    System.gc()
                                    Thread.sleep(200)
                                } finally {
                                    if (!raw.isRecycled) raw.recycle()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "initDocSlide: error at page $i: ${e.message}")
                        }
                    }
                    withContext(Dispatchers.Main) {
                        _listSlide.value = (accumulator.toList())
                    }
                    val midMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                    Log.e(
                        TAG,
                        "initDocSlide: memory after batch ${batchStart / batchSize}: ${midMem}MB"
                    )
                }
                _uiState.value= StateLoadData.Success
                val finalMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                Log.e(TAG, "initDocSlide: Final memory usage: ${finalMem}MB")
                Log.e(TAG, "initDocSlide: pages processed: $processed, successful: $successCount")
            }
        }
    }

    fun initXlsPages(view: ExcelView, callback: (Bitmap?) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            val thumbBmp = try {
                val sheet = view.sheetView.spreadsheet.sheetView.currentSheet
                val activeRow = sheet.physicalNumberOfRows
                var activeColumn = 0

                for (i in 0 .. activeRow-1){
                    if( sheet.getRow(i).lastCol !=null){
                        activeColumn= sheet.getRow(i).lastCol
                        break
                    }
                }

                var currentSheetWidth = 0f
                var currentSheetHeight = 0f
                for (i in 0..activeColumn + 1) {
                    currentSheetWidth += sheet.getColumnPixelWidth(i)
                }
                for (i in 0..activeRow + 1) {
                    val row: Row = sheet.getRow(i) ?: continue
                    val rowHeight: Float = row.rowPixelHeight
                    Log.d(
                        TAG,
                        "Row info: ${row.toString()} --${row.rowPixelHeight}"
                    )
                    currentSheetHeight += rowHeight
                }
                Log.d(
                    TAG,
                    "Data is active: $activeRow-$activeColumn"
                )
                val fullBitmap = view
                    .sheetView
                    .spreadsheet
                    .sheetView
                    .getThumbnail(sheet, currentSheetWidth.toInt(), currentSheetHeight.toInt(), 1f)
                view.sheetView.spreadsheet.control.view
                fullBitmap
            } catch (t: Throwable) {
                Log.e(TAG, "initXlsPage failed: ", t)
                null
            }
            withContext(Dispatchers.Main) {
                callback(thumbBmp)
            }
        }
    }

    fun convertToPdf(
        newName : String,
        model: RecentDocument,
        onNameExit:()-> Unit,
        onStart: () -> Unit,
        onFinish: (File) -> Unit,
        onError: () -> Unit
    ) {
        Log.d(TAG, "convertToPdf: start for ${model.path}")
        viewModelScope.launch(Dispatchers.IO) {
            // notify UI
            withContext(Dispatchers.Main) {
                Log.d(TAG, "convertToPdf: onStart()")
                onStart()
            }
            // prepare file
            var outputFile = File(
                AppUtils.documentPath,
                "$newName.pdf"
            ).apply {
                if (!exists()) createNewFile()
            }


            when (model.type) {
                DocumentType.Doc, DocumentType.Ppt -> {
                    Log.d(TAG, "convertToPdf: DOC/PPT branch, slides=${listSlide.value?.size}")
                    val bitmaps = listSlide.value?.map { it.bitmap } ?: emptyList()
                    if (bitmaps.isEmpty()) {
                        Log.e(TAG, "convertToPdf: no bitmaps to write")
                        withContext(Dispatchers.Main) { onError() }
                    } else {
                        val success = createPdfFromBitmaps(bitmaps, outputFile)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Log.d(TAG, "convertToPdf: finished DOC/PPT PDF")
                                onFinish(outputFile)
                            } else {
                                Log.e(TAG, "convertToPdf: error in DOC/PPT PDF")
                                onError()
                            }
                        }
                    }
                }
                DocumentType.Excel -> {
                    val bmp = excelBitmap.value
                    Log.d(TAG, "convertToPdf: EXCEL branch, bitmap=${bmp?.width}×${bmp?.height}")
                    if (bmp == null) {
                        withContext(Dispatchers.Main) { onError() }
                    } else {
                        // A4 @72dpi: 595×842 pts, margin 20pts → content height = 842–40 = 802px
                        val pageHeightPts = (842 * 1.55f).roundToInt()
                        val marginPts = 20
                        val contentHeightPx = pageHeightPts - marginPts * 2
                        val pages = splitBitmapVertically(bmp, contentHeightPx)
                        Log.d(TAG, "convertToPdf: split into ${pages.size} pages")
                        val success = createPdfFromBitmaps(pages, outputFile)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                Log.d(TAG, "convertToPdf: finished EXCEL PDF")
                                onFinish(outputFile)
                            } else {
                                Log.e(TAG, "convertToPdf: error in EXCEL PDF")
                                onError()
                            }
                        }
                    }
                }
                else -> {
                    Log.d(TAG, "convertToPdf: OTHER branch → immediately onFinish()")
                    withContext(Dispatchers.Main) { onFinish(outputFile) }
                }
            }
        }
    }

    private fun splitBitmapVertically(src: Bitmap, maxHeightPx: Int): List<Bitmap> {
        val pages = mutableListOf<Bitmap>()
        var yOffset = 0
        while (yOffset < src.height) {
            val chunkHeight = minOf(maxHeightPx, src.height - yOffset)
            Log.d(TAG, "splitBitmap: chunk at y=$yOffset height=$chunkHeight")
            val chunk = Bitmap.createBitmap(src, 0, yOffset, src.width, chunkHeight)
            pages += chunk
            yOffset += chunkHeight
        }
        return pages
    }

    private fun createPdfFromBitmaps(
        bitmaps: List<Bitmap?>,
        outputFile: File,
        pageWidthPts: Int = 595,
        pageHeightPts: Int = 842,
        marginPts: Int = 20
    ): Boolean {
        Log.d(TAG, "createPdfFromBitmaps: start, pages=${bitmaps.size}, file=$outputFile")
        val pdf = PdfDocument()
        return try {
            bitmaps.forEachIndexed { index, bitmap ->
                bitmap?.let {
                    val contentW = pageWidthPts - marginPts * 2
                    val contentH = pageHeightPts - marginPts * 2
                    val scale = minOf(
                        contentW.toFloat() / it.width,
                        contentH.toFloat() / it.height
                    )
                    Log.d(TAG, " page#${index + 1}: size=${it.width}×${it.height} scale=$scale")
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pageWidthPts, pageHeightPts, index + 1
                    ).create()
                    val page = pdf.startPage(pageInfo)
                    val dx = marginPts + (contentW - it.width * scale) / 2f
                    val dy = marginPts + (contentH - it.height * scale) / 2f
                    val matrix = Matrix().apply {
                        postScale(scale, scale)
                        postTranslate(dx, dy)
                    }

                    page.canvas.drawBitmap(it, matrix, null)
                    pdf.finishPage(page)
                    Log.d(TAG, " page#${index + 1} done")
                }
            }

            FileOutputStream(outputFile).use { out ->
                Log.d(TAG, "createPdf: writing to disk")
                pdf.writeTo(out)
            }
            Log.d(TAG, "createPdf: write complete")
            true
        } catch (e: IOException) {
            Log.e(TAG, "createPdf: ERROR", e)
            false
        } finally {
            pdf.close()
            Log.d(TAG, "createPdf: PDF closed")
        }
    }
}
