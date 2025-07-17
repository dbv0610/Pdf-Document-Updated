package com.dong.baselib.lifecycle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class Asynchronous<T> {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    protected abstract suspend fun doInBackground(): T
    protected open fun onPreExecute() {}
    protected open fun onPostExecute(result: T) {}
    protected open fun onProgressUpdate(progress: Int) {}
    protected open fun onFailure(throwable: Throwable) {}

    fun execute() {
        onPreExecute()
        coroutineScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { doInBackground() }
                withContext(Dispatchers.Main) {
                    onPostExecute(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }

    fun publishProgress(progress: Int) {
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                onProgressUpdate(progress)
            }
        }
    }

    fun cancel() {
        coroutineScope.cancel()
    }
}


fun <T> CoroutineScope.executeAsync(
    onPreExecute: suspend () -> Unit, doInBackground: suspend () -> T, onPostExecute: (T) -> Unit,
) = launch {
    onPreExecute()
    val result = withContext(Dispatchers.IO) {
        doInBackground()
    }
    onPostExecute(result)
}