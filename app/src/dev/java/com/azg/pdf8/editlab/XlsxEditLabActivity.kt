package com.azg.pdf8.editlab

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wxiwei.office.constant.wp.WPViewConstant
import com.wxiwei.office.editor.EditResult
import com.wxiwei.office.editor.xlsx.A1FormulaShifter
import com.wxiwei.office.editor.xlsx.SheetEditSession
import com.wxiwei.office.res.ResKit
import com.wxiwei.office.ss.control.ExcelView
import com.wxiwei.office.system.LayoutInfo
import com.wxiwei.office.system.IMainFrame
import com.wxiwei.office.system.MainControl
import com.wxiwei.office.system.beans.pagelist.IPageListViewListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Dev-only realtime XLSX harness: tap a cell, type a value or =formula, Set. Save patches the file. */
class XlsxEditLabActivity : AppCompatActivity(), IMainFrame {
    private lateinit var frame: FrameLayout
    private lateinit var status: TextView
    private lateinit var address: TextView
    private lateinit var input: EditText
    private lateinit var spinner: Spinner
    private var control: MainControl? = null
    private var session: SheetEditSession? = null
    private var source: File? = null
    private var files = emptyList<File>()
    private var busy = false
    private val buttons = ArrayList<Button>()
    private val documents get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    private val spreadsheet get() = (control?.getView() as? ExcelView)?.getSpreadsheet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        spinner = Spinner(this); root.addView(spinner)
        val row = LinearLayout(this); root.addView(HorizontalScrollView(this).apply { addView(row) })
        val edit = LinearLayout(this); root.addView(edit)
        address = TextView(this).apply { minWidth = 160; text = "—" }; edit.addView(address)
        input = EditText(this).apply { setSingleLine(); imeOptions = EditorInfo.IME_ACTION_DONE; hint = "value or =formula" }
        edit.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        input.setOnEditorActionListener { _, _, _ -> setInput(input.text.toString()); true }
        // Fixed height: a status that wraps differently would move the sheet under the finger
        status = TextView(this).apply { setLines(2); ellipsize = android.text.TextUtils.TruncateAt.END }; root.addView(status)
        frame = FrameLayout(this); root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        fun button(name: String, parent: LinearLayout = row, action: () -> Unit) {
            val b = Button(this).apply { text = name; isAllCaps = false; setOnClickListener { if (!busy) action() } }
            parent.addView(b); buttons.add(b)
        }
        button("Set", edit) { setInput(input.text.toString()) }
        for (f in listOf("=SUM(A1:A5)", "=AVERAGE(B1:B5)", "=A1*2", "=IF(A1>10,\"lớn\",\"nhỏ\")", "123", "Xin chào")) button(f) { input.setText(f); setInput(f) }
        button("Clear") { setInput("") }
        // Recalculate everything on a freshly opened file: every "changed" cell is one where our
        // engine disagrees with the value Excel saved (volatile functions like TODAY() excepted)
        button("Recalc check") {
            val s = session ?: return@button report("Open an XLSX first")
            fun show(c: com.wxiwei.office.ss.model.baseModel.Cell) = "t${c.getCellType()}:" + when (c.getCellType()) {
                com.wxiwei.office.ss.model.baseModel.Cell.CELL_TYPE_NUMERIC -> c.getNumberValue().toString()
                com.wxiwei.office.ss.model.baseModel.Cell.CELL_TYPE_STRING -> s.engine.adapter.stringOf(c)
                com.wxiwei.office.ss.model.baseModel.Cell.CELL_TYPE_ERROR -> c.getErrorValue().toString()
                else -> ""
            }
            val before = HashMap<com.wxiwei.office.ss.model.baseModel.Cell, String>()
            val book = s.engine.book
            for (i in 0 until book.getSheetCount()) { val sh = book.getSheet(i) ?: continue
                for (r in sh.getFirstRowNum()..sh.getLastRowNum()) sh.getRow(r)?.cellCollection()?.forEach { if (it.formula != null) before[it] = show(it) } }
            val warnings = ArrayList<String>(); val t0 = System.nanoTime()
            val changed = s.engine.recalc(warnings)
            val ms = (System.nanoTime() - t0) / 1_000_000
            val sample = changed.take(12).joinToString("\n") { c -> A1FormulaShifter.address(c.cell.getRowNumber(), c.cell.getColNumber()) + " " + s.getInput(c.sheetIndex, c.cell.getRowNumber(), c.cell.getColNumber()) + " saved=" + before[c.cell] + " ours=" + show(c.cell) }
            // Full list for offline analysis: adb shell run-as com.azg.pdf8 cat files/recalc.tsv
            java.io.File(filesDir, "recalc.tsv").bufferedWriter().use { out ->
                for (c in changed) out.append(book.getSheet(c.sheetIndex)?.getSheetName()).append('\t')
                    .append(A1FormulaShifter.address(c.cell.getRowNumber(), c.cell.getColNumber())).append('\t')
                    .append(s.getInput(c.sheetIndex, c.cell.getRowNumber(), c.cell.getColNumber()).replace('\n', ' ')).append('\t')
                    .append(before[c.cell].orEmpty().replace("\n", "\\n").replace("\r", "\\r")).append('\t')
                    .append(show(c.cell).replace("\n", "\\n").replace("\r", "\\r")).append('\n')
                for (w in warnings) out.append("WARN\t").append(w).append('\n')
            }
            android.util.Log.d("XLSXLAB", "recalc ${ms}ms changed=${changed.size} warnings=${warnings.size} ${warnings.take(8)} sample=$sample")
            report("Recalc ${ms} ms: ${changed.size} differ from saved values, ${warnings.size} warnings\n$sample")
        }
        button("Undo") { session?.let { report(if (it.undo()) "Undone" else "Nothing to undo"); showActive() } }
        button("Redo") { session?.let { report(if (it.redo()) "Redone" else "Nothing to redo"); showActive() } }
        button("Save") { save(reopen = false) }
        button("Reopen saved") { save(reopen = true) }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!busy) files.getOrNull(position)?.let { if (it != source) open(it) }
            }
        }
        files = documents.listFiles()?.filter { it.isFile && it.extension.equals("xlsx", true) }?.sortedBy { it.name }.orEmpty()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, files.map { it.name })
        // adb shell am start ... --es file "Name.xlsx" preselects a file
        intent.getStringExtra("file")?.let { name -> files.indexOfFirst { it.name == name }.takeIf { it >= 0 }?.let { spinner.setSelection(it, false) } }
        if (files.isEmpty()) report("No XLSX in $documents")
    }

    private fun report(text: String) { status.text = text }
    private fun setBusy(value: Boolean) { busy = value; spinner.isEnabled = !value; buttons.forEach { it.isEnabled = !value } }

    /** (sheetIndex, row, col) of the cell the viewer has selected. */
    private fun active(): Triple<Int, Int, Int>? {
        val ss = spreadsheet ?: return null; val s = session ?: return null
        val sheet = ss.getSheetView()?.getCurrentSheet() ?: return null
        return Triple(s.sheetIndexOf(sheet), sheet.getActiveCellRow(), sheet.getActiveCellColumn())
    }

    private fun showActive() {
        val (sheet, r, c) = active() ?: return
        address.text = A1FormulaShifter.address(r, c)
        input.setText(session?.getInput(sheet, r, c).orEmpty())
    }

    private fun setInput(text: String) {
        val s = session ?: return report("Open an XLSX first")
        val (sheet, r, c) = active() ?: return report("Tap a cell first")
        val t0 = System.nanoTime()
        val ok = s.setCellInput(sheet, r, c, text)
        val ms = (System.nanoTime() - t0) / 1_000_000
        android.util.Log.d("XLSXLAB", "set ${A1FormulaShifter.address(r, c)}='$text' ok=$ok in ${ms}ms warn=${s.warnings}")
        report(if (ok) "${A1FormulaShifter.address(r, c)} = ${s.getInput(sheet, r, c)}  (${ms} ms)" + (s.warnings.firstOrNull()?.let { "\n⚠ $it" } ?: "")
               else s.lastError?.message ?: "Failed")
    }

    private fun open(file: File) {
        setBusy(true); session = null
        frame.removeAllViews(); control?.dispose(); source = file
        report("Opening ${file.name}"); control = MainControl(this)
        frame.post { if (!isDestroyed) try { if (control?.openFile(file.absolutePath) != true) { setBusy(false); report("Could not open ${file.name}") } } catch (e: Exception) { setBusy(false); report(e.toString()) } }
    }

    override fun openFileFinish() {
        val view = control?.getView() ?: return
        frame.addView(view, FrameLayout.LayoutParams(-1, -1))
        session = source?.let { SheetEditSession(control!!, it) }
        setBusy(false); report("Opened ${source?.name}. Tap a cell, type, Set.")
    }

    private fun save(reopen: Boolean) {
        val s = session ?: return report("Open an XLSX first"); val file = source ?: return
        val target = File(File(documents, "edited"), "${file.nameWithoutExtension}_edited.xlsx")
        setBusy(true); report("Saving…")
        lifecycleScope.launch {
            // The writer reads the model only through values captured on the UI thread
            val result = try { target.parentFile?.mkdirs(); withContext(Dispatchers.IO) { s.save(target) } } catch (e: Exception) { EditResult.Error(com.wxiwei.office.editor.Reason.IO, e.toString()) }
            setBusy(false)
            report(result.toString())
            if (result is EditResult.Ok && reopen) open(result.file)
        }
    }

    override fun onEventMethod(v: View?, e1: MotionEvent?, e2: MotionEvent?, xValue: Float, yValue: Float, eventMethodType: Byte): Boolean {
        if (eventMethodType == IMainFrame.ON_SINGLE_TAP_UP || eventMethodType == IMainFrame.ON_SINGLE_TAP_CONFIRMED || eventMethodType == IMainFrame.ON_CLICK)
            frame.post { showActive() }
        return busy
    }
    override fun doActionEvent(actionID: Int, obj: Any?): Boolean { frame.post { showActive() }; return false }
    override fun onDestroy() { dispose(); super.onDestroy() }
    override fun getActivity(): Activity = this
    override fun updateToolsbarStatus() = Unit
    override fun setFindBackForwardState(state: Boolean) = Unit
    override fun getBottomBarHeight() = 0
    override fun getTopBarHeight() = 0
    override fun getAppName() = "XLSX Edit Lab"
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
    override fun changePage(pageNumber: Int, pageCount: Int) = Unit
    override fun completeLayout(info: LayoutInfo) = Unit
    override fun error(errorCode: Int) { runOnUiThread { setBusy(false); report("Open error: $errorCode") } }
    override fun fullScreen(fullscreen: Boolean) = Unit
    override fun showProgressBar(visible: Boolean) = Unit
    override fun updateViewImages(viewList: List<Int?>) = Unit
    override fun isChangePage() = true
    override fun setWriteLog(saveLog: Boolean) = Unit
    override fun isWriteLog() = false
    override fun setThumbnail(isThumbnail: Boolean) = Unit
    override fun isThumbnail() = false
    override fun getViewBackground(): Any = Color.WHITE
    override fun setIgnoreOriginalSize(ignoreOriginalSize: Boolean) = Unit
    override fun isIgnoreOriginalSize() = false
    override fun getPageListViewMovingPosition() = IPageListViewListener.Moving_Vertical
    override fun dispose() { control?.dispose(); control = null; session = null }
}
