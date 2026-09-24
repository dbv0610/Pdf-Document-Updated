package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.common.autoshape.ArbitraryPolygonShapePath
import com.wxiwei.office.common.autoshape.AutoShapeTypes
import com.wxiwei.office.common.bg.BackgroundAndFill
import com.wxiwei.office.common.shape.*
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.fc.LineKit
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.dom4j.io.SAXReader
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.fc.ppt.attribute.ParaAttr
import com.wxiwei.office.fc.ppt.attribute.SectionAttr
import com.wxiwei.office.pg.model.*
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.IAttributeSet
import com.wxiwei.office.simpletext.model.SectionElement
import com.wxiwei.office.system.IControl

class SmartArtReader {
    fun read(control: IControl, zipPackage: ZipPackage, pgModel: PGModel, pgMaster: PGMaster?, pgLayout: PGLayout?, pgSlide: PGSlide, slidePart: PackagePart, dataPart: PackagePart): SmartArt? {
        val reader = SAXReader()
        val dataInput = dataPart.inputStream
        val dataRoot = reader.read(dataInput).rootElement
        dataInput.close()
        val fill = BackgroundReader.instance().processBackground(control, zipPackage, dataPart, pgMaster, dataRoot.element("bg"))
        val line = LineKit.createLine(control, zipPackage, dataPart, pgMaster, dataRoot.element("whole")?.element("ln"))
        val ext = dataRoot.element("extLst")?.element("ext")?.element("dataModelExt")
        val drawingPart = ext?.attributeValue("relId")?.let { slidePart.getRelationship(it) }?.let { zipPackage.getPart(it.targetURI) } ?: return null
        val drawingInput = drawingPart.inputStream
        val root = reader.read(drawingInput).rootElement
        drawingInput.close()
        val smartArt = SmartArt()
        smartArt.setBackgroundAndFill(fill)
        smartArt.setLine(line)
        val tree = root.element("spTree") ?: return smartArt
        for (raw in tree.elements("sp")) {
            val sp = raw as Element
            processAutoShape(control, zipPackage, drawingPart, pgModel, pgMaster, pgLayout, pgSlide, sp)?.let {
                it.parent = smartArt
                smartArt.appendShapes(it)
            }
            getTextBoxData(control, pgMaster, pgLayout, sp)?.let { smartArt.appendShapes(it) }
        }
        return smartArt
    }

    private fun getBackgrouond(control: IControl, zipPackage: ZipPackage, part: PackagePart, pgModel: PGModel, pgMaster: PGMaster?, pgLayout: PGLayout?, pgSlide: PGSlide, sp: Element, shapeType: Int): BackgroundAndFill? {
        var fill: BackgroundAndFill? = null
        if (pptXmlBoolean(sp.attributeValue("useBgFill"))) {
            fill = pgSlide.getBackgroundAndFill() ?: pgLayout?.getBackgroundAndFill() ?: pgMaster?.getBackgroundAndFill()
        }
        val spPr = sp.element("spPr")
        if (fill == null && spPr?.element("noFill") == null && sp.name != "cxnSp") {
            fill = BackgroundReader.instance().processBackground(control, zipPackage, part, pgMaster, spPr)
            if (fill == null && shapeType !in setOf(ShapeTypes.Arc, ShapeTypes.BracketPair, ShapeTypes.LeftBracket, ShapeTypes.RightBracket, ShapeTypes.BracePair, ShapeTypes.LeftBrace, ShapeTypes.RightBrace, ShapeTypes.ArbitraryPolygon)) {
                fill = BackgroundReader.instance().processBackground(control, zipPackage, part, pgMaster, sp.element("style"))
            }
        }
        return fill
    }

