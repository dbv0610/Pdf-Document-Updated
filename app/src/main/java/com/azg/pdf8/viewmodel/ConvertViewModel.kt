package com.azg.pdf8.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.os.SystemClock
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
import com.wxiwei.office.constant.SSConstant
import com.wxiwei.office.ss.view.SheetView
import com.wxiwei.office.wp.control.Word
import com.wxiwei.office.system.OpenFileErrors
import com.wxiwei.office.system.OpenFileException
import com.wxiwei.office.system.OpenTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
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
    val listSlide = _listSlide.asStateFlow()

    private val _uiState = MutableStateFlow(StateLoadData.Loading)
    val uiState = _uiState.asStateFlow()
    private var slideShowJob: Job? = null

    fun initSlideShow(presentation: Presentation,dataSize: (Int)-> Unit) {
        // Rendering every PPT slide and the explicit GC/sleeps below are expensive.
        // This method is called from a lifecycle coroutine, which runs on Main by
        // default, so the whole thumbnail pipeline must be dispatched explicitly.
        if (slideShowJob?.isActive == true) return
        slideShowJob = viewModelScope.launch(Dispatchers.Default) {
        _uiState.value = StateLoadData.Loading
        val listSlide = mutableListOf<DocumentPage>()
        val runtime = Runtime.getRuntime()

        // Log memory before processing
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        Log.e(TAG, "Initial memory usage: ${initialMemory / 1024 / 1024}MB")

        val count = presentation.getPGModel()!!.getSlideCount()
        val realCount = presentation.getPGModel()!!.getRealSlideCount()
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
                    val pgSlide = presentation.getPGModel()!!.getSlide(i)
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
            val totalPages = view.getPageCount()
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

    fun convertToPdf(
        newName : String,
        model: RecentDocument,
        onNameExit:()-> Unit,
        onStart: () -> Unit,
        onFinish: (File) -> Unit,
        onError: (OpenFileException) -> Unit,
        excelSheetView: SheetView? = null
    ) {
        Log.d(TAG, "convertToPdf: start for ${model.path}")
        // Capture the selected sheet before starting the background conversion.
        val excelSheet = excelSheetView?.getCurrentSheet()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onStart() }
                val outputFile = File(AppUtils.documentPath, "$newName.pdf").apply {
                    if (model.type != DocumentType.Excel && !exists()) createNewFile()
                }
                when (model.type) {
                    DocumentType.Doc, DocumentType.Ppt -> {
                        val bitmaps = listSlide.value.map { it.bitmap }
                        check(bitmaps.any { it != null }) { "No rendered pages to convert" }
                        createPdfFromBitmaps(bitmaps, outputFile)
                    }
                    DocumentType.Excel -> {
                        try {
                            checkNotNull(excelSheetView) { "No Excel view" }
                            checkNotNull(excelSheet) { "No selected sheet" }
                            check(withTimeoutOrNull(30_000L) {
                                while (!excelSheet.isAccomplished()) delay(100)
                                true
                            } == true) { "Timed out waiting for Excel sheet" }
                            createPdfFromExcel(excelSheetView, excelSheet, outputFile)
                        } catch (error: Throwable) {
                            deleteExcelPdfFile(outputFile)
                            throw error
                        }
                    }
                    else -> Unit
                }
                withContext(Dispatchers.Main) { onFinish(outputFile) }
            } catch (error: Throwable) {
                if (OpenFileErrors.isCancellation(error)) throw error
                val failure = OpenFileErrors.wrap(error, model.path)
                OpenTrace.e("PDF conversion failed reason=${failure.reason} path=${failure.filePath}", failure)
                withContext(Dispatchers.Main) { onError(failure) }
            }
        }
    }

    private fun deleteExcelPdfFile(file: File) {
        if (file.exists() && !file.delete()) {
            Log.e(TAG, "Could not delete incomplete Excel PDF: $file")
        }
    }

    private suspend fun createPdfFromExcel(
        sheetView: SheetView,
        sheet: com.wxiwei.office.ss.model.baseModel.Sheet,
        outputFile: File
    ) {
        val startedAt = SystemClock.elapsedRealtime()
        var lastColumn = 0
        var totalHeight = 0.0
        for (index in 0..sheet.getLastRowNum()) {
            currentCoroutineContext().ensureActive()
            val row = sheet.getRow(index) ?: continue
            lastColumn = maxOf(lastColumn, row.getLastCol())
            totalHeight += row.getRowPixelHeight()
        }
        var totalWidth = 0.0
        // Preserve the old thumbnail's trailing column allowance.
        for (column in 0..lastColumn + 1) {
            totalWidth += sheet.getColumnPixelWidth(column)
        }
        require(totalWidth > 0 && totalWidth < Int.MAX_VALUE &&
            totalHeight > 0 && totalHeight < Int.MAX_VALUE) { "Invalid sheet bounds" }
        val width = totalWidth.toInt().coerceAtLeast(1)
        val height = totalHeight.toInt().coerceAtLeast(1)
        val contentHeightPx = (842 * 1.55f).roundToInt() - 20 * 2
        val headerHeight = SSConstant.DEFAULT_COLUMN_HEADER_HEIGHT
        val bodyHeightPx = contentHeightPx - headerHeight
        val tempFile = File.createTempFile("xlsx-pdf-", ".tmp", outputFile.absoluteFile.parentFile)
        var pageCount = 0
        try {
            val pdf = PdfDocument()
            try {
                var top = 0
                var pageNumber = 1
                while (top < height) {
                    currentCoroutineContext().ensureActive()
                    val bodyHeight = minOf(bodyHeightPx, height - top)
                    val regionHeight = bodyHeight + headerHeight
                    val scale = minOf(555f / width, 802f / regionHeight)
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    try {
                        val canvas = page.canvas
                        val saved = canvas.save()
                        try {
                            canvas.translate(
                                20f + (555f - width * scale) / 2f,
                                20f + (802f - regionHeight * scale) / 2f
                            )
                            canvas.scale(scale, scale)
                            // getClipBounds() is local to the current matrix: headers and
                            // rows see sheet pixels, not the 595 x 842 PDF page bounds.
                            canvas.clipRect(0, 0, width, regionHeight)
                            sheetView.drawRegion(sheet, 0, top, 1f, canvas)
                        } finally {
                            canvas.restoreToCount(saved)
                        }
                        currentCoroutineContext().ensureActive()
                    } finally {
                        pdf.finishPage(page)
                    }
                    pageCount++
                    top += bodyHeight
                    pageNumber++
                    // No SheetView monitor is held while yielding or writing the PDF.
                    yield()
                }
                currentCoroutineContext().ensureActive()
                FileOutputStream(tempFile).use { pdf.writeTo(it) }
            } finally {
                pdf.close()
            }
            currentCoroutineContext().ensureActive()
            check(tempFile.renameTo(outputFile)) { "Could not publish Excel PDF" }
            Log.d(TAG, "Excel PDF complete: pages=$pageCount, elapsedMs=${SystemClock.elapsedRealtime() - startedAt}")
        } finally {
            deleteExcelPdfFile(tempFile)
        }
    }

    private fun appendPdfPage(
        pdf: PdfDocument, bitmap: Bitmap, pageNumber: Int,
        pageWidthPts: Int, pageHeightPts: Int, marginPts: Int
    ) {
        val contentW = pageWidthPts - marginPts * 2
        val contentH = pageHeightPts - marginPts * 2
        val scale = minOf(contentW.toFloat() / bitmap.width, contentH.toFloat() / bitmap.height)
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidthPts, pageHeightPts, pageNumber).create())
        try {
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(
                    marginPts + (contentW - bitmap.width * scale) / 2f,
                    marginPts + (contentH - bitmap.height * scale) / 2f
                )
            }
            page.canvas.drawBitmap(bitmap, matrix, null)
        } finally {
            pdf.finishPage(page)
        }
    }

    private fun createPdfFromBitmaps(
        bitmaps: List<Bitmap?>,
        outputFile: File,
        pageWidthPts: Int = 595,
        pageHeightPts: Int = 842,
        marginPts: Int = 20
    ) {
        Log.d(TAG, "createPdfFromBitmaps: start, pages=${bitmaps.size}, file=$outputFile")
        val pdf = PdfDocument()
        try {
            bitmaps.forEachIndexed { index, bitmap ->
                bitmap?.let {
                    appendPdfPage(pdf, it, index + 1, pageWidthPts, pageHeightPts, marginPts)
                    Log.d(TAG, " page#${index + 1} done")
                }
            }

            FileOutputStream(outputFile).use { out ->
                Log.d(TAG, "createPdf: writing to disk")
                pdf.writeTo(out)
            }
            Log.d(TAG, "createPdf: write complete")
        } catch (e: IOException) {
            Log.e(TAG, "createPdf: ERROR", e)
            throw e
        } finally {
            pdf.close()
            Log.d(TAG, "createPdf: PDF closed")
        }
    }
}
