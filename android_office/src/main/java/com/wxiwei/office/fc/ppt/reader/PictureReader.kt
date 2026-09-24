package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.fc.dom4j.io.SAXReader
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.PackageRelationshipTypes
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.ss.model.drawing.AnchorPoint
import com.wxiwei.office.ss.model.drawing.CellAnchor
import java.util.Hashtable

class PictureReader private constructor() {
    fun getOLEPart(zipPackage: ZipPackage, packagePart: PackagePart, spid: String, bExcel: Boolean?): PackagePart? {
        if (vmlDrawingPart == null) {
            for (relationship in packagePart.getRelationshipsByType(PackageRelationshipTypes.VMLDRAWING_PART)) {
                vmlDrawingPart = zipPackage.getPart(relationship.targetURI)
                getShapeIds(bExcel)
            }
        }
        val id = spIDs?.get(spid) ?: return null
        val relationship = vmlDrawingPart?.getRelationship(id) ?: return null
        return zipPackage.getPart(relationship.targetURI)
    }

    private fun getShapeIds(bExcel: Boolean?) {
        val part = vmlDrawingPart ?: return
        val root = SAXReader().read(part.inputStream).rootElement ?: return
        if (spIDs == null) spIDs = Hashtable()
        if (bExcel == true && spIDAnchors == null) spIDAnchors = Hashtable()
        for (item in root.elements("shape")) {
            val shape = item as com.wxiwei.office.fc.dom4j.Element
            val image = shape.element("imagedata") ?: continue
            var value = shape.attributeValue("spid")
            if (bExcel == true) {
                if (value == null) value = shape.attributeValue("id")
                if (value == null || value.length <= 8) return
                value = value.substring(8)
                spIDs!![value] = image.attributeValue("relid")
                val anchorText = shape.element("ClientData")?.element("Anchor")?.text
                if (anchorText != null && anchorText.toString().isNotEmpty()) {
                    val values = anchorText.toString().trim().replace(" ", "").split(",")
                    if (values.size == 8) {
                        val from = AnchorPoint()
                        from.setColumn(values[0].toShort())
                        from.setDX(values[1].toInt())
                        from.setRow(values[2].toInt())
                        from.setDY(values[3].toInt())
                        val to = AnchorPoint()
                        to.setColumn(values[4].toShort())
                        to.setDX(values[5].toInt())
                        to.setRow(values[6].toInt())
                        to.setDY(values[7].toInt())
                        val anchor = CellAnchor(CellAnchor.TWOCELLANCHOR)
                        anchor.setStart(from); anchor.setEnd(to)
                        spIDAnchors!![value] = anchor
                    }
                }
            } else if (value != null && value.isNotEmpty()) {
                spIDs!![value] = image.attributeValue("relid")
            } else {
                spIDs!![shape.attributeValue("id")] = image.attributeValue("relid")
            }
        }
        part.inputStream.close()
    }

    fun getExcelShapeAnchor(shapeId: String?): CellAnchor? = if (shapeId != null) spIDAnchors?.get(shapeId) else null

    fun dispose() {
        vmlDrawingPart = null
        spIDs?.clear()
        spIDs = null
        spIDAnchors?.values?.forEach { it.dispose() }
        spIDAnchors?.clear()
        spIDAnchors = null
    }

    private var vmlDrawingPart: PackagePart? = null
    private var spIDs: MutableMap<String?, String?>? = null
    private var spIDAnchors: MutableMap<String, CellAnchor>? = null

    companion object {
        private val picReader = PictureReader()
        @JvmStatic fun instance(): PictureReader = picReader
    }
}
