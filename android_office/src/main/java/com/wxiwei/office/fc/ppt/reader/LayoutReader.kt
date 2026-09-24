package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.fc.dom4j.io.SAXReader
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.fc.ppt.ShapeManage
import com.wxiwei.office.pg.model.PGLayout
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.pg.model.PGModel
import com.wxiwei.office.pg.model.PGPlaceholderUtil
import com.wxiwei.office.pg.model.PGSlide
import com.wxiwei.office.pg.model.PGStyle
import com.wxiwei.office.system.IControl

class LayoutReader private constructor() {
    fun getLayouts(control: IControl?, zipPackage: ZipPackage, layoutPart: PackagePart, pgModel: PGModel, pgMaster: PGMaster, defaultStyle: PGStyle): PGLayout? {
        val control = control ?: return null
        val input = layoutPart.inputStream
        val layout = SAXReader().read(input).rootElement
        var pgLayout: PGLayout? = null
        if (layout != null) {
            pgLayout = PGLayout()
            if (!pptXmlBoolean(layout.attributeValue("showMasterSp"), default = true)) pgLayout.setAddShapes(false)
            val cSld = layout.element("cSld")
            val spTree = cSld?.element("spTree")
            if (cSld != null && spTree != null) {
                processBackgroundAndFill(control, zipPackage, layoutPart, pgMaster, pgLayout, cSld)
                processTextStyle(control, layoutPart, pgMaster, pgLayout, spTree)
                val slide = PGSlide()
                slide.setSlideType(PGSlide.Slide_Layout.toInt())
                val iterator = spTree.elementIterator()
                while (iterator.hasNext()) ShapeManage.instance().processShape(control, zipPackage, layoutPart, null, pgMaster, pgLayout, defaultStyle, slide, PGSlide.Slide_Layout, iterator.next() as com.wxiwei.office.fc.dom4j.Element, null, 1.0f, 1.0f)
                if (slide.getShapeCount() > 0) pgLayout.setSlideMasterIndex(pgModel.appendSlideMaster(slide))
            }
        }
        input.close()
        return pgLayout
    }

    private fun processTextStyle(control: IControl, layoutPart: PackagePart, pgMaster: PGMaster, pgLayout: PGLayout, spTree: com.wxiwei.office.fc.dom4j.Element) {
        val iterator = spTree.elementIterator()
        while (iterator.hasNext()) {
            val sp = iterator.next() as com.wxiwei.office.fc.dom4j.Element
            val type = ReaderKit.instance().getPlaceholderType(sp)
            val idx = ReaderKit.instance().getPlaceholderIdx(sp)
            val style = sp.element("txBody")?.element("lstStyle") ?: continue
            StyleReader.instance().setStyleIndex(styleIndex)
            if (!PGPlaceholderUtil.instance().isBody(type ?: "")) pgLayout.setStyleByType(type ?: "", StyleReader.instance().getStyles(control, pgMaster, sp, style))
            else if (idx > 0) pgLayout.setStyleByIdx(idx, StyleReader.instance().getStyles(control, pgMaster, sp, style))
            styleIndex = StyleReader.instance().getStyleIndex()
        }
    }

    private fun processBackgroundAndFill(control: IControl, zipPackage: ZipPackage, layoutPart: PackagePart, pgMaster: PGMaster, pgLayout: PGLayout, cSld: com.wxiwei.office.fc.dom4j.Element) {
        cSld.element("bg")?.let { pgLayout.setBackgroundAndFill(BackgroundReader.instance().getBackground(control, zipPackage, layoutPart, pgMaster, it)) }
    }

    fun dispose() { styleIndex = 1001 }

    private var styleIndex = 1001

    companion object {
        private val layoutReader = LayoutReader()
        @JvmStatic fun instance(): LayoutReader = layoutReader
    }
}
