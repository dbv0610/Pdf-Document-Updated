package com.wxiwei.office.system.beans

import com.wxiwei.office.system.ITimerListener
import java.util.Timer
import java.util.TimerTask

class ATimer(private var delay: Int, private var listener: ITimerListener?) {
    @JvmField
    var isRunning = false
    private var timer: Timer? = null

    fun start() {
        if (isRunning) {
            return
        }
        timer = Timer()
        timer?.schedule(ATimerTask(), delay.toLong())
        isRunning = true
    }

    fun isRunning(): Boolean {
        return isRunning
    }

    fun stop() {
        if (isRunning) {
            timer?.cancel()
            timer?.purge()
            isRunning = false
        }
    }

    fun restart() {
        stop()
        start()
    }

    private inner class ATimerTask : TimerTask() {
        override fun run() {
            try {
                timer?.schedule(ATimerTask(), delay.toLong())
                listener?.actionPerformed()
            } catch (_: Exception) {
            }
        }
    }

    fun dispose() {
        if (isRunning) {
            timer?.cancel()
            timer?.purge()
            isRunning = false
        }
        timer = null
        listener = null
    }
}
