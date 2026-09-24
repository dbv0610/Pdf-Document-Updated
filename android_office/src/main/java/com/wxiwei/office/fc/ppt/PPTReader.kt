package com.wxiwei.office.fc.ppt

import java.io.File
import com.wxiwei.office.common.bg.BackgroundAndFill
import com.wxiwei.office.common.shape.AbstractShape
import com.wxiwei.office.common.shape.ArbitraryPolygonShape
import com.wxiwei.office.common.shape.AutoShape
import com.wxiwei.office.common.shape.IShape
import com.wxiwei.office.common.shape.LineShape
import com.wxiwei.office.common.shape.PictureShape
import com.wxiwei.office.common.shape.TextBox
import com.wxiwei.office.common.shape.ShapeTypes
import com.wxiwei.office.fc.hslf.HSLFSlideShow
import com.wxiwei.office.fc.hslf.model.Shape
import com.wxiwei.office.fc.hslf.model.ShapeGroup
import com.wxiwei.office.fc.hslf.model.SimpleShape
import com.wxiwei.office.fc.hslf.model.Slide
import com.wxiwei.office.fc.hslf.model.TextShape
import com.wxiwei.office.fc.hslf.model.Line
import com.wxiwei.office.fc.hslf.model.Fill
import com.wxiwei.office.fc.hslf.usermodel.SlideShow
import com.wxiwei.office.java.awt.Dimension
import com.wxiwei.office.pg.model.PGModel
import com.wxiwei.office.pg.model.PGSlide
import com.wxiwei.office.pg.model.PGNotes
import com.wxiwei.office.system.AbstractReader
import com.wxiwei.office.system.BackReaderThread
import com.wxiwei.office.system.IControl
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.common.borders.Line as OfficeLine
import com.wxiwei.office.java.awt.Rectangle
import com.wxiwei.office.simpletext.model.LeafElement
import com.wxiwei.office.simpletext.model.ParagraphElement
import com.wxiwei.office.simpletext.model.SectionElement
import com.wxiwei.office.constant.wp.WPModelConstant
import com.wxiwei.office.system.sysKit

open class PPTReader @JvmOverloads constructor(control: IControl?, private var filePath: String?, private var isGetThumbnail: Boolean = false) : AbstractReader() {
    init {
        this.control = control
    }

    override fun getModel(): Any? {
        if (model != null) return model
        poiSlideShow = SlideShow(HSLFSlideShow(control, filePath), isGetThumbnail)
        model = PGModel()
        val pageSize: Dimension = poiSlideShow!!.pageSize
        pageSize.width = (pageSize.width * MainConstant.POINT_TO_PIXEL).toInt()
        pageSize.height = (pageSize.height * MainConstant.POINT_TO_PIXEL).toInt()
        model!!.setPageSize(pageSize)
        val count = poiSlideShow!!.slideCount
        model!!.setSlideCount(count)
        if (count == 0) throw Exception("Format error")
        val length = minOf(count, FIRST_READ_SLIDE_NUM)
        repeat(length) {
            processSlide(poiSlideShow!!.getSlide(currentReaderIndex++))
        }
        if (!isReaderFinish() && !isGetThumbnail) BackReaderThread(this, control).start()
        return model
    }

    override fun isReaderFinish(): Boolean = abortReader || model == null || currentReaderIndex >= (poiSlideShow?.slideCount ?: 0)

    override fun backReader() {
        if (poiSlideShow != null && currentReaderIndex < poiSlideShow!!.slideCount) {
            processSlide(poiSlideShow!!.getSlide(currentReaderIndex++))
            if (!isGetThumbnail) control?.actionEvent(com.wxiwei.office.constant.EventConstant.APP_COUNT_PAGES_CHANGE_ID, null)
        }
    }

    private fun processSlide(slide: Slide) {
        val result = PGSlide()
        result.setSlideType(PGSlide.Slide_Normal.toInt())
        result.setSlideNo(number++)
        slide.background?.fill?.let { result.setBackgroundAndFill(convertFill(result, it)) }
        processMaster(result, slide)
        for (shape in slide.shapes) processShape(result, null, shape, PGSlide.Slide_Normal.toInt())
        val notes = slide.notesSheet
        if (notes != null) {
            val noteText = notes.shapes.filterIsInstance<TextShape>()
                .mapNotNull { it.text }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .trim()
            if (noteText.isNotEmpty()) result.setNotes(PGNotes(noteText))
        }
        model?.appendSlide(result)
    }

