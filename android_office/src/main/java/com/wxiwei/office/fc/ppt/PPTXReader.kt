package com.wxiwei.office.fc.ppt

import java.io.File
import com.wxiwei.office.fc.dom4j.Attribute
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.PackageRelationshipTypes
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.fc.dom4j.io.SAXReader
import com.wxiwei.office.fc.ppt.reader.LayoutReader
import com.wxiwei.office.fc.ppt.reader.MasterReader
import com.wxiwei.office.fc.ppt.reader.BackgroundReader
import com.wxiwei.office.pg.model.PGModel
import com.wxiwei.office.pg.model.PGSlide
import com.wxiwei.office.pg.model.PGLayout
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.pg.model.PGStyle
import com.wxiwei.office.system.AbstractReader
import com.wxiwei.office.system.IControl
import com.wxiwei.office.system.OpenTrace

open class PPTXReader(control: IControl, private var filePath: String?) : AbstractReader() {
    init { this.control = control }
    override fun getModel(): Any? {
        val start = android.os.SystemClock.uptimeMillis()
        OpenTrace.mark("pptx.getModel.begin path=$filePath")
        if (model == null) {
            model = PGModel()
            initPackagePart()
            processPresentation()
        }
        OpenTrace.mark("pptx.getModel.end total=${model?.getSlideCount()} loaded=${model?.getRealSlideCount()}", start)
        return model
    }
    fun initPackagePart() {
        zipPackage = ZipPackage(filePath)
        packagePart = zipPackage?.getRelationshipsByType(com.wxiwei.office.fc.openxml4j.opc.PackageRelationshipTypes.CORE_DOCUMENT)
            ?.getRelationship(0)?.let { zipPackage?.getPart(it) }
    }
    private fun processPresentation() {
        val start = android.os.SystemClock.uptimeMillis()
        OpenTrace.mark("pptx.processPresentation.begin")
        val part = packagePart ?: return
        val root = SAXReader().read(part.inputStream).rootElement ?: return
        val size = root.element("sldSz")
        setPageSize(size)
        val ids: List<Element> = root.element("sldIdLst")?.elements("sldId")?.map { it as Element } ?: emptyList()
        model?.setSlideCount(ids.size)
        OpenTrace.d("pptx.presentation.slideIds count=${ids.size} values=${ids.take(3).map { element -> element.attributes().filterIsInstance<Attribute>().joinToString { attr -> "${attr.getQName()}=${attr.getValue()}" } }}")
        for (id in ids.take(FIRST_READ_SLIDE_NUM)) {
            // p:sldId has a numeric presentation id and an r:id relationship id.
            // The relationship id is the one required to resolve the slide part.
            val relationshipId = relationshipId(id)
            OpenTrace.d("pptx.presentation.resolveSlide relationshipId=$relationshipId r:id=${id.attributeValue("r:id")} id=${id.attributeValue("id")}")
            processSlide(relationshipId ?: continue)
            currentReaderIndex++
        }
        slideIds = ids.mapNotNull { relationshipId(it) }
        OpenTrace.mark("pptx.processPresentation.initialLoaded loaded=${model?.getRealSlideCount()} relationshipIds=${slideIds.size}", start)
        // FileReaderThread disposes the reader as soon as getModel() returns.
        // Therefore the remaining slide parts must be loaded here while the ZIP
        // package and relationship table are still alive.
        processSlidePart()
        check(model?.getRealSlideCount() == ids.size) {
            "Incomplete PPTX: expected ${ids.size} slides, loaded ${model?.getRealSlideCount()}"
        }
        OpenTrace.mark("pptx.processPresentation.end total=${ids.size} loaded=${model?.getRealSlideCount()}", start)
    }

    fun processMasterPart(@Suppress("UNUSED_PARAMETER") masterIdLst: Element) {}

    fun processSlidePart() {
        while (!isReaderFinish()) processSlide(slideIds.getOrNull(currentReaderIndex++) ?: return)
    }

    private fun relationshipId(element: Element): String? {
        for (index in 0 until element.attributeCount()) {
            val attribute = element.attribute(index)
            if (attribute.getName() == "id" &&
                attribute.getNamespaceURI() == "http://schemas.openxmlformats.org/officeDocument/2006/relationships") {
                return attribute.getValue()
            }
        }
        return null
    }

