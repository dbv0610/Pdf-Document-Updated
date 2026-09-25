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
import com.wxiwei.office.editor.pptx.*
import com.wxiwei.office.editor.slide.SlideGeometry
import com.wxiwei.office.pg.control.Presentation
import com.wxiwei.office.res.ResKit
import com.wxiwei.office.system.IMainFrame
import com.wxiwei.office.system.MainControl
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Dev-only realtime PPTX harness: edits show immediately (LivePptxSession); Save writes the file. */
class PptxEditLabActivity : AppCompatActivity(), IMainFrame {
    private lateinit var frame: FrameLayout
    private lateinit var status: TextView
    private lateinit var spinner: Spinner
    private var control: MainControl? = null
    private var editor: LivePptxSession? = null
    private var source: File? = null
    private var files = emptyList<File>()
    private var busy = false
    private var mode: String? = null
    private var selected: Pair<Int, PptxShapeInfo>? = null
    private var overlay: View? = null
    private var restoreSlide = 0
    private val buttons = ArrayList<Button>()
    private val documents get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    private val presentation get() = control?.getView() as? Presentation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        spinner = Spinner(this); root.addView(spinner)
        val row = LinearLayout(this); root.addView(HorizontalScrollView(this).apply { addView(row) })
        // Fixed height: a status that wraps differently would move the document under the finger
        status = TextView(this).apply { setLines(2); ellipsize = android.text.TextUtils.TruncateAt.END }; root.addView(status)
        frame = FrameLayout(this); root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        fun button(name: String, action: () -> Unit) {
            val b = Button(this).apply { text = name; setOnClickListener { if (!busy) action() } }
            row.addView(b); buttons.add(b)
        }
        button("Files / Access") { access() }
        for (tool in listOf("Text box", "Image", "Select")) button(tool) { mode = tool; report("$tool: tap the slide") }
        button("Edit text") { editSelected { e, index, shape -> e.setShapeText(index, shape.id, "ĐÃ SỬA") } }
        button("Move") { editSelected { e, index, shape ->
            e.moveShape(index, shape.id, shape.rectEmu.copy(x = shape.rectEmu.x + 457200, y = shape.rectEmu.y + 457200))
        } }
        button("Delete") { editSelected { e, index, shape -> e.deleteShape(index, shape.id) } }
        button("Undo") { editor?.let { report(if (it.undo()) "Undone" else "Nothing to undo"); refreshSelection() } }
        button("Redo") { editor?.let { report(if (it.redo()) "Redone" else "Nothing to redo"); refreshSelection() } }
        button("Save") { save(reopen = false) }
        button("Reopen saved") { save(reopen = true) }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!busy) files.getOrNull(position)?.let { if (it != source) open(it, 0) }
            }
        }
        refresh()
    }
    override fun onResume() { super.onResume(); if (::spinner.isInitialized && !busy) refresh() }
    private fun access() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        else if (Build.VERSION.SDK_INT < 30 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 44)
        else refresh()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == 44) refresh()
    }
    private fun refresh() {
        val found = documents.listFiles()?.filter { it.isFile && it.extension.equals("pptx", true) }?.sortedBy { it.name }.orEmpty()
        if (found != files || spinner.adapter == null) { files = found; spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, files.map { it.name }) }
        if (found.isEmpty()) report("No PPTX in $documents. Use Files / Access.")
    }
    private fun setBusy(value: Boolean) { busy = value; spinner.isEnabled = !value; buttons.forEach { it.isEnabled = !value } }
    private fun report(text: String) { status.text = text }
    private fun refreshSelection() { selected = selected?.let { (i, s) -> editor?.listShapes(i)?.firstOrNull { it.id == s.id }?.let { i to it } }; overlay?.invalidate() }
    private fun editSelected(block: (LivePptxSession, Int, PptxShapeInfo) -> Boolean) {
        val e = editor ?: return report("Open a PPTX first")
        val (index, shape) = selected ?: return report("Select a shape first")
        if (index != presentation?.getCurrentIndex()) return report("Select a shape on this slide")
        android.util.Log.d("PPTXLAB", "op on slide=$index id=${shape.id} ${shape.name} rect=${shape.rectEmu}")
        if (block(e, index, shape)) {
            selected = e.listShapes(index).firstOrNull { it.id == shape.id }?.let { index to it }
            report(if (e.needsReopen) "Saved on Save; reopen to see it" else "Done (live)"); overlay?.invalidate()
        } else report(e.lastError.toString())
    }
    private fun open(file: File, slide: Int) {
        setBusy(true); selected = null; mode = null; editor = null; overlay = null; restoreSlide = slide
        frame.removeAllViews(); control?.dispose(); source = file
        report("Opening ${file.name}"); control = MainControl(this)
        frame.post { if (!isDestroyed) try { if (control?.openFile(file.absolutePath) != true) { setBusy(false); report("Could not open ${file.name}") } } catch (e: Exception) { setBusy(false); report(e.toString()) } }
    }
    override fun openFileFinish() {
        val p = presentation ?: return
        frame.addView(p, FrameLayout.LayoutParams(-1, -1))
        editor = source?.let { LivePptxSession(control!!, it) }
        overlay = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.MAGENTA; strokeWidth = 3f; style = Paint.Style.STROKE }
            override fun onDraw(canvas: Canvas) {
                selected?.takeIf { it.first == p.getCurrentIndex() }?.let { (_, shape) ->
                    SlideGeometry.emuToView(p, shape.rectEmu)?.let { canvas.drawRect(it, paint) }
                }
            }
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (busy) return true
                if (mode == null) return false
                if (event.actionMasked == MotionEvent.ACTION_UP) { performClick(); tap(event.x, event.y) }
                return true
            }
            override fun performClick(): Boolean { super.performClick(); return true }
        }.also { frame.addView(it, FrameLayout.LayoutParams(-1, -1)) }
        // Repaint bounds after scrolling/zooming without intercepting normal viewer gestures.
        p.viewTreeObserver.addOnScrollChangedListener { overlay?.invalidate() }
        fun restore() {
            if (isDestroyed || presentation !== p) return
            if (restoreSlide < p.getRealSlideCount() && p.width > 0) {
                p.showSlide(restoreSlide, false); setBusy(false); report("Opened ${source?.name}. Choose a tool; edits show live.")
            } else p.postDelayed({ restore() }, 100)
        }
        p.post { restore() }
    }
    private fun tap(x: Float, y: Float) {
        val p = presentation ?: return; val e = editor ?: return
        val point = SlideGeometry.viewToEmu(p, x, y) ?: return report("Tap inside the current slide")
        val index = p.getCurrentIndex(); val tool = mode; mode = null
        val id = when (tool) {
            "Text box" -> e.addTextBox(index, Rect(point.x, point.y, 3657600, 914400), "Xin chào PowerPoint", 24f, "0000FF")
            "Image" -> {
                val file = File(cacheDir, "pptx-edit-lab.png")
                val bitmap = Bitmap.createBitmap(450, 300, Bitmap.Config.ARGB_8888)
                try {
                    Canvas(bitmap).apply { drawColor(Color.rgb(40, 130, 200)); drawText("Edit Lab", 30f, 160f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 60f }) }
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    e.addImage(index, Rect(point.x, point.y, 2743200, 1828800), file)
                } catch (ex: Exception) { report(ex.toString()); return } finally { bitmap.recycle() }
            }
            else -> SlideGeometry.hitTest(e.listShapes(index), point)?.id ?: -1
        }
        selected = e.listShapes(index).firstOrNull { it.id == id }?.let { index to it }
        report(selected?.second?.let { "id=${it.id} ${it.name}\n${it.text}\n${if (tool == "Select") "Selected" else "Added (live)"}" } ?: e.lastError?.toString() ?: "No shape here")
        overlay?.invalidate()
    }
    private fun save(reopen: Boolean) {
        val e = editor ?: return report("Open a PPTX first"); val file = source ?: return
        val target = File(File(documents, "edited"), "${file.nameWithoutExtension}_edited.pptx")
        val index = presentation?.getCurrentIndex() ?: 0
        setBusy(true); report("Saving…")
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                com.wxiwei.office.editor.runEdit { target.parentFile?.mkdirs(); e.save(target) }
            }
            setBusy(false)
            Toast.makeText(this@PptxEditLabActivity, result.toString(), Toast.LENGTH_LONG).show()
            if (result is EditResult.Ok && reopen) open(result.file, index) else report(result.toString())
        }
    }
    override fun completeLayout() = Unit
    override fun onEventMethod(v: View?, e1: MotionEvent?, e2: MotionEvent?, xValue: Float, yValue: Float, eventMethodType: Byte): Boolean = busy
    override fun onDestroy() { dispose(); super.onDestroy() }
    override fun getActivity(): Activity = this
    override fun doActionEvent(actionID: Int, obj: Any?): Boolean = false
    override fun updateToolsbarStatus() = Unit
    override fun setFindBackForwardState(state: Boolean) = Unit
    override fun getBottomBarHeight() = 0
    override fun getTopBarHeight() = 0
    override fun getAppName() = "PPTX Edit Lab"
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
    override fun changeZoom() { overlay?.invalidate() }
    override fun changePage() { overlay?.invalidate() }
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
    override fun dispose() { control?.dispose(); control = null; editor = null }
}
