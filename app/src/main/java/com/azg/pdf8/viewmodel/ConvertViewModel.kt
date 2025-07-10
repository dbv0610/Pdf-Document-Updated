package com.azg.pdf8.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azg.pdf8.model.DocumentPage
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

class ConvertViewModel : ViewModel() {
    private val TAG = "SlideShowViewModel"
    private val _listSlide = MutableStateFlow<List<DocumentPage>>(listOf())
    val listSlide = _listSlide.asStateFlow()

    fun initSlideShow(presentation: Presentation) {
        val listSlide = mutableListOf<DocumentPage>()
        val runtime = Runtime.getRuntime()
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
            Log.e(
                TAG,
                "Memory usage after batch ${batchStart / batchSize}: ${currentMemory / 1024 / 1024}MB"
            )
            // Update UI with current progress
            if (listSlide.isNotEmpty()) {
                _listSlide.update { listSlide.toList() }
            }
        }
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
        onBatchReady: (List<DocumentPage>) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val runtime = Runtime.getRuntime()
            val initialMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            Log.e(TAG, "initDocSlide: Initial memory usage: ${initialMem}MB")

            val totalPages = view.pageCount
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
                                val scaled = raw.scale(raw.width / scaleFactor, raw.height / scaleFactor)
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
                    onBatchReady(accumulator.toList())
                }
                val midMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                Log.e(TAG, "initDocSlide: memory after batch ${batchStart / batchSize}: ${midMem}MB")
            }
            val finalMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            Log.e(TAG, "initDocSlide: Final memory usage: ${finalMem}MB")
            Log.e(TAG, "initDocSlide: pages processed: $processed, successful: $successCount")
        }
    }
//    fun initDocSlide(view: Word) {
//        val runtime = Runtime.getRuntime()
//        // Prepare output list
//        val listBitmaps = mutableListOf<DocumentPage>()
//        // Log initial memory
//        val initialMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
//        Log.e(TAG, "initDocSlide: Initial memory usage: ${initialMem}MB")
//        val total = view.pageCount
//
//
//        Log.i(TAG, "initDocSlide: total pages to process: $total")
//
//        for (i in 0..total) {
//            try {
//                // Render view to bitmap
//                val rawBmp = view.pageToImage(i)
//
//                if (rawBmp != null && !rawBmp.isRecycled) {
//                    try {
//                        // Scale it down
//                        val scaled = rawBmp.scale(rawBmp.width / 2, rawBmp.height / 2)
//                        if (scaled != null && !scaled.isRecycled) {
//                            listBitmaps += DocumentPage(i, bitmap = scaled)
//                        }
//                        // Recycle the full-size bitmap
//                        rawBmp.recycle()
//                    } catch (oom: OutOfMemoryError) {
//                        Log.e(TAG, "initDocSlide: OOM at page $i, msg=${oom.message}")
//                        rawBmp.recycle()
//                        System.gc()
//                        Thread.sleep(200)
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "initDocSlide: error at page $i, msg=${e.message}")
//            }
//        }
//        // Final summary
//        val finalMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
//        Log.e(TAG, "initDocSlide: Final memory usage: ${finalMem}MB")
//        Log.e(TAG, "initDocSlide: list size=${listBitmaps.size}")
//    }
    fun initXlsPages(view: ExcelView, callback: (Bitmap?) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            val thumbBmp = try {
                val sheet = view.sheetView.spreadsheet.sheetView.currentSheet
                val activeColumn = sheet.getRow(0).lastCol
                val activeRow = sheet.physicalNumberOfRows
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
}
