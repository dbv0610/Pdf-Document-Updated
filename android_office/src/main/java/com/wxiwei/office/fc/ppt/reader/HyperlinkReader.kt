package com.wxiwei.office.fc.ppt.reader

import com.wxiwei.office.common.hyperlink.Hyperlink
import com.wxiwei.office.fc.openxml4j.opc.PackagePart
import com.wxiwei.office.fc.openxml4j.opc.PackageRelationshipTypes
import com.wxiwei.office.system.IControl
import com.wxiwei.office.system.sysKit
import java.util.Hashtable

class HyperlinkReader private constructor() {
    fun getHyperlinkList(control: IControl, packagePart: PackagePart) {
        link = Hashtable()
        val relationships = packagePart.getRelationshipsByType(PackageRelationshipTypes.HYPERLINK_PART)
        for (relationship in relationships) {
            val id = relationship.id
            if (getLinkIndex(id) < 0) {
                link!![id] = control.sysKit.getHyperlinkManage().addHyperlink(
                    relationship.targetURI.toString(), Hyperlink.LINK_URL
                )
            }
        }
    }

    fun getLinkIndex(id: String?): Int {
        val index = link?.get(id)
        return index ?: -1
    }

    fun disposs() {
        link?.clear()
        link = null
    }

    private var link: MutableMap<String, Int>? = null

    companion object {
        private val hyperlink = HyperlinkReader()

        @JvmStatic
        fun instance(): HyperlinkReader = hyperlink
    }
}