    private fun processAutoShape(control: IControl, zipPackage: ZipPackage, part: PackagePart, pgModel: PGModel, pgMaster: PGMaster?, pgLayout: PGLayout?, pgSlide: PGSlide, sp: Element): IShape? {
        val spPr = sp.element("spPr") ?: return null
        val rect = ReaderKit.instance().getShapeAnchor(spPr.element("xfrm"), 1f, 1f) ?: return null
        var shapeType = ShapeTypes.NotPrimitive
        var values: Array<Float>? = null
        var border = true
        val name = ReaderKit.instance().getPlaceholderName(sp) ?: ""
        if (sp.name == "cxnSp") shapeType = ShapeTypes.Line
        else if (name.contains("Text Box") || name.contains("TextBox")) shapeType = ShapeTypes.Rectangle
        val geom = spPr.element("prstGeom")
        if (geom != null) {
            geom.attributeValue("prst")?.takeIf { it.isNotEmpty() }?.let { shapeType = AutoShapeTypes.instance().getAutoShapeType(it) }
            val gd = geom.element("avLst")?.elements("gd")
            if (gd != null && gd.isNotEmpty()) values = Array(gd.size) { (gd[it] as Element).attributeValue("fmla").substring(4).toFloat() / 100000 }
        } else if (spPr.element("custGeom") != null) shapeType = ShapeTypes.ArbitraryPolygon
        val fill = getBackgrouond(control, zipPackage, part, pgModel, pgMaster, pgLayout, pgSlide, sp, shapeType)
        val line = LineKit.createShapeLine(control, zipPackage, part, pgMaster, sp)
        val ln = spPr.element("ln")
        val style = sp.element("style")
        if (ln != null) {
            if (ln.element("noFill") != null) border = false
        } else if (border && (style == null || style.element("lnRef") == null)) border = false
        if (shapeType == ShapeTypes.Line || shapeType == ShapeTypes.StraightConnector1 || shapeType == ShapeTypes.BentConnector3 || shapeType == ShapeTypes.CurvedConnector3) {
            val result = LineShape()
            result.shapeType = shapeType
            result.bounds = rect
            result.adjustData = values
            result.line = line
            ln?.element("headEnd")?.let { addArrow(result, it, true) }
            ln?.element("tailEnd")?.let { addArrow(result, it, false) }
            processGrpRotation(result, spPr)
            return result
        }
        if (shapeType == ShapeTypes.ArbitraryPolygon) {
            val result = ArbitraryPolygonShape()
            val lineFill = line?.backgroundAndFill
            ArbitraryPolygonShapePath.processArbitraryPolygonShape(result, sp, fill, border, lineFill, ln, rect)
            result.shapeType = shapeType
            result.line = line
            processGrpRotation(result, spPr)
            return result
        }
        if (fill != null || line != null) {
            val result = AutoShape(shapeType)
            result.bounds = rect
            result.backgroundAndFill = fill
            result.line = line
            result.adjustData = values
            processGrpRotation(result, spPr)
            return result
        }
        return null
    }

    private fun addArrow(shape: LineShape, end: Element, start: Boolean) {
        val type = Arrow.getArrowType(end.attributeValue("type"))
        if (type != Arrow.Arrow_None) {
            val arrow = Arrow.getArrowSize(end.attributeValue("w"))
            val length = Arrow.getArrowSize(end.attributeValue("len"))
            shape.createStartArrow(type, arrow, length)
        }
    }

    private fun processGrpRotation(shape: IShape, spPr: Element) = ReaderKit.instance().processRotation(spPr, shape)

    private fun getTextBoxData(control: IControl, pgMaster: PGMaster?, pgLayout: PGLayout?, sp: Element): IShape? {
        val txBody = sp.element("txBody") ?: return null
        val rect = ReaderKit.instance().getShapeAnchor(sp.element("txXfrm"), 1f, 1f) ?: return null
        val tb = TextBox()
        tb.bounds = rect
        val section = SectionElement()
        section.setStartOffset(0)
        tb.element = section
        val attr = section.getAttribute()
        AttrManage.instance().setPageWidth(attr, (rect.width * MainConstant.PIXEL_TO_TWIPS).toInt())
        AttrManage.instance().setPageHeight(attr, (rect.height * MainConstant.PIXEL_TO_TWIPS).toInt())
        val layoutAttr: IAttributeSet? = pgLayout?.getSectionAttr(null, 0)
        val masterAttr: IAttributeSet? = pgMaster?.getSectionAttr(null, 0)
        SectionAttr.instance().setSectionAttribute(txBody.element("bodyPr"), attr, layoutAttr, masterAttr, false)
        val offset = ParaAttr.instance().processParagraph(control, pgMaster, pgLayout, null, section, sp.element("style"), txBody, PGPlaceholderUtil.DIAGRAM, 0)
        section.setEndOffset(offset.toLong())
        if (!tb.element.getText(null).isNullOrEmpty() && tb.element.getText(null) != "\n") ReaderKit.instance().processRotation(tb, sp.element("txXfrm"))
        val wrap = txBody.element("bodyPr")?.attributeValue("wrap")
        tb.setWrapLine(wrap == null || wrap.equals("square", true))
        return tb
    }

    companion object {
        private val reader = SmartArtReader()

        @JvmStatic
        fun instance(): SmartArtReader = reader
    }
}
