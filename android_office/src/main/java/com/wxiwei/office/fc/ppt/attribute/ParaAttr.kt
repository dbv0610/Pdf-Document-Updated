package com.wxiwei.office.fc.ppt.attribute

import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.constant.SSConstant
import com.wxiwei.office.constant.wp.AttrIDConstant
import com.wxiwei.office.constant.wp.WPAttrConstant
import com.wxiwei.office.constant.wp.WPModelConstant
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.ppt.bulletnumber.BulletNumberManage
import com.wxiwei.office.fc.ppt.reader.ReaderKit
import com.wxiwei.office.pg.model.PGLayout
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.pg.model.PGPlaceholderUtil
import com.wxiwei.office.pg.model.PGStyle
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.AttributeSetImpl
import com.wxiwei.office.simpletext.model.IAttributeSet
import com.wxiwei.office.simpletext.model.ParagraphElement
import com.wxiwei.office.simpletext.model.SectionElement
import com.wxiwei.office.simpletext.model.StyleManage
import com.wxiwei.office.ss.model.style.CellStyle
import com.wxiwei.office.system.IControl

class ParaAttr private constructor() {
    companion object {
        const val POINT_PER_LINE_PER_FONTSIZE = 1.2f
        private val kit = ParaAttr()
        @JvmStatic fun instance(): ParaAttr = kit
    }

    fun processParagraph(control: IControl?, master: PGMaster?, pgLayout: PGLayout?, defaultStyle: PGStyle?, secElem: SectionElement, styleElement: Element?, txBody: Element, type: String, idx: Int): Int {
        var fontScale = 100
        var lnSpcReduction = 0
        var defaultFontColor = false
        val bodyPr = txBody.element("bodyPr")
        val normAutofit = bodyPr?.element("normAutofit")
        val fontScaleValue = normAutofit?.attributeValue("fontScale")
        if (!fontScaleValue.isNullOrEmpty()) fontScale = Integer.parseInt(fontScaleValue) / 1000
        val reductionValue = normAutofit?.attributeValue("lnSpcReduction")
        if (!reductionValue.isNullOrEmpty()) lnSpcReduction = Integer.parseInt(reductionValue)
        var offset = 0
        val subTitle = PGPlaceholderUtil.SUBTITLE == type
        val lstStyle = txBody.element("lstStyle")
        for (pAny in txBody.elements("p")) {
            val p = pAny as Element
            var lvl = 1
            val pPr = p.element("pPr")
            val level = pPr?.attributeValue("lvl")
            if (!level.isNullOrEmpty()) lvl += Integer.parseInt(level)
            val layoutStyle = pgLayout?.getStyleID(type, idx, lvl) ?: -1
            var masterStyle = master?.getTextStyle(type, idx, lvl) ?: -1
            if (masterStyle < 0 && defaultStyle != null) {
                defaultFontColor = true
                masterStyle = defaultStyle.getStyle(lvl)
            }
            val paraElem = ParagraphElement()
            paraElem.setStartOffset(offset.toLong())
            var attrLayout: IAttributeSet? = null
            if (lstStyle != null) {
                var ind = lvl
                if (lvl > 0 || lstStyle.element("defPPr") == null) ind++
                val tag = when (ind) {
                    1 -> "defPPr"
                    2 -> "lvl1pPr"
                    3 -> "lvl2pPr"
                    4 -> "lvl3pPr"
                    5 -> "lvl4pPr"
                    6 -> "lvl5pPr"
                    7 -> "lvl6pPr"
                    8 -> "lvl7pPr"
                    9 -> "lvl8pPr"
                    10 -> "lvl9pPr"
                    else -> null
                }
                val txStyle = tag?.let { lstStyle.element(it) }
                if (txStyle != null) {
                    attrLayout = AttributeSetImpl()
                    setParaAttribute(control, txStyle, attrLayout, null, -1, -1, 0, true, subTitle)
                    RunAttr.instance().setRunAttribute(master, txStyle.element("defRPr"), attrLayout, null, 100, -1, false)
                    processParaWithPct(txStyle, attrLayout)
                }
            }
            if (attrLayout == null && layoutStyle > 0) {
                attrLayout = StyleManage.instance().getStyle(layoutStyle)?.getAttrbuteSet()
            } else if (attrLayout == null && styleElement != null) {
                val fontRef = styleElement.element("fontRef")
                if (fontRef.elements().size > 0) {
                    attrLayout = AttributeSetImpl()
                    AttrManage.instance().setFontColor(attrLayout, ReaderKit.instance().getColor(master, fontRef))
                }
            } else if (defaultFontColor && attrLayout == null && defaultStyle != null) {
                val fontColor = defaultStyle.getDefaultFontColor(lvl)
                if (fontColor != null) {
                    attrLayout = AttributeSetImpl()
                    AttrManage.instance().setFontColor(attrLayout, master!!.getColor(fontColor))
                }
            }
            offset = RunAttr.instance().processRun(master, paraElem, p, attrLayout, offset, fontScale, masterStyle)
            setParaAttribute(control, pPr, paraElem.getAttribute(), attrLayout, layoutStyle, masterStyle, lnSpcReduction, p.elements("r").size > 0 || p.elements("fld").size > 0, subTitle)
            processParaWithPct(pPr, paraElem.getAttribute())
            paraElem.setEndOffset(offset.toLong())
            secElem.appendParagraph(paraElem, WPModelConstant.MAIN)
        }
        BulletNumberManage.instance().clearData()
        RunAttr.instance().setMaxFontSize(0)
        return offset
    }

