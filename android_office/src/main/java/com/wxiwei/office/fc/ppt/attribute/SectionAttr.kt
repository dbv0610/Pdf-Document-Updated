package com.wxiwei.office.fc.ppt.attribute

import com.wxiwei.office.fc.ppt.reader.pptXmlBoolean

import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.constant.wp.AttrIDConstant
import com.wxiwei.office.constant.wp.WPAttrConstant
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.AttributeSetImpl
import com.wxiwei.office.simpletext.model.IAttributeSet

class SectionAttr private constructor() {
    fun setPageMarginLeft(from: IAttributeSet?, to: IAttributeSet) = copyAttribute(from, to, AttrIDConstant.PAGE_LEFT_ID) { AttrManage.instance().getPageMarginLeft(from) }
    fun setPageMarginRight(from: IAttributeSet?, to: IAttributeSet) = copyAttribute(from, to, AttrIDConstant.PAGE_RIGHT_ID) { AttrManage.instance().getPageMarginRight(from) }
    fun setPageMarginTop(from: IAttributeSet?, to: IAttributeSet) = copyAttribute(from, to, AttrIDConstant.PAGE_TOP_ID) { AttrManage.instance().getPageMarginTop(from) }
    fun setPageMarginBottom(from: IAttributeSet?, to: IAttributeSet) = copyAttribute(from, to, AttrIDConstant.PAGE_BOTTOM_ID) { AttrManage.instance().getPageMarginBottom(from) }
    fun setPageVerticalAlign(from: IAttributeSet?, to: IAttributeSet) = copyAttribute(from, to, AttrIDConstant.PAGE_VERTICAL_ID) { AttrManage.instance().getPageVerticalAlign(from).toInt() }

    private fun copyAttribute(from: IAttributeSet?, to: IAttributeSet, id: Short, value: () -> Int) {
        if (from != null && AttrManage.instance().hasAttribute(from, id)) value().also { setValue(to, id, it) }
    }

    private fun setValue(set: IAttributeSet, id: Short, value: Int) {
        when (id) {
            AttrIDConstant.PAGE_LEFT_ID -> AttrManage.instance().setPageMarginLeft(set, value)
            AttrIDConstant.PAGE_RIGHT_ID -> AttrManage.instance().setPageMarginRight(set, value)
            AttrIDConstant.PAGE_TOP_ID -> AttrManage.instance().setPageMarginTop(set, value)
            AttrIDConstant.PAGE_BOTTOM_ID -> AttrManage.instance().setPageMarginBottom(set, value)
            AttrIDConstant.PAGE_VERTICAL_ID -> AttrManage.instance().setPageVerticalAlign(set, value.toByte())
        }
    }

    fun getDefautSectionAttr(layout: IAttributeSet?, master: IAttributeSet?): IAttributeSet? {
        if (layout == null && master == null) return null
        val attr = AttributeSetImpl()
        val source = layout ?: master
        if (layout == null || master == null) {
            setPageMarginLeft(source, attr)
            setPageMarginRight(source, attr)
            setPageMarginTop(source, attr)
            setPageMarginBottom(source, attr)
            setPageVerticalAlign(source, attr)
            return attr
        }
        copyOrFallback(layout, master, attr, AttrIDConstant.PAGE_LEFT_ID) { setPageMarginLeft(it, attr) }
        copyOrFallback(layout, master, attr, AttrIDConstant.PAGE_RIGHT_ID) { setPageMarginRight(it, attr) }
        copyOrFallback(layout, master, attr, AttrIDConstant.PAGE_TOP_ID) { setPageMarginTop(it, attr) }
        copyOrFallback(layout, master, attr, AttrIDConstant.PAGE_BOTTOM_ID) { setPageMarginBottom(it, attr) }
        copyOrFallback(layout, master, attr, AttrIDConstant.PAGE_VERTICAL_ID) { setPageVerticalAlign(it, attr) }
        return attr
    }

    private fun copyOrFallback(layout: IAttributeSet, master: IAttributeSet, attr: IAttributeSet, id: Short, action: (IAttributeSet) -> Unit) {
        action(if (AttrManage.instance().hasAttribute(layout, id)) layout else master)
    }

