package com.wxiwei.office.system

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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coroutine boundary for the legacy Java readers.
 * Parsing is kept off the main thread; only lifecycle callbacks are delivered on Main.
 */
class OfficeFileLoader @JvmOverloads constructor(
    private val control: IControl,
    private val callback: Callback,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    interface Callback {
        fun onLoading(loading: Boolean)
        fun onReaderCreated(reader: IReader)
        fun onSuccess(model: Any?)
        fun onFailure(error: Throwable)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var activeReader: IReader? = null

    fun start(filePath: String, encoding: String? = null) {
        OpenTrace.d("alternate loader started path=$filePath encoding=$encoding")
        cancel()
        job = scope.launch {
            callback.onLoading(true)
            try {
                val model = withContext(ioDispatcher) {
                    val reader = createReader(filePath, encoding)
                    activeReader = reader
                    OpenTrace.d("alternate loader reader picked=${reader.javaClass.name} path=$filePath")
                    withContext(Dispatchers.Main.immediate) {
                        OpenTrace.d("alternate loader reader delivered main=${Thread.currentThread().name}")
                        callback.onReaderCreated(reader)
                    }
                    OpenTrace.d("alternate loader getModel started reader=${reader.javaClass.name}")
                    reader.getModel().also {
                        if (it == null) OpenTrace.e("alternate loader returned NULL model reader=${reader.javaClass.name}")
                        else OpenTrace.d("alternate loader read succeeded model=${it.javaClass.name} reader=${reader.javaClass.name}")
                        OpenTrace.d("alternate loader getModel finished reader=${reader.javaClass.name}")
                    }
                }
                // SUCCESS handles dismissing the loading UI. Sending DISMISS first
                // would call MainControl.dismissProgressDialog(), which removes
                // the queued SUCCESS message as well.
                OpenTrace.d("alternate loader read succeeded callback main=${Thread.currentThread().name}")
                callback.onSuccess(model)
            } catch (cancelled: CancellationException) {
                OpenTrace.d("alternate loader cancelled path=$filePath")
                activeReader?.abortReader()
                callback.onLoading(false)
            } catch (error: Throwable) {
                OpenTrace.e("alternate loader read failed path=$filePath", error)
                callback.onLoading(false)
                callback.onFailure(error)
            }
        }
    }

    fun cancel() {
        activeReader?.abortReader()
        job?.cancel()
        job = null
        activeReader = null
    }

    fun dispose() {
        cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun createReader(filePath: String, encoding: String?): IReader {
        val name = filePath.lowercase()
        return when {
            name.endsWith(MainConstant.FILE_TYPE_DOC) || name.endsWith(MainConstant.FILE_TYPE_DOT) ->
                DOCReader(control, filePath)
            name.endsWith(MainConstant.FILE_TYPE_DOCX) || name.endsWith(MainConstant.FILE_TYPE_DOTX) ||
                name.endsWith(MainConstant.FILE_TYPE_DOTM) -> DOCXReader(control, filePath)
            name.endsWith(MainConstant.FILE_TYPE_XLS) || name.endsWith(MainConstant.FILE_TYPE_XLT) ->
                XLSReader(control, filePath)
            name.endsWith(MainConstant.FILE_TYPE_XLSX) || name.endsWith(MainConstant.FILE_TYPE_XLTX) ||
                name.endsWith(MainConstant.FILE_TYPE_XLTM) || name.endsWith(MainConstant.FILE_TYPE_XLSM) ->
                XLSXReader(control, filePath)
            name.endsWith(MainConstant.FILE_TYPE_PPT) || name.endsWith(MainConstant.FILE_TYPE_POT) ->
                PPTReader(control, filePath)
            name.endsWith(MainConstant.FILE_TYPE_PPTX) || name.endsWith(MainConstant.FILE_TYPE_PPTM) ||
                name.endsWith(MainConstant.FILE_TYPE_POTX) || name.endsWith(MainConstant.FILE_TYPE_POTM) ->
                PPTXReader(control, filePath)
            name.endsWith(MainConstant.FILE_TYPE_PDF) -> PDFReader(control, filePath)
            else -> TXTReader(control, filePath, encoding)
        }
    }

    private companion object {
        const val TAG = "OfficeFileLoader"
    }
}

/** Replaces the legacy polling Thread used for incremental slide loading. */
object ReaderCoroutineDispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @JvmStatic
    fun start(reader: IReader, control: IControl): Job = scope.launch {
        control.actionEvent(com.wxiwei.office.constant.EventConstant.SYS_START_BACK_READER_ID, true)
        try {
            while (currentCoroutineContext().isActive && !reader.isReaderFinish()) {
                reader.backReader()
                delay(50)
            }
        } catch (cancelled: CancellationException) {
            reader.abortReader()
        } catch (error: Throwable) {
            if (!reader.isAborted()) {
                control.getSysKit().getErrorKit().writerLog(error, true)
            }
        } finally {
            if (currentCoroutineContext().isActive) {
                control.actionEvent(com.wxiwei.office.constant.EventConstant.SYS_READER_FINSH_ID, true)
            }
        }
    }
}

/** Shared executor for legacy Java callbacks that still need background work. */
object OfficeCoroutineExecutor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @JvmStatic
    fun launch(task: Runnable): Job = scope.launch {
        task.run()
    }

    fun launchSuspend(block: suspend () -> Unit): Job =
        scope.launch { block() }
}
