package com.azg.pdf8.editlab

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wxiwei.office.constant.wp.WPViewConstant
import com.wxiwei.office.editor.EditResult
import com.wxiwei.office.editor.docx.DocxEditor
import com.wxiwei.office.editor.docx.DocxSourceMap
import com.wxiwei.office.editor.word.WordSelection
import com.wxiwei.office.res.ResKit
import com.wxiwei.office.system.IMainFrame
import com.wxiwei.office.system.MainControl
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener
import com.wxiwei.office.wp.control.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Dev-only DOCX round-trip harness. All queued offsets refer to the currently open original. */
class DocxEditLabActivity : AppCompatActivity(), IMainFrame {
    private lateinit var frame: FrameLayout
    private lateinit var status: TextView
    private lateinit var spinner: Spinner
    private var control: MainControl? = null
    private var selection: WordSelection? = null
    private var editor: DocxEditor? = null
    private var source: File? = null
    private var files = emptyList<File>()
    private var busy = false
    private var anchor: Long? = null
    private var anchorEnd = 0L
    private var queued = 0
    private val buttons = ArrayList<Button>()
    private val documents get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        spinner = Spinner(this); root.addView(spinner)
        val row = LinearLayout(this)
        root.addView(HorizontalScrollView(this).apply { addView(row) })
        // Fixed height: a status that wraps differently would move the document under the finger
        status = TextView(this).apply { setLines(2); ellipsize = android.text.TextUtils.TruncateAt.END }; root.addView(status)
        frame = FrameLayout(this); root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        fun button(name: String, action: () -> Unit) {
            val button = Button(this).apply { text = name; setOnClickListener { if (!busy) action() } }
            row.addView(button); buttons.add(button)
        }
        button("Files / Access") { access() }
        button("Highlight") { edit { e, s, t -> e.highlight(s, t) } }
        button("Bold") { edit { e, s, t -> e.setBold(s, t, true) } }
        button("Italic") { edit { e, s, t -> e.setItalic(s, t, true) } }
        button("Underline") { edit { e, s, t -> e.setUnderline(s, t, true) } }
        button("Red text") { edit { e, s, t -> e.setTextColor(s, t, "FF0000") } }
        button("Insert [chèn]") { edit { e, s, _ -> e.insertText(s, "[chèn]") } }
        button("Delete selection") { edit { e, s, t -> e.deleteText(s, t) } }
        button("Replace") { edit { e, s, t -> e.replaceText(s, t, "ĐÃ SỬA") } }
        button("Insert image") { edit { e, s, _ ->
            val file = File(cacheDir, "docx-edit-lab.png")
            val bitmap = Bitmap.createBitmap(200, 120, Bitmap.Config.ARGB_8888)
            try {
                Canvas(bitmap).apply { drawColor(Color.rgb(40, 130, 200)); drawText("Edit Lab", 20f, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 28f }) }
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally { bitmap.recycle() }
            e.insertImage(s, file, 200, 120)
        } }
        button("Append paragraph") {
            val e = editor ?: return@button report("Open a DOCX first")
            queued(e.appendParagraph("Đoạn văn mới — Edit Lab"))
        }
        button("Apply") { applyEdits() }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!busy) files.getOrNull(position)?.let { if (it != source) open(it) }
            }
        }
        refresh()
    }
    override fun onResume() { super.onResume(); if (::spinner.isInitialized && !busy) refresh() }
    private fun access() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        else if (Build.VERSION.SDK_INT < 30 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 43)
        else refresh()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == 43) refresh()
    }
    private fun refresh() {
        val found = documents.listFiles()?.filter { it.isFile && it.extension.equals("docx", true) }?.sortedBy { it.name }.orEmpty()
        if (found != files || spinner.adapter == null) {
            files = found; spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, files.map { it.name })
            // adb shell am start ... --es file "Name.docx" preselects a file
            intent.getStringExtra("file")?.let { name -> files.indexOfFirst { it.name == name }.takeIf { it >= 0 }?.let { spinner.setSelection(it, false) } }
        }
        if (found.isEmpty()) report("No DOCX in $documents. Use Files / Access.")
    }
    private fun setBusy(value: Boolean) { busy = value; spinner.isEnabled = !value; buttons.forEach { it.isEnabled = !value } }
    private fun report(text: String) { status.text = text }
    private fun queued(ok: Boolean) { if (ok) { queued++; report("$queued queued. Apply to save and reopen.") } else report(editor?.lastError.toString()) }
    private fun edit(block: (DocxEditor, Long, Long) -> Boolean) {
        val e = editor ?: return report("Open a DOCX first")
        val range = selection?.selection() ?: return report("Long-press a word, then drag to extend selection")
        try { queued(block(e, range.first, range.last + 1)) } catch (ex: Exception) { report(ex.message ?: "Edit failed") }
    }
    private fun open(file: File) {
        setBusy(true); selection = null; editor = null; anchor = null
        frame.removeAllViews(); control?.dispose(); source?.let { DocxSourceMap.clear(it.absolutePath) }
        source = file; queued = 0; report("Opening ${file.name}")
        control = MainControl(this)
        frame.post { if (!isDestroyed) try { if (control?.openFile(file.absolutePath) != true) { setBusy(false); report("Could not open ${file.name}") } } catch (e: Exception) { setBusy(false); report(e.toString()) } }
    }
    override fun openFileFinish() {
        val word = control?.getView() as? Word ?: return
        frame.addView(word, FrameLayout.LayoutParams(-1, -1))
        selection = WordSelection(word)
        source?.let { file -> DocxSourceMap.get(file.absolutePath)?.let { editor = DocxEditor(file, it) } }
        setBusy(false); report("Opened ${source?.name}. Long-press and drag to select.")
    }
    private fun applyEdits() {
        val e = editor ?: return report("Open a DOCX first")
        val file = source ?: return
        val target = File(File(documents, "edited"), "${file.nameWithoutExtension}_edited.docx")
        val oldWord = control?.getView() as? Word
        val sx = oldWord?.scrollX ?: 0; val sy = oldWord?.scrollY ?: 0
        setBusy(true); report("Saving…")
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                target.parentFile?.mkdirs(); e.save(target)
            }
            setBusy(false)
            Toast.makeText(this@DocxEditLabActivity, result.toString(), Toast.LENGTH_LONG).show()
            if (result is EditResult.Ok) { reopenScroll = sx to sy; open(result.file) } else report(result.toString())
        }
    }
    private var reopenScroll: Pair<Int, Int>? = null
    override fun completeLayout() {
        reopenScroll?.let { (x, y) -> (control?.getView() as? Word)?.post { (control?.getView() as? Word)?.scrollTo(x, y) }; reopenScroll = null }
    }
    override fun onEventMethod(v: View?, e1: MotionEvent?, e2: MotionEvent?, xValue: Float, yValue: Float, eventMethodType: Byte): Boolean {
        if (busy) return true
        val helper = selection ?: return false
        val word = control?.getView() as? Word ?: return false
        if (eventMethodType == IMainFrame.ON_LONG_PRESS && e1 != null) {
            val offset = helper.offsetAtScreen(e1.rawX, e1.rawY)
            if (offset < 0 || offset >= 0x1000000000000000L) return false
            val range = helper.wordAt(offset)
            if (!range.isEmpty()) { anchor = range.first; anchorEnd = range.last + 1; helper.setSelection(range.first, range.last + 1); word.getStatus().setSelectTextStatus(true); report(helper.selectedText()) }
            return true
        }
        if (eventMethodType == IMainFrame.ON_TOUCH && e1 != null) {
            if (e1.actionMasked == MotionEvent.ACTION_MOVE) anchor?.let { start ->
                val end = helper.offsetAtScreen(e1.rawX, e1.rawY)
                // Keep the long-pressed word selected; dragging only extends it
                if (end >= 0 && end < 0x1000000000000000L) { helper.setSelection(minOf(start, end), maxOf(anchorEnd, end)); report(helper.selectedText()) }
            }
            if (e1.actionMasked in listOf(MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL)) { anchor = null; word.getStatus().setSelectTextStatus(false) }
        }
        return false
    }
    override fun onDestroy() { dispose(); super.onDestroy() }
    override fun getActivity(): Activity = this
    override fun doActionEvent(actionID: Int, obj: Any?): Boolean = false
    override fun updateToolsbarStatus() = Unit
    override fun setFindBackForwardState(state: Boolean) = Unit
    override fun getBottomBarHeight() = 0
    override fun getTopBarHeight() = 0
    override fun getAppName() = "DOCX Edit Lab"
    override fun getTemporaryDirectory(): File = cacheDir
    override fun isDrawPageNumber() = false
    override fun isShowZoomingMsg() = false
    override fun isPopUpErrorDlg() = true
    override fun isShowPasswordDlg() = true
    override fun isShowProgressBar() = false
    override fun isShowFindDlg() = false
    override fun isShowTXTEncodeDlg() = false
    override fun getTXTDefaultEncode() = "UTF-8"
    override fun isTouchZoom() = true
    override fun isZoomAfterLayoutForWord() = true
    override fun getWordDefaultView() = WPViewConstant.PAGE_ROOT.toByte()
    override fun getLocalString(resName: String): String? = ResKit.instance().getLocalString(resName)
    override fun changeZoom() = Unit
    override fun changePage() = Unit
    override fun error(errorCode: Int) { runOnUiThread { setBusy(false); report("Open error: $errorCode") } }
    override fun fullScreen(fullscreen: Boolean) = Unit
    override fun showProgressBar(visible: Boolean) = Unit
    override fun updateViewImages(viewList: List<Int?>) = Unit
    override fun isChangePage() = true
    override fun setWriteLog(saveLog: Boolean) = Unit
    override fun isWriteLog() = false
    override fun setThumbnail(isThumbnail: Boolean) = Unit
    override fun isThumbnail() = false
    override fun getViewBackground(): Any = Color.LTGRAY
    override fun setIgnoreOriginalSize(ignoreOriginalSize: Boolean) = Unit
    override fun isIgnoreOriginalSize() = false
    override fun getPageListViewMovingPosition() = IPageListViewListener.Moving_Vertical
    override fun dispose() { control?.dispose(); control = null; source?.let { DocxSourceMap.clear(it.absolutePath) } }
}
