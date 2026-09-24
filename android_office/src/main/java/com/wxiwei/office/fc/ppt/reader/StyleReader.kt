package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.ppt.attribute.ParaAttr
import com.wxiwei.office.fc.ppt.attribute.RunAttr
import com.wxiwei.office.fc.ppt.attribute.SectionAttr
import com.wxiwei.office.fc.ppt.bulletnumber.BulletNumberManage
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.pg.model.PGStyle
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.AttributeSetImpl
import com.wxiwei.office.simpletext.model.Style
import com.wxiwei.office.simpletext.model.StyleManage
import com.wxiwei.office.system.IControl

class StyleReader private constructor() {
    fun getStyles(control: IControl, pgMaster: PGMaster?, sp: Element?, style: Element?): PGStyle {
        val pgStyle = PGStyle()
        processSp(pgStyle, sp)
        processStyle(control, pgStyle, pgMaster, style)
        return pgStyle
    }

    private fun processSp(pgStyle: PGStyle, sp: Element?) {
        if (sp != null) {
            val spPr = sp.element("spPr")
            if (spPr != null) {
                pgStyle.setAnchor(ReaderKit.instance().getShapeAnchor(spPr.element("xfrm"), 1.0f, 1.0f))
            }
            val txBody = sp.element("txBody")
            val bodyPr = txBody?.element("bodyPr")
            if (bodyPr != null) {
                val attr = AttributeSetImpl()
                SectionAttr.instance().setSectionAttribute(bodyPr, attr, null, null, false)
                pgStyle.setSectionAttr(attr)
            }
        }
    }

    private fun processStyle(control: IControl, pgStyle: PGStyle, pgMaster: PGMaster?, style: Element?) {
        if (style != null) {
            for (level in 1..9) {
                val levelStyle = style.element("lvl${level}pPr")
                if (levelStyle != null) {
                    processStyleAttribute(control, pgStyle, pgMaster, levelStyle, level)
                }
            }
        }
    }

    private fun processStyleAttribute(
        control: IControl,
        pgStyle: PGStyle,
        pgMaster: PGMaster?,
        paraStyle: Element,
        lvl: Int
    ) {
        val style = Style()
        style.setId(index)
        style.setType(0.toByte())
        val attrs = style.getAttrbuteSet() ?: AttributeSetImpl()
        ParaAttr.instance().setParaAttribute(control, paraStyle, attrs, null, -1, -1, 0, false, false)
        RunAttr.instance().setRunAttribute(pgMaster, paraStyle.element("defRPr"), attrs, null, 100, -1, false)
        RunAttr.instance().setMaxFontSize(AttrManage.instance().getFontSize(attrs, attrs))
        ParaAttr.instance().processParaWithPct(paraStyle, attrs)
        RunAttr.instance().resetMaxFontSize()
        StyleManage.instance().addStyle(style)
        pgStyle.addStyle(lvl, index)
        BulletNumberManage.instance().addBulletNumber(control, index, paraStyle)
        index++
    }

    fun getStyleIndex(): Int = index

    fun setStyleIndex(index: Int) {
        this.index = index
    }

    private var index = 0

    companion object {
        private val style = StyleReader()

        @JvmStatic
        fun instance(): StyleReader = style
    }
}
