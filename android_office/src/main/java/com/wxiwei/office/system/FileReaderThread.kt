package com.wxiwei.office.system

import android.os.Handler
import android.util.Log
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.fc.doc.DOCReader
import com.wxiwei.office.fc.doc.DOCXReader
import com.wxiwei.office.fc.doc.TXTReader
import com.wxiwei.office.fc.pdf.PDFReader
import com.wxiwei.office.fc.ppt.PPTReader
import com.wxiwei.office.fc.ppt.PPTXReader
import com.wxiwei.office.fc.xls.XLSReader
import com.wxiwei.office.fc.xls.XLSXReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Coroutine replacement for the legacy blocking FileReaderThread. */
class FileReaderThread @JvmOverloads constructor(
    private var control: IControl?,
    private var handler: Handler?,
    private var filePath: String?,
    private var encoding: String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var readJob: Job? = null
    private var activeReader: IReader? = null

    /** Kept for compatibility with existing MainControl/TXTKit call sites. */
    @Synchronized
    fun start() {
        val path = filePath
        val currentControl = control
        val currentHandler = handler
        if (path == null || path.isBlank() || currentControl == null || currentHandler == null) {
            OpenTrace.e("reader start rejected path=$path control=${currentControl != null} handler=${currentHandler != null}")
            return
        }
        cancel()
        OpenTrace.d("reader pipeline started path=$path thread=${Thread.currentThread().name}")
        readJob = scope.launch {
            val start = android.os.SystemClock.uptimeMillis()
            OpenTrace.mark("reader.begin path=$path")
            send(currentHandler, MainConstant.HANDLER_MESSAGE_SHOW_PROGRESS)
            try {
                val model = withContext(ioDispatcher) {
                    ensureActive()
                    val reader = createReader(currentControl, path, encoding)
                    activeReader = reader
                    OpenTrace.d("reader picked=${reader.javaClass.name} path=$path thread=${Thread.currentThread().name}")
                    send(currentHandler, MainConstant.HANDLER_MESSAGE_SEND_READER_INSTANCE, reader)
                    ensureActive()
                    OpenTrace.d("getModel started reader=${reader.javaClass.name} path=$path")
                    reader.getModel().also {
                        if (it == null) {
                            OpenTrace.e("read succeeded with NULL model reader=${reader.javaClass.name} path=$path")
                        } else {
                            OpenTrace.d("read succeeded model=${it.javaClass.name} reader=${reader.javaClass.name} path=$path")
                        }
                        OpenTrace.d("getModel finished reader=${reader.javaClass.name} path=$path")
                    }
                }
                send(currentHandler, MainConstant.HANDLER_MESSAGE_SUCCESS, model)
                OpenTrace.mark("reader.success path=$path", start)
            } catch (cancelled: CancellationException) {
                OpenTrace.d("read cancelled path=$path")
                activeReader?.abortReader()
                send(currentHandler, MainConstant.HANDLER_MESSAGE_DISMISS_PROGRESS)
            } catch (error: OutOfMemoryError) {
                OpenTrace.e("read failed with OOM path=$path", error)
                send(currentHandler, MainConstant.HANDLER_MESSAGE_ERROR, error)
            } catch (error: Throwable) {
                OpenTrace.e("read failed path=$path", error)
                send(currentHandler, MainConstant.HANDLER_MESSAGE_ERROR, error)
            } finally {
                OpenTrace.mark("reader.finally path=$path", start)
                activeReader?.dispose()
                activeReader = null
            }
        }
    }

    @Synchronized
    fun cancel() {
        activeReader?.abortReader()
        readJob?.cancel()
        readJob = null
    }

    @Synchronized
    fun dispose() {
        cancel()
        scope.cancel()
        control = null
        handler = null
        filePath = null
        encoding = null
    }

    private suspend fun send(handler: Handler, what: Int, obj: Any? = null) {
        withContext(Dispatchers.Main.immediate) {
            // Build the message explicitly. This keeps the model payload intact across
            // the coroutine -> Main handoff, including nullable/object overloads.
            val message = handler.obtainMessage(what)
            message.obj = obj
            message.sendToTarget()
        }
    }

    private fun createReader(control: IControl, path: String, encoding: String?): IReader {
        val name = path.lowercase()
        return when {
            name.endsWith(MainConstant.FILE_TYPE_DOC) || name.endsWith(MainConstant.FILE_TYPE_DOT) -> DOCReader(control, path)
            name.endsWith(MainConstant.FILE_TYPE_DOCX) || name.endsWith(MainConstant.FILE_TYPE_DOTX) || name.endsWith(MainConstant.FILE_TYPE_DOTM) -> DOCXReader(control, path)
            name.endsWith(MainConstant.FILE_TYPE_TXT) -> TXTReader(control, path, encoding)
            name.endsWith(MainConstant.FILE_TYPE_XLS) || name.endsWith(MainConstant.FILE_TYPE_XLT) -> XLSReader(control, path)
            name.endsWith(MainConstant.FILE_TYPE_XLSX) || name.endsWith(MainConstant.FILE_TYPE_XLTX) || name.endsWith(MainConstant.FILE_TYPE_XLTM) || name.endsWith(MainConstant.FILE_TYPE_XLSM) -> XLSXReader(control, path)
            name.endsWith(MainConstant.FILE_TYPE_PPT) || name.endsWith(MainConstant.FILE_TYPE_POT) -> PPTReader(control, path)
            name.endsWith(MainConstant.FILE_TYPE_PPTX) || name.endsWith(MainConstant.FILE_TYPE_PPTM) || name.endsWith(MainConstant.FILE_TYPE_POTX) || name.endsWith(MainConstant.FILE_TYPE_POTM) -> PPTXReader(control, path)
            name.endsWith(MainConstant.FILE_TYPE_PDF) -> PDFReader(control, path)
            else -> TXTReader(control, path, encoding)
        }
    }

    private companion object { const val TAG = "OfficeFileReader" }
}
