package com.wxiwei.office.wp.view

import com.wxiwei.office.system.*

import android.util.Log
import com.wxiwei.office.simpletext.control.IWord
import com.wxiwei.office.simpletext.view.IRoot
import com.wxiwei.office.simpletext.view.IView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LayoutThread(private var root: IRoot?) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var layoutJob: Job? = null

    @Synchronized
    fun start() {
        if (layoutJob?.isActive == true) {
            Log.d("OfficePageLayout", "layout coroutine already active")
            return
        }
        layoutJob = scope.launch {
            try {
                while (isActive) {
                    val currentRoot = root ?: break
                    if (currentRoot.canBackLayout()) {
                        Log.d("OfficePageLayout", "layout coroutine backLayout")
                        currentRoot.backLayout()
                        delay(50L)
                    } else {
                        delay(1000L)
                    }
                }
            } catch (e: CancellationException) {
                Log.d("OfficePageLayout", "layout coroutine cancelled")
                throw e
            } catch (e: Exception) {
                val word = (root as? IView)?.getContainer()
                val control = word?.getControl()
                if (word != null && control != null) {
                    control.getSysKit().getErrorKit().writerLog(e)
                }
            }
        }
    }

    @Synchronized
    fun setDied(isDied: Boolean) {
        if (isDied) dispose()
    }

    @Synchronized
    fun dispose() {
        Log.d("OfficePageLayout", "dispose layout coroutine")
        layoutJob?.cancel()
        layoutJob = null
        scope.cancel()
        root = null
    }
}
