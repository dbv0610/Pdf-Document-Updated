package com.azg.pdf8.ui.main.document.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class PdfThumbnailLoader(
    private val context: Context,
    private val uri: Uri,
    private val thumbnailWidth: Int = 200
) {
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val closed = AtomicBoolean(false)
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var unavailable = false
    private var renderedPages = 0
    private val cache = object : LruCache<Int, Bitmap>(
        minOf(Runtime.getRuntime().maxMemory() / 8, 32L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun pageCount(): Int = withContext(dispatcher) {
        currentCoroutineContext().ensureActive()
        openRenderer()?.pageCount ?: 0
    }

    suspend fun thumbnail(index: Int): Bitmap? = withContext(dispatcher) {
        currentCoroutineContext().ensureActive()
        if (closed.get()) return@withContext null
        cache.get(index)?.let { return@withContext it }
        val pdf = openRenderer() ?: return@withContext null
        if (index !in 0 until pdf.pageCount) return@withContext null
        try {
            currentCoroutineContext().ensureActive()
            val bitmap = pdf.openPage(index).use { page ->
                renderedPages++
                val width = thumbnailWidth.coerceAtLeast(1)
                val height = (width * (page.height.toFloat() / page.width)).toInt()
                    .coerceAtLeast(1)
                currentCoroutineContext().ensureActive()
                try {
                    render(page, width, height, Bitmap.Config.ARGB_8888)
                } catch (_: OutOfMemoryError) {
                    currentCoroutineContext().ensureActive()
                    render(
                        page,
                        (width / 2).coerceAtLeast(1),
                        (height / 2).coerceAtLeast(1),
                        Bitmap.Config.RGB_565
                    )
                }
            }
            currentCoroutineContext().ensureActive()
            if (closed.get()) return@withContext null
            cache.put(index, bitmap)
            bitmap
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            // Pdfium retains page resources until its document is closed.
            if (renderedPages >= 200) {
                closeRenderer()
                if (!closed.get()) openRenderer()
            }
        }
    }

    private fun render(
        page: PdfRenderer.Page,
        width: Int,
        height: Int,
        config: Bitmap.Config
    ): Bitmap {
        return createBitmap(width, height, config).also { bitmap ->
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }

    private fun openRenderer(): PdfRenderer? {
        if (closed.get() || unavailable) return null
        renderer?.let { return it }
        return try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            if (fd == null) {
                unavailable = true
                return null
            }
            descriptor = fd
            PdfRenderer(fd).also { renderer = it }
        } catch (_: Exception) {
            unavailable = true
            closeRenderer()
            null
        }
    }

    private fun closeRenderer() {
        runCatching { renderer?.close() }
        renderer = null
        runCatching { descriptor?.close() }
        descriptor = null
        renderedPages = 0
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        // Cleanup must outlive the ViewModel's cancelled scope and queued renders.
        CoroutineScope(dispatcher).launch {
            try {
                closeRenderer()
            } finally {
                cache.evictAll()
            }
        }
    }
}
