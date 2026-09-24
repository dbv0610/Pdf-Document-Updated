package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.dom4j.io.SAXReader
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.PackageRelationshipTypes
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.fc.ppt.ShapeManage
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.pg.model.PGModel
import com.wxiwei.office.pg.model.PGPlaceholderUtil
import com.wxiwei.office.pg.model.PGSlide
import com.wxiwei.office.system.IControl

class MasterReader private constructor() {
    fun getMasterData(control: IControl?, zipPackage: ZipPackage, masterPart: PackagePart, pgModel: PGModel): PGMaster? {
        val control = control ?: return null
        val input = masterPart.inputStream
        val master = SAXReader().read(input).rootElement
        var pgMaster: PGMaster? = null
        if (master != null) {
            pgMaster = PGMaster()
            processClrMap(pgMaster, zipPackage, masterPart, master)
            processStyle(control, pgMaster, master)
            val cSld = master.element("cSld")
            val spTree = cSld?.element("spTree")
            if (cSld != null) processBackgroundAndFill(control, pgMaster, zipPackage, masterPart, cSld)
            if (spTree != null) {
                processTextStyle(control, pgMaster, spTree)
                val slide = PGSlide()
                slide.setSlideType(PGSlide.Slide_Master.toInt())
                val iterator = spTree.elementIterator()
                while (iterator.hasNext()) ShapeManage.instance().processShape(control, zipPackage, masterPart, null, pgMaster, null, null, slide, PGSlide.Slide_Master, iterator.next() as Element, null, 1.0f, 1.0f)
                if (slide.getShapeCount() > 0) pgMaster.setSlideMasterIndex(pgModel.appendSlideMaster(slide))
            }
        }
        input.close()
        return pgMaster
    }

    private fun processClrMap(pgMaster: PGMaster, zipPackage: ZipPackage, masterPart: PackagePart, master: Element) {
        val relationship = masterPart.getRelationshipsByType(PackageRelationshipTypes.THEME_PART).getRelationship(0)
        val themePart = relationship?.let { zipPackage.getPart(it.targetURI) } ?: return
        val themeColor = ThemeReader.instance().getThemeColorMap(themePart) ?: return
        val clrMap = master.element("clrMap") ?: return
        for (i in 0 until clrMap.attributeCount()) {
            val name = clrMap.attribute(i).name
            val value = clrMap.attributeValue(name)
            if (name != value) pgMaster.addColor(value, themeColor[value] ?: 0)
            pgMaster.addColor(name, themeColor[value] ?: 0)
        }
    }

    private fun processBackgroundAndFill(control: IControl, pgMaster: PGMaster, zipPackage: ZipPackage, masterPart: PackagePart, cSld: Element) {
        cSld.element("bg")?.let { pgMaster.setBackgroundAndFill(BackgroundReader.instance().getBackground(control, zipPackage, masterPart, pgMaster, it)) }
    }

    private fun processTextStyle(control: IControl, pgMaster: PGMaster, spTree: Element) {
        val iterator = spTree.elementIterator()
        while (iterator.hasNext()) {
            val sp = iterator.next() as Element
            val type = PGPlaceholderUtil.instance().checkTypeName(ReaderKit.instance().getPlaceholderType(sp))
            val idx = ReaderKit.instance().getPlaceholderIdx(sp)
            val style = sp.element("txBody")?.element("lstStyle") ?: continue
            StyleReader.instance().setStyleIndex(styleIndex)
            if (!PGPlaceholderUtil.instance().isBody(type)) pgMaster.addStyleByType(type ?: "", StyleReader.instance().getStyles(control, pgMaster, sp, style))
            else if (idx > 0) pgMaster.addStyleByIdx(idx, StyleReader.instance().getStyles(control, pgMaster, sp, style))
            styleIndex = StyleReader.instance().getStyleIndex()
        }
    }

    private fun processStyle(control: IControl, pgMaster: PGMaster, master: Element) {
        val txStyles = master.element("txStyles") ?: return
        StyleReader.instance().setStyleIndex(styleIndex)
        pgMaster.setTitleStyle(StyleReader.instance().getStyles(control, pgMaster, null, txStyles.element("titleStyle")))
        pgMaster.setBodyStyle(StyleReader.instance().getStyles(control, pgMaster, null, txStyles.element("bodyStyle")))
        pgMaster.setDefaultStyle(StyleReader.instance().getStyles(control, pgMaster, null, txStyles.element("otherStyle")))
        styleIndex = StyleReader.instance().getStyleIndex()
    }

    fun dispose() { styleIndex = 10 }

    private var styleIndex = 10

    companion object {
        private val masterReader = MasterReader()
        @JvmStatic fun instance(): MasterReader = masterReader
    }
}