    private fun processShape(pgSlide: PGSlide, parent: com.wxiwei.office.common.shape.GroupShape?, shape: Shape, slideType: Int) {
        if (abortReader || shape.isHidden) return
        val anchor = if (shape is ShapeGroup) shape.getClientAnchor2D(shape) else shape.logicalAnchor2D
            ?: return
        val rect = Rectangle(
            (anchor.getX() * MainConstant.POINT_TO_PIXEL).toInt(),
            (anchor.getY() * MainConstant.POINT_TO_PIXEL).toInt(),
            (anchor.getWidth() * MainConstant.POINT_TO_PIXEL).toInt(),
            (anchor.getHeight() * MainConstant.POINT_TO_PIXEL).toInt()
        )
        if (shape is ShapeGroup) {
            val group = com.wxiwei.office.common.shape.GroupShape()
            group.setBounds(rect)
            group.setShapeID(shape.shapeId)
            group.setParent(parent)
            for (child in shape.shapes) processShape(pgSlide, group, child, slideType)
            if (parent == null) pgSlide.appendShapes(group) else parent.appendShapes(group)
            return
        }
        if (shape !is SimpleShape) return
        val fill = convertFill(pgSlide, shape.fill)
        val line = getShapeLine(shape)
        val output: IShape? = when {
            shape is Line && line != null -> LineShape().also {
                it.setShapeType(shape.shapeType)
                it.setBounds(rect)
                it.setLine(line)
                it.setBackgroundAndFill(fill)
                it.setShapeID(shape.shapeId)
                val startArrow = shape.startArrowType
                if (startArrow > 0) it.createStartArrow(startArrow.toByte(), shape.startArrowWidth, shape.startArrowLength)
                val endArrow = shape.endArrowType
                if (endArrow > 0) it.createEndArrow(endArrow.toByte(), shape.endArrowWidth, shape.endArrowLength)
                processGrpRotation(shape, it)
            }
            shape is com.wxiwei.office.fc.hslf.model.Freeform && (fill != null || line != null) ->
                ArbitraryPolygonShape().also {
                    it.setShapeType(ShapeTypes.ArbitraryPolygon)
                    it.setBounds(rect)
                    it.setBackgroundAndFill(fill)
                    it.setLine(line)
                    it.setShapeID(shape.shapeId)
                    processGrpRotation(shape, it)
                }
            shape is TextShape -> AutoShape(shape.shapeType).also {
                it.setBounds(rect)
                it.setBackgroundAndFill(fill)
                it.setLine(line)
                it.setShapeID(shape.shapeId)
                processGrpRotation(shape, it)
            }
            shape is com.wxiwei.office.fc.hslf.model.Picture -> PictureShape().also {
                val data = shape.pictureData
                if (data != null) it.setPictureIndex(control?.sysKit?.pictureManage?.addPicture(data) ?: -1)
                it.setBounds(rect)
                it.setShapeID(shape.shapeId)
                it.setBackgroundAndFill(fill)
                it.setLine(line)
                processGrpRotation(shape, it)
            }
            else -> null
        }
        if (output != null) {
            if (parent == null) pgSlide.appendShapes(output) else parent.appendShapes(output)
        }
        if (shape is TextShape) {
            val text = shape.text
            if (!text.isNullOrEmpty()) {
                val textBox = makeTextBox(text, rect, shape.shapeId)
                if (parent == null) pgSlide.appendShapes(textBox) else parent.appendShapes(textBox)
            }
        }
    }

    private fun makeTextBox(text: String, rect: Rectangle, shapeId: Int): TextBox {
        val textBox = TextBox()
        textBox.setBounds(rect)
        textBox.setShapeID(shapeId)
        val section = SectionElement()
        section.setStartOffset(0)
        val paragraph = ParagraphElement()
        paragraph.setStartOffset(0)
        val leaf = LeafElement(text)
        leaf.setStartOffset(0)
        leaf.setEndOffset(text.length.toLong())
        paragraph.appendLeaf(leaf)
        paragraph.setEndOffset(text.length.toLong())
        section.appendParagraph(paragraph, WPModelConstant.MAIN)
        section.setEndOffset(text.length.toLong())
        textBox.setElement(section)
        textBox.setWrapLine(true)
        return textBox
    }

