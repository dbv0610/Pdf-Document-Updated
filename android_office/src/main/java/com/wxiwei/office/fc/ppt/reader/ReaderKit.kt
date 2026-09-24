package com.wxiwei.office.fc.ppt.reader

import android.graphics.Color
import com.wxiwei.office.common.shape.IShape
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.java.awt.Rectangle
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.pg.model.PGPlaceholderUtil
import com.wxiwei.office.ss.util.ColorUtil

class ReaderKit {
    fun getPlaceholderName(sp: Element?): String? = getNonVisual(sp)?.element("cNvPr")?.attributeValue("name")

    fun getPlaceholderType(sp: Element?): String? = getNonVisual(sp)?.element("nvPr")?.element("ph")?.attributeValue("type")

    fun getPlaceholderIdx(sp: Element?): Int {
        val value = getNonVisual(sp)?.element("nvPr")?.element("ph")?.attributeValue("idx")
        return if (value != null) value.toDouble().toInt() else -1
    }

    private fun getNonVisual(sp: Element?): Element? {
        if (sp == null) return null
        return when (sp.name) {
            "sp" -> sp.element("nvSpPr")
            "pic" -> sp.element("nvPicPr")
            "graphicFrame" -> sp.element("nvGraphicFramePr")
            "grpSp" -> sp.element("nvGrpSpPr")
            else -> null
        }
    }

    fun getShapeAnchor(xfrm: Element?, zoomX: Float, zoomY: Float): Rectangle? =
        readAnchor(xfrm, "off", "ext", zoomX, zoomY)

    fun getChildShapeAnchor(xfrm: Element?, zoomX: Float, zoomY: Float): Rectangle? =
        readAnchor(xfrm, "chOff", "chExt", zoomX, zoomY)

    private fun readAnchor(xfrm: Element?, offName: String, extName: String, zoomX: Float, zoomY: Float): Rectangle? {
        if (xfrm == null) return null
        val rect = Rectangle()
        val off = xfrm.element(offName)
        val ext = xfrm.element(extName)
        rect.x = scaled(off?.attributeValue("x"), zoomX)
        rect.y = scaled(off?.attributeValue("y"), zoomY)
        rect.width = scaled(ext?.attributeValue("cx"), zoomX)
        rect.height = scaled(ext?.attributeValue("cy"), zoomY)
        return rect
    }

    private fun scaled(value: String?, zoom: Float): Int {
        if (value.isNullOrEmpty()) return 0
        val number = if (isDecimal(value)) value.toInt() else value.toInt(16)
        return (number * zoom * MainConstant.PIXEL_DPI / MainConstant.EMU_PER_INCH).toInt()
    }

    fun getNotes(root: Element): String? {
        val sps = (root.element("cSld")?.element("spTree")?.elements("sp") ?: return null)
            .map { it as Element }
        for (sp in sps) {
            if (PGPlaceholderUtil.BODY == getPlaceholderType(sp)) {
                val notes = StringBuilder()
                val paragraphs = (sp.element("txBody")?.elements("p") ?: emptyList<Any>()).map { it as Element }
                for (p in paragraphs) {
                    for (r in p.elements("r").map { it as Element }) {
                        r.element("t")?.let { notes.append(it.text) }
                    }
                    notes.append('\n')
                }
                val text = notes.toString().trim()
                if (text.isNotEmpty()) return text
            }
        }
        return null
    }

    fun getColor(master: PGMaster?, solidFill: Element?): Int = getColor(master, solidFill, false)

