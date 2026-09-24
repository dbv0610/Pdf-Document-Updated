/*
 * 文件名称:          SafeAsyncTask.java
 *
 * 编译器:            android2.2
 * 时间:              下午9:36:57
 */
package com.wxiwei.office.simpletext.control

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * PDF document rending
 *
 * Read版本:        Read V1.0
 *
 * 作者:            ljj8494
 *
 * 日期:            2012-9-19
 *
 * 负责人:          ljj8494
 */
/** Coroutine replacement for the legacy Android AsyncTask API used by PDF rendering. */
abstract class SafeAsyncTask<Params, Progress, Result> {
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null
    private val cancelledCallbackSent = AtomicBoolean(false)

    protected abstract fun doInBackground(vararg params: Params): Result

    protected open fun onPreExecute() {}
    protected open fun onProgressUpdate(vararg values: Progress?) {}
    protected open fun onPostExecute(result: Result) {}
    protected open fun onCancelled() {}

    fun safeExecute(vararg params: Params) {
        job?.cancel()
        cancelledCallbackSent.set(false)
        job = scope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main.immediate) {
                if (isActive) onPreExecute()
            }
            try {
                val result = doInBackground(*params)
                if (isActive) {
                    withContext(Dispatchers.Main.immediate) {
                        if (isActive) onPostExecute(result)
                    }
                }
            } catch (_: CancellationException) {
                dispatchCancelled()
            }
        }
    }

    fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        val wasActive = job?.isActive == true
        job?.cancel()
        dispatchCancelled()
        return wasActive
    }

    fun cancel() {
        cancel(true)
    }

    fun isCancelled(): Boolean = job?.isCancelled == true

    protected fun publishProgress(vararg values: Progress?) {
        scope.launch(Dispatchers.Main.immediate) {
            if (!isCancelled()) onProgressUpdate(*values)
        }
    }

    private fun dispatchCancelled() {
        if (cancelledCallbackSent.compareAndSet(false, true)) {
            scope.launch(Dispatchers.Main.immediate) { onCancelled() }
        }
    }

    fun dispose() {
        cancel()
        scope.cancel()
    }
}
