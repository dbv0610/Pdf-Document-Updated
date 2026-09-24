package com.wxiwei.office.fc.ppt

import com.wxiwei.office.fc.ppt.reader.pptXmlBoolean

import com.wxiwei.office.common.autoshape.ArbitraryPolygonShapePath
import com.wxiwei.office.common.autoshape.AutoShapeTypes
import com.wxiwei.office.common.bg.BackgroundAndFill
import com.wxiwei.office.common.pictureefftect.PictureEffectInfo
import com.wxiwei.office.common.pictureefftect.PictureEffectInfoFactory
import com.wxiwei.office.common.shape.*
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.fc.LineKit
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.PackageRelationshipTypes
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.fc.ppt.attribute.ParaAttr
import com.wxiwei.office.fc.ppt.attribute.RunAttr
import com.wxiwei.office.fc.ppt.attribute.SectionAttr
import com.wxiwei.office.fc.ppt.reader.BackgroundReader
import com.wxiwei.office.fc.ppt.reader.PictureReader
import com.wxiwei.office.fc.ppt.reader.ReaderKit
import com.wxiwei.office.fc.ppt.reader.TableReader
import com.wxiwei.office.fc.xls.Reader.drawing.ChartReader
import com.wxiwei.office.java.awt.Rectangle
import com.wxiwei.office.pg.model.*
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.SectionElement
import com.wxiwei.office.system.IControl

class ShapeManage private constructor() {
    fun processShape(
        control: IControl?, zipPackage: ZipPackage, packagePart: PackagePart,
        pgModel: PGModel?, pgMaster: PGMaster?, pgLayout: PGLayout?, defaultStyle: PGStyle?,
        pgSlide: PGSlide, slideType: Byte, sp: Element, parent: GroupShape?, zoomX: Float,
        zoomY: Float
    ): Int? {
        val control = control ?: return null
        if (ReaderKit.instance().isHidden(sp)) return null
        var addShape = packagePart.partName.name.contains("/ppt/slides/")
        addShape = addShape || (!addShape && ReaderKit.instance().isUserDrawn(sp))
        RunAttr.instance().setSlide(addShape)
        val name = sp.name
        if (name == "sp" || name == "cxnSp") {
            return processAutoShapeAndTextShape(control, zipPackage, packagePart, pgModel ?: return null, pgMaster,
                pgLayout, defaultStyle, pgSlide, slideType, sp, parent, zoomX, zoomY, addShape)
        } else if (name == "pic") {
            if (addShape) return processPicture(control, zipPackage, packagePart, pgMaster, pgLayout,
                pgSlide, sp, parent, zoomX, zoomY)
        } else if (name == "graphicFrame") {
            if (addShape) return processGraphicFrame(control, zipPackage, packagePart, pgModel ?: return null, pgMaster,
                pgLayout, pgSlide, sp, parent, zoomX, zoomY)
        } else if (name == "grpSp") {
            var e = sp.element("nvGrpSpPr")
            var groupId = 0
            e = e?.element("cNvPr")
            if (e != null) groupId = e.attributeValue("id").toInt()
            val grpSpPr = sp.element("grpSpPr")
            var groupShape: GroupShape? = null
            var zoomXY = floatArrayOf(1f, 1f)
            if (grpSpPr != null) {
                var rect = ReaderKit.instance().getShapeAnchor(grpSpPr.element("xfrm"), zoomX, zoomY) ?: return null
                rect = processGrpSpRect(parent, rect)
                zoomXY = ReaderKit.instance().getAnchorFitZoom(grpSpPr.element("xfrm"))
                val childRect = ReaderKit.instance().getChildShapeAnchor(grpSpPr.element("xfrm"), zoomXY[0] * zoomX, zoomXY[1] * zoomY) ?: return null
                groupShape = GroupShape()
                groupShape.setOffPostion(rect.x - childRect.x, rect.y - childRect.y)
                groupShape.shapeID = groupId
                groupShape.bounds = rect
                groupShape.parent = parent
                processGrpRotation(parent, groupShape, grpSpPr)
            }
            val childIds = ArrayList<Int>()
            val iterator = sp.elementIterator()
            while (iterator.hasNext()) {
                val childId = processShape(control, zipPackage, packagePart, pgModel, pgMaster, pgLayout,
                    defaultStyle, pgSlide, slideType, iterator.next() as Element, groupShape,
                    zoomXY[0] * zoomX, zoomXY[1] * zoomY)
                if (childId != null) childIds.add(childId)
            }
            if (parent == null) pgSlide.appendShapes(groupShape) else parent.appendShapes(groupShape)
            pgSlide.addGroupShape(groupId, childIds)
            return groupId
        } else if (name == "AlternateContent") {
            val choice = sp.element("Fallback")
            if (choice != null) {
                val iterator = choice.elementIterator()
                while (iterator.hasNext()) {
                    processShape(control, zipPackage, packagePart, pgModel, pgMaster, pgLayout,
                        defaultStyle, pgSlide, slideType, iterator.next() as Element, parent, zoomX, zoomY)
                }
            }
        }
        RunAttr.instance().setSlide(false)
        return null
    }