    fun setSectionAttribute(bodyPr: Element?, attr: IAttributeSet, attrLayout: IAttributeSet?, attrMaster: IAttributeSet?, table: Boolean) {
        var vertical = WPAttrConstant.PAGE_V_TOP
        val style = getDefautSectionAttr(attrLayout, attrMaster)
        fun margin(name: String, setter: (IAttributeSet, Int) -> Unit, fallback: (IAttributeSet?) -> Unit) {
            val value = bodyPr?.attributeValue(name)
            if (value != null && value.isNotEmpty()) {
                setter(attr, (value.toInt() * MainConstant.POINT_DPI / MainConstant.EMU_PER_INCH * MainConstant.POINT_TO_TWIPS).toInt())
            } else fallback(style)
        }
        margin("lIns", AttrManage.instance()::setPageMarginLeft) { setPageMarginLeft(it, attr) }
        margin("rIns", AttrManage.instance()::setPageMarginRight) { setPageMarginRight(it, attr) }
        margin("tIns", AttrManage.instance()::setPageMarginTop) { setPageMarginTop(it, attr) }
        margin("bIns", AttrManage.instance()::setPageMarginBottom) { setPageMarginBottom(it, attr) }
        if (bodyPr != null) {
            when (bodyPr.attributeValue("anchor")) {
                "t" -> vertical = WPAttrConstant.PAGE_V_TOP
                "ctr", "just", "dist" -> vertical = WPAttrConstant.PAGE_V_CENTER
                "b" -> vertical = WPAttrConstant.PAGE_V_BOTTOM
                null -> setPageVerticalAlign(style, attr)
            }
            if (bodyPr.attributeValue("anchor") != null) AttrManage.instance().setPageVerticalAlign(attr, vertical)
            if (pptXmlBoolean(bodyPr.attributeValue("anchorCtr"))) {
                AttrManage.instance().setPageHorizontalAlign(attr, WPAttrConstant.PAGE_H_CENTER)
            } else if (style != null && AttrManage.instance().hasAttribute(style, AttrIDConstant.PAGE_HORIZONTAL_ID)) {
                AttrManage.instance().setPageHorizontalAlign(attr, AttrManage.instance().getPageHorizontalAlign(style))
            }
        } else {
            setPageMarginLeft(style, attr)
            setPageMarginRight(style, attr)
            setPageMarginTop(style, attr)
            setPageMarginBottom(style, attr)
            setPageVerticalAlign(style, attr)
        }
        if (RunAttr.instance().isSlide()) {
            if (!AttrManage.instance().hasAttribute(attr, AttrIDConstant.PAGE_LEFT_ID)) AttrManage.instance().setPageMarginLeft(attr, if (table) DEFAULT_TABLE_MARGIN else DEFAULT_MARGIN_LEFT_RIGHT)
            if (!AttrManage.instance().hasAttribute(attr, AttrIDConstant.PAGE_RIGHT_ID)) AttrManage.instance().setPageMarginRight(attr, if (table) DEFAULT_TABLE_MARGIN else DEFAULT_MARGIN_LEFT_RIGHT)
            if (!AttrManage.instance().hasAttribute(attr, AttrIDConstant.PAGE_TOP_ID)) AttrManage.instance().setPageMarginTop(attr, if (table) DEFAULT_TABLE_MARGIN else DEFAULT_MARGIN_TOP_BOTTOM)
            if (!AttrManage.instance().hasAttribute(attr, AttrIDConstant.PAGE_BOTTOM_ID)) AttrManage.instance().setPageMarginBottom(attr, if (table) 0 else DEFAULT_MARGIN_TOP_BOTTOM)
        }
    }

    companion object {
        const val DEFAULT_MARGIN_LEFT_RIGHT = 144
        const val DEFAULT_MARGIN_TOP_BOTTOM = 72
        const val DEFAULT_TABLE_MARGIN = 30
        private val kit = SectionAttr()

        @JvmStatic
        fun instance(): SectionAttr = kit
    }
}
