package com.wxiwei.office.fc.ppt.reader

import android.graphics.Color
import com.wxiwei.office.fc.dom4j.io.SAXReader
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import java.util.HashMap

class ThemeReader private constructor() {
    fun getThemeColorMap(themePart: PackagePart): Map<String, Int>? {
        val input = themePart.inputStream
        val document = SAXReader().read(input)
        val root = document.rootElement
        if (root != null) {
            val themeElements = root.element("themeElements")
            if (themeElements != null) {
                val colorScheme = themeElements.element("clrScheme")
                val colorMap = HashMap<String, Int>()
                if (colorScheme != null) {
                    val iterator = colorScheme.elementIterator()
                    while (iterator.hasNext()) {
                        val color = iterator.next() as com.wxiwei.office.fc.dom4j.Element
                        val name = color.name
                        val srgb = color.element("srgbClr")
                        val system = color.element("sysClr")
                        if (srgb != null) {
                            colorMap[name] = Color.parseColor("#" + srgb.attributeValue("val"))
                        } else if (system != null) {
                            colorMap[name] = Color.parseColor("#" + system.attributeValue("lastClr"))
                        } else {
                            colorMap[name] = Color.WHITE
                        }
                    }
                }
                input.close()
                return colorMap
            }
        }
        input.close()
        return null
    }

    companion object {
        private val themeReader = ThemeReader()

        @JvmStatic
        fun instance(): ThemeReader = themeReader
    }
}
