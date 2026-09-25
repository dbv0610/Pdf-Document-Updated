package com.wxiwei.office.editor.xlsx

import com.wxiwei.office.editor.EditResult
import com.wxiwei.office.editor.ooxml.OoxmlPackage
import com.wxiwei.office.editor.ooxml.SS
import com.wxiwei.office.editor.ooxml.childrenNamed
import com.wxiwei.office.editor.ooxml.firstChild
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.ss.model.baseModel.Cell
import com.wxiwei.office.ss.model.baseModel.Row
import com.wxiwei.office.ss.model.baseModel.Sheet
import com.wxiwei.office.ss.model.baseModel.Workbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxEditTest {
    @get:Rule val temp = TemporaryFolder()

    private fun book(): Workbook {
        val book = Workbook(false)
        for ((i, name) in listOf("S1", "S2").withIndex()) {
            val sheet = Sheet(); sheet.setWorkbook(book); sheet.setSheetName(name); sheet.setState(Sheet.State_Accomplished)
            book.addSheet(i, sheet)
        }
        return book
    }

    private fun session(book: Workbook, source: File = temp.newFile("x.xlsx")) = SheetEditSession(book, source) {}
    private fun num(book: Workbook, s: Int, ref: String): Double {
        val (r, c) = rc(ref); return book.getSheet(s)!!.getRow(r)!!.getCell(c)!!.getNumberValue()
    }
    private fun rc(ref: String) = (ref.dropWhile { it.isLetter() }.toInt() - 1) to (ref.takeWhile { it.isLetter() }.fold(0) { n, ch -> n * 26 + (ch - 'A' + 1) } - 1)
    private fun set(s: SheetEditSession, sheet: Int, ref: String, input: String) { val (r, c) = rc(ref); val ok = s.setCellInput(sheet, r, c, input); assertTrue("$ref=$input: ${s.lastError}", ok) }

    @Test fun shifterMovesRelativeRefsOnly() {
        assertEquals("SUM(B2:B4)+\$A\$1+C\$1+S2!B2+\"A1\"", A1FormulaShifter.shift("SUM(A1:A3)+\$A\$1+B\$1+S2!A1+\"A1\"", 1, 1))
        assertEquals("#REF!+1", A1FormulaShifter.shift("A1+1", -1, 0))
        assertEquals("LOG10(A2)", A1FormulaShifter.shift("LOG10(A1)", 1, 0))
    }

    @Test fun formulasCalculateAndDependentsFollowEdits() {
        val book = book(); val s = session(book)
        set(s, 0, "A1", "1"); set(s, 0, "A2", "2"); set(s, 0, "A3", "3")
        set(s, 0, "A4", "=SUM(A1:A3)"); assertEquals(6.0, num(book, 0, "A4"), 0.0)
        set(s, 0, "B1", "=AVERAGE(A1:A3)*2"); assertEquals(4.0, num(book, 0, "B1"), 0.0)
        set(s, 1, "A1", "10"); set(s, 0, "C1", "=S2!A1+A4")
        assertEquals(16.0, num(book, 0, "C1"), 0.0)
        set(s, 0, "D1", "=IF(A1>0,\"pos\",\"neg\")&\"!\"")
        assertEquals("pos!", s.engine.adapter.stringOf(book.getSheet(0)!!.getRow(0)!!.getCell(3)!!))
        set(s, 0, "E1", "=A1/0"); assertEquals(Cell.CELL_TYPE_ERROR, book.getSheet(0)!!.getRow(0)!!.getCell(4)!!.getCellType())
        // Change an input: every dependent follows, across sheets
        set(s, 0, "A1", "4")
        assertEquals(9.0, num(book, 0, "A4"), 0.0); assertEquals(6.0, num(book, 0, "B1"), 0.0); assertEquals(19.0, num(book, 0, "C1"), 0.0)
        set(s, 1, "A1", "20"); assertEquals(29.0, num(book, 0, "C1"), 0.0)
        assertEquals("=SUM(A1:A3)", s.getInput(0, 3, 0))
    }

    @Test fun badFormulaIsRejectedAndUndoRedoRestoreValues() {
        val book = book(); val s = session(book)
        set(s, 0, "A1", "5"); set(s, 0, "B1", "=A1*2")
        assertFalse(s.setCellInput(0, 0, 2, "=SUM(A1:"))
        assertTrue(s.lastError!!.message.startsWith("Formula error"))
        set(s, 0, "A1", "7"); assertEquals(14.0, num(book, 0, "B1"), 0.0)
        assertTrue(s.undo()); assertEquals(10.0, num(book, 0, "B1"), 0.0)
        assertTrue(s.redo()); assertEquals(14.0, num(book, 0, "B1"), 0.0)
        set(s, 0, "B1", "text"); assertNull(book.getSheet(0)!!.getRow(0)!!.getCell(1)!!.formula)
        assertTrue(s.undo()); assertEquals("=A1*2", s.getInput(0, 0, 1))
    }

    @Test fun textFunctionFormatsDates() {
        val book = book(); val s = session(book)
        set(s, 0, "A1", "46174") // 2026-06-01
        for ((fmt, want) in listOf("yyyy-mm" to "2026-06", "dd/mm/yyyy" to "01/06/2026", "0.00" to "46174.00", "#,##0" to "46,174")) {
            set(s, 0, "B1", "=TEXT(A1,\"$fmt\")")
            println("TEXT $fmt -> '${s.engine.adapter.stringOf(book.getSheet(0)!!.getRow(0)!!.getCell(1)!!)}'")
            assertEquals(fmt, want, s.engine.adapter.stringOf(book.getSheet(0)!!.getRow(0)!!.getCell(1)!!))
        }
    }

    private fun str(s: SheetEditSession, book: Workbook, ref: String): String {
        val (r, c) = rc(ref); return s.engine.adapter.stringOf(book.getSheet(0)!!.getRow(r)!!.getCell(c)!!)
    }
    private fun err(book: Workbook, ref: String): Int {
        val (r, c) = rc(ref); val cell = book.getSheet(0)!!.getRow(r)!!.getCell(c)!!
        assertEquals(ref, Cell.CELL_TYPE_ERROR, cell.getCellType()); return cell.getErrorValue().toInt()
    }

    @Test fun modernFunctionsEvaluate() {
        val book = book(); val s = session(book)
        // A: region, B: product, C: qty
        val rows = listOf(Triple("North", "Pen", 10), Triple("South", "Pen", 20), Triple("North", "Book", 5), Triple("North", "Pen", 7))
        rows.forEachIndexed { i, (region, product, qty) ->
            set(s, 0, "A${i + 1}", region); set(s, 0, "B${i + 1}", product); set(s, 0, "C${i + 1}", qty.toString())
        }
        set(s, 0, "E1", "=SUMIFS(C1:C4,A1:A4,\"North\",B1:B4,\"Pen\")"); assertEquals(17.0, num(book, 0, "E1"), 0.0)
        set(s, 0, "E2", "=COUNTIFS(A1:A4,\"North\",C1:C4,\">6\")"); assertEquals(2.0, num(book, 0, "E2"), 0.0)
        set(s, 0, "E3", "=AVERAGEIFS(C1:C4,A1:A4,\"North\")"); assertEquals(22.0 / 3, num(book, 0, "E3"), 1e-9)
        set(s, 0, "E4", "=MAXIFS(C1:C4,B1:B4,\"Pen\")"); assertEquals(20.0, num(book, 0, "E4"), 0.0)
        set(s, 0, "E5", "=MINIFS(C1:C4,B1:B4,\"Pen\")"); assertEquals(7.0, num(book, 0, "E5"), 0.0)
        set(s, 0, "E6", "=AVERAGEIF(A1:A4,\"North\",C1:C4)"); assertEquals(22.0 / 3, num(book, 0, "E6"), 1e-9)
        set(s, 0, "E7", "=AVERAGEIFS(C1:C4,A1:A4,\"East\")"); assertEquals(com.wxiwei.office.fc.ss.usermodel.ErrorConstants.ERROR_DIV_0, err(book, "E7"))
        set(s, 0, "F1", "=IFERROR(1/0,\"x\")"); assertEquals("x", str(s, book, "F1"))
        set(s, 0, "F2", "=IFERROR(C1*2,\"x\")"); assertEquals(20.0, num(book, 0, "F2"), 0.0)
        set(s, 0, "F3", "=IFNA(XLOOKUP(\"Pencil\",B1:B4,C1:C4),-1)"); assertEquals(-1.0, num(book, 0, "F3"), 0.0)
        set(s, 0, "F4", "=IFNA(1/0,2)"); assertEquals(com.wxiwei.office.fc.ss.usermodel.ErrorConstants.ERROR_DIV_0, err(book, "F4"))
        set(s, 0, "F5", "=XLOOKUP(\"Book\",B1:B4,C1:C4)"); assertEquals(5.0, num(book, 0, "F5"), 0.0)
        set(s, 0, "F6", "=XLOOKUP(\"Pen\",B1:B4,C1:C4,0,0,-1)"); assertEquals(7.0, num(book, 0, "F6"), 0.0) // search from last
        set(s, 0, "F7", "=XLOOKUP(\"S*\",A1:A4,C1:C4,,2)"); assertEquals(20.0, num(book, 0, "F7"), 0.0) // wildcard
        set(s, 0, "F8", "=XLOOKUP(15,C1:C4,A1:A4,\"none\",-1)"); assertEquals("North", str(s, book, "F8")) // next smaller = 10
        set(s, 0, "F9", "=XLOOKUP(15,C1:C4,A1:A4,\"none\",1)"); assertEquals("South", str(s, book, "F9")) // next larger = 20
        set(s, 0, "G1", "=IFS(C1>50,\"big\",C1>5,\"mid\",TRUE,\"small\")"); assertEquals("mid", str(s, book, "G1"))
        set(s, 0, "G2", "=SWITCH(B3,\"Pen\",1,\"Book\",2,0)"); assertEquals(2.0, num(book, 0, "G2"), 0.0)
        set(s, 0, "G3", "=SWITCH(\"x\",\"a\",1)"); assertEquals(com.wxiwei.office.fc.ss.usermodel.ErrorConstants.ERROR_NA, err(book, "G3"))
        set(s, 0, "G4", "=CONCAT(A1:B2,\"!\")"); assertEquals("NorthPenSouthPen!", str(s, book, "G4"))
        set(s, 0, "G5", "=TEXTJOIN(\", \",TRUE,A1:A4,\"\",B3)"); assertEquals("North, South, North, North, Book", str(s, book, "G5"))
        // Dependents follow edits
        set(s, 0, "C1", "100")
        assertEquals(107.0, num(book, 0, "E1"), 0.0); assertEquals(100.0, num(book, 0, "E4"), 0.0); assertEquals("big", str(s, book, "G1"))
        // Stored with Excel's _xlfn. prefix where needed, shown without it
        val (r, c) = rc("F3")
        assertEquals("_xlfn.IFNA(_xlfn.XLOOKUP(\"Pencil\",B1:B4,C1:C4),-1)", book.getSheet(0)!!.getRow(r)!!.getCell(c)!!.formula)
        assertEquals("=IFNA(XLOOKUP(\"Pencil\",B1:B4,C1:C4),-1)", s.getInput(0, r, c))
        assertEquals("SUMIFS(C1:C4,A1:A4,\"North\")", XlfnNames.addPrefix("SUMIFS(C1:C4,A1:A4,\"North\")"))
        assertEquals("\"IFS(\"&_xlfn.IFS(A1,1)", XlfnNames.addPrefix("\"IFS(\"&IFS(A1,1)"))
    }

    @Test fun exactLookupIndexFollowsEdits() {
        val book = book(); val s = session(book)
        listOf("apple", "Pear", "plum").forEachIndexed { i, v -> set(s, 0, "A${i + 1}", v); set(s, 0, "B${i + 1}", "${(i + 1) * 10}") }
        set(s, 0, "D1", "=MATCH(\"pear\",A1:A3,0)"); assertEquals(2.0, num(book, 0, "D1"), 0.0)
        set(s, 0, "D2", "=VLOOKUP(\"plum\",A1:B3,2,FALSE)"); assertEquals(30.0, num(book, 0, "D2"), 0.0)
        set(s, 0, "D3", "=IFERROR(MATCH(\"kiwi\",A1:A3,0),-1)"); assertEquals(-1.0, num(book, 0, "D3"), 0.0)
        // The looked-up column changes: every lookup must see the new value, not a stale index
        set(s, 0, "A1", "kiwi")
        assertEquals(1.0, num(book, 0, "D3"), 0.0)
        set(s, 0, "A2", "x"); assertEquals(Cell.CELL_TYPE_ERROR, book.getSheet(0)!!.getRow(0)!!.getCell(3)!!.getCellType())
        set(s, 0, "A3", "=\"pe\"&\"ar\"") // a formula cell in the column
        assertEquals(3.0, num(book, 0, "D1"), 0.0)
        assertEquals(Cell.CELL_TYPE_ERROR, book.getSheet(0)!!.getRow(1)!!.getCell(3)!!.getCellType()) // plum gone
        set(s, 0, "B3", "99"); set(s, 0, "D2", "=VLOOKUP(\"PEAR\",A1:B3,2,FALSE)"); assertEquals(99.0, num(book, 0, "D2"), 0.0)
    }

    @Test fun lowercaseWholeColumnVlookupAcrossSheets() {
        val book = book(); val s = session(book)
        set(s, 1, "B1", "Mã order"); set(s, 1, "Q1", "Version"); set(s, 1, "B2", "OA2888"); set(s, 1, "Q2", "1.2")
        set(s, 0, "E1", "Mã Order"); set(s, 0, "E2", "OA2888")
        set(s, 0, "U1", "=iferror(vlookup(\$E1,'S2'!\$B:\$Q,16,0),\"\")"); assertEquals("Version", str(s, book, "U1"))
        set(s, 0, "U2", "=iferror(vlookup(\$E2,'S2'!\$B:\$Q,16,0),\"\")"); assertEquals(1.2, num(book, 0, "U2"), 0.0)
    }

    @Test fun referencesToUnloadedSheetKeepSavedValue() {
        val book = book(); val s = session(book)
        set(s, 1, "A1", "5")
        set(s, 0, "A1", "=IFERROR(S2!A1*2,\"none\")"); assertEquals(10.0, num(book, 0, "A1"), 0.0)
        book.getSheet(1)!!.setState(Sheet.State_Reading) // S2 not loaded yet: its rows may be missing
        val warnings = ArrayList<String>()
        s.engine.recalc(warnings)
        assertEquals(10.0, num(book, 0, "A1"), 0.0) // kept, not "none"
        assertTrue(warnings.toString(), warnings.any { "S2" in it })
    }

    private fun xlsx(): File {
        val ns = SS.uri
        val rel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        val entries = linkedMapOf(
            "[Content_Types].xml" to """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/calcChain.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.calcChain+xml"/></Types>""",
            "xl/workbook.xml" to """<workbook xmlns="$ns" xmlns:r="$rel"><sheets><sheet name="S1" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="$rel/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId9" Type="$rel/calcChain" Target="calcChain.xml"/></Relationships>""",
            "xl/calcChain.xml" to """<calcChain xmlns="$ns"><c r="B1" i="1"/></calcChain>""",
            "xl/worksheets/sheet1.xml" to """<worksheet xmlns="$ns"><dimension ref="A1:C2"/><sheetData>""" +
                """<row r="1" spans="1:3"><c r="A1"><v>1</v></c><c r="B1"><f t="shared" ref="B1:B2" si="0">A1*2</f><v>2</v></c><c r="C1" s="3" t="s"><v>0</v></c></row>""" +
                """<row r="2"><c r="A2"><v>5</v></c><c r="B2"><f t="shared" si="0"/><v>10</v></c></row></sheetData></worksheet>""")
        val f = temp.newFile("book-${System.nanoTime()}.xlsx")
        ZipOutputStream(f.outputStream()).use { z -> entries.forEach { (n, x) -> z.putNextEntry(ZipEntry(n)); z.write(x.toByteArray()); z.closeEntry() } }
        return f
    }

    private fun cells(pkg: OoxmlPackage): Map<String, Element> = pkg.xml("xl/worksheets/sheet1.xml").rootElement.firstChild(SS, "sheetData")!!
        .childrenNamed(SS, "row").flatMap { it.childrenNamed(SS, "c") }.associateBy { it.attributeValue("r") }

    @Test fun writerPatchesCellsKeepsStylesAndDropsCalcChain() {
        val src = xlsx(); val out = File(temp.root, "out.xlsx")
        val result = XlsxWriter(src) { _, r, c -> if (r == 1 && c == 1) "A2*2" else null }.save(out, listOf(
            CellWrite.Text(0, 0, 2, " hi "),                    // replace a shared-string cell, keep s=3
            CellWrite.Number(0, 0, 1, 3.0, "A1*3"),             // overwrite the shared formula master
            CellWrite.Number(0, 4, 3, 42.0, "SUM(A1:A2)")))     // new row 5 + new cell D5
        assertTrue(result.toString(), result is EditResult.Ok)
        val pkg = OoxmlPackage.open(out); val c = cells(pkg)
        assertEquals("3", c["C1"]!!.attributeValue("s")); assertEquals("inlineStr", c["C1"]!!.attributeValue("t"))
        assertEquals(" hi ", c["C1"]!!.firstChild(SS, "is")!!.firstChild(SS, "t")!!.text)
        assertEquals("A1*3", c["B1"]!!.firstChild(SS, "f")!!.text); assertNull(c["B1"]!!.firstChild(SS, "f")!!.attributeValue("t"))
        assertEquals("A2*2", c["B2"]!!.firstChild(SS, "f")!!.text) // dependent expanded to an explicit formula
        assertEquals("SUM(A1:A2)", c["D5"]!!.firstChild(SS, "f")!!.text); assertEquals("42", c["D5"]!!.firstChild(SS, "v")!!.text)
        val sheet = pkg.xml("xl/worksheets/sheet1.xml").rootElement
        assertEquals(listOf("1", "2", "5"), sheet.firstChild(SS, "sheetData")!!.childrenNamed(SS, "row").map { it.attributeValue("r") })
        assertEquals("A1:D5", sheet.firstChild(SS, "dimension")!!.attributeValue("ref"))
        assertFalse(pkg.has("xl/calcChain.xml"))
        assertTrue(pkg.relationships("xl/workbook.xml").none { it.type.endsWith("/calcChain") })
        assertEquals("1", pkg.xml("xl/workbook.xml").rootElement.firstChild(SS, "calcPr")!!.attributeValue("fullCalcOnLoad"))
        assertFalse(String(pkg.bytes("[Content_Types].xml")!!).contains("calcChain"))
    }
}