    fun setParaAlign(attr: IAttributeSet, align: String) {
        when (align) {
            "l" -> AttrManage.instance().setParaHorizontalAlign(attr, WPAttrConstant.PARA_HOR_ALIGN_LEFT.toInt())
            "ctr" -> AttrManage.instance().setParaHorizontalAlign(attr, WPAttrConstant.PARA_HOR_ALIGN_CENTER.toInt())
            "r" -> AttrManage.instance().setParaHorizontalAlign(attr, WPAttrConstant.PARA_HOR_ALIGN_RIGHT.toInt())
        }
    }

    fun setParaHorizontalAlign(from: IAttributeSet?, to: IAttributeSet) {
        if (from != null && AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_HORIZONTAL_ID)) {
            AttrManage.instance().setParaHorizontalAlign(to, AttrManage.instance().getParaHorizontalAlign(from))
        }
    }
    fun setParaBefore(from: IAttributeSet?, to: IAttributeSet) { if (from != null && AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_BEFORE_ID)) AttrManage.instance().setParaBefore(to, AttrManage.instance().getParaBefore(from)) }
    fun setParaAfter(from: IAttributeSet?, to: IAttributeSet) { if (from != null && AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_AFTER_ID)) AttrManage.instance().setParaAfter(to, AttrManage.instance().getParaAfter(from)) }
    fun setParaLineSpace(from: IAttributeSet?, to: IAttributeSet) {
        if (from != null) {
            if (AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_LINESPACE_TYPE_ID)) AttrManage.instance().setParaLineSpaceType(to, AttrManage.instance().getParaLineSpaceType(from))
            if (AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_LINESPACE_ID)) AttrManage.instance().setParaLineSpace(to, AttrManage.instance().getParaLineSpace(from))
        }
    }
    fun setParaIndentLeft(from: IAttributeSet?, to: IAttributeSet) { if (from != null && AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_INDENT_LEFT_ID)) AttrManage.instance().setParaIndentLeft(to, AttrManage.instance().getParaIndentLeft(from)) }
    fun setParaIndentRight(from: IAttributeSet?, to: IAttributeSet) { if (from != null && AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_INDENT_RIGHT_ID)) AttrManage.instance().setParaIndentRight(to, AttrManage.instance().getParaIndentRight(from)) }
    fun setParaSpecialIndent(from: IAttributeSet?, to: IAttributeSet) { if (from != null && AttrManage.instance().hasAttribute(from, AttrIDConstant.PARA_SPECIALINDENT_ID)) AttrManage.instance().setParaSpecialIndent(to, AttrManage.instance().getParaSpecialIndent(from)) }

    fun setParaAttribute(control: IControl?, pPr: Element?, attr: IAttributeSet, attrLayout: IAttributeSet?, layoutStyle: Int, masterStyle: Int, lnSpcReduction: Int, addBullet: Boolean, subTitle: Boolean) {
        val style = StyleManage.instance().getStyle(masterStyle)
        if (pPr != null) {
            pPr.attributeValue("algn")?.let { setParaAlign(attr, it) } ?: setParaHorizontalAlign(attrLayout, attr)
            fun points(tag: String, setter: (Int) -> Unit) {
                val value = pPr.element(tag)?.element("spcPts")?.attributeValue("val")
                if (!value.isNullOrEmpty()) setter((Integer.parseInt(value) / 100 * MainConstant.POINT_TO_TWIPS).toInt())
            }
            if (pPr.element("spcBef") != null) points("spcBef") { AttrManage.instance().setParaBefore(attr, it) } else setParaBefore(attrLayout, attr)
            if (pPr.element("spcAft") != null) points("spcAft") { AttrManage.instance().setParaAfter(attr, it) } else setParaAfter(attrLayout, attr)
            val lnSpc = pPr.element("lnSpc")
            val exact = lnSpc?.element("spcPts")?.attributeValue("val")
            val pct = lnSpc?.element("spcPct")?.attributeValue("val")
            when {
                !exact.isNullOrEmpty() -> {
                    AttrManage.instance().setParaLineSpaceType(attr, WPAttrConstant.LINE_SPACE_EXACTLY.toInt())
                    AttrManage.instance().setParaLineSpace(attr, (Integer.parseInt(exact) / 100 * MainConstant.POINT_TO_TWIPS).toFloat())
                }
                !pct.isNullOrEmpty() -> {
                    AttrManage.instance().setParaLineSpaceType(attr, WPAttrConstant.LINE_SAPCE_MULTIPLE.toInt())
                    AttrManage.instance().setParaLineSpace(attr, (Integer.parseInt(pct) - lnSpcReduction).toFloat() / 100000)
                }
                lnSpcReduction > 0 -> {
                    AttrManage.instance().setParaLineSpaceType(attr, WPAttrConstant.LINE_SAPCE_MULTIPLE.toInt())
                    AttrManage.instance().setParaLineSpace(attr, (100000 - lnSpcReduction).toFloat() / 100000)
                }
                else -> setParaLineSpace(attrLayout, attr)
            }
            val right = pPr.attributeValue("marR")
            if (!right.isNullOrEmpty()) AttrManage.instance().setParaIndentRight(attr, (Integer.parseInt(right) * MainConstant.POINT_DPI / MainConstant.EMU_PER_INCH * MainConstant.POINT_TO_TWIPS).toInt()) else setParaIndentRight(attrLayout, attr)
        } else {
            setParaHorizontalAlign(attrLayout, attr)
            setParaBefore(attrLayout, attr)
            setParaAfter(attrLayout, attr)
            if (lnSpcReduction > 0) {
                AttrManage.instance().setParaLineSpaceType(attr, WPAttrConstant.LINE_SAPCE_MULTIPLE.toInt())
                AttrManage.instance().setParaLineSpace(attr, (100000 - lnSpcReduction).toFloat() / 100000)
            } else {
                setParaLineSpace(attrLayout, attr)
            }
            setParaIndentLeft(attrLayout, attr)
            setParaIndentRight(attrLayout, attr)
        }
        var left = 0
        val leftValue = pPr?.attributeValue("marL")
        if (!leftValue.isNullOrEmpty()) {
            left = (Integer.parseInt(leftValue) * MainConstant.POINT_DPI / MainConstant.EMU_PER_INCH * MainConstant.POINT_TO_TWIPS).toInt()
            AttrManage.instance().setParaIndentInitLeft(attr, left)
            AttrManage.instance().setParaIndentLeft(attr, left)
        } else if (attrLayout != null && AttrManage.instance().hasAttribute(attrLayout, AttrIDConstant.PARA_INDENT_LEFT_ID)) {
            left = AttrManage.instance().getParaIndentInitLeft(attrLayout)
            AttrManage.instance().setParaIndentLeft(attr, left)
        } else if (style?.getAttrbuteSet() != null && AttrManage.instance().hasAttribute(style.getAttrbuteSet(), AttrIDConstant.PARA_INDENT_LEFT_ID)) {
            left = AttrManage.instance().getParaIndentInitLeft(style.getAttrbuteSet())
            AttrManage.instance().setParaIndentLeft(attr, left)
        }
        val indentValue = pPr?.attributeValue("indent")
        if (!indentValue.isNullOrEmpty()) setSpecialIndent(attr, left, (Integer.parseInt(indentValue) * MainConstant.POINT_DPI / MainConstant.EMU_PER_INCH * MainConstant.POINT_TO_TWIPS).toInt(), true)
        else if (attrLayout != null && AttrManage.instance().hasAttribute(attrLayout, AttrIDConstant.PARA_SPECIALINDENT_ID)) setSpecialIndent(attr, left, AttrManage.instance().getParaSpecialIndent(attrLayout), true)
        else if (style?.getAttrbuteSet() != null && AttrManage.instance().hasAttribute(style.getAttrbuteSet(), AttrIDConstant.PARA_SPECIALINDENT_ID)) setSpecialIndent(attr, left, AttrManage.instance().getParaSpecialIndent(style.getAttrbuteSet()), true)
        if (addBullet && (pPr == null || pPr.element("buNone") == null)) {
            var id = BulletNumberManage.instance().addBulletNumber(control, -1, pPr)
            if (id == -1 && attrLayout != null) id = AttrManage.instance().getPGParaBulletID(attrLayout)
            if (id == -1 && layoutStyle >= 0) id = BulletNumberManage.instance().getBulletID(layoutStyle)
            if (id == -1 && masterStyle > 0 && !subTitle) id = BulletNumberManage.instance().getBulletID(masterStyle)
            if (id >= 0) AttrManage.instance().setPGParaBulletID(attr, id)
        }
        if (masterStyle > 0) AttrManage.instance().setParaStyleID(attr, masterStyle)
    }

    fun setSpecialIndent(attr: IAttributeSet, left: Int, indentValue: Int, bSet: Boolean) {
        var indent = indentValue
        if (indent < 0 && kotlin.math.abs(indent) > left) indent = -left
        AttrManage.instance().setParaSpecialIndent(attr, indent)
        if (bSet && indent < 0) AttrManage.instance().setParaIndentLeft(attr, left + indent)
    }

    fun setParaAttribute(style: CellStyle?, attr: IAttributeSet, attrLayout: IAttributeSet?) {
        if (attrLayout == null) return
        if (style != null) {
            val indent = (style.getIndent() * SSConstant.INDENT_TO_PIXEL).toInt()
            when (style.getHorizontalAlign()) {
                CellStyle.ALIGN_LEFT, CellStyle.ALIGN_GENERAL -> {
                    attrLayout.setAttribute(AttrIDConstant.PARA_INDENT_LEFT_ID, Math.round(indent * MainConstant.PIXEL_TO_TWIPS))
                    attrLayout.setAttribute(AttrIDConstant.PARA_INDENT_RIGHT_ID, 0)
                    AttrManage.instance().setParaHorizontalAlign(attr, WPAttrConstant.PARA_HOR_ALIGN_LEFT.toInt())
                }
                CellStyle.ALIGN_RIGHT -> {
                    attrLayout.setAttribute(AttrIDConstant.PARA_INDENT_LEFT_ID, 0)
                    attrLayout.setAttribute(AttrIDConstant.PARA_INDENT_RIGHT_ID, Math.round(indent * MainConstant.PIXEL_TO_TWIPS))
                    AttrManage.instance().setParaHorizontalAlign(attr, WPAttrConstant.PARA_HOR_ALIGN_RIGHT.toInt())
                }
                CellStyle.ALIGN_CENTER, CellStyle.ALIGN_FILL, CellStyle.ALIGN_JUSTIFY, CellStyle.ALIGN_CENTER_SELECTION -> AttrManage.instance().setParaHorizontalAlign(attr, WPAttrConstant.PARA_HOR_ALIGN_CENTER.toInt())
            }
            setParaBefore(attrLayout, attr)
            setParaAfter(attrLayout, attr)
            setParaLineSpace(attrLayout, attr)
            setParaIndentLeft(attrLayout, attr)
            setParaIndentRight(attrLayout, attr)
            setParaSpecialIndent(attrLayout, attr)
        } else {
            setParaHorizontalAlign(attrLayout, attr)
            setParaBefore(attrLayout, attr)
            setParaAfter(attrLayout, attr)
            setParaLineSpace(attrLayout, attr)
        }
    }

    fun processParaWithPct(pPr: Element?, attr: IAttributeSet) {
        val fontSize = RunAttr.instance().getMaxFontSize()
        if (pPr != null) {
            val before = pPr.element("spcBef")?.element("spcPct")?.attributeValue("val")
            if (!before.isNullOrEmpty()) AttrManage.instance().setParaBefore(attr, (Integer.parseInt(before) / 100000f * fontSize * POINT_PER_LINE_PER_FONTSIZE * MainConstant.POINT_TO_TWIPS).toInt())
            val after = pPr.element("spcAft")?.element("spcPct")?.attributeValue("val")
            if (!after.isNullOrEmpty()) AttrManage.instance().setParaAfter(attr, (Integer.parseInt(after) / 100000f * fontSize * POINT_PER_LINE_PER_FONTSIZE * MainConstant.POINT_TO_TWIPS).toInt())
        }
    }
}
