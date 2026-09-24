package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.dom4j.ElementHandler
import com.wxiwei.office.fc.dom4j.ElementPath
import com.wxiwei.office.fc.dom4j.io.SAXReader
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.pg.model.PGModel
import com.wxiwei.office.pg.model.tableStyle.TableCellBorders
import com.wxiwei.office.pg.model.tableStyle.TableCellStyle
import com.wxiwei.office.pg.model.tableStyle.TableStyle
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.AttributeSetImpl

class TableStyleReader private constructor() {
    fun read(pgModel: PGModel, tableStyle: PackagePart, defaultFontSize: Int) {
        this.pgModel = pgModel
        this.defaultFontSize = defaultFontSize
        val saxReader = SAXReader()
        try {
            val input = tableStyle.inputStream
            saxReader.addHandler("/tblStyleLst/tblStyle", TableStyleSaxHandler())
            saxReader.read(input)
            input.close()
            this.pgModel = null
        } finally {
            saxReader.resetHandlers()
        }
    }

    private fun processTableStyle(element: Element) {
        val style = TableStyle()
        val names = arrayOf("wholeTbl", "band1H", "band2H", "band1V", "band2V", "lastCol", "firstCol", "lastRow", "firstRow")
        val setters: Array<(TableCellStyle) -> Unit> = arrayOf(style::setWholeTable, style::setBand1H, style::setBand2H, style::setBand1V, style::setBand2V, style::setLastCol, style::setFirstCol, style::setLastRow, style::setFirstRow)
        for (i in names.indices) element.element(names[i])?.let { setters[i](processTableCellStyle(it)) }
        pgModel?.putTableStyle(element.attributeValue("styleId"), style)
    }

    private fun processTableCellStyle(element: Element): TableCellStyle {
        val cellStyle = TableCellStyle()
        val textStyle = element.element("tcTxStyle")
        if (textStyle != null) {
            val attr = AttributeSetImpl()
            if (textStyle.attributeValue("b") == "on") AttrManage.instance().setFontBold(attr, true)
            if (textStyle.attributeValue("i") == "on") AttrManage.instance().setFontItalic(attr, true)
            AttrManage.instance().setFontSize(attr, defaultFontSize)
            cellStyle.setFontAttributeSet(attr)
        }
        val cellStyleElement = element.element("tcStyle")!!
        cellStyleElement.element("tcBdr")?.let { cellStyle.setTableCellBorders(getTableCellBorders(it)) }
        cellStyle.setTableCellBgFill(cellStyleElement.element("fill"))
        return cellStyle
    }

    private fun getTableCellBorders(element: Element): TableCellBorders {
        val borders = TableCellBorders()
        element.element("left")?.element("ln")?.let { borders.setLeftBorder(it) }
        element.element("right")?.element("ln")?.let { borders.setRightBorder(it) }
        element.element("top")?.element("ln")?.let { borders.setTopBorder(it) }
        element.element("bottom")?.element("ln")?.let { borders.setBottomBorder(it) }
        return borders
    }

    private inner class TableStyleSaxHandler : ElementHandler {
        override fun onStart(elementPath: ElementPath) {}

        override fun onEnd(elementPath: ElementPath) {
            val element = elementPath.current
            try {
                if (element.name == "tblStyle") processTableStyle(element)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            element.detach()
        }
    }

    private var pgModel: PGModel? = null
    private var defaultFontSize = 12

    companion object {
        private val tableStyleReader = TableStyleReader()
        @JvmStatic fun instance(): TableStyleReader = tableStyleReader
    }
}
