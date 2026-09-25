/*
 * 文件名称:          DOCReader.java
 *
 * 编译器:            android2.2
 * 时间:              下午2:57:34
 */
package com.wxiwei.office.fc.doc

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import com.wxiwei.office.common.PaintKit
import com.wxiwei.office.common.autoshape.ExtendPath
import com.wxiwei.office.common.autoshape.pathbuilder.ArrowPathAndTail
import com.wxiwei.office.common.bg.BackgroundAndFill
import com.wxiwei.office.common.bg.Gradient
import com.wxiwei.office.common.bg.LinearGradientShader
import com.wxiwei.office.common.bg.RadialGradientShader
import com.wxiwei.office.common.bg.TileShader
import com.wxiwei.office.common.bookmark.Bookmark
import com.wxiwei.office.common.borders.Border
import com.wxiwei.office.common.borders.Borders
import com.wxiwei.office.common.borders.Line
import com.wxiwei.office.common.bulletnumber.ListData
import com.wxiwei.office.common.bulletnumber.ListLevel
import com.wxiwei.office.common.hyperlink.Hyperlink
import com.wxiwei.office.common.picture.Picture
import com.wxiwei.office.common.pictureefftect.PictureEffectInfoFactory
import com.wxiwei.office.common.shape.AbstractShape
import com.wxiwei.office.common.shape.Arrow
import com.wxiwei.office.common.shape.GroupShape
import com.wxiwei.office.common.shape.IShape
import com.wxiwei.office.common.shape.LineShape
import com.wxiwei.office.common.shape.PictureShape
import com.wxiwei.office.common.shape.ShapeTypes
import com.wxiwei.office.common.shape.WPAbstractShape
import com.wxiwei.office.common.shape.WPAutoShape
import com.wxiwei.office.common.shape.WPGroupShape
import com.wxiwei.office.common.shape.WPPictureShape
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.constant.wp.WPAttrConstant
import com.wxiwei.office.constant.wp.WPModelConstant
import com.wxiwei.office.fc.FCKit
import com.wxiwei.office.fc.ShapeKit
import com.wxiwei.office.fc.ddf.EscherContainerRecord
import com.wxiwei.office.fc.ddf.EscherOptRecord
import com.wxiwei.office.fc.ddf.EscherProperties
import com.wxiwei.office.fc.ddf.EscherSimpleProperty
import com.wxiwei.office.fc.ddf.EscherTextboxRecord
import com.wxiwei.office.fc.hwpf.HWPFDocument
import com.wxiwei.office.fc.hwpf.model.FieldsDocumentPart
import com.wxiwei.office.fc.hwpf.model.ListFormatOverride
import com.wxiwei.office.fc.hwpf.model.ListTables
import com.wxiwei.office.fc.hwpf.model.POIListData
import com.wxiwei.office.fc.hwpf.model.POIListLevel
import com.wxiwei.office.fc.hwpf.model.PicturesTable
import com.wxiwei.office.fc.hwpf.usermodel.Bookmarks
import com.wxiwei.office.fc.hwpf.usermodel.BorderCode
import com.wxiwei.office.fc.hwpf.usermodel.CharacterRun
import com.wxiwei.office.fc.hwpf.usermodel.Field
import com.wxiwei.office.fc.hwpf.usermodel.HWPFAutoShape
import com.wxiwei.office.fc.hwpf.usermodel.HWPFShape
import com.wxiwei.office.fc.hwpf.usermodel.HWPFShapeGroup
import com.wxiwei.office.fc.hwpf.usermodel.HeaderStories
import com.wxiwei.office.fc.hwpf.usermodel.InlineWordArt
import com.wxiwei.office.fc.hwpf.usermodel.LineSpacingDescriptor
import com.wxiwei.office.fc.hwpf.usermodel.OfficeDrawing
import com.wxiwei.office.fc.hwpf.usermodel.OfficeDrawings
import com.wxiwei.office.fc.hwpf.usermodel.POIBookmark
import com.wxiwei.office.fc.hwpf.usermodel.Paragraph
import com.wxiwei.office.fc.hwpf.usermodel.PictureType
import com.wxiwei.office.fc.hwpf.usermodel.Range
import com.wxiwei.office.fc.hwpf.usermodel.Section
import com.wxiwei.office.fc.hwpf.usermodel.Table
import com.wxiwei.office.fc.hwpf.usermodel.TableCell
import com.wxiwei.office.fc.hwpf.usermodel.TableRow
import com.wxiwei.office.fc.util.LittleEndian
import com.wxiwei.office.java.awt.Rectangle
import com.wxiwei.office.java.util.Arrays
import com.wxiwei.office.simpletext.font.FontTypefaceManage
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.IAttributeSet
import com.wxiwei.office.simpletext.model.IElement
import com.wxiwei.office.simpletext.model.LeafElement
import com.wxiwei.office.simpletext.model.ParagraphElement
import com.wxiwei.office.simpletext.model.SectionElement
import com.wxiwei.office.simpletext.view.PageAttr
import com.wxiwei.office.ss.util.ModelUtil
import com.wxiwei.office.system.AbstractReader
import com.wxiwei.office.system.IControl
import com.wxiwei.office.wp.model.CellElement
import com.wxiwei.office.wp.model.HFElement
import com.wxiwei.office.wp.model.RowElement
import com.wxiwei.office.wp.model.TableElement
import com.wxiwei.office.wp.model.WPDocument
import java.io.File
import java.io.FileInputStream
import java.util.Vector
import java.util.regex.Pattern

