package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.common.bg.BackgroundAndFill
import com.wxiwei.office.common.borders.Line
import com.wxiwei.office.common.shape.TableCell
import com.wxiwei.office.common.shape.TableShape
import com.wxiwei.office.common.shape.TextBox
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.constant.wp.WPAttrConstant
import com.wxiwei.office.constant.wp.WPModelConstant
import com.wxiwei.office.fc.ShapeKit
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.fc.ppt.attribute.ParaAttr
import com.wxiwei.office.fc.ppt.attribute.RunAttr
import com.wxiwei.office.fc.ppt.attribute.SectionAttr
import com.wxiwei.office.java.awt.Rectangle
import com.wxiwei.office.java.awt.Rectanglef
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.pg.model.PGModel
import com.wxiwei.office.pg.model.tableStyle.TableCellBorders
import com.wxiwei.office.pg.model.tableStyle.TableCellStyle
import com.wxiwei.office.pg.model.tableStyle.TableStyle
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.ParagraphElement
import com.wxiwei.office.simpletext.model.SectionElement
import com.wxiwei.office.system.IControl

class TableReader private constructor() {
    fun getTable(control: IControl, zipPackage: ZipPackage, packagePart: PackagePart, pgModel: PGModel, master: PGMaster?, tbl: Element, rect: Rectangle): TableShape? {
        RunAttr.instance().setTable(true)
        var table: TableShape? = null
        val tblGrid = tbl.element("tblGrid")
        if (tblGrid != null) {
            val gridCols = tblGrid.elements("gridCol").map { it as Element }
            val colWidths = IntArray(gridCols.size)
            gridCols.forEachIndexed { index, col ->
                val width = (Integer.parseInt(col.attributeValue("w")) * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH).toInt()
                colWidths[index] = if (width > 0) width else (DEFAULT_CELL_WIDTH * MainConstant.POINT_TO_PIXEL).toInt()
            }
            val trs: List<Element> = tbl.elements("tr").map { it as Element }
            val rowHeights = IntArray(trs.size)
            trs.forEachIndexed { index, tr ->
                val height = (Integer.parseInt(tr.attributeValue("h")) * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH).toInt()
                rowHeights[index] = if (height > 0) height else (DEFAULT_CELL_HEIGHT * MainConstant.POINT_TO_PIXEL).toInt()
            }
            table = TableShape(rowHeights.size, colWidths.size)
            val tblPr = tbl.element("tblPr")
            val tableStyleId = tblPr?.element("tableStyleId")
            var style: TableStyle? = null
            if (tableStyleId != null) {
                style = pgModel.getTableStyle(tableStyleId.getText())
                table.setFirstRow("1".equals(tblPr?.attributeValue("firstRow"), true))
                table.setLastRow("1".equals(tblPr?.attributeValue("lastRow"), true))
                table.setFirstCol("1".equals(tblPr?.attributeValue("firstCol"), true))
                table.setLastCol("1".equals(tblPr?.attributeValue("lastCol"), true))
                table.setBandRow("1".equals(tblPr?.attributeValue("bandRow"), true))
                table.setBandCol("1".equals(tblPr?.attributeValue("bandCol"), true))
            }
            processTable(control, zipPackage, packagePart, master, trs, rect, table, colWidths, rowHeights, style)
        }
        RunAttr.instance().setTable(false)
        return table
    }

    private fun processLine(control: IControl, zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, tableStyle: TableStyle?, ln: Element?, styleColor: Int): Line? {
        return try {
            var width = 1
            var dash = false
            val fill: BackgroundAndFill?
            if (ln != null && ln.element("noFill") == null) {
                ln.attributeValue("w")?.let { width = Math.round(Integer.parseInt(it) * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH.toFloat()).toInt() }
                dash = ln.element("prstDash")?.attributeValue("val")?.equals("solid", true) == false
                fill = BackgroundReader.instance().processBackground(control, zipPackage, packagePart, master, ln)
            } else {
                fill = BackgroundAndFill()
                fill.setForegroundColor(styleColor)
            }
            Line().also { it.setBackgroundAndFill(fill); it.setLineWidth(width); it.setDash(dash) }
        } catch (_: Exception) { null }
    }

