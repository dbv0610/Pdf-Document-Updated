package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.common.bg.BackgroundAndFill
import com.wxiwei.office.common.pictureefftect.PictureStretchInfo
import com.wxiwei.office.fc.ShaderKit
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.ZipPackage
import com.wxiwei.office.pg.model.PGMaster
import com.wxiwei.office.system.IControl
import com.wxiwei.office.system.sysKit
import kotlin.math.roundToInt

class BackgroundReader private constructor() {
    fun getBackground(control: IControl?, zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, bg: Element?): BackgroundAndFill? {
        if (bg != null) {
            val bgPr = bg.element("bgPr")
            val bgRef = bg.element("bgRef")
            if (bgRef != null) {
                val fill = BackgroundAndFill()
                fill.fillType = BackgroundAndFill.FILL_SOLID
                fill.foregroundColor = ReaderKit.instance().getColor(master, bgRef)
                return fill
            }
            return processBackground(control, zipPackage, packagePart, master, bgPr)
        }
        return null
    }

    fun processBackground(control: IControl?, zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, bgPr: Element?): BackgroundAndFill? {
        return processBackground(control, zipPackage, packagePart, master, bgPr, false)
    }

    fun processBackground(control: IControl?, zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, bgPr: Element?, isTableStyle: Boolean): BackgroundAndFill? {
        if (bgPr == null || control == null) return null
        val bgFill = BackgroundAndFill()
        var fill = bgPr.element("solidFill")
        if (fill != null) {
            bgFill.fillType = BackgroundAndFill.FILL_SOLID
            bgFill.foregroundColor = ReaderKit.instance().getColor(master, fill, isTableStyle)
            return bgFill
        }
        fill = bgPr.element("blipFill")
        if (fill != null) {
            val blip = fill.element("blip")
            val id = blip?.attributeValue("embed")
            if (id != null) {
                val relationship = packagePart.getRelationship(id)
                val picturePart = relationship?.let { zipPackage.getPart(it.targetURI) }
                if (picturePart != null) {
                    val tile = fill.element("tile")
                    if (tile == null) {
                        bgFill.fillType = BackgroundAndFill.FILL_PICTURE
                        val fillRect = fill.element("stretch")?.element("fillRect")
                        if (fillRect != null) {
                            val stretch = PictureStretchInfo()
                            var valid = false
                            fillRect.attributeValue("l")?.let { stretch.leftOffset = it.toFloat() / 100000; valid = true }
                            fillRect.attributeValue("r")?.let { stretch.rightOffset = it.toFloat() / 100000; valid = true }
                            fillRect.attributeValue("t")?.let { stretch.topOffset = it.toFloat() / 100000; valid = true }
                            fillRect.attributeValue("b")?.let { stretch.bottomOffset = it.toFloat() / 100000; valid = true }
                            if (valid) bgFill.stretch = stretch
                        }
                        bgFill.pictureIndex = control.sysKit.pictureManage.addPicture(picturePart)
                    } else {
                        val pictureIndex = control.sysKit.pictureManage.addPicture(picturePart)
                        bgFill.fillType = BackgroundAndFill.FILL_SHADE_TILE
                        val shader = ShaderKit.readTile(control.sysKit.pictureManage.getPicture(pictureIndex), tile)
                        blip.element("alphaModFix")?.attributeValue("amt")?.let {
                            shader.setAlpha((it.toInt() / 100000f * 255).roundToInt())
                        }
                        bgFill.shader = shader
                    }
                    return bgFill
                }
            }
        }
        fill = bgPr.element("gradFill")
        if (fill != null && fill.element("gsLst") != null) {
            bgFill.fillType = ShaderKit.getGradientType(fill)
            bgFill.shader = ShaderKit.readGradient(master, fill)
            return bgFill
        }
        fill = bgPr.element("fillRef")
        if (fill != null) {
            bgFill.fillType = BackgroundAndFill.FILL_SOLID
            bgFill.foregroundColor = ReaderKit.instance().getColor(master, fill)
            return bgFill
        }
        fill = bgPr.element("pattFill")
        if (fill != null) {
            bgFill.fillType = BackgroundAndFill.FILL_SOLID
            bgFill.foregroundColor = ReaderKit.instance().getColor(master, fill.element("bgClr"))
            return bgFill
        }
        return null
    }

    fun getBackgroundColor(zipPackage: ZipPackage, packagePart: PackagePart, master: PGMaster?, bgPr: Element?, isTableStyle: Boolean): Int {
        if (bgPr != null) {
            var fill = bgPr.element("solidFill")
            if (fill != null) return ReaderKit.instance().getColor(master, fill, isTableStyle)
            fill = bgPr.element("gradFill")
            if (fill != null && fill.element("gsLst") != null) return ReaderKit.instance().getColor(master, fill.element("gsLst")?.element("gs"))
            fill = bgPr.element("fillRef")
            if (fill != null) return ReaderKit.instance().getColor(master, fill)
            fill = bgPr.element("pattFill")
            if (fill != null) return ReaderKit.instance().getColor(master, fill.element("bgClr"))
        }
        return 0
    }

    companion object {
        private val bgReader = BackgroundReader()

        @JvmStatic
        fun instance(): BackgroundReader = bgReader
    }
}