    fun processAutoShapeAndTextShape(control: IControl?, zipPackage: ZipPackage, packagePart: PackagePart,
        pgModel: PGModel, pgMaster: PGMaster?, pgLayout: PGLayout?, defaultStyle: PGStyle?, pgSlide: PGSlide,
        slideType: Byte, sp: Element, parent: GroupShape?, zoomX: Float, zoomY: Float, addShape: Boolean): Int {
        var temp = sp.element("nvSpPr") ?: sp.element("nvCxnSpPr")
        val id = temp!!.element("cNvPr").attributeValue("id").toInt()
        val type = ReaderKit.instance().getPlaceholderType(sp)
        val idx = ReaderKit.instance().getPlaceholderIdx(sp)
        if (slideType == PGSlide.Slide_Layout) pgLayout?.addShapeType(idx, type ?: "")
        val placeHolderID = if (PGPlaceholderUtil.instance().isTitleOrBody(type)) {
            if (slideType == PGSlide.Slide_Master) pgMaster?.addTitleBodyID(idx, idx)
            if (slideType == PGSlide.Slide_Layout) pgLayout?.addTitleBodyID(idx, id)
            -1
        } else if ((slideType == PGSlide.Slide_Master || slideType == PGSlide.Slide_Layout) && ReaderKit.instance().isUserDrawn(sp)) 0 else -1
        var rect = sp.element("spPr")?.let { ReaderKit.instance().getShapeAnchor(it.element("xfrm"), zoomX, zoomY) }
        if (rect == null && pgLayout != null) rect = pgLayout.getAnchor(type, idx)
        if (rect == null && pgMaster != null) rect = pgMaster.getAnchor(type, idx)
        if (rect != null) {
            rect = processGrpSpRect(parent, rect)
            var shape: AbstractShape? = null
            if (addShape || !PGPlaceholderUtil.instance().isHeaderFooter(type)) {
                shape = processAutoShape(control, zipPackage, packagePart, pgModel, pgMaster, pgLayout, pgSlide, sp,
                    id, idx, rect, isRect(type, idx), parent, slideType, type, !addShape && PGPlaceholderUtil.instance().isTitleOrBody(type))
            }
            if (shape != null) {
                if (parent == null) pgSlide.appendShapes(shape) else parent.appendShapes(shape)
                shape.placeHolderID = placeHolderID
                processGrpRotation(parent, shape, sp.element("spPr"))
            }
            temp = sp.element("txBody")
            if (temp != null && addShape) {
                val tb = TextBox()
                tb.bounds = rect
                tb.placeHolderID = placeHolderID
                tb.shapeID = id
                val secElem = SectionElement()
                secElem.setStartOffset(0)
                tb.element = secElem
                val attr = secElem.getAttribute()
                AttrManage.instance().setPageWidth(attr, (rect.width * MainConstant.PIXEL_TO_TWIPS).toInt())
                AttrManage.instance().setPageHeight(attr, (rect.height * MainConstant.PIXEL_TO_TWIPS).toInt())
                SectionAttr.instance().setSectionAttribute(temp.element("bodyPr"), attr,
                    pgLayout?.getSectionAttr(type, idx), pgMaster?.getSectionAttr(type, idx), false)
                val offset = ParaAttr.instance().processParagraph(control, pgMaster, pgLayout, defaultStyle, secElem,
                    sp.element("style"), temp, type ?: "", idx)
                secElem.setEndOffset(offset.toLong())
                val text = tb.element?.getText(null)
                if (!text.isNullOrEmpty() && text != "\n") {
                    processGrpRotation(parent, tb, sp.element("spPr"))
                    if (parent == null) pgSlide.appendShapes(tb) else parent.appendShapes(tb)
                } else if (shape != null) {
                    processGrpRotation(parent, shape, sp.element("spPr"))
                }
                val wrap = temp.element("bodyPr")?.attributeValue("wrap")
                tb.isWrapLine = wrap == null || wrap.equals("square", true)
            }
        }
        return id
    }