    fun getColor(master: PGMaster?, solidFill: Element?, isTableStyle: Boolean): Int {
        if (solidFill == null) return Color.WHITE
        var temp = solidFill.element("srgbClr")
        if (temp != null) {
            val value = temp.attributeValue("val")
            if (!value.isNullOrEmpty()) return processColorAttribute(temp, Color.parseColor("#$value"), isTableStyle)
        }
        temp = solidFill.element("scrgbClr")
        if (temp != null) {
            val r = temp.attributeValue("r").toInt() * 255 / 100
            val g = temp.attributeValue("g").toInt() * 255 / 100
            val b = temp.attributeValue("b").toInt() * 255 / 100
            return processColorAttribute(temp, ColorUtil.rgb(r, g, b), isTableStyle)
        }
        temp = solidFill.element("schemeClr")
        if (temp != null) {
            val value = temp.attributeValue("val")
            if (!value.isNullOrEmpty()) return processColorAttribute(temp, master?.getColor(value) ?: -1, isTableStyle)
        }
        temp = solidFill.element("sysClr")
        val lastColor = temp?.attributeValue("lastClr")
        if (!lastColor.isNullOrEmpty()) return Color.parseColor("#$lastColor")
        temp = solidFill.element("prstClr")
        if (temp != null) {
            val value = temp.attributeValue("val") ?: ""
            return when {
                value.contains("gray") -> Color.GRAY
                value.contains("white") -> Color.WHITE
                value.contains("red") -> Color.RED
                value.contains("green") -> Color.GREEN
                value.contains("blue") -> Color.BLUE
                value.contains("yellow") -> Color.YELLOW
                value.contains("cyan") -> Color.CYAN
                else -> Color.BLACK
            }
        }
        return Color.WHITE
    }

    private fun processColorAttribute(colorE: Element, initial: Int, isTableStyle: Boolean): Int {
        var color = initial
        val tint = colorE.element("tint")
        val lumOff = colorE.element("lumOff")
        val lumMod = colorE.element("lumMod")
        val shade = colorE.element("shade")
        color = when {
            tint != null -> {
                val value = tint.attributeValue("val").toInt() / 100000.0
                ColorUtil.instance().getColorWithTint(color, if (isTableStyle) 1 - value else value)
            }
            lumOff != null -> ColorUtil.instance().getColorWithTint(color, lumOff.attributeValue("val").toInt() / 100000.0)
            lumMod != null -> ColorUtil.instance().getColorWithTint(color, lumMod.attributeValue("val").toInt() / 100000.0 - 1)
            shade != null -> ColorUtil.instance().getColorWithTint(color, -shade.attributeValue("val").toInt() / 200000.0)
            else -> color
        }
        val alpha = colorE.element("alpha")?.attributeValue("val")
        if (alpha != null) {
            val alphaValue = (alpha.toInt() / 100000f * 255).toInt()
            color = (0xFFFFFF and color) or (alphaValue shl 24)
        }
        return color
    }

    fun isDecimal(num: String): Boolean = num.none { it in "abcdefABCDEF" }

    fun isUserDrawn(sp: Element): Boolean {
        val nvPr = getNonVisual(sp)?.element("nvPr") ?: return false
        val ph = nvPr.element("ph")
        if (ph == null) return true
        val value = nvPr.attributeValue("userDrawn")
        return pptXmlBoolean(value)
    }

    fun getAnchorFitZoom(xfrm: Element?): FloatArray {
        val zoom = floatArrayOf(1f, 1f)
        if (xfrm == null) return zoom
        val ext = xfrm.element("ext")
        val childExt = xfrm.element("chExt")
        val groupWidth = number(ext?.attributeValue("cx"))
        val groupHeight = number(ext?.attributeValue("cy"))
        val childWidth = number(childExt?.attributeValue("cx"))
        val childHeight = number(childExt?.attributeValue("cy"))
        if (childWidth != 0f && childHeight != 0f) {
            zoom[0] = groupWidth / childWidth
            zoom[1] = groupHeight / childHeight
        }
        return zoom
    }

    private fun number(value: String?): Float {
        if (value.isNullOrEmpty()) return 0f
        return if (isDecimal(value)) value.toFloat() else value.toInt(16).toFloat()
    }

    fun processRotation(spPr: Element?, shape: IShape) {
        if (spPr != null) processRotation(shape, spPr.element("xfrm"))
    }

    fun processRotation(shape: IShape, xfrm: Element?) {
        if (xfrm == null) return
        if (pptXmlBoolean(xfrm.attributeValue("flipH"))) shape.setFlipHorizontal(true)
        if (pptXmlBoolean(xfrm.attributeValue("flipV"))) shape.setFlipVertical(true)
        xfrm.attributeValue("rot")?.takeIf { it.isNotEmpty() }?.let { shape.setRotation(it.toFloat() / 60000) }
    }

    fun isHidden(sp: Element): Boolean {
        val value = getNonVisual(sp)?.element("cNvPr")?.attributeValue("hidden")
        return pptXmlBoolean(value)
    }

    companion object {
        private val kit = ReaderKit()

        @JvmStatic
        fun instance(): ReaderKit = kit
    }
}