    private fun processTable(control: IControl, zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, trs: List<Element>, rect: Rectangle, table: TableShape, colWidths: IntArray, rowHeights: IntArray, style: TableStyle?) {
        trs.forEachIndexed { row, tr ->
            var col = 0
            for (tc in tr.elements("tc").map { it as Element }) {
                if (tc.attribute("hMerge") == null && tc.attribute("vMerge") == null) {
                    val cell = TableCell()
                    val anchor = Rectanglef(rect.x.toFloat(), rect.y.toFloat(), 0f, 0f)
                    repeat(col) { anchor.x += colWidths[it] }
                    repeat(row) { anchor.y += rowHeights[it] }
                    var width = colWidths[col]
                    var height = rowHeights[row]
                    tc.attributeValue("rowSpan")?.let { span -> repeat(Integer.parseInt(span) - 1) { height += rowHeights[row + it + 1] } }
                    tc.attributeValue("gridSpan")?.let { span -> repeat(Integer.parseInt(span) - 1) { width += colWidths[col + it + 1] } }
                    anchor.width = width.toFloat(); anchor.height = height.toFloat(); cell.setBounds(anchor)
                    var cellStyle = getTableCellBorders(style, row, col, table)
                    val tcPr = tc.element("tcPr")
                    if (tcPr != null) {
                        cell.setLeftLine(processLine(control, zipPackage, packagePart, master, style, tcPr.element("lnL"), borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getLeftBorder() }))
                        cell.setRightLine(processLine(control, zipPackage, packagePart, master, style, tcPr.element("lnR"), borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getRightBorder() }))
                        cell.setTopLine(processLine(control, zipPackage, packagePart, master, style, tcPr.element("lnT"), borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getTopBorder() }))
                        cell.setBottomLine(processLine(control, zipPackage, packagePart, master, style, tcPr.element("lnB"), borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getBottomBorder() }))
                    } else if (cellStyle != null) {
                        cell.setLeftLine(processLine(control, zipPackage, packagePart, master, style, null, borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getLeftBorder() }))
                        cell.setRightLine(processLine(control, zipPackage, packagePart, master, style, null, borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getRightBorder() }))
                        cell.setTopLine(processLine(control, zipPackage, packagePart, master, style, null, borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getTopBorder() }))
                        cell.setBottomLine(processLine(control, zipPackage, packagePart, master, style, null, borderColor(zipPackage, packagePart, master, style, cellStyle) { it.getBottomBorder() }))
                    } else {
                        val line = processLine(control, zipPackage, packagePart, master, style, null, 0xFF000000.toInt())
                        cell.setLeftLine(line); cell.setRightLine(line); cell.setTopLine(line); cell.setBottomLine(line)
                    }
                    var fill = BackgroundReader.instance().processBackground(control, zipPackage, packagePart, master, tcPr)
                    if (fill == null && cellStyle != null) fill = getTableCellFill(control, zipPackage, packagePart, master, style, cellStyle)
                    cell.setBackgroundAndFill(fill)
                    val box = TextBox()
                    val bounds = Rectangle(anchor.x.toInt(), anchor.y.toInt(), anchor.width.toInt(), anchor.height.toInt())
                    box.setBounds(bounds)
                    if (style != null && (cellStyle == null || cellStyle.getFontAttributeSet() == null)) cellStyle = style.getWholeTable()
                    processCellSection(control, master, box, bounds, tc, cellStyle)
                    cell.setText(box)
                    table.addCell(row * colWidths.size + col, cell)
                }
                col++
            }
        }
    }

    private fun borderColor(zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, style: TableStyle?, cellStyle: TableCellStyle?, pick: (TableCellBorders) -> Element?): Int {
        return try {
            var borders = cellStyle?.getTableCellBorders()
            if (borders == null) borders = style?.getWholeTable()?.getTableCellBorders()
            BackgroundReader.instance().getBackgroundColor(zipPackage, packagePart, master, borders?.let(pick), true)
        } catch (_: Exception) { 0xFF000000.toInt() }
    }

    private fun getTableCellFill(control: IControl, zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, style: TableStyle?, cellStyle: TableCellStyle): BackgroundAndFill? = try {
        var fill = cellStyle.getTableCellBgFill()
        if (fill == null) fill = style?.getWholeTable()?.getTableCellBgFill()
        BackgroundReader.instance().processBackground(control, zipPackage, packagePart, master, fill, true)
    } catch (_: Exception) { null }

    private fun getTableCellBorders(style: TableStyle?, row: Int, col: Int, table: TableShape): TableCellStyle? {
        if (style == null) return null
        val firstRow = table.isFirstRow(); val firstCol = table.isFirstCol()
        return when {
            firstRow && firstCol -> choose(style, row, col, table, true, true)
            firstRow -> choose(style, row, col, table, true, false)
            firstCol -> choose(style, row, col, table, false, true)
            else -> choose(style, row, col, table, false, false)
        }
    }

    private fun choose(s: TableStyle, row: Int, col: Int, t: TableShape, firstRow: Boolean, firstCol: Boolean): TableCellStyle? {
        var result: TableCellStyle? = when {
            firstRow && row == 0 -> s.getFirstRow()
            t.isLastRow() && row == t.getRowCount() - 1 -> s.getLastRow()
            firstCol && col == 0 -> s.getFirstCol()
            t.isLastCol() && col == t.getColumnCount() - 1 -> s.getLastCol()
            t.isBandRow() && row % 2 != 0 -> s.getBand1H()
            t.isBandCol() && ((firstCol && col % 2 != 0) || (!firstCol && col % 2 == 0)) -> s.getBand1V()
            else -> null
        }
        if (result == null) result = s.getWholeTable()
        return result
    }

    fun processCellSection(control: IControl, master: PGMaster?, tb: TextBox, rect: Rectangle, tc: Element, cellStyle: TableCellStyle?) {
        val section = SectionElement(); section.setStartOffset(0L); tb.setElement(section)
        val attr = section.getAttribute()
        AttrManage.instance().setPageWidth(attr, (rect.width * MainConstant.PIXEL_TO_TWIPS).toInt())
        AttrManage.instance().setPageHeight(attr, (rect.height * MainConstant.PIXEL_TO_TWIPS).toInt())
        val tcBody = tc.element("txBody") ?: return
        SectionAttr.instance().setSectionAttribute(tcBody.element("bodyPr"), attr, null, null, true)
        var left = ShapeKit.DefaultMargin_Twip * 2; var top = ShapeKit.DefaultMargin_Twip
        var right = ShapeKit.DefaultMargin_Twip * 2; var bottom = ShapeKit.DefaultMargin_Twip
        val tcPr = tc.element("tcPr")
        if (tcPr != null) {
            tcPr.attributeValue("marL")?.let { left = (Integer.parseInt(it) * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH * MainConstant.PIXEL_TO_TWIPS).toInt() }
            tcPr.attributeValue("marT")?.let { top = (Integer.parseInt(it) * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH * MainConstant.PIXEL_TO_TWIPS).toInt() }
            tcPr.attributeValue("marR")?.let { right = (Integer.parseInt(it) * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH * MainConstant.PIXEL_TO_TWIPS).toInt() }
            tcPr.attributeValue("marB")?.let { bottom = (Integer.parseInt(it) * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH * MainConstant.PIXEL_TO_TWIPS).toInt() }
            AttrManage.instance().setPageMarginTop(attr, top); AttrManage.instance().setPageMarginBottom(attr, bottom)
            AttrManage.instance().setPageMarginLeft(attr, left); AttrManage.instance().setPageMarginRight(attr, right)
            when (tcPr.attributeValue("anchor")) { "ctr", "just", "dist" -> AttrManage.instance().setPageVerticalAlign(attr, WPAttrConstant.PAGE_V_CENTER); "b" -> AttrManage.instance().setPageVerticalAlign(attr, WPAttrConstant.PAGE_V_BOTTOM); "t" -> AttrManage.instance().setPageVerticalAlign(attr, WPAttrConstant.PAGE_V_TOP) }
            if (pptXmlBoolean(tcPr.attributeValue("anchorCtr"))) AttrManage.instance().setPageHorizontalAlign(attr, WPAttrConstant.PAGE_H_CENTER)
        }
        val bodyPr = tcBody.element("bodyPr")
        tb.setWrapLine(bodyPr?.attributeValue("wrap")?.equals("square", true) != false)
        section.setEndOffset(processParagraph(control, master, section, tcBody, cellStyle).toLong())
    }

    fun processParagraph(control: IControl, master: PGMaster?, section: SectionElement, txBody: Element, cellStyle: TableCellStyle?): Int {
        var offset = 0; var reduction = 0
        txBody.element("bodyPr")?.element("normAutofit")?.attributeValue("lnSpcReduction")?.takeIf { it.isNotEmpty() }?.let { reduction = Integer.parseInt(it) }
        val paragraphs = txBody.elements("p").map { it as Element }
        paragraphs.forEachIndexed { index, p ->
            val para = ParagraphElement(); para.setStartOffset(offset.toLong())
            ParaAttr.instance().setParaAttribute(control, p.element("pPr"), para.getAttribute(), null, -1, -1, reduction, true, false)
            offset = RunAttr.instance().processRun(master, para, p, cellStyle?.getFontAttributeSet(), offset, 100, -1)
            ParaAttr.instance().processParaWithPct(p.element("pPr"), para.getAttribute())
            if (index == 0) AttrManage.instance().setParaBefore(para.getAttribute(), 0)
            if (index == paragraphs.size - 1) AttrManage.instance().setParaAfter(para.getAttribute(), 0)
            para.setEndOffset(offset.toLong()); section.appendParagraph(para, WPModelConstant.MAIN)
        }
        return offset
    }

    companion object {
        const val DEFAULT_CELL_WIDTH = 100
        const val DEFAULT_CELL_HEIGHT = 40
        private val kit = TableReader()
        @JvmStatic fun instance(): TableReader = kit
    }
}