    fun processPicture(control: IControl, zipPackage: ZipPackage, packagePart: PackagePart, pgMaster: PGMaster?,
        pgLayout: PGLayout?, pgSlide: PGSlide, sp: Element, parent: GroupShape?, zoomX: Float, zoomY: Float): Int {
        var e = sp.element("nvPicPr")
        var shapeID = 0
        e = e?.element("cNvPr")
        if (e != null) shapeID = e.attributeValue("id").toInt()
        var blipFill = sp.element("blipFill")
        if (blipFill == null) blipFill = sp.element("AlternateContent")?.element("Fallback")?.element("blipFill")
        val embed = blipFill?.element("blip")?.attributeValue("embed")
        if (blipFill != null && embed != null) {
            val spPr = sp.element("spPr")
            var rect = spPr?.let { ReaderKit.instance().getShapeAnchor(it.element("xfrm"), zoomX, zoomY) }
            if (rect == null && pgLayout != null) rect = pgLayout.getAnchor(ReaderKit.instance().getPlaceholderType(sp), ReaderKit.instance().getPlaceholderIdx(sp))
            if (rect == null && pgMaster != null) rect = pgMaster.getAnchor(ReaderKit.instance().getPlaceholderType(sp), ReaderKit.instance().getPlaceholderIdx(sp))
            val relationship = packagePart.getRelationship(embed)
            if (rect != null && relationship != null && spPr != null) {
                val fill = BackgroundReader.instance().processBackground(control, zipPackage, packagePart, pgMaster, spPr)
                val line = LineKit.createShapeLine(control, zipPackage, packagePart, pgMaster, sp)
                val pic = addPicture(control, zipPackage.getPart(relationship.targetURI), pgSlide, shapeID, processGrpSpRect(parent, rect), spPr, parent,
                    PictureEffectInfoFactory.getPictureEffectInfor(blipFill))
                pic?.backgroundAndFill = fill
                pic?.setLine(line)
            }
        }
        return shapeID
    }

    fun addPicture(control: IControl, picPart: PackagePart?, pgSlide: PGSlide, shapeID: Int, rect: Rectangle,
        spPr: Element?, parent: GroupShape?, effectInfor: PictureEffectInfo?): PictureShape? {
        if (picPart == null) return null
        val picShape = PictureShape()
        picShape.pictureIndex = control.getSysKit().pictureManage.addPicture(picPart)
        picShape.bounds = rect
        processGrpRotation(parent, picShape, spPr)
        picShape.shapeID = shapeID
        picShape.pictureEffectInfor = effectInfor
        if (parent == null) pgSlide.appendShapes(picShape) else parent.appendShapes(picShape)
        return picShape
    }

