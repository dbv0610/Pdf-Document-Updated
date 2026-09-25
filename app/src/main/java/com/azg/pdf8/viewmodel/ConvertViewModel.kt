package com.azg.pdf8.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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
import com.wxiwei.office.ss.model.baseModel.Cell
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
                                    val slide = DocumentPage(i, bitmap = scaledBitmap)
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
                for (i in 0 until totalPages) {
                    try {
                        val raw = view.pageToImage(i + 1)
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
                            val raw = view.pageToImage(i + 1)
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
        excelSheetView: SheetView? = null,
        wordView: Word? = null,
        presentation: Presentation? = null,
        onProgress: (page: Int, total: Int) -> Unit = { _, _ -> }
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
                    DocumentType.Doc, DocumentType.Ppt, DocumentType.Txt -> {
                        // Vector pages stay sharp; the halved preview bitmaps are the fallback.
                        val vectorDone = if (model.type == DocumentType.Ppt) {
                            presentation != null && createPdfFromSlides(presentation, outputFile)
                        } else {
                            wordView != null && createPdfFromWord(wordView, outputFile)
                        }
                        if (!vectorDone) {
                            val bitmaps = listSlide.value.map { it.bitmap }
                            check(bitmaps.any { it != null }) { "No rendered pages to convert" }
                            createPdfFromBitmaps(bitmaps, outputFile)
                        }
                    }
                    DocumentType.Excel -> {
                        try {
                            checkNotNull(excelSheetView) { "No Excel view" }
                            checkNotNull(excelSheet) { "No selected sheet" }
                            check(withTimeoutOrNull(30_000L) {
                                while (!excelSheet.isAccomplished()) delay(100)
                                true
                            } == true) { "Timed out waiting for Excel sheet" }
                            createPdfFromExcel(excelSheetView, excelSheet, outputFile) { page, total ->
                                viewModelScope.launch(Dispatchers.Main) { onProgress(page, total) }
                            }
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
        outputFile: File,
        onProgress: (page: Int, total: Int) -> Unit
    ) {
        val startedAt = SystemClock.elapsedRealtime()
        // Bound the export by cells that hold data (plus merges and shapes), not by
        // row.lastCol, which also counts styled-but-empty cells and pads the PDF
        // with blank columns and rows.
        var lastColumn = -1
        var lastRow = -1
        for (index in 0..sheet.getLastRowNum()) {
            currentCoroutineContext().ensureActive()
            val row = sheet.getRow(index) ?: continue
            for (cell in row.cellCollection()) {
                if (cell.getCellType() == Cell.CELL_TYPE_BLANK) continue
                lastColumn = maxOf(lastColumn, cell.getColNumber())
                lastRow = maxOf(lastRow, index)
            }
        }
        for (index in 0 until sheet.getMergeRangeCount()) {
            val range = sheet.getMergeRange(index) ?: continue
            val anchor = sheet.getRow(range.getFirstRow())?.getCell(range.getFirstColumn(), false)
            if (anchor == null || anchor.getCellType() == Cell.CELL_TYPE_BLANK) continue
            lastColumn = maxOf(lastColumn, range.getLastColumn())
            lastRow = maxOf(lastRow, range.getLastRow())
        }
        var shapeRight = 0
        var shapeBottom = 0
        for (index in 0 until sheet.getShapeCount()) {
            val bounds = sheet.getShape(index)?.getBounds() ?: continue
            shapeRight = maxOf(shapeRight, bounds.x + bounds.width)
            shapeBottom = maxOf(shapeBottom, bounds.y + bounds.height)
        }
        var totalHeight = 0.0
        for (index in 0..lastRow) {
            totalHeight += sheet.getRow(index)?.getRowPixelHeight() ?: sheet.getDefaultRowHeight().toFloat()
        }
        var totalWidth = 0.0
        for (column in 0..lastColumn) {
            totalWidth += sheet.getColumnPixelWidth(column)
        }
        Log.d(TAG, "Excel bounds: lastRowNum=${sheet.getLastRowNum()} lastRow=$lastRow lastColumn=$lastColumn " +
            "cellsH=$totalHeight shapeBottom=$shapeBottom shapeRight=$shapeRight merges=${sheet.getMergeRangeCount()}")
        totalWidth = maxOf(totalWidth, shapeRight.toDouble())
        totalHeight = maxOf(totalHeight, shapeBottom.toDouble())
        require(totalWidth > 0 && totalWidth < Int.MAX_VALUE &&
            totalHeight > 0 && totalHeight < Int.MAX_VALUE) { "Sheet has no data" }
        // Cells are drawn right of the row-number header, which RowHeader sizes from
        // the widest row number (text + 10px padding, min DEFAULT_ROW_HEADER_WIDTH).
        val rowHeaderWidth = maxOf(
            SSConstant.DEFAULT_ROW_HEADER_WIDTH,
            Paint().apply { textSize = SSConstant.HEADER_TEXT_FONTSZIE.toFloat() }
                .measureText((lastRow + 1).toString()).roundToInt() + 10
        )
        val width = (totalWidth + rowHeaderWidth).toInt().coerceAtLeast(1)
        val height = totalHeight.toInt().coerceAtLeast(1)
        val headerHeight = SSConstant.DEFAULT_COLUMN_HEADER_HEIGHT
        // Fit the sheet width to the page, then fill the whole page height at that
        // scale; narrow sheets are capped at the old ~1.55 px/pt density.
        val scale = minOf(555f / width, 802f / ((842 * 1.55f).roundToInt() - 20 * 2))
        val bodyHeightPx = ((802f / scale).toInt() - headerHeight).coerceAtLeast(1)
        val drawStart = SystemClock.elapsedRealtime()
        val tempFile = File.createTempFile("xlsx-pdf-", ".tmp", outputFile.absoluteFile.parentFile)
        var pageCount = 0
        try {
            val pdf = PdfDocument()
            try {
                val pageTotal = (height + bodyHeightPx - 1) / bodyHeightPx
                val left = 20f + (555f - width * scale) / 2f
                var drawMs = 0L
                // Draws one sheet band; getClipBounds() is local to the current matrix, so
                // headers and rows see sheet pixels, not the 595 x 842 PDF page bounds.
                fun drawBand(canvas: Canvas, top: Int, regionHeight: Int) {
                    canvas.clipRect(0, 0, width, regionHeight)
                    sheetView.drawRegion(sheet, 0, top, 1f, canvas)
                }
                fun startPage(pageNumber: Int): PdfDocument.Page =
                    pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()).also {
                        it.canvas.translate(left, 20f)
                        it.canvas.scale(scale, scale)
                    }
                var top = 0
                while (top < height) {
                    currentCoroutineContext().ensureActive()
                    val pageStart = SystemClock.elapsedRealtime()
                    val bodyHeight = minOf(bodyHeightPx, height - top)
                    val page = startPage(pageCount + 1)
                    try {
                        drawBand(page.canvas, top, bodyHeight + headerHeight)
                    } finally {
                        pdf.finishPage(page)
                    }
                    pageCount++
                    drawMs += SystemClock.elapsedRealtime() - pageStart
                    onProgress(pageCount, pageTotal)
                    top += bodyHeight
                    // No SheetView monitor is held while yielding or writing the PDF.
                    yield()
                }
                currentCoroutineContext().ensureActive()
                val writeStart = SystemClock.elapsedRealtime()
                FileOutputStream(tempFile).use { pdf.writeTo(it) }
                Log.d(TAG, "Excel PDF timing: boundsMs=${drawStart - startedAt}, drawMs=$drawMs, " +
                    "writeMs=${SystemClock.elapsedRealtime() - writeStart}, size=${width}x$height")
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

    /**
     * Draws each Word page as vector content, keeping the document's own page size
     * (layout px are 96 dpi, PDF points are 72 dpi). Returns false to fall back to bitmaps.
     */
    private suspend fun createPdfFromWord(word: Word, outputFile: File): Boolean {
        val pageCount = word.getPageCount()
        if (pageCount <= 0) return false
        val pdf = PdfDocument()
        try {
            var written = 0
            for (pageNumber in 1..pageCount) {
                currentCoroutineContext().ensureActive()
                val size = word.getPageBounds(pageNumber) ?: continue
                val pxToPt = 72f / 96f
                val info = PdfDocument.PageInfo.Builder(
                    (size.width() * pxToPt).roundToInt().coerceAtLeast(1),
                    (size.height() * pxToPt).roundToInt().coerceAtLeast(1),
                    written + 1
                ).create()
                val page = pdf.startPage(info)
                try {
                    page.canvas.scale(pxToPt, pxToPt)
                    if (word.drawPage(pageNumber, page.canvas)) written++
                } finally {
                    pdf.finishPage(page)
                }
            }
            if (written == 0) return false
            FileOutputStream(outputFile).use { pdf.writeTo(it) }
            Log.d(TAG, "Word PDF complete: pages=$written")
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "createPdfFromWord failed, falling back to bitmaps", e)
            return false
        } finally {
            pdf.close()
        }
    }

    /** Draws each slide as vector content at the deck's own slide size. */
    private suspend fun createPdfFromSlides(presentation: Presentation, outputFile: File): Boolean {
        val slideCount = presentation.getRealSlideCount()
        val size = presentation.getPageSize() ?: return false
        if (slideCount <= 0 || size.width <= 0 || size.height <= 0) return false
        val pxToPt = 72f / 96f
        val pdf = PdfDocument()
        try {
            var written = 0
            for (slideNumber in 1..slideCount) {
                currentCoroutineContext().ensureActive()
                val info = PdfDocument.PageInfo.Builder(
                    (size.width * pxToPt).roundToInt().coerceAtLeast(1),
                    (size.height * pxToPt).roundToInt().coerceAtLeast(1),
                    written + 1
                ).create()
                val page = pdf.startPage(info)
                try {
                    page.canvas.scale(pxToPt, pxToPt)
                    if (presentation.drawSlide(slideNumber, page.canvas)) written++
                } finally {
                    pdf.finishPage(page)
                }
            }
            if (written == 0) return false
            FileOutputStream(outputFile).use { pdf.writeTo(it) }
            Log.d(TAG, "Slides PDF complete: pages=$written")
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "createPdfFromSlides failed, falling back to bitmaps", e)
            return false
        } finally {
            pdf.close()
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
