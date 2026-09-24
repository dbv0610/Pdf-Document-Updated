/*
 * 文件名称:           BulletNumberManage.java
 */
package com.wxiwei.office.fc.ppt.bulletnumber

import com.wxiwei.office.common.bulletnumber.ListKit
import com.wxiwei.office.fc.dom4j.Element
import com.wxiwei.office.system.IControl

class BulletNumberManage {
    private var lvlFmt: MutableMap<Int, Int>? = HashMap()
    private var lvlStartAt: MutableMap<Int, Int>? = HashMap()
    private var lvlNum: MutableMap<Int, Int>? = HashMap()
    private var styleBulletIDs: MutableMap<Int, Int>? = HashMap()
    private var bulletIDs: MutableMap<String, Int>? = HashMap()

    fun getBulletID(styleID: Int): Int = styleBulletIDs?.get(styleID) ?: -1

    fun addBulletNumber(control: IControl?, styleID: Int, pPr: Element?): Int {
        val existing = styleBulletIDs?.get(styleID)
        if (existing != null) {
            return existing
        }
        val text = getBulletText(pPr)
        if (text != null) {
            val id = bulletIDs?.get(text)
            if (id != null) {
                if (styleID > 0) {
                    styleBulletIDs?.set(styleID, id)
                }
                return id
            }
            val newId = control?.getSysKit()?.getPGBulletText()?.addBulletText(text) ?: -1
            bulletIDs?.set(text, newId)
            if (styleID > 0) {
                styleBulletIDs?.set(styleID, newId)
            }
            return newId
        }
        if (pPr != null && pPr.element("buNone") != null) {
            styleBulletIDs?.set(styleID, -2)
        }
        return -1
    }

    private fun getBulletText(pPr: Element?): String? {
        if (pPr == null || pPr.element("buNone") != null) {
            return null
        }
        var lvl = 0
        val levelValue = pPr.attributeValue("lvl")
        if (!levelValue.isNullOrEmpty()) {
            lvl = levelValue.toInt()
        }
        var temp = pPr.element("buAutoNum")
        if (temp != null) {
            var startAt = 1
            val startValue = temp.attributeValue("startAt")
            if (!startValue.isNullOrEmpty()) {
                startAt = startValue.toInt()
            }
            return getText(lvl, convertedNumberFormat(temp.attributeValue("type")), startAt)
        }
        temp = pPr.element("buBlip")
        if (temp != null && temp.element("blip")?.attributeValue("embed") != null) {
            return storeBulletFormat(lvl, converterNumberChar('l'.code))
        }
        temp = pPr.element("buChar")
        val value = temp?.attributeValue("char")
        if (!value.isNullOrEmpty()) {
            return storeBulletFormat(lvl, converterNumberChar(value[0].code))
        }
        return null
    }

    private fun storeBulletFormat(lvl: Int, c: Char): String {
        val previous = lvlFmt?.get(lvl)
        if (previous == null || previous != c.code) {
            if (previous != null && lvl == 0) {
                lvlFmt?.clear()
                lvlStartAt?.clear()
                lvlNum?.clear()
            }
            lvlFmt?.set(lvl, c.code)
        }
        return c.toString()
    }

    private fun getText(lvl: Int, type: Int, startValue: Int): String {
        var start = startValue
        val beforeType = lvlFmt?.get(lvl)
        if (beforeType == null || beforeType != type) {
            if (beforeType != null && lvl == 0) {
                lvlFmt?.clear()
                lvlStartAt?.clear()
                lvlNum?.clear()
            }
            lvlFmt?.set(lvl, type)
            lvlStartAt?.set(lvl, start)
            lvlNum?.set(lvl, start)
        } else {
            val beforeStart = lvlStartAt?.get(lvl)
            if (beforeStart == null || beforeStart != start) {
                lvlStartAt?.set(lvl, start)
                lvlNum?.set(lvl, start)
            } else {
                start = (lvlNum?.get(lvl) ?: start) + 1
                lvlNum?.set(lvl, start)
            }
        }
        var numberId = type
        if (numberId == 5 || numberId == 6 || numberId == 11) numberId = 0
        else if (numberId == 7 || numberId == 12) numberId = 1
        else if (numberId == 8 || numberId == 13) numberId = 2
        else if (numberId == 9 || numberId == 14) numberId = 3
        else if (numberId == 10 || numberId == 15) numberId = 4
        val result = StringBuilder()
        if (type in 11..15) result.append('(')
        result.append(ListKit.instance().getNumberStr(start, numberId))
        if (type in 6..15) result.append(')')
        else if (type != 5) result.append('.')
        return result.toString()
    }