    fun processGraphicFrame(control: IControl, zipPackage: ZipPackage, packagePart: PackagePart, pgModel: PGModel,
        pgMaster: PGMaster?, pgLayout: PGLayout?, pgSlide: PGSlide, sp: Element, parent: GroupShape?, zoomX: Float, zoomY: Float): Int {
        var e = sp.element("nvGraphicFramePr")?.element("cNvPr")
        val shapeId = e?.attributeValue("id")?.toInt() ?: 0
        var rect = ReaderKit.instance().getShapeAnchor(sp.element("xfrm"), zoomX, zoomY)
        if (rect == null && pgLayout != null) rect = pgLayout.getAnchor(ReaderKit.instance().getPlaceholderType(sp), ReaderKit.instance().getPlaceholderIdx(sp))
        if (rect == null && pgMaster != null) rect = pgMaster.getAnchor(ReaderKit.instance().getPlaceholderType(sp), ReaderKit.instance().getPlaceholderIdx(sp))
        if (rect == null) return shapeId
        rect = processGrpSpRect(parent, rect)
        val data = sp.element("graphic")?.element("graphicData") ?: return shapeId
        when (data.attributeValue("uri")) {
            PackageRelationshipTypes.OLE_TYPE -> {
                val ole = data.element("oleObj") ?: data.element("AlternateContent")?.element("Fallback")?.element("oleObj")
                val spid = ole?.attributeValue("spid")
                if (spid != null) addPicture(control, PictureReader.instance().getOLEPart(zipPackage, packagePart, spid, false), pgSlide, shapeId, rect, sp.element("spPr"), parent, null)
                ole?.element("pic")?.let { processPicture(control, zipPackage, packagePart, pgMaster, pgLayout, pgSlide, it, parent, zoomX, zoomY) }
            }
            PackageRelationshipTypes.CHART_TYPE -> {
                val id = data.element("chart")?.attributeValue("id")
                val rel = id?.let { packagePart.getRelationship(it) }
                if (rel != null) {
                    val chart = ChartReader.instance().read(control, zipPackage, zipPackage.getPart(rel.targetURI), pgMaster?.getSchemeColor() ?: emptyMap(), MainConstant.APPLICATION_TYPE_PPT)
                    if (chart != null) {
                        val shape = AChart()
                        shape.aChart = chart
                        shape.bounds = rect
                        shape.shapeID = shapeId
                        pgSlide.appendShapes(shape)
                    }
                }
            }
            PackageRelationshipTypes.TABLE_TYPE -> data.element("tbl")?.element("tblPr")?.let {
                val table = TableReader.instance().getTable(control, zipPackage, packagePart, pgModel, pgMaster, data.element("tbl"), rect)
                if (table != null) {
                    table.bounds = rect
                    table.shapeID = shapeId
                    pgSlide.appendShapes(table)
                }
            }
            PackageRelationshipTypes.DIAGRAM_TYPE -> processSmartArt(pgSlide, data, rect)
        }
        return shapeId
    }

    private fun processGrpSpRect(parent: GroupShape?, rect: Rectangle): Rectangle {
        if (parent != null) {
            rect.x += parent.offX
            rect.y += parent.offY
        }
        return rect
    }

    private fun processGrpRotation(parent: IShape?, shape: IShape, spPr: Element?) {
        ReaderKit.instance().processRotation(spPr, shape)
    }

    private fun isRect(type: String?, idx: Int): Boolean = type != null &&
        (type.contains("Title") || type.contains("title") || type.contains("ctrTitle") || type.contains("subTitle") ||
            type.contains("body") || type.contains("half") || type.contains("dt") || type.contains("ftr") || type.contains("sldNum")) || idx > 0

    private fun getBackgrouond(control: IControl?, zipPackage: ZipPackage, packagePart: PackagePart, pgModel: PGModel,
        pgMaster: PGMaster?, pgLayout: PGLayout?, pgSlide: PGSlide, sp: Element, shapeIDX: Int, slideType: Byte,
        phType: String?, shapeType: Int): BackgroundAndFill? {
        if (pptXmlBoolean(sp.attributeValue("useBgFill"))) return pgSlide.getBackgroundAndFill() ?: pgLayout?.getBackgroundAndFill() ?: pgMaster?.getBackgroundAndFill()
        val spPr = sp.element("spPr") ?: return null
        var fill: BackgroundAndFill? = null
        if (spPr.element("noFill") == null && sp.name != "cxnSp") {
            fill = BackgroundReader.instance().processBackground(control, zipPackage, packagePart, pgMaster, spPr)
            if (fill == null && shapeType != ShapeTypes.Arc && shapeType != ShapeTypes.BracketPair && shapeType != ShapeTypes.LeftBracket && shapeType != ShapeTypes.RightBracket && shapeType != ShapeTypes.BracePair && shapeType != ShapeTypes.LeftBrace && shapeType != ShapeTypes.RightBrace && shapeType != ShapeTypes.ArbitraryPolygon) {
                fill = BackgroundReader.instance().processBackground(control, zipPackage, packagePart, pgMaster, sp.element("style"))
                if (fill?.fillType == BackgroundAndFill.FILL_SOLID && (fill.foregroundColor and 0xFFFFFF) == 0) fill = null
            }
        }
        return fill
    }

