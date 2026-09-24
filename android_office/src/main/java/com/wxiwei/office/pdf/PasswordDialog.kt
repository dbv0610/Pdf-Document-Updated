/*
 * 文件名称:          PasswordDialog.java
 *
 * 编译器:            android2.2
 * 时间:              下午8:17:29
 */
package com.wxiwei.office.pdf

import com.wxiwei.office.system.*

import android.app.AlertDialog
import android.content.DialogInterface
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.wxiwei.office.common.ICustomDialog
import com.wxiwei.office.constant.EventConstant
import com.wxiwei.office.fc.pdf.PDFLib
import com.wxiwei.office.res.ResKit
import com.wxiwei.office.system.IControl

/**
 *
 */
class PasswordDialog(private val control: IControl, private val lib: PDFLib) {

    private var alertBuilder: AlertDialog.Builder? = null

    private var pwView: EditText? = null

    fun show() {
        if (control.mainFrame.isShowPasswordDlg) {
            alertBuilder = AlertDialog.Builder(control.activity)
            requestPassword(lib)
        } else {
            val dlgListener = control.customDialog
            dlgListener?.showDialog(ICustomDialog.DIALOGTYPE_PASSWORD)
        }
    }

    private fun requestPassword(lib: PDFLib) {
        val pwView = EditText(control.activity)
        this.pwView = pwView
        pwView.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        pwView.transformationMethod = PasswordTransformationMethod()

        val alert = alertBuilder!!.create()
        alert.setTitle(ResKit.instance().getLocalString("DIALOG_ENTER_PASSWORD"))
        alert.setView(pwView)
        alert.setButton(
            AlertDialog.BUTTON_POSITIVE,
            ResKit.instance().getLocalString("BUTTON_OK"),
            DialogInterface.OnClickListener { dialog, which ->
                if (lib.authenticatePasswordSync(pwView.text.toString())) {
                    control.actionEvent(EventConstant.APP_PASSWORD_OK_INIT, null)
                    return@OnClickListener
                } else {
                    requestPassword(lib)
                }
            })
        alert.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            ResKit.instance().getLocalString("BUTTON_CANCEL"),
            DialogInterface.OnClickListener { dialog, which ->
                //lib.dispose();
                control.activity.onBackPressed()
                //control.getMainFrame().destroyEngine();
            })

        alert.setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dialog.dismiss()
                control.activity.onBackPressed()
                //control.getMainFrame().destroyEngine();
                return@OnKeyListener true
            }
            false
        })
        alert.show()
    }
}
