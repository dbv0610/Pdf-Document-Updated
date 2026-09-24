package com.wxiwei.office.system

interface OnOpenFileListener {
    fun onOpenFileSuccess()
    /** Return true to suppress the library error dialog. Called on the main thread. */
    fun onOpenFileFailure(error: OpenFileException): Boolean
}