    private fun getShapeLine(shape: SimpleShape): OfficeLine? {
        if (!shape.hasLine()) return null
        val color = shape.lineColor ?: return null
        return OfficeLine().also {
            it.setBackgroundAndFill(BackgroundAndFill().also { fill -> fill.setForegroundColor(color.getRGB()) })
            it.setLineWidth((shape.lineWidth * MainConstant.POINT_TO_PIXEL).toInt())
            it.setDash(shape.lineDashing > 0)
        }
    }

    private fun convertFill(pgSlide: PGSlide, fill: Fill?): BackgroundAndFill? {
        if (fill == null) return null
        return when (fill.fillType.toInt()) {
            BackgroundAndFill.FILL_BACKGROUND.toInt() -> pgSlide.getBackgroundAndFill()
            BackgroundAndFill.FILL_SOLID.toInt() -> fill.foregroundColor?.let {
                BackgroundAndFill().also { result ->
                    result.setFillType(BackgroundAndFill.FILL_SOLID)
                    result.setForegroundColor(it.getRGB())
                }
            }
            BackgroundAndFill.FILL_PATTERN.toInt() -> fill.fillbackColor?.let {
                BackgroundAndFill().also { result ->
                    result.setFillType(BackgroundAndFill.FILL_SOLID)
                    result.setForegroundColor(it.getRGB())
                }
            }
            BackgroundAndFill.FILL_PICTURE.toInt() -> fill.pictureData?.let {
                BackgroundAndFill().also { result ->
                    result.setFillType(BackgroundAndFill.FILL_PICTURE)
                    result.setPictureIndex(control?.sysKit?.pictureManage?.addPicture(it) ?: -1)
                }
            }
            else -> null
        }
    }

    override fun searchContent(file: File?, key: String): Boolean {
        val slideShow = SlideShow(HSLFSlideShow(control, file?.absolutePath), false)
        for (slide in slideShow.slides) {
            for (shape in slide.shapes) if (searchShape(shape, key)) return true
        }
        return false
    }

    fun searchShape(shape: Shape, key: String): Boolean {
        if (shape is TextShape && (shape.text ?: "").contains(key)) return true
        if (shape is ShapeGroup) {
            for (child in shape.shapes) if (searchShape(child, key)) return true
        }
        return false
    }

    fun processMaster(pgSlide: PGSlide, slide: Slide) {
        // Slide master records expose package-private record containers which are
        // intentionally kept on the Java side of this legacy API.
    }

    fun setMaxFontSize(size: Int) {
        if (size > maxFontSize) maxFontSize = size
    }

    fun isRectangle(ts: TextShape): Boolean {
        val type = ts.shapeType
        return type == ShapeTypes.Rectangle || type == ShapeTypes.RoundRectangle || type == ShapeTypes.TextBox
    }

    fun processGrpRotation(shape: Shape, autoShape: IShape) {
        var angle = shape.rotation.toFloat()
        if (shape.getFlipHorizontal()) {
            autoShape.setFlipHorizontal(true)
            angle = -angle
        }
        if (shape.getFlipVertical()) {
            autoShape.setFlipVertical(true)
            angle = -angle
        }
        if (autoShape is LineShape && (angle == 45f || angle == 135f || angle == 225f) && !autoShape.getFlipHorizontal() && !autoShape.getFlipVertical()) angle -= 90f
        autoShape.setRotation(angle)
    }

    override fun dispose() {
        if (isReaderFinish()) {
            super.dispose()
            model = null
            filePath = null
            poiSlideShow?.dispose()
            poiSlideShow = null
        }
    }

    private var number = 1
    private var currentReaderIndex = 0
    private var model: PGModel? = null
    private var poiSlideShow: SlideShow? = null
    private var maxFontSize = 0

    companion object {
        const val FIRST_READ_SLIDE_NUM = 2
        const val POINT_PER_LINE_PER_FONTSIZE = 1.2f
        const val DEFAULT_CELL_WIDTH = 100
        const val DEFAULT_CELL_HEIGHT = 40
    }
}
