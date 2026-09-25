package com.azg.pdf8.editlab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.reader.pdfviewer.PDFView
import com.reader.pdfviewer.TextMarkupType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Dev-only manual harness for annotation editing and saved-file round trips. */
class EditLabActivity : AppCompatActivity() {
    private lateinit var pdf: PDFView
    private lateinit var status: TextView
    private lateinit var spinner: Spinner
    private val documents get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    private var files = emptyList<File>()
    private var source: File? = null
    private var saved: File? = null
    private var tool: String? = null
    private var saving = false
    private var lastResult = "Long-press text to select; choose a tool."
    private val controls = ArrayList<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        spinner = Spinner(this)
        root.addView(spinner)
        val row = LinearLayout(this)
        root.addView(HorizontalScrollView(this).apply { addView(row) })
        status = TextView(this).apply { setLines(2); ellipsize = android.text.TextUtils.TruncateAt.END }
        root.addView(status)
        pdf = PDFView(this, null)
        root.addView(pdf, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        fun button(label: String, action: () -> Unit) {
            val button = Button(this).apply { text = label; setOnClickListener { if (!saving) action() } }
            controls.add(button)
            row.addView(button)
        }
        button("DOCX") { startActivity(Intent(this, DocxEditLabActivity::class.java)) }
        button("PPTX") { startActivity(Intent(this, PptxEditLabActivity::class.java)) }
        button("XLSX") { startActivity(Intent(this, XlsxEditLabActivity::class.java)) }
        button("Files / Access") { requestFilesAccess() }
        button("Highlight") { markup(TextMarkupType.HIGHLIGHT) }
        button("Underline") { markup(TextMarkupType.UNDERLINE) }
        button("Strike") { markup(TextMarkupType.STRIKETHROUGH) }
        for (name in listOf("Text", "Image", "Delete")) button(name) {
            pdf.clearTextSelection()
            pdf.setDrawingMode(false)
            tool = name
            report("$name: tap a page")
        }
        button("Ink") { tool = null; pdf.setDrawingMode(!pdf.isDrawingMode); report("Ink: ${pdf.isDrawingMode}") }
        button("Undo") { report("Undo: ${pdf.undoEdit()}") }
        button("Redo") { report("Redo: ${pdf.redoEdit()}") }
        button("Save") { save() }
        button("Reopen saved") { saved?.takeIf { it.isFile }?.let { load(it) } ?: report("Save a file first") }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!saving) files.getOrNull(position)?.let { if (it != source) load(it) }
            }
        }
        refreshFiles()
    }

    override fun onResume() {
        super.onResume()
        if (::spinner.isInitialized && !saving) refreshFiles()
    }

    private fun requestFilesAccess() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        } else if (Build.VERSION.SDK_INT < 30 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 41)
        } else refreshFiles()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41) refreshFiles()
    }

    private fun refreshFiles() {
        val found = documents.listFiles()?.filter { it.isFile && it.extension.equals("pdf", true) }?.sortedBy { it.name }.orEmpty()
        if (found != files || spinner.adapter == null) {
            files = found
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, files.map { it.name })
        }
        if (files.isEmpty()) report("No PDFs in $documents. Use Files / Access to grant storage access.")
    }

    private fun load(file: File) {
        source = file
        tool = null
        pdf.fromFile(file).enableTextSelection(true).enableAnnotationRendering(true)
            .onEditChange { _, _ -> report(lastResult) }
            .onLoad { report("Loaded ${file.name}") }
            .onError { report("Load failed: ${it?.message}") }
            .onTap(object : com.reader.pdfviewer.listener.OnTapListener {
                override fun onTap(event: android.view.MotionEvent?): Boolean {
                    if (saving) return true
                    val selected = tool ?: return false
                    val point = event?.let { pdf.viewToPagePoint(it.x, it.y) } ?: return true
                    tool = null
                    when (selected) {
                        "Text" -> report("Text: ${pdf.addText(point.first, point.second.x, point.second.y, "Xin chào tiếng Việt ✓ 123", 16f, Color.RED)}")
                        "Image" -> {
                            val bitmap = Bitmap.createBitmap(450, 300, Bitmap.Config.ARGB_8888)
                            Canvas(bitmap).apply {
                                drawColor(Color.rgb(40, 130, 200))
                                drawText("Edit Lab", 30f, 160f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 60f })
                            }
                            val p = point.second
                            report("Image: ${pdf.addImage(point.first, RectF(p.x, p.y, p.x + 150, p.y - 100), bitmap)}")
                            bitmap.recycle()
                        }
                        "Delete" -> {
                            val info = pdf.findAnnotationAt(event.x, event.y)
                            val result = info?.let { "Delete subtype ${it.subtype}: ${pdf.removeAnnotation(it)}" } ?: "No annotation here"
                            report(result)
                            Toast.makeText(this@EditLabActivity, result, Toast.LENGTH_SHORT).show()
                        }
                    }
                    return true
                }
            }).load()
    }

    private fun markup(type: TextMarkupType) {
        tool = null
        report("$type: ${pdf.addTextMarkupToSelection(type)}")
    }

    private fun report(message: String) {
        lastResult = message
        status.text = "canUndo=${pdf.canUndoEdit()} canRedo=${pdf.canRedoEdit()}\n$message"
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean =
        if (saving) true else super.dispatchTouchEvent(event)

    private fun save() {
        val input = source ?: return report("Open a PDF first")
        val target = File(File(documents, "edited"), "${input.nameWithoutExtension}_edited.pdf")
        saving = true
        controls.forEach { it.isEnabled = false }
        spinner.isEnabled = false
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { (target.parentFile!!.isDirectory || target.parentFile!!.mkdirs()) && pdf.saveDocument(target) }.getOrDefault(false)
            }
            saving = false
            controls.forEach { it.isEnabled = true }
            spinner.isEnabled = true
            val message = if (ok) { saved = target; target.absolutePath } else "Save failed: $target"
            report(message)
            Toast.makeText(this@EditLabActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}
