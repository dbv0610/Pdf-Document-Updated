package com.wxiwei.office.fc.ppt.attribute

import android.graphics.Color
import com.wxiwei.office.constant.wp.AttrIDConstant
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.ppt.reader.HyperlinkReader
import com.wxiwei.office.fc.ppt.reader.ReaderKit
import com.wxiwei.office.fc.xls.Reader.SchemeColorUtil
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.simpletext.font.Font
import com.wxiwei.office.simpletext.font.FontTypefaceManage
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.IAttributeSet
import com.wxiwei.office.simpletext.model.LeafElement
import com.wxiwei.office.simpletext.model.ParagraphElement
import com.wxiwei.office.simpletext.model.StyleManage
import com.wxiwei.office.ss.model.baseModel.Cell
import com.wxiwei.office.ss.model.baseModel.Sheet
import com.wxiwei.office.ss.model.baseModel.Workbook
import com.wxiwei.office.ss.model.style.CellStyle
import com.wxiwei.office.ss.util.ColorUtil
import com.wxiwei.office.ss.util.format.NumericFormatter
import java.util.Date

class RunAttr private constructor() {
    companion object {
        private val kit = RunAttr()
        @JvmStatic fun instance(): RunAttr = kit
    }

    fun processRun(pgMaster: PGMaster?, paraElem: ParagraphElement, p: Element, attrLayout: IAttributeSet?, offset0: Int, fontScale: Int, styleID: Int): Int {
        maxFontSize = 0
        var offset = offset0
        var leaf: LeafElement? = null
        var pPr = p.element("pPr")
        if (p.elements("r").isEmpty() && p.elements("fld").isEmpty() && p.elements("br").isEmpty()) {
            leaf = LeafElement("\n")
            pPr = pPr?.element("rPr") ?: p.element("endParaRPr")
            setRunAttribute(pgMaster, pPr, leaf.getAttribute(), attrLayout, fontScale, styleID, true)
            setMaxFontSize(AttrManage.instance().getFontSize(paraElem.getAttribute(), leaf.getAttribute()))
            leaf.setStartOffset(offset.toLong())
            offset++
            leaf.setEndOffset(offset.toLong())
            paraElem.appendLeaf(leaf)
            return offset
        }
        val iterator = p.elementIterator()
        while (iterator.hasNext()) {
            val r = iterator.next() as Element
            val name = r.getName()
            if (name == "r" || name == "fld" || name == "br") {
                val text = if (name == "fld" && r.attributeValue("type")?.contains("datetime") == true) {
                    NumericFormatter.instance().getFormatContents("yyyy/m/d", Date(System.currentTimeMillis()))
                } else if (name == "br") {
                    "\u000b"
                } else {
                    r.element("t")?.getText()
                }
                if (text != null && text.isNotEmpty()) {
                    val value = text.replace(160.toChar(), ' ')
                    leaf = LeafElement(value)
                    setRunAttribute(pgMaster, r.element("rPr"), leaf.getAttribute(), attrLayout, fontScale, styleID, value == "\n")
                    setMaxFontSize(AttrManage.instance().getFontSize(paraElem.getAttribute(), leaf.getAttribute()))
                    leaf.setStartOffset(offset.toLong())
                    offset += value.length
                    leaf.setEndOffset(offset.toLong())
                    paraElem.appendLeaf(leaf)
                }
            }
        }
        if (leaf != null) {
            leaf.setText(leaf.getText(null) + "\n")
            offset++
        }
        return offset
    }

    private fun copy(from: IAttributeSet?, to: IAttributeSet, id: Int, action: (IAttributeSet) -> Unit) {
        if (from != null && AttrManage.instance().hasAttribute(from, id.toShort())) action(to)
    }

