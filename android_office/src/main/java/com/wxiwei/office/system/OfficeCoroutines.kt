package com.wxiwei.office.system

import android.util.Log
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
        fun onFailure(error: OpenFileException)
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
                if (model == null) throw IllegalStateException("Document with password")
                callback.onSuccess(model)
            } catch (cancelled: CancellationException) {
                OpenTrace.d("alternate loader cancelled path=$filePath")
                activeReader?.abortReader()
                callback.onLoading(false)
            } catch (error: Throwable) {
                if (OpenFileErrors.isCancellation(error)) {
                    callback.onLoading(false)
                    return@launch
                }
                OpenTrace.e("alternate loader read failed path=$filePath", error)
                callback.onLoading(false)
                callback.onFailure(OpenFileErrors.wrap(error, filePath))
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
        return OfficeReaderFactory.createReader(control, filePath, encoding)
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