/**
 * 处理doc文档
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2011-12-31
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
class DOCReader(control: IControl?, filePath: String?) : AbstractReader() {

    init {
        this.control = control
        //controlForReader = control;
    }

    /**
     *
     */
    @Throws(Exception::class)
    override fun getModel(): Any? {
        if (wpdoc != null) {
            return wpdoc
        }
        wpdoc = WPDocument()
        processDoc()
        return wpdoc
    }

    /**
     * 处理doc文档
     */
    @Throws(Exception::class)
    private fun processDoc() {
        poiDoc = HWPFDocument(FileInputStream(File(filePath)))

        /*URL url = new URL("http://172.25.3.147:8080/word_test.doc");
        poiDoc = new HWPFDocument(url.openStream());*/

        /*InputStream is = SocketClient.instance().getFile("E:/workdocument/reader/testdocument/word_test.doc");
        poiDoc = new HWPFDocument(is);*/
        //
        processBulletNumber()
        //
        processBookmark()
        //
        offset = WPModelConstant.MAIN
        docRealOffset = WPModelConstant.MAIN
        val range = poiDoc!!.getRange()
        val numSection = range.numSections()
        var i = 0
        while (i < numSection && !abortReader) {
            // poi的section，后期需要修改
            processSection(range.getSection(i))
            if (isBreakChar) {
                val elem = wpdoc!!.getLeaf(offset - 1)
                if (elem != null && elem is LeafElement) {
                    val s = elem.getText(wpdoc)
                    if (s != null && s.length == 1 && s[0] == '\u000C') {
                        elem.setText('\n'.toString())
                    }
                }
            }
            i++
        }
        //
        processHeaderFooter()
    }

    /**
     *
     */
    private fun processBookmark() {
        val bks: Bookmarks? = poiDoc!!.getBookmarks()
        if (bks != null) {
            for (i in 0 until bks.getBookmarksCount()) {
                val poiBM: POIBookmark = bks.getBookmark(i)
                val bm = Bookmark(poiBM.getName(), poiBM.getStart().toLong(), poiBM.getEnd().toLong())
                control!!.getSysKit().getBookmarkManage().addBookmark(bm)
                bms!!.add(bm)
            }
        }
    }

    /**
     *
     */
    private fun processBulletNumber() {
        val listTables: ListTables = poiDoc!!.getListTables() ?: return
        val size = listTables.getOverrideCount()
        for (i in 0 until size) {
            val listData = ListData()

            val poiData: POIListData = listTables.getListData(listTables.getOverride(i + 1).getLsid()) ?: continue

            // list ID
            listData.setListID(poiData.getLsid())
            // list level
            val levels: Array<POIListLevel> = poiData.getLevels()
            val len = levels.size
            val listLevels = arrayOfNulls<ListLevel>(len)
            for (j in 0 until len) {
                listLevels[j] = ListLevel()
                processListLevel(levels[j], listLevels[j]!!)
            }
            listData.setLevels(listLevels)
            // simpleLevel
            listData.setSimpleList(len.toByte())

            control!!.getSysKit().getListManage().putListData(listData.getListID(), listData)
        }
    }

    /**
     *
     */
    private fun processListLevel(level: POIListLevel, listLevel: ListLevel) {
        // start at
        listLevel.setStartAt(level.getStartAt())
        // horizontal alignment;
        listLevel.setAlign(level.getAlignment().toByte())
        // follow char
        listLevel.setFollowChar(level.getTypeOfCharFollowingTheNumber())
        // number format
        listLevel.setNumberFormat(level.getNumberFormat())
        // number text
        listLevel.setNumberText(converterNumberChar(level.getNumberChar()))
        // special indent, default 21 POINT
        listLevel.setSpecialIndent(level.getSpecialIndnet())
        // left text indent, default 21 point * level
        listLevel.setTextIndent(level.getTextIndent())
    }

    /**
     *
     */
    private fun converterNumberChar(numChar: CharArray?): CharArray? {
        if (numChar == null) {
            return null
        }
        for (i in numChar.indices) {
            val c = numChar[i].code
            if (c == 0xF06C) {
                numChar[i] = 0x25CF.toChar()
            } else if (c == 0xF06E) {
                numChar[i] = 0x25A0.toChar()
            } else if (c == 0xF075) {
                numChar[i] = 0x25C6.toChar()
            } else if (c == 0xF0FC) {
                numChar[i] = 0x221A.toChar()
            } else if (c == 0xF0D8) {
                numChar[i] = 0x2605.toChar()
            } else if (c == 0xF0B2) {
                numChar[i] = 0x2606.toChar()
            } else if (c >= 0xF060) {
                numChar[i] = 0x25CF.toChar()
            }
        }
        return numChar
    }

    /**
     *
     * @param section
     */
    private fun processSection(section: Section) {
        // 建立章节
        val secElem = SectionElement()
        // 属性
        val attr: IAttributeSet = secElem.getAttribute()
        // 宽度 default a4 paper
        AttrManage.instance().setPageWidth(attr, section.getPageWidth())//11906);
        // 高度 default a4 paper
        AttrManage.instance().setPageHeight(attr, section.getPageHeight())//16838);
        // 左边距 default a4 paper
        AttrManage.instance().setPageMarginLeft(attr, section.getMarginLeft())//1800);
        // 右边距 default a4 paper
        AttrManage.instance().setPageMarginRight(attr, section.getMarginRight())//1800);
        // 上边距 default a4 paper
        AttrManage.instance().setPageMarginTop(attr, section.getMarginTop())//1440)
        // 下边距 default a4 paper
        AttrManage.instance().setPageMarginBottom(attr, section.getMarginBottom())//1440);
        // 页眉高度
        AttrManage.instance().setPageHeaderMargin(attr, section.getMarginHeader())//850);
        // 页脚高度
        AttrManage.instance().setPageFooterMargin(attr, section.getMarginFooter())
        //网格类型，高度
        if (section.getGridType() != PageAttr.GRIDTYPE_NONE.toInt()) {
            AttrManage.instance().setPageLinePitch(attr, section.getLinePitch())
        }
        processSectionBorder(secElem, section)
        // 开始Offset
        secElem.setStartOffset(offset)
        //
        val paraCount = section.numParagraphs()
        var i = 0
        while (i < paraCount && !abortReader) {
            val para = section.getParagraph(i)
            if (para.isInTable()) {
                val table = section.getTable(para)
                processTable(table)
                i += table.numParagraphs() - 1
                i++
                continue
            }
            processParagraph(section.getParagraph(i))
            i++
        }
        // 结束Offset
        secElem.setEndOffset(offset)
        wpdoc!!.appendSection(secElem)
    }

    /**
     *
     */
    private fun processSectionBorder(secElem: SectionElement, section: Section) {
        val top: BorderCode? = section.getTopBorder()
        val bottom: BorderCode? = section.getBottomBorder()
        val left: BorderCode? = section.getLeftBorder()
        val right: BorderCode? = section.getRightBorder()
        if (top != null || bottom != null || left != null || right != null) {
            val pageBorderInfo = section.getPageBorderInfo().toByte()
            val offsetFrom = (pageBorderInfo.toInt() shr 5 and 0x07).toByte()
            //int depth = pageBorderInfo >> 3 & 0x03;
            //int applyTo = pageBorderInfo >> 5 & 0x07;
            val borders = Borders()
            borders.setOnType(offsetFrom)
            var border: Border
            // top
            if (top != null) {
                border = Border()
                border.setColor(if (top.getColor().toInt() == 0) Color.BLACK else converterColorForIndex(top.getColor().toInt()))
                border.setSpace((top.getSpace() * MainConstant.POINT_TO_PIXEL).toInt().toShort())
                borders.setTopBorder(border)
            }
            // bottom
            if (bottom != null) {
                border = Border()
                border.setColor(if (bottom.getColor().toInt() == 0) Color.BLACK else converterColorForIndex(bottom.getColor().toInt()))
                border.setSpace((bottom.getSpace() * MainConstant.POINT_TO_PIXEL).toInt().toShort())
                borders.setBottomBorder(border)
            }
            // left
            if (left != null) {
                border = Border()
                border.setColor(if (left.getColor().toInt() == 0) Color.BLACK else converterColorForIndex(left.getColor().toInt()))
                border.setSpace((left.getSpace() * MainConstant.POINT_TO_PIXEL).toInt().toShort())
                borders.setLeftBorder(border)
            }
            // right
            if (right != null) {
                border = Border()
                border.setColor(if (right.getColor().toInt() == 0) Color.BLACK else converterColorForIndex(right.getColor().toInt()))
                border.setSpace((right.getSpace() * MainConstant.POINT_TO_PIXEL).toInt().toShort())
                borders.setRightBorder(border)
            }
            AttrManage.instance().setPageBorder(secElem.getAttribute(), control!!.getSysKit().getBordersManage().addBorders(borders))
        }
    }

    /**
     *
     */
    private fun processHeaderFooter() {
        val hs = HeaderStories(poiDoc)
        // header
        offset = WPModelConstant.HEADER
        docRealOffset = WPModelConstant.HEADER
        // first page
        var range: Range?
        /* range = hs.getFirstHeaderSubrange();
        if (range != null)
        {
            processHeaderFooterPara(range, WPModelConstant.HEADER_ELEMENT, WPModelConstant.HF_FIRST);
        }*/
        // odd page
        range = hs.getOddHeaderSubrange()
        if (range != null) {
            processHeaderFooterPara(range, WPModelConstant.HEADER_ELEMENT, WPModelConstant.HF_ODD)
        }
        // even page
        /*range = hs.getEvenHeaderSubrange();
        if (range != null)
        {
            processHeaderFooterPara(range, WPModelConstant.HEADER_ELEMENT, WPModelConstant.HF_ODD);
        }*/

        // footer
        offset = WPModelConstant.FOOTER
        docRealOffset = WPModelConstant.FOOTER
        // first page
        /*range = hs.getFirstFooterSubrange();
        if (range != null)
        {
            processHeaderFooterPara(range, WPModelConstant.FOOTER_ELEMENT, WPModelConstant.HF_FIRST);
        }*/
        // odd page
        range = hs.getOddFooterSubrange()
        if (range != null) {
            processHeaderFooterPara(range, WPModelConstant.FOOTER_ELEMENT, WPModelConstant.HF_ODD)
        }
        // even page
        /*range = hs.getEvenFooterSubrange();
        if (range != null)
        {
            processHeaderFooterPara(range, WPModelConstant.FOOTER_ELEMENT, WPModelConstant.HF_ODD);
        }*/
    }

    /**
     *
     */
    private fun processHeaderFooterPara(range: Range, elemType: Short, hfType: Byte) {
        val elem: IElement = HFElement(elemType, hfType)
        elem.setStartOffset(offset)
        val paraCount = range.numParagraphs()
        var i = 0
        while (i < paraCount && !abortReader) {
            val para = range.getParagraph(i)
            if (para.isInTable()) {
                val table = range.getTable(para)
                processTable(table)
                i += table.numParagraphs() - 1
                i++
                continue
            }
            processParagraph(para)
            i++
        }
        // 结束Offset
        elem.setEndOffset(offset)
        //
        wpdoc!!.appendElement(elem, offset)
    }

    /**
     * 处理 表格
     * @param table
     */
    private fun processTable(table: Table) {
        val tableElem = TableElement()
        tableElem.setStartOffset(offset)
        val dxs = Vector<Int>()
        // row
        val rowNum = table.numRows()
        for (i in 0 until rowNum) {
            val tableRow = table.getRow(i)
            // table attributes
            if (i == 0) {
                processTableAttribute(tableRow, tableElem.getAttribute())
            }
            val rowElem = RowElement()
            rowElem.setStartOffset(offset)
            // row attribute
            processRowAttribute(tableRow, rowElem.getAttribute())
            // cell
            val numCell = tableRow.numCells()
            var dx = 0
            for (j in 0 until numCell) {
                val cell = tableRow.getCell(j)
                cell.isBackward()
                val cellElem = CellElement()
                cellElem.setStartOffset(offset)
                // cell attribute
                processCellAttribute(cell, cellElem.getAttribute())
                // paragraph
                val numPara = cell.numParagraphs()
                for (k in 0 until numPara) {
                    processParagraph(cell.getParagraph(k))
                }
                cellElem.setEndOffset(offset)
                if (offset > cellElem.getStartOffset()) {
                    rowElem.appendCell(cellElem)
                }

                dx += cell.getWidth()
                //
                if (!dxs.contains(dx)) {
                    dxs.add(dx)
                }
            }
            rowElem.setEndOffset(offset)
            if (offset > rowElem.getStartOffset()) {
                tableElem.appendRow(rowElem)
            }
        }
        tableElem.setEndOffset(offset)
        if (offset > tableElem.getStartOffset()) {
            wpdoc!!.appendParagraph(tableElem, offset)

            // 计算水平合并单元的跨度
            val size = dxs.size
            val maxDx = IntArray(size)
            for (i in 0 until size) {
                maxDx[i] = dxs[i]
            }
            Arrays.sort(maxDx)

            var rowIndex = 0
            var row = tableElem.getElementForIndex(rowIndex++) as RowElement?
            while (row != null) {
                var cellIndex = 0
                var cell = row.getElementForIndex(cellIndex)
                var i = 0
                var dx = 0
                while (cell != null) {
                    dx += AttrManage.instance().getTableCellWidth(cell.getAttribute())
                    while (i < size) {
                        if (dx > maxDx[i]) {
                            row.insertElementForIndex(CellElement(), cellIndex + 1)
                            cellIndex++
                        } else {
                            i++
                            break
                        }
                        i++
                    }

                    cellIndex++
                    cell = row.getElementForIndex(cellIndex)
                }
                row = tableElem.getElementForIndex(rowIndex++) as RowElement?
            }
        }
    }

    /**
     * process table attribute
     */
    private fun processTableAttribute(row: TableRow, attr: IAttributeSet) {
        // table horizontal alignment
        if (row.getRowJustification() != WPAttrConstant.PARA_HOR_ALIGN_LEFT.toInt()) {
            AttrManage.instance().setParaHorizontalAlign(attr, row.getRowJustification())
        }
        if (row.getTableIndent() != 0) {
            AttrManage.instance().setParaIndentLeft(attr, row.getTableIndent())
        }
    }

    /**
     * process table row attribute
     */
    private fun processRowAttribute(row: TableRow, attr: IAttributeSet) {
        // row height
        if (row.getRowHeight() != 0) {
            AttrManage.instance().setTableRowHeight(attr, row.getRowHeight())
        }
        // table header
        if (row.isTableHeader()) {
            AttrManage.instance().setTableHeaderRow(attr, true)
        }
        // table split
        if (row.cantSplit()) {
            AttrManage.instance().setTableRowSplit(attr, true)
        }
        // top border
        /*BorderCode border = row.getTopBorder();
        if (border != null)
        {
            AttrManage.instance().setTableTopBorder(attr, 1);
            AttrManage.instance().setTableTopBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }
        // bottom border
        border = row.getBottomBorder();
        if (border != null)
        {
            AttrManage.instance().setTableBottomBorder(attr, 1);
            AttrManage.instance().setTableBottomBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }
        // left border
        border = row.getLeftBorder();
        if (border != null)
        {
            AttrManage.instance().setTableLeftBorder(attr, 1);
            AttrManage.instance().setTableLeftBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }
        // right border
        border = row.getRightBorder();
        if (border != null)
        {
            AttrManage.instance().setTableRightBorder(attr, 1);
            AttrManage.instance().setTableRightBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }*/
    }

    /**
     *
     */
    private fun processCellAttribute(cell: TableCell, attr: IAttributeSet) {
        // first horizontal merged
        if (cell.isFirstMerged()) {
            AttrManage.instance().setTableHorFirstMerged(attr, true)
        }
        // horizontal merged
        if (cell.isMerged()) {
            AttrManage.instance().setTableHorMerged(attr, true)
        }
        // first vertical merged
        if (cell.isFirstVerticallyMerged()) {
            AttrManage.instance().setTableVerFirstMerged(attr, true)
        }
        // vertical merged
        if (cell.isVerticallyMerged()) {
            AttrManage.instance().setTableVerMerged(attr, true)
        }
        // vertical alignment
        AttrManage.instance().setTableCellVerAlign(attr, cell.getVertAlign().toInt())
        // cell width
        AttrManage.instance().setTableCellWidth(attr, cell.getWidth())

        /*// 上边距
        int margin = cell.getDescriptor().getWCellPaddingTop();
        if (margin != 0)
        {
            AttrManage.instance().setTableTopMargin(attr, margin);
        }
        // 下边距
        margin = cell.getDescriptor().getWCellPaddingBottom();
        if (margin != 0)
        {
            AttrManage.instance().setTableBottomMargin(attr, margin);
        }

        // 左边距
        margin = cell.getDescriptor().getWCellPaddingLeft();
        if (margin != 0)
        {
            AttrManage.instance().setTableLeftMargin(attr, margin);
        }

        // 右边距
        margin = cell.getDescriptor().getWCellPaddingRight();
        if (margin != 0)
        {
            AttrManage.instance().setTableRightMargin(attr, margin);
        }*/

        // top border
        /*BorderCode border = cell.getBrcTop();
        if (border != null)
        {
            AttrManage.instance().setTableTopBorder(attr, 1);
            AttrManage.instance().setTableTopBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }
        // bottom border
        border = cell.getBrcBottom();
        if (border != null)
        {
            AttrManage.instance().setTableBottomBorder(attr, 1);
            AttrManage.instance().setTableBottomBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }
        // left border
        border = cell.getBrcLeft();
        if (border != null)
        {
            AttrManage.instance().setTableLeftBorder(attr, 1);
            AttrManage.instance().setTableLeftBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }
        // right border
        border = cell.getBrcRight();
        if (border != null)
        {
            AttrManage.instance().setTableRightBorder(attr, 1);
            AttrManage.instance().setTableRightBorderColor(attr,
                converterColorForIndex(border.getColor()));
        }*/
    }

    /**
     *
     */
    private fun processParagraph(para: Paragraph) {
        val paraElem = ParagraphElement()
        // 属性
        val attr: IAttributeSet = paraElem.getAttribute()
        // 段前
        AttrManage.instance().setParaBefore(attr, para.getSpacingBefore())
        // 段后
        AttrManage.instance().setParaAfter(attr, para.getSpacingAfter())
        // 左缩进
        AttrManage.instance().setParaIndentLeft(attr, para.getIndentFromLeft())
        // 右缩进
        AttrManage.instance().setParaIndentRight(attr, para.getIndentFromRight())
        // 水平对齐
        AttrManage.instance().setParaHorizontalAlign(attr, converterParaHorAlign(para.getJustification().toInt()).toInt())
        // 垂直对齐
        AttrManage.instance().setParaVerticalAlign(attr, para.getFontAlignment())
        // 特殊缩进
        converterSpecialIndent(attr, para.getFirstLineIndent())
        // 行距
        converterLineSpace(para.getLineSpacing(), attr)
        // list level
        if (para.getIlfo() > 0) {
            val listTables: ListTables? = poiDoc!!.getListTables()
            if (listTables != null) {
                // list ID
                val listFormatOverride: ListFormatOverride? = listTables.getOverride(para.getIlfo())
                if (listFormatOverride != null) {
                    AttrManage.instance().setParaListID(attr, listFormatOverride.getLsid())
                }

                // list level
                AttrManage.instance().setParaListLevel(attr, para.getIlvl().toInt())
            }
        }
        if (para.isInTable()) {
            AttrManage.instance().setParaLevel(attr, para.getTableLevel())
        }
        // 开始 offset
        paraElem.setStartOffset(offset)

        val runCount = para.numCharacterRuns()
        var isFieldCode = false
        var isFieldText = false
        var field: Field? = null
        var preRun: CharacterRun?
        var run: CharacterRun? = null
        var fieldCode = ""
        var fieldText = ""
        val before = docRealOffset
        var i = 0
        while (i < runCount && !abortReader) {
            preRun = run
            run = para.getCharacterRun(i)
            val text = run.text()
            if (text.length == 0 || run.isMarkedDeleted()) {
                i++
                continue
            }
            docRealOffset += text.length.toLong()
            val ch = text[0].code
            val lastch = text[text.length - 1].code
            if ((ch == '\t'.code && text.length == 1)
                || ch == 0x05 // 批注框
                /*|| ch == 0x01*/) { // 嵌入对象占位符
                i++
                continue
            } else if (ch == 0x13 || lastch == 0x13) { // 域开始符
                if (ch != 0x15 || lastch != 0x13) {
                    val area = offset and WPModelConstant.AREA_MASK
                    val fieldsPart = if (area == WPModelConstant.HEADER || area == WPModelConstant.FOOTER)
                        FieldsDocumentPart.HEADER else FieldsDocumentPart.MAIN
                    field = poiDoc!!.getFields().getFieldByStartOffset(fieldsPart, run.getStartOffset().toInt())
                    isFieldCode = true
                }
                i++
                continue
            } else if (ch == 0x14 || lastch == 0x14) { // 域分隔符
                isFieldCode = false
                isFieldText = true
                i++
                continue
            } else if (ch == 0x15 || lastch == 0x15) { // 域结束符
                if (preRun != null && fieldText != null
                    && field != null && field.getType() == Field.EMBED.toInt()) {
                    //EMBED object
                    if (fieldText.indexOf("EQ") >= 0 &&
                        fieldText.indexOf("jc") >= 0) {
                        processRun(preRun, para, field, paraElem, fieldCode, fieldText)
                    } else {
                        if (lastch == 0x15) {
                            fieldText += text.substring(0, text.length - 1)
                        }
                        processRun(run, para, field, paraElem, fieldCode, fieldText)
                    }
                } else if (isPageNumber(field, fieldCode)) {
                    processRun(run, para, field, paraElem, fieldCode, fieldText)
                }

                isFieldText = false
                isFieldCode = false
                field = null
                hyperlinkAddress = null
                fieldCode = ""
                fieldText = ""
                i++
                continue
            }
            if (isFieldCode) {
                fieldCode += run.text()
                i++
                continue
            }

            if (isFieldText && isPageNumber(field, fieldCode)) {
                fieldText += run.text()
                i++
                continue
            }

            processRun(run, para, field, paraElem, null, null)
            //fieldText = "";
            i++
        }
        if (para.getTabClearPosition() > 0) {
            AttrManage.instance().setParaTabsClearPostion(attr, para.getTabClearPosition().toInt())
        }

        // 结束 offset
        if (offset == paraElem.getStartOffset()) {
            //this paragraph is not Marked Deleted
            paraElem.dispose()

            return
        }

        paraElem.setEndOffset(offset)
        wpdoc!!.appendParagraph(paraElem, offset)
        //
        adjustBookmarkOffset(before, docRealOffset)
    }

    private fun isPageNumber(field: Field?, fieldCode: String?): Boolean {
        if (field != null && (field.getType() == Field.PAGE.toInt() || field.getType() == Field.NUMPAGES.toInt())) {
            return true
        } else if (fieldCode != null && (fieldCode.contains("NUMPAGES") || fieldCode.contains("PAGE"))) {
            return true
        }

        return false
    }

    /**
     * 先后顺序：
     * 1、如果出现了sprmPDxcLeft，则取该数据作为左缩进字符单位，数据格式为浮点数乘以100；
     * 2、如果没有出现sprmPDxcLeft记录，则取sprmPDxaleft数据作为左缩进，数据格式为浮点数乘以20；
     * 3、如果前两条记录都没出现，则取sprmPDxaLeft80作为左缩进，数据格式为浮点数乘以20；
     * 其他缩进方式参照这个执行
     */
    private fun converterSpecialIndent(attr: IAttributeSet, firstLintIndent: Int) {
        /*
         * 特殊缩进
         * 规则：firstLineIndent > 0，表示首行缩进
         *       firstLineIndent < 0，表示悬挂缩进
         */
        AttrManage.instance().setParaSpecialIndent(attr, firstLintIndent)
        //  悬挂缩进值也设置到左缩进，左缩进需要减去悬挂缩进
        if (firstLintIndent < 0) {
            AttrManage.instance().setParaIndentLeft(attr,
                AttrManage.instance().getParaIndentLeft(attr) + firstLintIndent)
        }
    }

    /**
     * 行距规则
     * 如果 _fMultiLinespace = 1，表示为倍数，用_dyaLine / 240就是设置倍数
     *      _fMultiLinespace = 0，表示为非倍数，
     *      如果 _dyaLine > 0，表示为最小值，用_dyaLine / 20 就是设置的最小值，单位磅
     *           _dyaLine < 0，表示为固定值，用_dyaLine / 20 就是设置的固定值，单位磅
     */
    private fun converterLineSpace(sd: LineSpacingDescriptor, attr: IAttributeSet) {
        var lineSpaceType = WPAttrConstant.LINE_SPACE_SINGLE.toInt()
        var lineSpaceValue = 1f

        // 倍数
        if (sd.getMultiLinespace().toInt() == 1) {
            val t = sd.getDyaLine() / 240.0f
            if (t == 1f) {
                lineSpaceType = WPAttrConstant.LINE_SPACE_SINGLE.toInt()
                lineSpaceValue = 1f
            } else if (t == 1.5f) {
                lineSpaceType = WPAttrConstant.LINE_SPACE_ONE_HALF.toInt()
                lineSpaceValue = 1.5f
            } else if (t == 2f) {
                lineSpaceType = WPAttrConstant.LINE_SPACE_DOUBLE.toInt()
                lineSpaceValue = 2f
            } else {
                lineSpaceType = WPAttrConstant.LINE_SPACE_DOUBLE.toInt()
                lineSpaceValue = t
            }
        }
        // 设定值
        else {
            val t = sd.getDyaLine().toFloat()
            if (t >= 0) {
                lineSpaceType = WPAttrConstant.LINE_SAPCE_LEAST.toInt()
                lineSpaceValue = t
            } else {
                lineSpaceType = WPAttrConstant.LINE_SPACE_EXACTLY.toInt()
                lineSpaceValue = -t
            }
        }
        // 行距
        AttrManage.instance().setParaLineSpace(attr, lineSpaceValue)
        // 行距类型
        AttrManage.instance().setParaLineSpaceType(attr, lineSpaceType)
    }

    /**
     * 转换段落对齐试
     * @param js
     * @return
     */
    private fun converterParaHorAlign(js: Int): Byte {
        return when (js) {
            0, 3, 4, 6, 9, 7 -> WPAttrConstant.PARA_HOR_ALIGN_LEFT
            1, 5 -> WPAttrConstant.PARA_HOR_ALIGN_CENTER
            2, 8 -> WPAttrConstant.PARA_HOR_ALIGN_RIGHT
            else -> WPAttrConstant.PARA_HOR_ALIGN_LEFT
        }
    }

    /**
     *
     */
    private fun processRun(run: CharacterRun, parentRange: Range, field: Field?, paraElem: ParagraphElement,
                           fieldCode: String?, fieldText: String?) {
        var text: String? = run.text()
        if (fieldText != null) {
            text = fieldText
        }

        if (text != null && text.length > 0) {
            // 嵌入对象占位符
            var ch = text[0]
            isBreakChar = ch == '\u000C'
            if (ch == '\b' || ch.code == 0x01) {
                var i = 0
                while (i < text.length && !run.isVanished()) {
                    ch = text[i]
                    if (ch == '\b' || ch.code == 0x01) {
                        val leaf = LeafElement(ch.toString())
                        // process shape error
                        if (!processShape(run, leaf, ch == '\b', i)) {
                            return
                        }
                        // 开始 offset
                        leaf.setStartOffset(offset)
                        offset += 1
                        // 结束 offset
                        leaf.setEndOffset(offset)
                        paraElem.appendLeaf(leaf)
                    }
                    i++
                }
                return
            }
        }

        val leaf = LeafElement(text)
        // 属性
        val attr: IAttributeSet = leaf.getAttribute()
        // 字号
        AttrManage.instance().setFontSize(attr, (run.getFontSize() / 2f + 0.5).toInt())
        // 字体
        var index = FontTypefaceManage.instance().addFontName(run.getFontName())
        if (index >= 0) {
            AttrManage.instance().setFontName(attr, index)
        }
        // 字符颜色
        AttrManage.instance().setFontColor(attr, FCKit.BGRtoRGB(run.getIco24()))
        // 粗体
        AttrManage.instance().setFontBold(attr, run.isBold())
        // 斜体
        AttrManage.instance().setFontItalic(attr, run.isItalic())
        // 删除线
        AttrManage.instance().setFontStrike(attr, run.isStrikeThrough())
        // 双删除线
        AttrManage.instance().setFontDoubleStrike(attr, run.isDoubleStrikeThrough())
        // 下划线
        AttrManage.instance().setFontUnderline(attr, run.getUnderlineCode())
        // 下划线颜色
        AttrManage.instance().setFontUnderlineColr(attr, FCKit.BGRtoRGB(run.getUnderlineColor()))
        // 上下标
        AttrManage.instance().setFontScript(attr, run.getSubSuperScriptIndex().toInt())
        // 高亮
        AttrManage.instance().setFontHighLight(attr, converterColorForIndex(run.getHighlightedColor().toInt()))

        if (field != null && field.getType() == Field.HYPERLINK.toInt()) {
            // hyperlink
            if (hyperlinkAddress == null) {
                val firstSubrange: Range? = field.firstSubrange(parentRange)
                if (firstSubrange != null) {
                    val formula = firstSubrange.text()
                    val matcher = hyperlinkPattern.matcher(formula)
                    if (matcher.find()) {
                        hyperlinkAddress = matcher.group(1)
                    }
                }
            }
            if (hyperlinkAddress != null) {
                index = control!!.getSysKit().getHyperlinkManage().addHyperlink(hyperlinkAddress, Hyperlink.LINK_URL.toInt())
                if (index >= 0) {
                    AttrManage.instance().setFontColor(attr, Color.BLUE)
                    AttrManage.instance().setFontUnderline(attr, 1)
                    AttrManage.instance().setFontUnderlineColr(attr, Color.BLUE)
                    AttrManage.instance().setHyperlinkID(attr, index)
                }
            }
        } else if (fieldCode != null) {
            if (fieldCode.indexOf("HYPERLINK") > 0) {
                index = fieldCode.indexOf("_Toc")
                if (index > 0) {
                    val endIndex = fieldCode.lastIndexOf('"')
                    if (endIndex > 0 && endIndex > index) {
                        val bmName = fieldCode.substring(index, endIndex)
                        index = control!!.getSysKit().getHyperlinkManage().addHyperlink(bmName, Hyperlink.LINK_BOOKMARK.toInt())
                        if (index >= 0) {
                            AttrManage.instance().setFontColor(attr, Color.BLUE)
                            AttrManage.instance().setFontUnderline(attr, 1)
                            AttrManage.instance().setFontUnderlineColr(attr, Color.BLUE)
                            AttrManage.instance().setHyperlinkID(attr, index)
                        }
                    }
                }
            } else {
                val area = offset and WPModelConstant.AREA_MASK
                if (area == WPModelConstant.HEADER || area == WPModelConstant.FOOTER) {
                    var pageNumberType = -1
                    if (fieldCode != null) {
                        if (fieldCode.contains("NUMPAGES")) {
                            pageNumberType = WPModelConstant.PN_TOTAL_PAGES.toInt()
                        } else if (fieldCode.contains("PAGE")) {
                            pageNumberType = WPModelConstant.PN_PAGE_NUMBER.toInt()
                        }
                    }

                    if (pageNumberType > 0) {
                        AttrManage.instance().setFontPageNumberType(leaf.getAttribute(), pageNumberType)
                    }
                }
            }
        }

        // 开始 offset
        leaf.setStartOffset(offset)
        offset += text!!.length.toLong()
        // 结束 offset
        leaf.setEndOffset(offset)
        paraElem.appendLeaf(leaf)
    }

    /**
     *
     */
    private fun converterColorForIndex(color: Int): Int {
        return when (color) {
            1 -> Color.BLACK
            2 -> Color.BLUE
            3 -> Color.CYAN
            4 -> Color.GREEN
            5 -> Color.MAGENTA
            6 -> Color.RED
            7 -> Color.YELLOW
            8 -> Color.WHITE
            9 -> Color.BLUE
            10 -> Color.DKGRAY
            11 -> Color.GREEN
            12 -> Color.MAGENTA
            13 -> Color.RED
            14 -> Color.YELLOW
            15 -> Color.GRAY
            16 -> Color.LTGRAY
            else -> -1
        }
    }

    private fun converFill(shape: HWPFAutoShape?, drawing: OfficeDrawing, shapeType: Int): BackgroundAndFill? {
        if (shapeType == ShapeTypes.Line || shapeType == ShapeTypes.StraightConnector1
            || shapeType == ShapeTypes.BentConnector2 || shapeType == ShapeTypes.BentConnector3
            || shapeType == ShapeTypes.CurvedConnector3) {
            return null
        }

        var bgFill: BackgroundAndFill? = null
        if (shape != null) {
            val type = shape.getFillType()
            // 填充类型
            if (type == BackgroundAndFill.FILL_SOLID.toInt() || type == BackgroundAndFill.FILL_BACKGROUND.toInt()) {
                if (shape.getForegroundColor() != null) {
                    bgFill = BackgroundAndFill()
                    bgFill.setFillType(BackgroundAndFill.FILL_SOLID)
                    // 前景颜色
                    bgFill.setForegroundColor(shape.getForegroundColor().getRGB())
                }
            } else if (type == BackgroundAndFill.FILL_SHADE_LINEAR.toInt() || type == BackgroundAndFill.FILL_SHADE_RADIAL.toInt()
                || type == BackgroundAndFill.FILL_SHADE_RECT.toInt() || type == BackgroundAndFill.FILL_SHADE_SHAPE.toInt()) {
                bgFill = BackgroundAndFill()
                var angle = shape.getFillAngle()
                when (angle) {
                    -90, 0 -> angle += 90
                    -45 -> angle = 135
                    -135 -> angle = 45
                }
                val focus = shape.getFillFocus()
                val fillColor: com.wxiwei.office.java.awt.Color? = shape.getForegroundColor()
                val fillbackColor: com.wxiwei.office.java.awt.Color? = shape.getFillbackColor()

                var colors: IntArray? = null
                var positions: FloatArray? = null
                if (shape.isShaderPreset()) {
                    colors = shape.getShaderColors()
                    positions = shape.getShaderPositions()
                }

                if (colors == null) {
                    colors = intArrayOf(fillColor?.getRGB() ?: 0xFFFFFFFF.toInt(),
                        fillbackColor?.getRGB() ?: 0xFFFFFFFF.toInt())
                }
                if (positions == null) {
                    positions = floatArrayOf(0f, 1f)
                }

                var gradient: Gradient? = null
                if (type == BackgroundAndFill.FILL_SHADE_LINEAR.toInt()) {
                    gradient = LinearGradientShader(angle.toFloat(), colors, positions)
                } else if (type == BackgroundAndFill.FILL_SHADE_RADIAL.toInt()
                    || type == BackgroundAndFill.FILL_SHADE_RECT.toInt()
                    || type == BackgroundAndFill.FILL_SHADE_SHAPE.toInt()) {
                    gradient = RadialGradientShader(shape.getRadialGradientPositionType(), colors, positions)
                }

                gradient?.setFocus(focus)

                bgFill.setFillType(type.toByte())
                bgFill.setShader(gradient)
            } else if (type == BackgroundAndFill.FILL_SHADE_TILE.toInt()) {
                // 背景为图片
                val data = drawing.getPictureData(control, shape.getBackgroundPictureIdx())
                if (data != null && isSupportPicture(PictureType.findMatchingType(data))) {
                    bgFill = BackgroundAndFill()
                    bgFill.setFillType(BackgroundAndFill.FILL_SHADE_TILE)
                    // 图片数据
                    var index = control!!.getSysKit().getPictureManage().getPictureIndex(drawing.getTempFilePath(control))
                    if (index < 0) {
                        val picture = Picture()
                        // 图片数据
                        picture.setTempFilePath(drawing.getTempFilePath(control))
                        // 图片类型
                        picture.setPictureType(PictureType.findMatchingType(data).getExtension())
                        index = control!!.getSysKit().getPictureManage().addPicture(picture)
                        bgFill.setShader(
                            TileShader(control!!.getSysKit().getPictureManage().getPicture(index),
                                TileShader.Flip_None, 1f, 1.0f))
                    }
                }
            } else if (type == BackgroundAndFill.FILL_PICTURE.toInt()) {
                // 背景为图片
                val data = drawing.getPictureData(control, shape.getBackgroundPictureIdx())
                if (data != null && isSupportPicture(PictureType.findMatchingType(data))) {
                    bgFill = BackgroundAndFill()
                    bgFill.setFillType(BackgroundAndFill.FILL_PICTURE)
                    // 图片数据
                    var index = control!!.getSysKit().getPictureManage().getPictureIndex(drawing.getTempFilePath(control))
                    if (index < 0) {
                        val picture = Picture()
                        // 图片数据
                        picture.setTempFilePath(drawing.getTempFilePath(control))
                        // 图片类型
                        picture.setPictureType(PictureType.findMatchingType(data).getExtension())
                        index = control!!.getSysKit().getPictureManage().addPicture(picture)
                    }
                    bgFill.setPictureIndex(index)
                }
            } else if (type == BackgroundAndFill.FILL_PATTERN.toInt()) {
                /*         	byte[] data = drawing.getBGPictureData(shape.getBackgroundPictureIdx());
                if (data != null  && isSupportPicture(PictureType.findMatchingType(data)))
                {
                	bgFill = new BackgroundAndFill();
                    bgFill.setFillType(BackgroundAndFill.FILL_PATTERN);
                    // 图片数据
                    int index = control.getSysKit().getPictureManage().getPictureIndex(drawing.getTempFilePath());
                    if (index < 0)
                    {
                    	control.getSysKit().getPictureManage().writeTempFile(data);

                        Picture picture = new Picture();
                        // 图片数据
                        picture.setTempFilePath(drawing.getTempFilePath());
                        // 图片类型
                        picture.setPictureType(PictureType.findMatchingType(data).getExtension());
                        index = control.getSysKit().getPictureManage().addPicture(picture);
                        bgFill.setShader(
                				new PatternShader(control.getSysKit().getPictureManage().getPicture(index),
                						shape.getBackgroundColor().getRGB(), shape.getForegroundColor().getRGB()));
                    }
                }
                else*/ if (shape.getFillbackColor() != null) {
                    bgFill = BackgroundAndFill()
                    bgFill.setFillType(BackgroundAndFill.FILL_SOLID)
                    // 前景颜色
                    bgFill.setForegroundColor(shape.getFillbackColor().getRGB())
                }
            }
        }
        return bgFill
    }

    private fun processRotation(shape: HWPFAutoShape, autoShape: IShape) {
        var angle = shape.getRotation().toFloat()
        if (shape.getFlipHorizontal()) {
            autoShape.setFlipHorizontal(true)
            angle = -angle
        }
        if (shape.getFlipVertical()) {
            autoShape.setFlipVertical(true)
            angle = -angle
        }

        if (autoShape is LineShape) {
            if ((angle == 45f || angle == 135f || angle == 225f)
                && !autoShape.getFlipHorizontal()
                && !autoShape.getFlipVertical()) {
                angle -= 90f
            }
        }
        autoShape.setRotation(angle)
    }

    /**
     * 重新计算group shape中的child shape的位置
     * @param grpRect
     * @param offsetRect
     * @param rect
     * @return
     */
    private fun processGrpSpRect(parent: GroupShape?, rect: Rectangle): Rectangle {
        if (parent != null) {
            rect.x += parent.getOffX()
            rect.y += parent.getOffY()
        }
        return rect
    }

    /**
     *
     * @param shape
     * @param autoShape
     */
    private fun processAutoshapePosition(shape: HWPFAutoShape, autoShape: WPAutoShape) {
        //horizontal alignment
        when (shape.getPosition_H()) {
            HWPFShape.POSH_ABS.toInt() -> autoShape.setHorPositionType(WPAbstractShape.POSITIONTYPE_ABSOLUTE)
            HWPFShape.POSH_LEFT.toInt() -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_LEFT)
            HWPFShape.POSH_CENTER.toInt() -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_CENTER)
            HWPFShape.POSH_RIGHT.toInt() -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_RIGHT)
            HWPFShape.POSH_INSIDE.toInt() -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_INSIDE)
            HWPFShape.POSH_OUTSIDE.toInt() -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_OUTSIDE)
        }

        //relative to in horizontal
        when (shape.getPositionRelTo_H()) {
            HWPFShape.POSRELH_MARGIN.toInt() -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_MARGIN)
            HWPFShape.POSRELH_PAGE.toInt() -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_PAGE)
            HWPFShape.POSRELH_COLUMN.toInt() -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_COLUMN)
            HWPFShape.POSRELH_CHAR.toInt() -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_CHARACTER)
        }

        //alignment in vertical
        when (shape.getPosition_V()) {
            HWPFShape.POSV_ABS.toInt() -> autoShape.setVerPositionType(WPAbstractShape.POSITIONTYPE_ABSOLUTE)
            HWPFShape.POSV_TOP.toInt() -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_TOP)
            HWPFShape.POSV_CENTER.toInt() -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_CENTER)
            HWPFShape.POSV_BOTTOM.toInt() -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_BOTTOM)
            HWPFShape.POSV_INSIDE.toInt() -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_INSIDE)
            HWPFShape.POSV_OUTSIDE.toInt() -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_OUTSIDE)
        }

        //relative to in vertical
        when (shape.getPositionRelTo_V()) {
            HWPFShape.POSRELV_MARGIN.toInt() -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_MARGIN)
            HWPFShape.POSRELV_PAGE.toInt() -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_PAGE)
            HWPFShape.POSRELV_TEXT.toInt() -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_PARAGRAPH)
            HWPFShape.POSRELV_LINE.toInt() -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_LINE)
        }
    }

    private fun getPictureframeData(darwing: OfficeDrawing, poiShape: HWPFShape): ByteArray? {
        val escherOptRecord: EscherOptRecord =
            poiShape.getSpContainer().getChildById<EscherOptRecord>(EscherOptRecord.RECORD_ID) ?: return null

        val escherProperty: EscherSimpleProperty = escherOptRecord
            .lookup(EscherProperties.BLIP__BLIPTODISPLAY.toInt()) ?: return null

        val bitmapIndex = escherProperty.getPropertyValue()
        return darwing.getPictureData(control, bitmapIndex)
    }

    private fun convertShape(leaf: IElement, drawing: OfficeDrawing, parent: GroupShape?,
                             poiShape: HWPFShape, rect: Rectangle?, zoomX: Float, zoomY: Float): Boolean {
        var rect = rect ?: return false

        if (poiShape is HWPFAutoShape) {
            val shape = poiShape
            val shapeType = shape.getShapeType()
            var fill = converFill(shape, drawing, shapeType)
            val line: Line? = poiShape.getLine(shapeType == ShapeTypes.Line)
            if (line != null || fill != null || shapeType == ShapeTypes.TextBox || shapeType == ShapeTypes.PictureFrame) {
                rect = processGrpSpRect(parent, rect)

                val autoShape: WPAutoShape = if (shapeType == ShapeTypes.PictureFrame) {
                    WPPictureShape()
                } else {
                    WPAutoShape()
                }

                autoShape.setShapeType(shapeType)
                autoShape.setAuotShape07(false)

                val angle = Math.abs(shape.getRotation())
                autoShape.setBounds(ModelUtil.processRect(rect, angle.toFloat()))

                autoShape.setBackgroundAndFill(fill)
                if (line != null) {
                    autoShape.setLine(line)
                }
                val adj: Array<Float>? = shape.getAdjustmentValue()
                autoShape.setAdjustData(adj)
                processRotation(shape, autoShape)

                processAutoshapePosition(shape, autoShape)

                var isLineShape = false
                if (shapeType == ShapeTypes.PictureFrame) {
                    val b = getPictureframeData(drawing, shape)
                    if (b != null) {
                        if (isSupportPicture(PictureType.findMatchingType(b))) {
                            val picShape = PictureShape()

                            var index = control!!.getSysKit().getPictureManage().getPictureIndex(drawing.getTempFilePath(control))
                            if (index < 0) {
                                val picture = Picture()
                                // 图片数据
                                picture.setTempFilePath(drawing.getTempFilePath(control))
                                // 图片类型
                                picture.setPictureType(PictureType.findMatchingType(b).getExtension())
                                index = control!!.getSysKit().getPictureManage().addPicture(picture)
                            }
                            picShape.setPictureIndex(index)
                            picShape.setBounds(rect)
                            picShape.setZoomX(1000.toShort())
                            picShape.setZoomY(1000.toShort())
                            picShape.setPictureEffectInfor(drawing.getPictureEffectInfor())
                            (autoShape as WPPictureShape).setPictureShape(picShape)
                        }
                    }
                } else if (shapeType == ShapeTypes.Line || shapeType == ShapeTypes.StraightConnector1
                    || shapeType == ShapeTypes.BentConnector2 || shapeType == ShapeTypes.BentConnector3
                    || shapeType == ShapeTypes.CurvedConnector3) {
                    isLineShape = true
                    if (autoShape.getShapeType() == ShapeTypes.BentConnector2 && adj == null) {
                        autoShape.setAdjustData(arrayOf(1.0f))
                    }

                    var type = shape.getStartArrowType()
                    if (type > 0) {
                        autoShape.createStartArrow(type.toByte(),
                            shape.getStartArrowWidth(),
                            shape.getStartArrowLength())
                    }

                    type = shape.getEndArrowType()
                    if (type > 0) {
                        autoShape.createEndArrow(type.toByte(),
                            shape.getEndArrowWidth(),
                            shape.getEndArrowLength())
                    }
                } else if (shapeType == ShapeTypes.NotPrimitive || shapeType == ShapeTypes.NotchedCircularArrow) {
                    isLineShape = true
                    autoShape.setShapeType(ShapeTypes.ArbitraryPolygon)

                    var startArrowTailCenter: PointF? = null
                    var endArrowTailCenter: PointF? = null

                    val startArrowType = shape.getStartArrowType()
                    if (startArrowType > 0) {
                        val arrowPathAndTail: ArrowPathAndTail? = shape.getStartArrowPath(rect)
                        if (arrowPathAndTail != null && arrowPathAndTail.arrowPath != null) {
                            startArrowTailCenter = arrowPathAndTail.arrowTailCenter
                            val pathExtend = ExtendPath()
                            pathExtend.path = arrowPathAndTail.arrowPath
                            pathExtend.setArrowFlag(true)
                            if (startArrowType != Arrow.Arrow_Arrow.toInt()) {
                                if ((line == null || line.getBackgroundAndFill() == null) && poiShape.getLineColor() != null) {
                                    val arrowFill = BackgroundAndFill()
                                    arrowFill.setFillType(BackgroundAndFill.FILL_SOLID)
                                    arrowFill.setForegroundColor(poiShape.getLineColor().getRGB())
                                    pathExtend.backgroundAndFill = arrowFill
                                } else {
                                    pathExtend.backgroundAndFill = line!!.getBackgroundAndFill()
                                }
                            } else {
                                pathExtend.setLine(line)
                            }

                            autoShape.appendPath(pathExtend)
                        }
                    }

                    val endArrowType = shape.getEndArrowType()
                    if (endArrowType > 0) {
                        val arrowPathAndTail: ArrowPathAndTail? = shape.getEndArrowPath(rect)
                        if (arrowPathAndTail != null && arrowPathAndTail.arrowPath != null) {
                            endArrowTailCenter = arrowPathAndTail.arrowTailCenter
                            val pathExtend = ExtendPath()
                            pathExtend.path = arrowPathAndTail.arrowPath

                            pathExtend.setArrowFlag(true)
                            if (endArrowType != Arrow.Arrow_Arrow.toInt()) {
                                if ((line == null || line.getBackgroundAndFill() == null) && poiShape.getLineColor() != null) {
                                    val arrowFill = BackgroundAndFill()
                                    arrowFill.setFillType(BackgroundAndFill.FILL_SOLID)
                                    arrowFill.setForegroundColor(poiShape.getLineColor().getRGB())
                                    pathExtend.backgroundAndFill = arrowFill
                                } else {
                                    pathExtend.backgroundAndFill = line!!.getBackgroundAndFill()
                                }
                            } else {
                                pathExtend.setLine(line)
                            }

                            autoShape.appendPath(pathExtend)
                        }
                    }

                    val paths: Array<Path> = shape.getFreeformPath(rect,
                        startArrowTailCenter, startArrowType.toByte(), endArrowTailCenter, endArrowType.toByte())
                    for (i in paths.indices) {
                        val pathExtend = ExtendPath()
                        pathExtend.path = paths[i]
                        if (line != null) {
                            pathExtend.setLine(line)
                        }
                        if (fill != null) {
                            pathExtend.backgroundAndFill = fill
                        }
                        autoShape.appendPath(pathExtend)
                    }
                } else {
                    processTextbox(shape.getSpContainer(),
                        autoShape,
                        poiDoc!!.getMainTextboxRange().getSection(0))
                }

                if (parent == null) {
                    // wrap
                    if (drawing.getWrap() == 3 && !drawing.isAnchorLock()) {
                        if (drawing.isBelowText()) {
                            autoShape.setWrap(WPAutoShape.WRAP_BOTTOM)
                        } else {
                            autoShape.setWrap(WPAutoShape.WRAP_TOP)
                            fill = autoShape.getBackgroundAndFill()
                        }
                    } else {
                        autoShape.setWrap(WPAutoShape.WRAP_OLE)
                    }
                    AttrManage.instance().setShapeID(leaf.getAttribute(), control!!.getSysKit().getWPShapeManage().addShape(autoShape))
                    return true
                } else {
                    parent.appendShapes(autoShape)
                    return false
                }
            }
        } else if (poiShape is HWPFShapeGroup) {
            val poiGroup = poiShape

            val shape: AbstractShape
            val groupShape = WPGroupShape()
            if (parent == null) {
                shape = WPAutoShape()
                shape.addGroupShape(groupShape)
            } else {
                shape = groupShape
            }

            val zoom: FloatArray = poiGroup.getShapeAnchorFit(rect, zoomX, zoomY)
            rect = processGrpSpRect(parent, rect)
            val childRect: Rectangle = poiGroup.getCoordinates(zoom[0] * zoomX, zoom[1] * zoomY)
            groupShape.setOffPostion(rect.x - childRect.x, rect.y - childRect.y)

            groupShape.setBounds(rect)
            groupShape.setParent(parent)
            groupShape.setRotation(poiGroup.getGroupRotation().toFloat())
            groupShape.setFlipHorizontal(poiGroup.getFlipHorizontal())
            groupShape.setFlipVertical(poiGroup.getFlipVertical())

            val shapes: Array<HWPFShape>? = poiGroup.getShapes()
            if (shapes != null) {
                for (i in shapes.indices) {
                    convertShape(leaf, drawing, groupShape, shapes[i], shapes[i].getAnchor(rect, zoom[0] * zoomX, zoom[1] * zoomY), zoom[0] * zoomX, zoom[1] * zoomY)
                }
            }

            if (parent == null) {
                // wrap
                if (drawing.getWrap() == 3 && !drawing.isAnchorLock()) {
                    (shape as WPAutoShape).setWrap(WPAutoShape.WRAP_TOP)
                } else {
                    (shape as WPAutoShape).setWrap(WPAutoShape.WRAP_OLE)
                }
                AttrManage.instance().setShapeID(leaf.getAttribute(), control!!.getSysKit().getWPShapeManage().addShape(shape))
            } else {
                shape.setParent(parent)
                parent.appendShapes(shape)
            }
            return true
        }
        return false
    }

    /**
     * textbox id based on 1
     * @return
     */
    private fun getTextboxId(escherContainer: EscherContainerRecord): Short {
        val escherTextboxRecord: EscherTextboxRecord? = escherContainer
            .getChildById<EscherTextboxRecord>(EscherTextboxRecord.RECORD_ID)
        if (escherTextboxRecord != null) {
            val data: ByteArray? = escherTextboxRecord.getData()
            if (data != null && data.size == 4) {
                return LittleEndian.getShort(data, 2)
            }
        }

        return -1
    }

    /**
     *
     * @param autoShape
     * @param section
     * @param startPara start with startPara-th paragraph
     * @param endPara not contain end para
     */
    private fun processTextbox(escherContainer: EscherContainerRecord, autoShape: WPAutoShape, section: Section?) {
        if (section == null) {
            return
        }

        //based on 1
        val txbx = getTextboxId(escherContainer) - 1
        if (txbx >= 0) {
            processSimpleTextBox(escherContainer, autoShape, section)
        } else {
            processWordArtTextbox(escherContainer, autoShape)
        }
    }

    private fun processSimpleTextBox(escherContainer: EscherContainerRecord, autoShape: WPAutoShape, section: Section) {
        val txbx = getTextboxId(escherContainer) - 1

        val startCP = poiDoc!!.getTextboxStart(txbx)
        val endCP = poiDoc!!.getTextboxEnd(txbx)

        val oldOffset = offset
        offset = WPModelConstant.TEXTBOX + (textboxIndex shl 32)
        autoShape.setElementIndex(textboxIndex.toInt())
        val textboxElement = SectionElement()
        textboxElement.setStartOffset(offset)
        wpdoc!!.appendElement(textboxElement, offset)

        // 属性
        val attr: IAttributeSet = textboxElement.getAttribute()
        // 宽度 default a4 paper
        AttrManage.instance().setPageWidth(attr, (autoShape.getBounds().width * MainConstant.PIXEL_TO_TWIPS).toInt())
        // 高度 default a4 paper
        AttrManage.instance().setPageHeight(attr, (autoShape.getBounds().height * MainConstant.PIXEL_TO_TWIPS).toInt())

        //网格类型，高度
        if (section.getGridType() != PageAttr.GRIDTYPE_NONE.toInt()) {
            AttrManage.instance().setPageLinePitch(attr, section.getLinePitch())
        }

        // 上边距
        AttrManage.instance().setPageMarginTop(attr, (ShapeKit.getTextboxMarginTop(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())
        // 下边距
        AttrManage.instance().setPageMarginBottom(attr, (ShapeKit.getTextboxMarginBottom(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())
        // 左边距
        AttrManage.instance().setPageMarginLeft(attr, (ShapeKit.getTextboxMarginLeft(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())
        // 右边距
        AttrManage.instance().setPageMarginRight(attr, (ShapeKit.getTextboxMarginRight(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())

        AttrManage.instance().setPageVerticalAlign(attr, WPAttrConstant.PAGE_V_TOP)

        autoShape.setTextWrapLine(ShapeKit.isTextboxWrapLine(escherContainer))

        // 开始Offset
        textboxElement.setStartOffset(offset)
        //
        val paraCount = section.numParagraphs()
        var charOffset = 0
        var i = 0
        while (i < paraCount && !abortReader) {
            val para = section.getParagraph(i)
            charOffset += para.text().length
            if (charOffset > startCP && charOffset <= endCP) {
                if (para.isInTable()) {
                    val table = section.getTable(para)
                    processTable(table)
                    i += table.numParagraphs() - 1
                    i++
                    continue
                }
                processParagraph(section.getParagraph(i))
            }
            i++
        }
        autoShape.setElementIndex(textboxIndex.toInt())

        // 结束Offset
        textboxElement.setEndOffset(offset)
        textboxIndex++
        offset = oldOffset
    }

    private fun processWordArtTextbox(escherContainer: EscherContainerRecord, autoShape: WPAutoShape) {
        val text: String? = ShapeKit.getUnicodeGeoText(escherContainer)
        if (text != null && text.length > 0) {
            val oldOffset = offset
            offset = WPModelConstant.TEXTBOX + (textboxIndex shl 32)
            autoShape.setElementIndex(textboxIndex.toInt())
            val textboxElement = SectionElement()
            textboxElement.setStartOffset(offset)
            wpdoc!!.appendElement(textboxElement, offset)

            // 属性
            val attr: IAttributeSet = textboxElement.getAttribute()
            // 宽度 default a4 paper
            AttrManage.instance().setPageWidth(attr, (autoShape.getBounds().width * MainConstant.PIXEL_TO_TWIPS).toInt())
            // 高度 default a4 paper
            AttrManage.instance().setPageHeight(attr, (autoShape.getBounds().height * MainConstant.PIXEL_TO_TWIPS).toInt())

            // 上边距
            AttrManage.instance().setPageMarginTop(attr, (ShapeKit.getTextboxMarginTop(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())
            // 下边距
            AttrManage.instance().setPageMarginBottom(attr, (ShapeKit.getTextboxMarginBottom(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())
            // 左边距
            AttrManage.instance().setPageMarginLeft(attr, (ShapeKit.getTextboxMarginLeft(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())
            // 右边距
            AttrManage.instance().setPageMarginRight(attr, (ShapeKit.getTextboxMarginRight(escherContainer) * MainConstant.PIXEL_TO_TWIPS).toInt())

            AttrManage.instance().setPageVerticalAlign(attr, WPAttrConstant.PAGE_V_TOP)

            autoShape.setTextWrapLine(ShapeKit.isTextboxWrapLine(escherContainer))

            val width = (autoShape.getBounds().width - ShapeKit.getTextboxMarginLeft(escherContainer) - ShapeKit.getTextboxMarginRight(escherContainer)).toInt()
            val height = (autoShape.getBounds().height - ShapeKit.getTextboxMarginTop(escherContainer) - ShapeKit.getTextboxMarginBottom(escherContainer)).toInt()
            var fontsize = 12
            val paint: Paint = PaintKit.instance().getPaint()
            paint.setTextSize(fontsize.toFloat())
            var fm: Paint.FontMetrics = paint.getFontMetrics()
            while (paint.measureText(text).toInt() < width && Math.ceil((fm.descent - fm.ascent).toDouble()).toInt() < height) {
                paint.setTextSize((++fontsize).toFloat())
                fm = paint.getFontMetrics()
            }

            // 开始Offset
            textboxElement.setStartOffset(offset)
            //
            val paraElem = ParagraphElement()
            // 开始 offset
            paraElem.setStartOffset(offset)
            val before = docRealOffset

            val leaf = LeafElement(text)
            // 属性
            val leafAttr: IAttributeSet = leaf.getAttribute()
            // 字号
            AttrManage.instance().setFontSize(leafAttr, ((fontsize - 1) * MainConstant.PIXEL_TO_POINT).toInt())
            // 字符颜色
            val color: com.wxiwei.office.java.awt.Color? = ShapeKit.getForegroundColor(escherContainer, null, MainConstant.APPLICATION_TYPE_PPT.toInt())
            if (color != null) {
                AttrManage.instance().setFontColor(leafAttr, color.getRGB())
            }

            // 开始 offset
            leaf.setStartOffset(offset)
            offset += text.length.toLong()
            // 结束 offset
            leaf.setEndOffset(offset)
            paraElem.appendLeaf(leaf)

            paraElem.setEndOffset(offset)
            wpdoc!!.appendParagraph(paraElem, offset)
            //
            adjustBookmarkOffset(before, docRealOffset)

            autoShape.setElementIndex(textboxIndex.toInt())

            // 结束Offset
            textboxElement.setEndOffset(offset)
            textboxIndex++
            offset = oldOffset
        }
    }

    private fun processPicturePosition(drawing: OfficeDrawing, autoShape: WPAutoShape) {
        //horizontal alignment
        when (drawing.getHorizontalPositioning()) {
            HWPFShape.POSH_ABS -> autoShape.setHorPositionType(WPAbstractShape.POSITIONTYPE_ABSOLUTE)
            HWPFShape.POSH_LEFT -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_LEFT)
            HWPFShape.POSH_CENTER -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_CENTER)
            HWPFShape.POSH_RIGHT -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_RIGHT)
            HWPFShape.POSH_INSIDE -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_INSIDE)
            HWPFShape.POSH_OUTSIDE -> autoShape.setHorizontalAlignment(WPAbstractShape.ALIGNMENT_OUTSIDE)
        }

        //relative to in horizontal
        when (drawing.getHorizontalRelative()) {
            HWPFShape.POSRELH_MARGIN -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_MARGIN)
            HWPFShape.POSRELH_PAGE -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_PAGE)
            HWPFShape.POSRELH_COLUMN -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_COLUMN)
            HWPFShape.POSRELH_CHAR -> autoShape.setHorizontalRelativeTo(WPAbstractShape.RELATIVE_CHARACTER)
        }

        //alignment in vertical
        when (drawing.getVerticalPositioning()) {
            HWPFShape.POSV_ABS -> autoShape.setVerPositionType(WPAbstractShape.POSITIONTYPE_ABSOLUTE)
            HWPFShape.POSV_TOP -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_TOP)
            HWPFShape.POSV_CENTER -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_CENTER)
            HWPFShape.POSV_BOTTOM -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_BOTTOM)
            HWPFShape.POSV_INSIDE -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_INSIDE)
            HWPFShape.POSV_OUTSIDE -> autoShape.setVerticalAlignment(WPAbstractShape.ALIGNMENT_OUTSIDE)
        }

        //relative to in vertical
        when (drawing.getVerticalRelativeElement()) {
            HWPFShape.POSRELV_MARGIN -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_MARGIN)
            HWPFShape.POSRELV_PAGE -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_PAGE)
            HWPFShape.POSRELV_TEXT -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_PARAGRAPH)
            HWPFShape.POSRELV_LINE -> autoShape.setVerticalRelativeTo(WPAbstractShape.RELATIVE_LINE)
        }
    }

    /**
     *
     */
    private fun processShape(run: CharacterRun, leaf: IElement, isWrap: Boolean, runIndex: Int): Boolean {
        if (isWrap) {
            val drawings: OfficeDrawings = poiDoc!!.getOfficeDrawingsMain()
            val drawing: OfficeDrawing = drawings.getOfficeDrawingAt(run.getStartOffset().toInt() + runIndex) ?: return false

            val rect = Rectangle()
            rect.x = (drawing.getRectangleLeft() * MainConstant.TWIPS_TO_PIXEL).toInt()
            rect.y = (drawing.getRectangleTop() * MainConstant.TWIPS_TO_PIXEL).toInt()
            rect.width = ((drawing.getRectangleRight() - drawing.getRectangleLeft()) * MainConstant.TWIPS_TO_PIXEL).toInt()
            rect.height = ((drawing.getRectangleBottom() - drawing.getRectangleTop()) * MainConstant.TWIPS_TO_PIXEL).toInt()

            val b = drawing.getPictureData(control)
            if (b != null) {
                if (isSupportPicture(PictureType.findMatchingType(b))) {
                    val picShape = PictureShape()

                    var index = control!!.getSysKit().getPictureManage().getPictureIndex(drawing.getTempFilePath(control))
                    if (index < 0) {
                        val picture = Picture()
                        // 图片数据
                        picture.setTempFilePath(drawing.getTempFilePath(control))
                        // 图片类型
                        picture.setPictureType(PictureType.findMatchingType(b).getExtension())
                        index = control!!.getSysKit().getPictureManage().addPicture(picture)
                    }
                    picShape.setPictureIndex(index)
                    picShape.setBounds(rect)
                    picShape.setZoomX(1000.toShort())
                    picShape.setZoomY(1000.toShort())
                    picShape.setPictureEffectInfor(drawing.getPictureEffectInfor())
                    val wpPictureShape = WPPictureShape()
                    wpPictureShape.setPictureShape(picShape)

                    // wrap
                    if (drawing.getWrap() == 3 && !drawing.isAnchorLock()) {
                        if (drawing.isBelowText()) {
                            wpPictureShape.setWrap(WPAutoShape.WRAP_BOTTOM)
                        } else {
                            wpPictureShape.setWrap(WPAutoShape.WRAP_TOP)
                        }

                        processPicturePosition(drawing, wpPictureShape)
                    } else {
                        wpPictureShape.setWrap(WPAutoShape.WRAP_OLE)
                    }

                    AttrManage.instance().setShapeID(leaf.getAttribute(), control!!.getSysKit().getWPShapeManage().addShape(wpPictureShape))
                    return true
                }
            } else {
                val poiShape: HWPFShape? = drawing.getAutoShape()
                if (poiShape != null) {
                    return convertShape(leaf, drawing, null, poiShape, rect, 1.0f, 1.0f)
                }
            }
        } else {
            // Picture
            val pictureTable: PicturesTable = poiDoc!!.getPicturesTable()
            val pic: com.wxiwei.office.fc.hwpf.usermodel.Picture? = pictureTable.extractPicture(control!!.getSysKit().getPictureManage().getPicTempPath(),
                run, false)

            if (pic != null && isSupportPicture(pic.suggestPictureType())) {
                val picShape = PictureShape()

                var index = control!!.getSysKit().getPictureManage().getPictureIndex(pic.getTempFilePath())
                if (index < 0) {
                    val picture = Picture()
                    // 图片数据
                    picture.setTempFilePath(pic.getTempFilePath())
                    // 图片类型
                    picture.setPictureType(pic.suggestPictureType().getExtension())
                    index = control!!.getSysKit().getPictureManage().addPicture(picture)
                }
                picShape.setPictureIndex(index)

                val rect = Rectangle()
                rect.width = (pic.getDxaGoal() * MainConstant.TWIPS_TO_PIXEL * pic.getHorizontalScalingFactor() / 1000f).toInt()
                rect.height = (pic.getDyaGoal() * MainConstant.TWIPS_TO_PIXEL * pic.getVerticalScalingFactor() / 1000f).toInt()
                picShape.setBounds(rect)

                picShape.setZoomX(pic.getZoomX())
                picShape.setZoomY(pic.getZoomY())
                picShape.setPictureEffectInfor(PictureEffectInfoFactory.getPictureEffectInfor(pic))

                val wpPictureShape = WPPictureShape()
                wpPictureShape.setPictureShape(picShape)

                wpPictureShape.setWrap(WPAutoShape.WRAP_OLE)

                AttrManage.instance().setShapeID(leaf.getAttribute(), control!!.getSysKit().getWPShapeManage().addShape(wpPictureShape))
                return true
            } else {
                //inline word art
                val inlineShape: InlineWordArt? = pictureTable.extracInlineWordArt(run)
                if (inlineShape != null && inlineShape.getInlineWordArt() != null) {
                    val autoShape = WPAutoShape()

                    val rect = Rectangle()
                    rect.width = (inlineShape.getDxaGoal() * MainConstant.TWIPS_TO_PIXEL * inlineShape.getHorizontalScalingFactor() / 1000f).toInt()
                    rect.height = (inlineShape.getDyaGoal() * MainConstant.TWIPS_TO_PIXEL * inlineShape.getVerticalScalingFactor() / 1000f).toInt()
                    autoShape.setBounds(rect)
                    autoShape.setWrap(WPAutoShape.WRAP_OLE)

                    processWordArtTextbox(inlineShape.getInlineWordArt().getSpContainer(), autoShape)

                    AttrManage.instance().setShapeID(leaf.getAttribute(), control!!.getSysKit().getWPShapeManage().addShape(autoShape))

                    return true
                }
            }
        }
        return false
    }

    /**
     *
     */
    private fun isSupportPicture(picType: PictureType): Boolean {
        val mineType = picType.getExtension()
        return mineType.equals("gif", ignoreCase = true)
            || mineType.equals("jpeg", ignoreCase = true)
            || mineType.equals("jpg", ignoreCase = true)
            || mineType.equals("bmp", ignoreCase = true)
            || mineType.equals("png", ignoreCase = true)
            || mineType.equals("wmf", ignoreCase = true)
            || mineType.equals("emf", ignoreCase = true)
    }

    /**
     *
     * @param file
     * @param key
     * @return
     */
    @Throws(Exception::class)
    override fun searchContent(file: File?, key: String): Boolean {
        var isContain = false
        val poiDoc = HWPFDocument(FileInputStream(file))
        val range = poiDoc.getRange()
        val sb = StringBuilder()

        for (i in 0 until range.numSections()) {
            val section = range.getSection(i)
            for (j in 0 until section.numParagraphs()) {
                val para = section.getParagraph(j)
                for (k in 0 until para.numCharacterRuns()) {
                    sb.append(para.getCharacterRun(k).text())
                }
                if (sb.indexOf(key) >= 0) {
                    isContain = true
                    break
                }
                sb.delete(0, sb.length)
            }
        }
        return isContain
    }

    /**
     *
     */
    private fun adjustBookmarkOffset(before: Long, after: Long) {
        for (bm in bms!!) {
            if (bm.getStart() >= before && bm.getStart() <= after) {
                bm.setStart(offset)
            }
        }
    }

    /**
     *
     */
    override fun dispose() {
        if (isReaderFinish()) {
            wpdoc = null
            filePath = null
            poiDoc = null
            control = null
            hyperlinkAddress = null
            //controlForReader = null;
            if (bms != null) {
                bms!!.clear()
                bms = null
            }
        }
    }

    //
    private var isBreakChar = false
    //
    private var offset: Long = 0
    //
    private var textboxIndex: Long = 0
    //
    private var docRealOffset: Long = 0
    //
    private var filePath: String? = filePath
    //
    private var wpdoc: WPDocument? = null
    //
    private var poiDoc: HWPFDocument? = null
    //
    private val hyperlinkPattern: Pattern = Pattern.compile("[ \\t\\r\\n]*HYPERLINK \"(.*)\"[ \\t\\r\\n]*")
    //
    private var hyperlinkAddress: String? = null
    //
    private var bms: MutableList<Bookmark>? = ArrayList()
}