    private fun setFontSize(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_SIZE_ID.toInt()) { AttrManage.instance().setFontSize(it, AttrManage.instance().getFontSize(null, from).toInt()) }
    private fun setFontTypeface(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_NAME_ID.toInt()) { AttrManage.instance().setFontName(it, AttrManage.instance().getFontName(null, from).toInt()) }
    private fun setFontColor(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_COLOR_ID.toInt()) { AttrManage.instance().setFontColor(it, AttrManage.instance().getFontColor(null, from).toInt()) }
    private fun setFontBold(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_BOLD_ID.toInt()) { AttrManage.instance().setFontBold(it, AttrManage.instance().getFontBold(null, from)) }
    private fun setFontItalic(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_ITALIC_ID.toInt()) { AttrManage.instance().setFontItalic(it, AttrManage.instance().getFontItalic(null, from)) }
    private fun setFontStrike(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_STRIKE_ID.toInt()) { AttrManage.instance().setFontStrike(it, AttrManage.instance().getFontStrike(null, from)) }
    private fun setFontDoubleStrike(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_DOUBLESTRIKE_ID.toInt()) { AttrManage.instance().setFontDoubleStrike(it, AttrManage.instance().getFontDoubleStrike(null, from)) }
    private fun setFontScript(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_SCRIPT_ID.toInt()) { AttrManage.instance().setFontScript(it, AttrManage.instance().getFontScript(null, from).toInt()) }
    private fun setHyperlinkID(from: IAttributeSet?, to: IAttributeSet) = copy(from, to, AttrIDConstant.FONT_HYPERLINK_ID.toInt()) { AttrManage.instance().setHyperlinkID(it, AttrManage.instance().getHperlinkID(from).toInt()) }

    private fun setFontUnderline(from: IAttributeSet?, to: IAttributeSet) {
        if (from != null && AttrManage.instance().hasAttribute(from, AttrIDConstant.FONT_UNDERLINE_ID)) {
            AttrManage.instance().setFontUnderline(to, AttrManage.instance().getFontUnderline(null, from))
            if (AttrManage.instance().hasAttribute(from, AttrIDConstant.FONT_UNDERLINE_COLOR_ID)) AttrManage.instance().setFontUnderlineColr(to, AttrManage.instance().getFontUnderlineColor(null, from))
            else if (AttrManage.instance().hasAttribute(from, AttrIDConstant.FONT_COLOR_ID)) AttrManage.instance().setFontUnderlineColr(to, AttrManage.instance().getFontColor(null, from))
        }
    }

    fun setRunAttribute(master: PGMaster?, rPr: Element?, attr: IAttributeSet, attrLayout: IAttributeSet?, fontScale: Int, styleID: Int, newLine: Boolean) {
        if (rPr != null) {
            val size = rPr.attributeValue("sz")
            if (!size.isNullOrEmpty()) AttrManage.instance().setFontSize(attr, (size.toFloat() / 100).toInt()) else setFontSize(attrLayout, attr)
            if (!newLine) {
                val typeface = rPr.element("latin") ?: rPr.element("ea")
                if (typeface != null) {
                    val index = FontTypefaceManage.instance().addFontName(typeface.attributeValue("typeface"))
                    if (index >= 0) AttrManage.instance().setFontName(attr, index)
                } else setFontTypeface(attrLayout, attr)
                val fill = rPr.element("solidFill") ?: rPr.element("gradFill")?.element("gsLst")?.element("gs")
                if (fill != null) AttrManage.instance().setFontColor(attr, ReaderKit.instance().getColor(master, fill)) else setFontColor(attrLayout, attr)
                rPr.attributeValue("b")?.let { AttrManage.instance().setFontBold(attr, it == "1" || it.equals("true", true)) } ?: setFontBold(attrLayout, attr)
                rPr.attributeValue("i")?.let { AttrManage.instance().setFontItalic(attr, it == "1" || it.equals("true", true)) } ?: setFontItalic(attrLayout, attr)
                val underline = rPr.attributeValue("u")
                if (!underline.isNullOrEmpty() && !underline.equals("none", true)) AttrManage.instance().setFontUnderline(attr, 1) else setFontUnderline(attrLayout, attr)
                when (rPr.attributeValue("strike")) { "dblStrike" -> AttrManage.instance().setFontDoubleStrike(attr, true); "sngStrike" -> AttrManage.instance().setFontStrike(attr, true); else -> { setFontStrike(attrLayout, attr); setFontDoubleStrike(attrLayout, attr) } }
                val baseline = rPr.attributeValue("baseline")
                if (!baseline.isNullOrEmpty() && baseline != "0") AttrManage.instance().setFontScript(attr, if (baseline.toInt() > 0) 1 else 2) else setFontScript(attrLayout, attr)
                val link = rPr.element("hlinkClick")
                if (link != null) {
                    val color = master?.getSchemeColor()?.get("hlink") ?: Color.BLUE
                    AttrManage.instance().setFontColor(attr, color)
                    AttrManage.instance().setFontUnderline(attr, 1)
                    AttrManage.instance().setFontUnderlineColr(attr, color)
                    link.attributeValue("id")?.let { AttrManage.instance().setHyperlinkID(attr, HyperlinkReader.instance().getLinkIndex(it)) }
                } else setHyperlinkID(attrLayout, attr)
            }
        } else if (attrLayout != null) {
            setFontSize(attrLayout, attr)
            if (!newLine) { setFontTypeface(attrLayout, attr); setFontColor(attrLayout, attr); setFontBold(attrLayout, attr); setFontItalic(attrLayout, attr); setFontUnderline(attrLayout, attr); setFontStrike(attrLayout, attr); setFontDoubleStrike(attrLayout, attr); setFontScript(attrLayout, attr); setHyperlinkID(attrLayout, attr) }
        }
        AttrManage.instance().setFontScale(attr, fontScale)
        if (!AttrManage.instance().hasAttribute(attr, AttrIDConstant.FONT_SIZE_ID) && StyleManage.instance().getStyle(styleID)?.getAttrbuteSet()?.let { AttrManage.instance().hasAttribute(it, AttrIDConstant.FONT_SIZE_ID) } != true && !table && slide) AttrManage.instance().setFontSize(attr, 18)
    }

