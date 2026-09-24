package com.wxiwei.office.system

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.util.Log
import com.wxiwei.office.common.ICustomDialog
import com.wxiwei.office.constant.EventConstant
import com.wxiwei.office.fc.OldFileFormatException
import com.wxiwei.office.fc.poifs.filesystem.OfficeXmlFileException
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Calendar

class ErrorUtil(sysKitValue: SysKit) {
    private var sysKit: SysKit? = sysKitValue
    private var logFile: File? = null
    private var message: AlertDialog? = null

    init {
        if (sysKitValue.getControl().getMainFrame().isWriteLog()) {
            logFile = sysKitValue.getControl().getMainFrame().getTemporaryDirectory()
            val file = logFile
            if (file != null && file.exists() && file.canWrite()) {
                logFile = File(file.absolutePath + File.separatorChar + "ASReader")
            }
            if (logFile != null) {
                if (!logFile!!.exists()) logFile!!.mkdirs()
                logFile = File(logFile!!.absolutePath + File.separatorChar + "errorLog.txt")
            }
        }
    }

    fun writerLog(ex: Throwable) {
        writerLog(ex, false)
    }

    fun writerLog(ex: Throwable, isReaderFile: Boolean) {
        writerLog(ex, isReaderFile, true)
    }

    fun writerLog(exValue: Throwable, isReaderFile: Boolean, isShowErrorDialog: Boolean) {
        var ex = exValue
        try {
            if (ex is AbortReaderError) return
            val file = logFile
            if (file == null) {
                ex = Throwable("SD CARD ERROR")
            } else if (file.exists() && !file.canWrite()) {
                ex = Throwable("Write Permission denied")
            } else if (sysKit!!.getControl().getMainFrame().isWriteLog() && ex !is OutOfMemoryError) {
                val info = FileWriter(file, true)
                val printWriter = PrintWriter(info, true)
                printWriter.println()
                printWriter.println("--------------------------------------------------------------------------")
                printWriter.println("Exception occurs: ${sdf24.format(Calendar.getInstance().time)}  $VERSION")
                ex.printStackTrace(printWriter)
                info.close()
            }
            if (isShowErrorDialog) processThrowable(ex, isReaderFile)
        } catch (e: OutOfMemoryError) {
            sysKit!!.getControl().getMainFrame().getActivity().onBackPressed()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processThrowable(ex: Throwable, isReaderFile: Boolean) {
        val kit = sysKit ?: return
        val control = kit.getControl()
        val activity: Activity = control.getMainFrame().getActivity()
        if (control.isAutoTest()) {
            System.exit(0)
        } else if (message == null) {
            control.getActivity().window.decorView.post {
                try {
                    var err = ""
                    val error = ex.toString()
                    var errorCode = SYSTEM_CRASH
                    when {
                        error.contains("SD") -> { err = control.getMainFrame().getLocalString("SD_CARD") ?: ""; errorCode = SD_CARD_ERROR }
                        error.contains("Write Permission denied") -> { err = control.getMainFrame().getLocalString("SD_CARD_WRITEDENIED") ?: ""; errorCode = SD_CARD_WRITEDENIED }
                        error.contains("No space left on device") -> { err = control.getMainFrame().getLocalString("SD_CARD_NOSPACELEFT") ?: ""; errorCode = SD_CARD_NOSPACELEFT }
                        ex is OutOfMemoryError || error.contains("OutOfMemoryError") -> { err = control.getMainFrame().getLocalString("DIALOG_INSUFFICIENT_MEMORY") ?: ""; errorCode = INSUFFICIENT_MEMORY }
                        error.contains("no such entry") || error.contains("Format error") || error.contains("Unable to read entire header") || ex is OfficeXmlFileException || error.contains("The text piece table is corrupted") || error.contains("Invalid header signature") -> { err = control.getMainFrame().getLocalString("DIALOG_FORMAT_ERROR") ?: ""; errorCode = BAD_FILE }
                        error.contains("The document is really a RTF file") -> { err = control.getMainFrame().getLocalString("DIALOG_RTF_FILE") ?: ""; errorCode = RTF_DOCUMENT }
                        ex is OldFileFormatException -> { err = control.getMainFrame().getLocalString("DIALOG_OLD_DOCUMENT") ?: ""; errorCode = OLD_DOCUMENT }
                        error.contains("Cannot process encrypted office file") -> { err = control.getMainFrame().getLocalString("DIALOG_CANNOT_ENCRYPTED_FILE") ?: ""; errorCode = PASSWORD_DOCUMENT }
                        error.contains("Password is incorrect") -> { err = control.getMainFrame().getLocalString("DIALOG_PASSWORD_INCORRECT") ?: ""; errorCode = PASSWORD_INCORRECT }
                        isReaderFile -> { Log.e("DIALOG_PARSE_ERROR", "DIALOG_PARSE_ERROR"); err = control.getMainFrame().getLocalString("DIALOG_PARSE_ERROR") ?: ""; errorCode = PARSE_ERROR }
                        ex is IllegalArgumentException -> { err = ex.message ?: ""; errorCode = SYSTEM_PERMISSION }
                        ex is NullPointerException || ex is ClassCastException -> { err = control.getMainFrame().getLocalString("DIALOG_SYSTEM_CRASH") ?: ""; errorCode = SYSTEM_CRASH }
                        kit.isDebug() -> err = control.getMainFrame().getLocalString("DIALOG_SYSTEM_CRASH") ?: ""
                    }
                    if (err.isNotEmpty() && errorCode != SYSTEM_PERMISSION) {
                        control.getMainFrame().error(errorCode)
                        control.actionEvent(EventConstant.APP_ABORTREADING, true)
                        if (control.getMainFrame().isPopUpErrorDlg() && message == null) {
                            val builder = AlertDialog.Builder(activity)
                            builder.setMessage(err).setCancelable(false).setTitle(control.getMainFrame().getAppName())
                            val ok = control.getMainFrame().getLocalString("BUTTON_OK")
                            builder.setPositiveButton(ok) { _, _ -> message = null; activity.finish() }
                            message = builder.create()
                            message!!.show()
                        } else {
                            control.getCustomDialog()?.showDialog(ICustomDialog.DIALOGTYPE_ERROR)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun dispose() {
        sysKit = null
    }

    companion object {
        const val INSUFFICIENT_MEMORY = 0
        const val SYSTEM_CRASH = 1
        const val SYSTEM_PERMISSION = 10
        const val BAD_FILE = 2
        const val OLD_DOCUMENT = 3
        const val PARSE_ERROR = 4
        const val RTF_DOCUMENT = 5
        const val PASSWORD_DOCUMENT = 6
        const val PASSWORD_INCORRECT = 7
        const val SD_CARD_ERROR = 8
        const val SD_CARD_WRITEDENIED = 9
        const val SD_CARD_NOSPACELEFT = 10
        private val sdf24 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        private const val VERSION = "2.0.0.4"
    }
}