    fun addBulletNumber(control: IControl, lvl: Int, type: Int, start: Int, c: Char): Int {
        val text = converterNumberChar(c.code).toString()
        val old = bulletIDs?.get(text)
        if (old != null) return old
        val id = control.getSysKit().getPGBulletText().addBulletText(text)
        bulletIDs?.set(text, id)
        return id
    }

    private fun convertedNumberFormat(numFormat: String?): Int = when {
        numFormat.equals("arabicPeriod", true) -> 0
        numFormat.equals("romanUcPeriod", true) -> 1
        numFormat.equals("romanLcPeriod", true) -> 2
        numFormat.equals("alphaUcPeriod", true) -> 3
        numFormat.equals("alphaLcPeriod", true) -> 4
        numFormat.equals("arabicPlain", true) || numFormat.equals("circleNumDbPlain", true) -> 5
        numFormat.equals("arabicParenR", true) -> 6
        numFormat.equals("romanUcParenR", true) -> 7
        numFormat.equals("romanLcParenR", true) -> 8
        numFormat.equals("alphaUcParenR", true) -> 9
        numFormat.equals("alphaLcParenR", true) -> 10
        numFormat.equals("arabicParenBoth", true) -> 11
        numFormat.equals("romanUcParentBoth", true) -> 12
        numFormat.equals("romanLcParenBoth", true) -> 13
        numFormat.equals("alphaUcParenBoth", true) -> 14
        numFormat.equals("alphaLcParenBoth", true) -> 15
        numFormat.equals("ea1JpnChsDbPeriod", true) -> 39
        else -> 0
    }

    private fun convertedNumberFormat(type: Int): Int = when (type) {
        0 -> 4
        1 -> 3
        2 -> 6
        3 -> 0
        4 -> 13
        5 -> 8
        6 -> 2
        7 -> 1
        8 -> 15
        9 -> 10
        10 -> 14
        11 -> 9
        12 -> 11
        13, 18 -> 5
        14 -> 12
        15 -> 7
        38 -> 39
        else -> 0
    }

    private fun converterNumberChar(value: Int): Char {
        var c = value
        if (c == 0x2022 || c == 0x006C || c == 0x0070) c = 0x25CF
        else if (c == 0x006E || c == 0x00A7) c = 0x25A0
        else if (c == 0x0075) c = 0x25C6
        else if (c == 0x00FC) c = 0x221A
        else if (c == 0x00D8) c = 0x2605
        else if (c != 0x2013) c = 0x25CF
        return c.toChar()
    }

    fun clearData() {
        lvlFmt?.clear()
        lvlStartAt?.clear()
        lvlNum?.clear()
    }

    fun dispose() {
        lvlFmt?.clear()
        lvlFmt = null
        lvlStartAt?.clear()
        lvlStartAt = null
        lvlNum?.clear()
        lvlNum = null
        styleBulletIDs?.clear()
        styleBulletIDs = null
        bulletIDs?.clear()
        bulletIDs = null
        kit = null
    }

    companion object {
        private var kit: BulletNumberManage? = null

        @JvmStatic
        fun instance(): BulletNumberManage {
            if (kit == null) {
                kit = BulletNumberManage()
            }
            return kit!!
        }
    }
}