    fun getColor(book: Workbook, solidFillElement: Element): Int {
        val clr = solidFillElement.element("srgbClr") ?: solidFillElement.element("schemeClr") ?: solidFillElement.element("sysClr") ?: return -1
        var color = if (clr.getName() == "srgbClr") (0xFF000000.toInt() or clr.attributeValue("val").toInt(16)) else if (clr.getName() == "sysClr") (0xFF000000.toInt() or clr.attributeValue("lastClr").toInt(16)) else SchemeColorUtil.getSchemeColor(book)[clr.attributeValue("val")] ?: -1
        clr.element("tint")?.attributeValue("val")?.let { color = ColorUtil.instance().getColorWithTint(color, it.toInt() / 100000.0) }
        clr.element("lumOff")?.attributeValue("val")?.let { color = ColorUtil.instance().getColorWithTint(color, it.toInt() / 100000.0) }
        clr.element("lumMod")?.attributeValue("val")?.let { color = ColorUtil.instance().getColorWithTint(color, it.toInt() / 100000.0 - 1) }
        clr.element("alpha")?.attributeValue("val")?.let { color = (color and 0xFFFFFF) or ((it.toInt() / 100000f * 255).toInt() shl 24) }
        return color
    }

    fun setRunAttribute(sheet: Sheet, rPr: Element?, attr: IAttributeSet, attrLayout: IAttributeSet?) = setSheetAttribute(sheet.getWorkbook()!!, rPr, attr, attrLayout)
    fun setRunAttribute(book: Workbook, fontID: Int, rPr: Element?, attr: IAttributeSet, attrLayout: IAttributeSet?) {
        if (rPr == null) { setFontFromBook(book, fontID, attr); return }
        rPr.element("sz")?.attributeValue("val")?.let { AttrManage.instance().setFontSize(attr, it.toFloat().toInt()) } ?: setFontSize(attrLayout, attr)
        rPr.element("color")?.let { AttrManage.instance().setFontColor(attr, getRunPropColor(book, it)) } ?: setFontColor(attrLayout, attr)
        if (rPr.element("b") != null) AttrManage.instance().setFontBold(attr, true) else setFontBold(attrLayout, attr)
        if (rPr.element("i") != null) AttrManage.instance().setFontItalic(attr, true) else setFontItalic(attrLayout, attr)
        if (rPr.element("u") != null) AttrManage.instance().setFontUnderline(attr, 1) else setFontUnderline(attrLayout, attr)
        if (rPr.element("strike") != null) AttrManage.instance().setFontStrike(attr, true) else setFontStrike(attrLayout, attr)
        rPr.element("vertAlign")?.attributeValue("val")?.let { AttrManage.instance().setFontScript(attr, (if (it.equals("superscript", true)) Font.SS_SUPER else if (it.equals("subscript", true)) Font.SS_SUB else Font.SS_NONE).toInt()) } ?: setFontScript(attrLayout, attr)
    }
    fun setRunAttribute(sheet: Sheet, cell: Cell?, attr: IAttributeSet, attrLayout: IAttributeSet?) { if (cell != null) setFontFromBook(sheet.getWorkbook()!!, cell.getCellStyle()!!.getFontIndex().toInt(), attr) else if (attrLayout != null) { setFontSize(attrLayout, attr); setFontColor(attrLayout, attr); setFontBold(attrLayout, attr); setFontItalic(attrLayout, attr); setFontUnderline(attrLayout, attr); setFontStrike(attrLayout, attr); setFontDoubleStrike(attrLayout, attr); setFontScript(attrLayout, attr); setHyperlinkID(attrLayout, attr) } }
    fun setRunAttribute(sheet: Sheet, font: Font?, attr: IAttributeSet, attrLayout: IAttributeSet?) { if (font != null) setFontFromBook(sheet.getWorkbook()!!, font, attr) else if (attrLayout != null) { setFontSize(attrLayout, attr); setFontColor(attrLayout, attr); setFontBold(attrLayout, attr); setFontItalic(attrLayout, attr); setFontUnderline(attrLayout, attr); setFontStrike(attrLayout, attr); setFontDoubleStrike(attrLayout, attr); setFontScript(attrLayout, attr); setHyperlinkID(attrLayout, attr) } }
    private fun setSheetAttribute(book: Workbook, rPr: Element?, attr: IAttributeSet, layout: IAttributeSet?) { if (rPr == null) { if (layout != null) { setFontSize(layout, attr); setFontColor(layout, attr); setFontBold(layout, attr); setFontItalic(layout, attr); setFontUnderline(layout, attr); setFontStrike(layout, attr) }; return }; rPr.element("sz")?.attributeValue("val")?.let { AttrManage.instance().setFontSize(attr, it.toFloat().toInt()) } ?: setFontSize(layout, attr); rPr.element("solidFill")?.let { AttrManage.instance().setFontColor(attr, getColor(book, it)) } ?: setFontColor(layout, attr); if (rPr.attributeValue("b") == "1") AttrManage.instance().setFontBold(attr, true) else setFontBold(layout, attr); if (rPr.attributeValue("i") == "1") AttrManage.instance().setFontItalic(attr, true) else setFontItalic(layout, attr) }
    private fun setFontFromBook(book: Workbook, id: Int, attr: IAttributeSet) { setFontFromBook(book, book.getFont(id), attr) }
    private fun setFontFromBook(book: Workbook, font: Font?, attr: IAttributeSet) { if (font != null) { AttrManage.instance().setFontSize(attr, (font.getFontSize() + .5f).toInt()); AttrManage.instance().setFontColor(attr, book.getColor(font.getColorIndex())); AttrManage.instance().setFontBold(attr, font.isBold()); AttrManage.instance().setFontItalic(attr, font.isItalic()); AttrManage.instance().setFontUnderline(attr, font.getUnderline()); AttrManage.instance().setFontStrike(attr, font.isStrikeline()) } }
    private fun getRunPropColor(book: Workbook, clr: Element): Int { var color = when { clr.attributeValue("indexed") != null -> book.getColor(clr.attributeValue("indexed").toInt()); clr.attributeValue("theme") != null -> SchemeColorUtil.getThemeColor(book, clr.attributeValue("theme").toInt()); else -> clr.attributeValue("rgb").toLong(16).toInt() }; clr.attributeValue("tint")?.let { color = ColorUtil.instance().getColorWithTint(color, it.toDouble()) }; return color }
    fun getMaxFontSize(): Int = maxFontSize
    fun setMaxFontSize(size: Int) { if (size > maxFontSize) maxFontSize = size }
    fun resetMaxFontSize() { maxFontSize = 0 }
    fun setTable(value: Boolean) { table = value }
    fun setSlide(value: Boolean) { slide = value }
    fun isTable(): Boolean = table
    fun isSlide(): Boolean = slide
    fun dispose() { maxFontSize = 0 }
    private var maxFontSize = 0
    private var table = false
    private var slide = false
}