    fun setPageSize(slideSize: Element?) {
        val width = slideSize?.attributeValue("cx")?.toIntOrNull() ?: return
        val height = slideSize.attributeValue("cy")?.toIntOrNull() ?: return
        val pageSize = com.wxiwei.office.java.awt.Dimension(
            (width.toDouble() * com.wxiwei.office.constant.MainConstant.PIXEL_DPI / com.wxiwei.office.constant.MainConstant.EMU_PER_INCH).toInt(),
            (height.toDouble() * com.wxiwei.office.constant.MainConstant.PIXEL_DPI / com.wxiwei.office.constant.MainConstant.EMU_PER_INCH).toInt())
        OpenTrace.d("pptx.pageSize emu=${width}x$height pixels=${pageSize.width}x${pageSize.height}")
        model?.setPageSize(pageSize)
    }

    fun searchContentForText(elem: Element, key: String): Boolean {
        if ((elem.text ?: "").contains(key)) return true
        val iterator = elem.elementIterator()
        while (iterator.hasNext()) if (searchContentForText(iterator.next() as Element, key)) return true
        return false
    }
    override fun isReaderFinish(): Boolean = abortReader || model == null || currentReaderIndex >= slideIds.size
    override fun searchContent(file: File?, key: String): Boolean {
        val old = filePath
        filePath = file?.absolutePath
        return try {
            initPackagePart()
            val root = packagePart?.let { SAXReader().read(it.inputStream).rootElement }
            root?.let { searchContentForText(it, key) } ?: false
        } finally {
            filePath = old
        }
    }

    private fun processSlide(id: String) {
        val start = android.os.SystemClock.uptimeMillis()
        val presentation = packagePart ?: run {
            OpenTrace.e("pptx.processSlide.skip reason=noPresentationPart id=$id")
            return
        }
        val relationship = presentation.getRelationship(id) ?: run {
            OpenTrace.e("pptx.processSlide.skip reason=relationshipNotFound id=$id relationshipCount=${presentation.getRelationships().size()}")
            return
        }
        OpenTrace.d("pptx.processSlide.relationship id=$id target=${relationship.targetURI}")
        val slidePart = zipPackage?.getPart(relationship.targetURI) ?: run {
            OpenTrace.e("pptx.processSlide.skip reason=slidePartNotFound target=${relationship.targetURI}")
            return
        }
        val slideRoot = SAXReader().read(slidePart.inputStream).rootElement ?: run {
            OpenTrace.e("pptx.processSlide.skip reason=emptySlideXml target=${relationship.targetURI}")
            return
        }
        val slide = PGSlide()
        slide.setSlideType(PGSlide.Slide_Normal.toInt())
        slide.setSlideNo(slideNumber++)
        val layoutRel = slidePart.getRelationshipsByType(PackageRelationshipTypes.LAYOUT_PART).getRelationship(0)
        val layoutPart = layoutRel?.let { zipPackage?.getPart(it.targetURI) }
        val masterRel = layoutPart?.getRelationshipsByType(PackageRelationshipTypes.SLIDE_MASTER)?.getRelationship(0)
        val masterPart = masterRel?.let { zipPackage?.getPart(it.targetURI) }
        val master: PGMaster? = masterPart?.let { MasterReader.instance().getMasterData(control, zipPackage!!, it, model!!) }
        val layout: PGLayout? = if (layoutPart != null && master != null) {
            LayoutReader.instance().getLayouts(control, zipPackage!!, layoutPart, model!!, master, PGStyle())
        } else null
        val cSld = slideRoot.element("cSld")
        cSld?.element("bg")?.let { slide.setBackgroundAndFill(BackgroundReader.instance().getBackground(control, zipPackage!!, slidePart, master, it)) }
        val tree = cSld?.element("spTree")
        if (tree != null) {
            val iterator = tree.elementIterator()
            while (iterator.hasNext()) ShapeManage.instance().processShape(control, zipPackage!!, slidePart, model, master, layout, null, slide, PGSlide.Slide_Normal, iterator.next() as Element, null, 1f, 1f)
        }
        if (master != null) slide.setMasterSlideIndex(master.getSlideMasterIndex())
        if (layout != null) slide.setLayoutSlideIndex(layout.getSlideMasterIndex())
        model?.appendSlide(slide)
        OpenTrace.mark("pptx.processSlide.end index=${model?.getRealSlideCount()} shapes=${slide.getShapeCount()}", start)
    }
    override fun dispose() {
        super.dispose()
        model = null
        filePath = null
        zipPackage = null
        packagePart = null
    }
    private var model: PGModel? = null
    private var zipPackage: ZipPackage? = null
    private var packagePart: PackagePart? = null
    private var slideIds: List<String> = emptyList()
    private var currentReaderIndex = 0
    private var slideNumber = 1
    companion object { const val FIRST_READ_SLIDE_NUM = 2 }
}
