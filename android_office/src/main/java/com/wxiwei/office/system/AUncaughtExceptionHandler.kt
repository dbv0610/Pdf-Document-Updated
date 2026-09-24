package com.wxiwei.office.system

class AUncaughtExceptionHandler(private var control: IControl?) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        control?.getSysKit()?.getErrorKit()?.writerLog(ex)
    }

    fun dispose() {
        control = null
    }
}