    fun processAutoShape(control: IControl?, zipPackage: ZipPackage, packagePart: PackagePart, pgModel: PGModel,
        pgMaster: PGMaster?, pgLayout: PGLayout?, pgSlide: PGSlide, sp: Element, id: Int, idx: Int, rect: Rectangle,
        isRect: Boolean, parent: GroupShape?, slideType: Byte, phType: String?, hidden: Boolean): AbstractShape? {
        val spPr = sp.element("spPr") ?: return null
        var shapeType = ShapeTypes.NotPrimitive
        var values: Array<Float>? = null
        var border = true
        val placeholderName = ReaderKit.instance().getPlaceholderName(sp)
        if (sp.name == "cxnSp") shapeType = ShapeTypes.StraightConnector1
        else if (isRect || (placeholderName ?: "").contains("Text Box") || (placeholderName ?: "").contains("TextBox")) shapeType = ShapeTypes.Rectangle
        val prstGeom = spPr.element("prstGeom")
        if (prstGeom != null) {
            prstGeom.attributeValue("prst")?.let { shapeType = AutoShapeTypes.instance().getAutoShapeType(it) }
            val gds = prstGeom.element("avLst")?.elements("gd")
            if (gds != null && gds.isNotEmpty()) values = Array(gds.size) { i -> (gds[i] as Element).attributeValue("fmla").substring(4).toFloat() / 100000f }
        } else if (spPr.element("custGeom") != null) shapeType = ShapeTypes.ArbitraryPolygon
        else if (isRect) shapeType = ShapeTypes.Rectangle
        val fill = getBackgrouond(control, zipPackage, packagePart, pgModel, pgMaster, pgLayout, pgSlide, sp, idx, slideType, phType, shapeType)
        val line = LineKit.createShapeLine(control, zipPackage, packagePart, pgMaster, sp)
        val ln = spPr.element("ln")
        if (ln != null && ln.element("noFill") != null) border = false
        else if (ln == null && border && (sp.element("style") == null || sp.element("style").element("lnRef") == null)) border = false
        if (shapeType == ShapeTypes.Line || shapeType == ShapeTypes.StraightConnector1 || shapeType == ShapeTypes.BentConnector2 || shapeType == ShapeTypes.BentConnector3 || shapeType == ShapeTypes.CurvedConnector2 || shapeType == ShapeTypes.CurvedConnector3 || shapeType == ShapeTypes.CurvedConnector4 || shapeType == ShapeTypes.CurvedConnector5) {
            if (!border) return null
            val result = LineShape()
            result.shapeType = shapeType
            result.bounds = rect
            result.shapeID = id
            result.isHidden = hidden
            result.adjustData = values
            result.setLine(line)
            ln?.element("headEnd")?.attributeValue("type")?.let { type ->
                val arrow = Arrow.getArrowType(type)
                if (arrow != Arrow.Arrow_None) result.createStartArrow(arrow, Arrow.getArrowSize(ln.element("headEnd").attributeValue("w")), Arrow.getArrowSize(ln.element("headEnd").attributeValue("len")))
            }
            ln?.element("tailEnd")?.attributeValue("type")?.let { type ->
                val arrow = Arrow.getArrowType(type)
                if (arrow != Arrow.Arrow_None) result.createEndArrow(arrow, Arrow.getArrowSize(ln.element("tailEnd").attributeValue("w")), Arrow.getArrowSize(ln.element("tailEnd").attributeValue("len")))
            }
            return result
        }
        if (shapeType == ShapeTypes.ArbitraryPolygon) {
            val result = ArbitraryPolygonShape()
            ArbitraryPolygonShapePath.processArbitraryPolygonShape(result, sp, fill, border, line?.backgroundAndFill, ln, rect)
            result.shapeType = shapeType
            result.shapeID = id
            processGrpRotation(parent, result, spPr)
            result.isHidden = hidden
            result.setLine(line)
            return result
        }
        if (fill != null || line != null) {
            val result = AutoShape(shapeType)
            result.bounds = rect
            result.shapeID = id
            result.isHidden = hidden
            result.backgroundAndFill = fill
            result.setLine(line)
            result.adjustData = values
            return result
        }
        return null
    }

    private fun processSmartArt(pgslide: PGSlide, graphicData: Element, rect: Rectangle) {
        try {
            val relIds = graphicData.element("relIds") ?: return
            val cs = relIds.attributeValue("dm") ?: return
            val id = cs.substring("rId".length).toInt()
            val smartArt = pgslide.getSmartArt(cs) ?: return
            smartArt.bounds = rect
            for (shape in smartArt.shapes) shape.setShapeID(id)
            pgslide.appendShapes(smartArt)
        } catch (_: Exception) {
        }
    }

    companion object {
        private val kit = ShapeManage()
        @JvmStatic fun instance(): ShapeManage = kit
    }
}
