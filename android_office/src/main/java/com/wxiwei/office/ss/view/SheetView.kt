/*
 * 文件名称:          SheetView.java
 *
 * 编译器:            android2.2
 * 时间:              下午2:37:48
 */
package com.wxiwei.office.ss.view

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.Rect
import android.graphics.RectF
import com.wxiwei.office.common.PaintKit
import com.wxiwei.office.common.picture.PictureKit
import com.wxiwei.office.constant.EventConstant
import com.wxiwei.office.constant.MainConstant
import com.wxiwei.office.constant.SSConstant
import com.wxiwei.office.constant.wp.WPAttrConstant
import com.wxiwei.office.constant.wp.WPViewConstant
import com.wxiwei.office.simpletext.font.FontKit
import com.wxiwei.office.simpletext.model.AttrManage
import com.wxiwei.office.simpletext.model.IDocument
import com.wxiwei.office.simpletext.model.STDocument
import com.wxiwei.office.simpletext.model.SectionElement
import com.wxiwei.office.simpletext.view.STRoot
import com.wxiwei.office.ss.control.Spreadsheet
import com.wxiwei.office.ss.model.CellRangeAddress
import com.wxiwei.office.ss.model.baseModel.Cell
import com.wxiwei.office.ss.model.baseModel.Row
import com.wxiwei.office.ss.model.baseModel.Sheet
import com.wxiwei.office.ss.model.sheetProperty.ColumnInfo
import com.wxiwei.office.ss.model.style.CellStyle
import com.wxiwei.office.ss.model.table.SSTableCellStyle
import com.wxiwei.office.ss.other.DrawingCell
import com.wxiwei.office.ss.other.ExpandedCellRangeAddress
import com.wxiwei.office.ss.other.FindingMgr
import com.wxiwei.office.ss.other.FocusCell
import com.wxiwei.office.ss.other.SheetScroller
import com.wxiwei.office.ss.util.ModelUtil
import com.wxiwei.office.system.OpenTrace
import java.util.function.Consumer
import kotlin.math.abs

/**
 * 负责sheet表绘制和布局计算
 *
 * Read版本:        Read V1.0
 *
 * 作者:            ljj8494
 *
 * 日期:            2011-11-8
 *
 * 负责人:          ljj8494
 *
 * 负责小组:
 */
class SheetView(spreadsheet: Spreadsheet?, sheet: Sheet?) {
    // 当前显示Sheet
    private var sheet: Sheet? = null

    //
    private var rowHeader: RowHeader? = null

    //
    private var columnHeader: ColumnHeader? = null

    //
    private var spreadsheet: Spreadsheet? = null

    //current zoom rate
    private var zoom = 1f

    //scroll postion in horizental and vertical based on zoom = 1
    private var clipRect: Rect? = null
    private var scrollX = 0f
    private var scrollY = 0f
    private var lastScrollerX = Int.MIN_VALUE
    private var lastScrollerY = Int.MIN_VALUE

    //
    private var shapeView: ShapeView? = null

    private var tableFormatView: TableFormatView? = null

    //
    private var cellView: CellView? = null

    //current min row and column information of current sheet
    private var sheetScroller: SheetScroller? = SheetScroller()

    //single cell information of current drawing
    private var cellInfor: DrawingCell? = null

    private var selecetedCellsRange: CellRangeAddress? = null

    //only draw it when changing header height or width
    private var isDrawMovingHeaderLine = false

    //row header, column header
    private var selectedHeaderInfor: FocusCell? = null

    //used to draw dotted line
    private var effects: PathEffect? = DashPathEffect(floatArrayOf(5f, 5f, 5f, 5f), 1f)

    //find data you're searching for
    private var findingMgr: FindingMgr? = null

    private var extendCell: MutableList<ExtendCell>? = ArrayList()
    private var drawTraceLogged = false
    private var firstVisibleValueTraceLogged = false

    /**
     *
     * @param sheet model中值
     */
    init {
        this.spreadsheet = spreadsheet
        this.sheet = sheet
        rowHeader = RowHeader(this)
        columnHeader = ColumnHeader(this)

        shapeView = ShapeView(this)
        tableFormatView = TableFormatView(this)
        cellView = CellView(this)

        selecetedCellsRange = CellRangeAddress(0, 0, 0, 0)
        cellInfor = DrawingCell()

        initForDrawing()
    }

    /**
     *
     */
    private fun initForDrawing() {
        val sheet = this.sheet!!
        scrollX = sheet.getScrollX().toFloat()
        scrollY = sheet.getScrollY().toFloat()
        updateScroller(sheet, Math.round(scrollX), Math.round(scrollY), true)

        // A number of Excel files contain formatting/empty rows before the
        // actual table.  The grid renderer normally starts at A1, which makes
        // those files look blank.  Position the initial viewport at the first
        // cell that has a real value; A1 still remains the origin when it is
        // populated.
        if (scrollX == 0f && scrollY == 0f) {
            scrollToFirstDataCell(sheet)
        }

        setZoom(sheet.getZoom(), true)

        //selected cell
        selectedCell(sheet.getActiveCellRow(), sheet.getActiveCellColumn())

        spreadsheet!!.getControl().actionEvent(EventConstant.APP_CONTENT_SELECTED, cellInfor)
    }

    fun positionAtFirstDataCell() {
        scrollToFirstDataCell(sheet!!)
    }

    private fun scrollToFirstDataCell(sheet: Sheet) {
        var firstCell: Cell? = null
        val firstRow = sheet.getFirstRowNum()
        val lastRow = sheet.getLastRowNum()

        for (rowIndex in firstRow..lastRow) {
            val row = sheet.getRow(rowIndex) ?: continue
            for (cell in row.cellCollection()) {
                if (!cell.hasValidValue() || cell.getCellType() == Cell.CELL_TYPE_BLANK) continue
                if (firstCell == null || cell.getColNumber() < firstCell!!.getColNumber()) {
                    firstCell = cell
                }
            }
            if (firstCell != null) break
        }

        val cell = firstCell ?: return
        val anchor = ModelUtil.instance().getCellAnchor(sheet, cell.getRowNumber(), cell.getColNumber())
        val targetX = (anchor.left - SSConstant.DEFAULT_ROW_HEADER_WIDTH).coerceAtLeast(0)
        val targetY = (anchor.top - SSConstant.DEFAULT_COLUMN_HEADER_HEIGHT).coerceAtLeast(0)

        OpenTrace.d(
            "excel.first-data sheet=${sheet.getSheetName()} row=${cell.getRowNumber()} " +
                "col=${cell.getColNumber()} type=${cell.getCellType()} anchor=${anchor.left},${anchor.top} " +
                "target=$targetX,$targetY"
        )

        scrollX = targetX.toFloat()
        scrollY = targetY.toFloat()
        sheet.setScroll(targetX, targetY)
        updateScroller(sheet, targetX, targetY, true)
    }

    private fun resizeCalloutView() {
        val spreadsheet = this.spreadsheet!!
        if (spreadsheet.getCalloutView() != null) {
            spreadsheet.getCalloutView()!!.setZoom(zoom)

            val left = (scrollX * zoom).toInt()
            val top = (scrollY * zoom).toInt()

            spreadsheet.getCalloutView()!!.layout(
                getRowHeaderWidth() - left,
                getColumnHeaderHeight() - top,
                spreadsheet.getCalloutView()!!.right,
                spreadsheet.getCalloutView()!!.bottom
            )

            spreadsheet.getCalloutView()!!.setClip(left, top)
        }
    }

    /**
     * 改变显示的sheet
     *
     * @param sheet 需要显示的sheet
     */
    fun changeSheet(sheet: Sheet?) {
        synchronized(this) {
            this.sheet!!.removeSTRoot()
            this.sheet = sheet
            drawTraceLogged = false
            firstVisibleValueTraceLogged = false

            initForDrawing()
            resizeCalloutView()

            // to picture
            // TODO(coroutine): posts a UI-thread callback that fires APP_GENERATED_PICTURE_ID after a sheet switch; suggested Dispatchers.Main
            spreadsheet!!.post(object : Runnable {
                override fun run() {
                    spreadsheet!!.getControl().actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null)
                }
            })
        }
    }

    /**
     * get sheet thumbnail
     * @param sheet         sheet
     * @param width         thumbnail width when zoomValue is 100
     * @param height        thumbnail height when zoomValue is 100
     * @param zoomValue     zoom value
     * @return
     */
    fun getThumbnail(sheet: Sheet, width: Int, height: Int, zoomValue: Float): Bitmap? {
        synchronized(this) {
            val bitmap = Bitmap.createBitmap((width * zoomValue).toInt(), (height * zoomValue).toInt(), Config.ARGB_8888)
            if (bitmap == null) {
                return null
            }

            val b = PictureKit.instance().isDrawPictrue()
            PictureKit.instance().setDrawPictrue(true)

            val picCanvas = Canvas(bitmap)
            picCanvas.drawColor(Color.WHITE)

            //save sheet(not current sheet) info
            val oldScrollX = sheet.getScrollX()
            val oldScrollY = sheet.getScrollY()
            val oldZoom = sheet.getZoom()

            val oldSheet = this.sheet!!

            //set specific sheet thumbnail info
            this.sheet = sheet
            scrollX = 0f
            scrollY = 0f
            sheet.setScroll(0, 0)
            setZoom(zoomValue, true)

            updateScroller(sheet, Math.round(scrollX), Math.round(scrollY), true)

            drawThumbnail(picCanvas)

            //restore specific sheet info
            sheet.setScroll(oldScrollX, oldScrollY)
            sheet.setZoom(oldZoom)

            //restore to current sheet
            this.sheet = oldSheet
            scrollX = oldSheet.getScrollX().toFloat()
            scrollY = oldSheet.getScrollY().toFloat()
            setZoom(oldSheet.getZoom(), true)

            updateScroller(sheet, Math.round(scrollX), Math.round(scrollY))

            PictureKit.instance().setDrawPictrue(b)
            return bitmap
        }
    }

    private fun drawThumbnail(canvas: Canvas) {
        val rowHeader = this.rowHeader!!
        val columnHeader = this.columnHeader!!
        spreadsheet!!.startDrawing()

        clipRect = canvas.clipBounds
        val clipRect = this.clipRect!!
        val colRightBound = columnHeader.getColumnRightBound(canvas, zoom)
        val rowBottomBound = rowHeader.getRowBottomBound(canvas, zoom)

        var rightPos = clipRect.right + 10
        if (colRightBound < clipRect.right) {
            rightPos = colRightBound
        }

        var bottomPos = clipRect.bottom + 50
        if (rowBottomBound < clipRect.bottom) {
            bottomPos = rowBottomBound
        }

        //draw rowheader and columnheader
        rowHeader.draw(canvas, rightPos, zoom)
        columnHeader.draw(canvas, bottomPos, zoom)

        val rowWidth = rowHeader.getRowHeaderWidth().toFloat()
        val columnHeight = columnHeader.getColumnHeaderHeight().toFloat()
        canvas.save()
        canvas.clipRect(rowWidth, columnHeight, rightPos.toFloat(), bottomPos.toFloat())

        //draw cell
        drawRows(canvas)

        //table format
        tableFormatView!!.draw(canvas)

        //draw shape(textbox, pict, chart)
        shapeView!!.draw(canvas)

        canvas.restore()
    }

    fun getMaxScrollY(): Int {
        return Math.round(sheet!!.getMaxScrollY() * zoom)
    }

    fun getMaxScrollX(): Int {
        return Math.round(sheet!!.getMaxScrollX() * zoom)
    }

    /**
     *
     * @param canvas
     */
    fun drawSheet(canvas: Canvas) {
        synchronized(this) {
            val rowHeader = this.rowHeader!!
            val columnHeader = this.columnHeader!!
            //reset drawing flag
            spreadsheet!!.startDrawing()

            clipRect = canvas.clipBounds
            val clipRect = this.clipRect!!
            val colRightBound = columnHeader.getColumnRightBound(canvas, zoom)
            val rowBottomBound = rowHeader.getRowBottomBound(canvas, zoom)

            var rightPos = clipRect.right + 10
            if (colRightBound < clipRect.right) {
                rightPos = colRightBound
            }

            var bottomPos = clipRect.bottom + 50
            if (rowBottomBound < clipRect.bottom) {
                bottomPos = rowBottomBound
            }

            //draw rowheader and columnheader
            rowHeader.draw(canvas, rightPos, zoom)
            columnHeader.draw(canvas, bottomPos, zoom)

            val rowWidth = rowHeader.getRowHeaderWidth().toFloat()
            val columnHeight = columnHeader.getColumnHeaderHeight().toFloat()
            canvas.save()
            canvas.clipRect(rowWidth, columnHeight, rightPos.toFloat(), bottomPos.toFloat())

            //draw cell
            drawRows(canvas)

            //table format
            tableFormatView!!.draw(canvas)

            //draw active cell border
            drawActiveCellBorder(canvas)

            //if(!moving)
            run {
                //draw shape(textbox, pict, chart)
                shapeView!!.draw(canvas)
            }

            //draw moving header line when changing header height or width
            drawMovingHeaderLine(canvas)
            canvas.restore()
        }
    }

    /**
     *
     * @param canvas
     */
    private fun drawActiveCellBorder(canvas: Canvas) {
        val sheet = this.sheet!!
        val area = ModelUtil.instance().getCellAnchor(
            this,
            sheet.getActiveCellRow(), sheet.getActiveCellColumn()
        )

        cellView!!.drawActiveCellBorder(canvas, area, sheet.getActiveCellType())
    }

    /**
     * draw moving header line when changing header height or width
     * @param canvas
     */
    private fun drawMovingHeaderLine(canvas: Canvas) {
        val selectedHeaderInfor = this.selectedHeaderInfor
        if (isDrawMovingHeaderLine && selectedHeaderInfor != null) {
            val paint = PaintKit.instance().getPaint()
            //save paint property
            val oldColor = paint.color
            val oldPathEffect = paint.pathEffect
            val clipRect = canvas.clipBounds

            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            val path = Path()
            if (selectedHeaderInfor.getType() == FocusCell.ROWHEADER.toInt()) {
                //Rect rect = ModelUtil.instance().getCellAnchor(this, selectedHeaderInfor.getRow(), 0);
                path.moveTo(0f, selectedHeaderInfor.getRect()!!.bottom.toFloat())
                path.lineTo(clipRect.right.toFloat(), selectedHeaderInfor.getRect()!!.bottom.toFloat())
            } else if (selectedHeaderInfor.getType() == FocusCell.COLUMNHEADER.toInt()) {
                path.moveTo(selectedHeaderInfor.getRect()!!.right.toFloat(), 0f)
                path.lineTo(selectedHeaderInfor.getRect()!!.right.toFloat(), clipRect.bottom.toFloat())
            }

            paint.pathEffect = effects
            canvas.drawPath(path, paint)

            //restore
            paint.pathEffect = oldPathEffect
            paint.style = Paint.Style.FILL
            paint.color = oldColor
        }
    }

    /**
     *
     * @param row
     * @param currentCol
     * @param textWidth
     * @return
     */
    private fun getExtendTextRightBound(row: Row, currentCol: Int, textWidth: Float): Int {
        val sheet = this.sheet!!
        var textWidth = textWidth
        var cell: Cell?
        var columnIndex = currentCol + 1
        while (textWidth > 0) {
            cell = row.getCell(columnIndex, false)

            var content: String? = null
            if (cell == null ||
                (cell.getRangeAddressIndex() < 0 &&
                    (ModelUtil.instance().getFormatContents(sheet.getWorkbook()!!, cell).also { content = it } == null || content!!.length == 0))
            ) {
                textWidth -= sheet.getColumnPixelWidth(columnIndex) * zoom
            } else {
                return columnIndex - 1
            }
            columnIndex++
        }

        return columnIndex - 1
    }

    /**
     *
     * @param row
     * @param currentCol
     * @param textWidth
     * @return
     */
    private fun getExtendTextLeftBound(row: Row, currentCol: Int, textWidth: Float): Int {
        val sheet = this.sheet!!
        var textWidth = textWidth
        var columnIndex = currentCol - 1
        var cell: Cell?
        while (columnIndex >= 0 && textWidth > 0) {
            cell = row.getCell(columnIndex, false)

            var content: String? = null
            if (cell == null ||
                (cell.getRangeAddressIndex() < 0 &&
                    (ModelUtil.instance().getFormatContents(sheet.getWorkbook()!!, cell).also { content = it } == null || content!!.length == 0))
            ) {
                textWidth -= sheet.getColumnPixelWidth(columnIndex) * zoom
            } else {
                return columnIndex + 1
            }
            columnIndex--
        }

        return columnIndex + 1
    }

    fun getIndentWidth(indent: Int): Int {
        val fontSize = sheet!!.getWorkbook()!!.getFont(0)!!.getFontSize()
        return Math.round(2 * fontSize * indent * MainConstant.POINT_TO_PIXEL).toInt()
    }

    fun getIndentWidthWithZoom(indent: Int): Int {
        val fontSize = sheet!!.getWorkbook()!!.getFont(0)!!.getFontSize()
        return Math.round(2 * fontSize * indent * MainConstant.POINT_TO_PIXEL * zoom).toInt()
    }

    /**
     *
     */
    private fun initRowExtendedRangeAddress(row: Row) {
        val sheet = this.sheet!!
        var colsWidth: Float
        val cells: Collection<Cell> = row.cellCollection()
        for (cell in cells) {
            colsWidth = sheet.getColumnPixelWidth(cell.getColNumber()) * zoom
            //
            if ((cell.getCellStyle() != null && cell.getCellStyle()!!.isWrapText()) //wrapText cell
                || cell.getCellType() == Cell.CELL_TYPE_BOOLEAN //not string cell
                || cell.getCellNumericType() == Cell.CELL_TYPE_NUMERIC_SIMPLEDATE
                || (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && cell.getCellNumericType() != Cell.CELL_TYPE_NUMERIC_STRING)
            ) {
                continue
            }

            var restWidth = 0f
            val style = cell.getCellStyle()
            val indent: Float = if (style != null) getIndentWidth(style.getIndent().toInt()).toFloat() else 0f

            if (CellView.isComplexText(cell)) {
                val rect = ModelUtil.instance().getCellAnchor(
                    sheet, cell.getRowNumber(), cell.getColNumber()
                )

                val elem = (cell.getSheet()!!.getWorkbook()!!.getSharedItem(cell.getStringCellValueIndex())) as SectionElement?
                if (elem == null || elem.getEndOffset() - elem.getStartOffset() == 0L) {
                    continue
                }

                val elemCollection = elem.getParaCollection() ?: continue
                val paraHorAlignList: MutableList<Int> = ArrayList(elemCollection.size())
                for (i in 0 until elemCollection.size()) {
                    val para = elemCollection.getElementForIndex(i) ?: continue
                    paraHorAlignList.add(AttrManage.instance().getParaHorizontalAlign(para.getAttribute()))
                    AttrManage.instance().setParaHorizontalAlign(para.getAttribute(), WPAttrConstant.PARA_HOR_ALIGN_LEFT.toInt())
                }

                val attr = elem.getAttribute()
                // 宽度
                AttrManage.instance().setPageWidth(attr, Math.round(Int.MAX_VALUE * MainConstant.PIXEL_TO_TWIPS))
                // 高度
                AttrManage.instance().setPageHeight(attr, Math.round(rect.height() * MainConstant.PIXEL_TO_TWIPS))

                val doc: IDocument = STDocument()
                doc.appendSection(elem)
                var root = STRoot(spreadsheet!!.getEditor(), doc)

                root.setWrapLine(false)
                root.doLayout()

                //just one lineview
                val paraView = root.getChildView() ?: continue
                val lineWidth = paraView.getLayoutSpan(WPViewConstant.X_AXIS)
                root.dispose()
                // 宽度
                AttrManage.instance().setPageWidth(attr, Math.round((lineWidth + indent + SSConstant.SHEET_SPACETOBORDER * 2) * MainConstant.PIXEL_TO_TWIPS))
                root = STRoot(spreadsheet!!.getEditor(), doc)
                root.doLayout()
//                view = root.getChildView();
//                if(view != null)
//                {
//                	switch (style.getHorizontalAlign())
//                    {
//                        case CellStyle.ALIGN_GENERAL:
//                        case CellStyle.ALIGN_LEFT:
//                        case CellStyle.ALIGN_FILL:
//                        case CellStyle.ALIGN_JUSTIFY:
//                        case CellStyle.ALIGN_CENTER_SELECTION:
//                        	view.setLeftIndent(Math.round(indent));
//                            break;
//                        case CellStyle.ALIGN_RIGHT:
//                        	view.setRightIndent(Math.round(indent));
//                            break;
//                    }
//
//                }
                for (i in 0 until elemCollection.size()) {
                    elemCollection.getElementForIndex(i)?.let { AttrManage.instance().setParaHorizontalAlign(it.getAttribute(), paraHorAlignList[i]) }
                }
                restWidth = ((lineWidth + indent + SSConstant.SHEET_SPACETOBORDER * 2) * zoom).toInt() - colsWidth

                cell.setSTRoot(root)
            } else if (cell.getRangeAddressIndex() < 0) {
                //simple and not merged cell
                val content = ModelUtil.instance().getFormatContents(sheet.getWorkbook()!!, cell)
                if (content == null || content.length == 0) {
                    continue
                }

                //save paint text size
                val paint = FontKit.instance().getCellPaint(cell, sheet.getWorkbook(), null)
                val textSize = paint.textSize

                paint.textSize = textSize * zoom
                restWidth = paint.measureText(content) + indent + SSConstant.SHEET_SPACETOBORDER - colsWidth

                //restore paint style
                paint.textSize = textSize
            }

            if (restWidth > 0 && cell.getRangeAddressIndex() < 0) {
                //ATOD
                var right = cell.getColNumber()
                var left = cell.getColNumber()

                when (style!!.getHorizontalAlign()) {
                    CellStyle.ALIGN_GENERAL,
                    CellStyle.ALIGN_LEFT,
                    CellStyle.ALIGN_FILL,
                    CellStyle.ALIGN_JUSTIFY,
                    CellStyle.ALIGN_CENTER_SELECTION -> right = getExtendTextRightBound(row, cell.getColNumber(), restWidth)

                    CellStyle.ALIGN_RIGHT -> left = getExtendTextLeftBound(row, cell.getColNumber(), restWidth)

                    CellStyle.ALIGN_CENTER -> {
                        left = getExtendTextLeftBound(row, cell.getColNumber(), restWidth / 2)
                        right = getExtendTextRightBound(row, cell.getColNumber(), restWidth / 2)
                    }

                    else -> {
                    }
                }
                if (left == right) {
                    continue
                }

                val rangeAddr = ExpandedCellRangeAddress(cell, row.getRowNumber(), left, row.getRowNumber(), right)
                val rangeAddrIndex = row.getExpandedCellCount()
                row.addExpandedRangeAddress(rangeAddrIndex, rangeAddr)
            }
        }

        //validate the Extended Cell Range
        val cnt = row.getExpandedCellCount()
        for (i in 0 until cnt) {
            val rangeAddr = row.getExpandedRangeAddress(i)!!
            var j = rangeAddr.getRangedAddress()!!.getFirstColumn()
            while (j <= rangeAddr.getRangedAddress()!!.getLastColumn()) {
                var cell = row.getCell(j)
                if (cell == null) {
                    cell = Cell(Cell.CELL_TYPE_BLANK)
                    cell.setColNumber(j)
                    cell.setRowNumber(row.getRowNumber())
                    cell.setSheet(sheet)
                    cell.setCellStyle(row.getRowStyle())

                    row.addCell(cell)
                }
                cell.setExpandedRangeAddressIndex(i)
                j++
            }
        }
    }

    /**
     *
     */
    private fun drawCells(canvas: Canvas, row: Row?) {
        var row = row
        val sheet = this.sheet!!
        val cellInfor = this.cellInfor!!
        val sheetScroller = this.sheetScroller!!
        val height = (if (row == null) sheet.getDefaultRowHeight().toFloat() else row.getRowPixelHeight())
        cellInfor.setHeight(height * zoom)
        if (cellInfor.getRowIndex() != sheetScroller.getMinRowIndex()
            || sheetScroller.isRowAllVisible()
        ) {
            cellInfor.setVisibleHeight(cellInfor.getHeight())
        } else {
            cellInfor.setVisibleHeight(sheetScroller.getVisibleRowHeight().toFloat() * zoom)
        }

        if (row == null && sheet.isAccomplished()) {
            row = sheet.getRowByColumnsStyle(cellInfor.getRowIndex())
        }

        if (row == null || (!sheet.isAccomplished() && !row.isCompleted())) {
            return
        }

        //reset when at row begin
        cellInfor.setLeft(rowHeader!!.getRowHeaderWidth().toFloat())
        cellInfor.setColumnIndex(sheetScroller.getMinColumnIndex())
        var iter: MutableIterator<ExtendCell> = extendCell!!.iterator()
        while (iter.hasNext()) {
            iter.next().dispose()
        }
        extendCell!!.clear()

        if (sheet.isAccomplished() && !row.isInitExpandedRangeAddress()) {
            initRowExtendedRangeAddress(row)
            row.setInitExpandedRangeAddress(true)
        }
        val clip = canvas.clipBounds
        val maxColumn = sheet.getWorkbook()!!.getMaxColumn()
        var columnInfo: ColumnInfo?
        var colWidth: Float
        while (cellInfor.getLeft() <= clip.right && cellInfor.getColumnIndex() < maxColumn) {
            columnInfo = sheet.getColumnInfo(cellInfor.getColumnIndex())

            if (columnInfo != null && (columnInfo.isHidden() /*|| columnInfo.getColWidth() <= 2 * SSConstant.SHEET_SPACETOBORDER*/)) {
                cellInfor.increaseColumn()
                continue
            }
            colWidth = (if (columnInfo != null) columnInfo.getColWidth() else sheet.getDefaultColWidth().toFloat())
            cellInfor.setWidth(colWidth * zoom)
            //
            if (cellInfor.getColumnIndex() != sheetScroller.getMinColumnIndex()
                || sheetScroller.isColumnAllVisible()
            ) {
                cellInfor.setVisibleWidth(cellInfor.getWidth())
            } else {
                cellInfor.setVisibleWidth(sheetScroller.getVisibleColumnWidth().toFloat() * zoom)
            }

            val cell = row.getCell(cellInfor.getColumnIndex())
            if (!firstVisibleValueTraceLogged && cell != null && cell.hasValidValue()
                && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                val content = try {
                    ModelUtil.instance().getFormatContents(sheet.getWorkbook()!!, cell)
                } catch (t: Throwable) {
                    "<format-error:${t.javaClass.simpleName}:${t.message}>"
                }
                OpenTrace.d(
                    "excel.render.first-visible-value sheet=${sheet.getSheetName()} " +
                        "row=${cell.getRowNumber()} col=${cell.getColNumber()} type=${cell.getCellType()} " +
                        "content=$content left=${cellInfor.getLeft()} top=${cellInfor.getTop()} " +
                        "width=${cellInfor.getWidth()} height=${cellInfor.getHeight()}"
                )
                firstVisibleValueTraceLogged = true
            }
            cellView!!.draw(canvas, cell, cellInfor)

            cellInfor.increaseLeftWithVisibleWidth()
            cellInfor.increaseColumn()
        }

        //draw extend cell contents
        iter = extendCell!!.iterator()
        var extendCell: ExtendCell
        var paint: Paint
        while (iter.hasNext()) {
            extendCell = iter.next()
            val cell = extendCell.getCell()!!
            val table = cell.getTableInfo()
            var tableCellStyle: SSTableCellStyle? = null
            if (table != null) {
                tableCellStyle = cellView!!.getTableCellStyle(
                    table,
                    sheet.getWorkbook()!!,
                    cell.getRowNumber(), cell.getColNumber()
                )
            }
            paint = FontKit.instance().getCellPaint(cell, getSpreadsheet()!!.getWorkbook(), tableCellStyle)

            canvas.save()
            canvas.clipRect(extendCell.getRect()!!)

            val content = extendCell.getContent()
            if (content is String) {
                //save paint text size
                val textSize = paint.textSize
                paint.textSize = textSize * zoom

                canvas.drawText(content, extendCell.getX(), extendCell.getY(), paint)
                //restore paint style
                paint.textSize = textSize
            } else {
                (content as STRoot).draw(canvas, extendCell.getX().toInt(), extendCell.getY().toInt(), zoom)
            }

            canvas.restore()
        }
    }

    /**
     * 绘制单元格式
     *
     */
    private fun drawRows(canvas: Canvas) {
        val sheet = this.sheet!!
        val cellInfor = this.cellInfor!!
        val clip = canvas.clipBounds
        if (!drawTraceLogged) {
            OpenTrace.d(
                "excel.render sheet=${sheet.getSheetName()} state=${sheet.getState()} " +
                    "minRow=${sheetScroller!!.getMinRowIndex()} minColumn=${sheetScroller!!.getMinColumnIndex()} " +
                    "scrollX=$scrollX scrollY=$scrollY firstRow=${sheet.getFirstRowNum()} lastRow=${sheet.getLastRowNum()} " +
                    "clip=${clip.left},${clip.top},${clip.right},${clip.bottom} " +
                    "abort=${spreadsheet!!.isAbortDrawing()} row0=${sheet.getRow(0) != null} " +
                    "row0cell0=${sheet.getRow(0)?.getCell(0, false)?.hasValidValue()}"
            )
            drawTraceLogged = true
        }
        cellInfor.setTop(columnHeader!!.getColumnHeaderHeight().toFloat())
        cellInfor.setRowIndex(sheetScroller!!.getMinRowIndex())

        // 逐行绘制
        val maxRow = sheet.getWorkbook()!!.getMaxRow()
        while (!spreadsheet!!.isAbortDrawing() && cellInfor.getTop() <= clip.bottom && cellInfor.getRowIndex() < maxRow) {
            val row = sheet.getRow(cellInfor.getRowIndex())
            if (row != null && row.isZeroHeight()) {
                cellInfor.increaseRow()
                continue
            }

            drawCells(canvas, row)

            cellInfor.increaseTopWithVisibleHeight()
            cellInfor.increaseRow()
        }
    }

    /**
     * 得到当前显示sheet
     */
    fun getCurrentSheet(): Sheet? {
        return sheet
    }

    /**
     * 得到列标题高度
     */
    fun getColumnHeaderHeight(): Int {
        return columnHeader!!.getColumnHeaderHeight()
    }

    /**
     * 得到行标题宽度
     */
    fun getRowHeaderWidth(): Int {
        return getRowHeader()!!.getRowHeaderWidth()
    }

    /**
     *
     * @param x the amount of pixels to scroll by horizontally
     * @param y the amount of pixels to scroll by vertically
     */
    fun scrollBy(x: Float, y: Float) {
        synchronized(this) {
            val sheet = this.sheet!!
            scrollX += x / zoom
            scrollX = sheet.getMaxScrollX().coerceAtMost(Math.max(0f, scrollX))

            scrollY += y / zoom
            scrollY = sheet.getMaxScrollY().coerceAtMost(Math.max(0f, scrollY))

            sheet.setScroll(Math.round(scrollX), Math.round(scrollY))

            updateScroller(sheet, Math.round(scrollX), Math.round(scrollY))

            resizeCalloutView()
        }
    }

    /**
     *
     * @param x the amount of pixels to scroll by horizontally
     * @param y the amount of pixels to scroll by vertically
     */
    fun scrollTo(x: Float, y: Float) {
        synchronized(this) {
            val sheet = this.sheet!!
            scrollX = x
            scrollX = sheet.getMaxScrollX().coerceAtMost(Math.max(0f, scrollX))

            scrollY = y
            scrollY = sheet.getMaxScrollY().coerceAtMost(Math.max(0f, scrollY))

            sheet.setScroll(Math.round(scrollX), Math.round(scrollY))

            updateScroller(sheet, Math.round(scrollX), Math.round(scrollY))
        }
    }

    /**
     * called when changed header size by SSEventManage
     */
    fun updateMinRowAndColumnInfo() {
            updateScroller(sheet!!, Math.round(scrollX), Math.round(scrollY))
    }

    private fun updateScroller(sheet: Sheet, x: Int, y: Int, force: Boolean = false) {
        if (!force && x == lastScrollerX && y == lastScrollerY) return
        sheetScroller!!.update(sheet, x, y)
        lastScrollerX = x
        lastScrollerY = y
    }

    fun getMinRowAndColumnInformation(): SheetScroller? {
        return sheetScroller
    }

    fun getScrollX(): Float {
        return scrollX
    }

    fun getScrollY(): Float {
        return scrollY
    }

    /**
     *
     * @return Returns the current zoom rate
     */
    fun getZoom(): Float {
        return zoom
    }

    /**
     * set current zoom rate
     * @param zoomRate
     */
    fun setZoom(zoom: Float) {
        synchronized(this) {
            setZoom(zoom, false)
            resizeCalloutView()
        }
    }

    /**
     *
     * @param zoom
     * @param isInit
     */
    @Synchronized
    fun setZoom(zoom: Float, isInit: Boolean) {
        val sheet = this.sheet!!
        val sheetScroller = this.sheetScroller!!
        val rowHeader = this.rowHeader!!
        val columnHeader = this.columnHeader!!
        var bottom = 0
        var checkActiveCellVisible = false
        if (this.zoom < zoom && !isInit) {
            val clipRect = this.clipRect!!
            bottom = clipRect.bottom - spreadsheet!!.getBottomBarHeight()

            when (sheet.getActiveCellType()) {
                Sheet.ACTIVECELL_ROW -> {
                    val yPostion = ModelUtil.instance().getValueY(
                        this,
                        sheet.getActiveCellRow() + 1,
                        sheetScroller.getVisibleRowHeight().toFloat()
                    )
                    if (yPostion < bottom) {
                        checkActiveCellVisible = true
                    }
                }

                Sheet.ACTIVECELL_COLUMN -> {
                    val xPostion = ModelUtil.instance().getValueX(
                        this,
                        sheet.getActiveCellColumn() + 1,
                        sheetScroller.getVisibleColumnWidth().toFloat()
                    )
                    if (xPostion < clipRect.right) {
                        checkActiveCellVisible = true
                    }
                }

                Sheet.ACTIVECELL_SINGLE -> {
                    val activeArea = ModelUtil.instance().getCellAnchor(
                        this,
                        sheet.getActiveCellRow(),
                        sheet.getActiveCellColumn()
                    )
                    if (activeArea.width() > 1 && activeArea.height() > 1
                        && activeArea.intersect(clipRect.left.toFloat(), clipRect.top.toFloat(), clipRect.right.toFloat(), bottom.toFloat())
                    ) {
                        checkActiveCellVisible = true
                    }
                }
            }
        }
        this.zoom = zoom
        sheet.setZoom(zoom)

        rowHeader.calculateRowHeaderWidth(zoom)
        columnHeader.calculateColumnHeaderHeight(zoom)

        val clipRect = this.clipRect
        if (checkActiveCellVisible && clipRect != null) {
            val bodyWidth = (clipRect.right - rowHeader.getRowHeaderWidth()).toFloat()
            val bodyHeight = (bottom - columnHeader.getColumnHeaderHeight()).toFloat()

            when (sheet.getActiveCellType()) {
                Sheet.ACTIVECELL_ROW -> {
                    var yPostion = ModelUtil.instance().getValueY(this, sheet.getActiveCellRow() + 1, Math.round(sheetScroller.getVisibleRowHeight()).toFloat())
                    while (yPostion > bottom && Math.abs(yPostion - bottom) > 1) {
                        val row = sheet.getRow(sheetScroller.getMinRowIndex())
                        val h = if (row == null) getCurrentSheet()!!.getDefaultRowHeight().toFloat() else row.getRowPixelHeight()
                        if (h * zoom > bodyHeight) {
                            //row height is larger than sheet body height
                            break
                        }
                        scrollY += h
                        sheetScroller.setMinRowIndex(sheetScroller.getMinRowIndex() + 1)
                        yPostion = ModelUtil.instance().getValueY(this, sheet.getActiveCellRow() + 1, Math.round(sheetScroller.getVisibleRowHeight()).toFloat())
                    }
                }

                Sheet.ACTIVECELL_COLUMN -> {
                    var xPostion = ModelUtil.instance().getValueX(this, sheet.getActiveCellColumn() + 1, Math.round(sheetScroller.getVisibleColumnWidth()).toFloat())
                    while (xPostion > clipRect.right && Math.abs(xPostion - clipRect.right) > 1) {
                        val columnWidth = sheet.getColumnPixelWidth(sheetScroller.getMinColumnIndex())
                        if (columnWidth * zoom > bodyWidth) {
                            //column width is larger than sheet body width
                            break
                        }

                        scrollX += columnWidth
                        sheetScroller.setMinColumnIndex(sheetScroller.getMinColumnIndex() + 1)
                        xPostion = ModelUtil.instance().getValueX(this, sheet.getActiveCellColumn() + 1, Math.round(sheetScroller.getVisibleColumnWidth()).toFloat())
                    }
                }

                Sheet.ACTIVECELL_SINGLE -> {
                    var activeArea = ModelUtil.instance().getCellAnchor(this, sheet.getActiveCellRow(), sheet.getActiveCellColumn())
                    while (abs(activeArea.right - rowHeader.getRowHeaderWidth()) < 1
                        || activeArea.right > clipRect.right
                        || abs(activeArea.bottom - columnHeader.getColumnHeaderHeight()) < 1
                        || activeArea.bottom > bottom
                    ) {
                        if (abs(activeArea.right - rowHeader.getRowHeaderWidth()) < 1) {
                            val columnWidth = sheet.getColumnPixelWidth(sheetScroller.getMinColumnIndex())
                            if (columnWidth * zoom > bodyWidth) {
                                //column width is larger than sheet body width
                                break
                            }

                            scrollX -= columnWidth
                            sheetScroller.setMinColumnIndex(sheetScroller.getMinColumnIndex() - 1)
                        } else if (activeArea.right > clipRect.right) {
                            val columnWidth = sheet.getColumnPixelWidth(sheetScroller.getMinColumnIndex())
                            if (columnWidth * zoom > bodyWidth) {
                                //column width is larger than sheet body width
                                break
                            }

                            scrollX += columnWidth
                            sheetScroller.setMinColumnIndex(sheetScroller.getMinColumnIndex() + 1)
                        }

                        if (Math.abs(activeArea.bottom - columnHeader.getColumnHeaderHeight()) < 1) {
                            val row = sheet.getRow(sheetScroller.getMinRowIndex())
                            val h = if (row == null) getCurrentSheet()!!.getDefaultRowHeight().toFloat() else row.getRowPixelHeight()
                            if (h * zoom > bodyHeight) {
                                //row height is larger than sheet body height
                                break
                            }
                            scrollY -= h
                            sheetScroller.setMinRowIndex(sheetScroller.getMinRowIndex() - 1)
                        } else if (activeArea.bottom > bottom) {
                            val row = sheet.getRow(sheetScroller.getMinRowIndex())
                            val h = if (row == null) getCurrentSheet()!!.getDefaultRowHeight().toFloat() else row.getRowPixelHeight()
                            if (h * zoom > bodyHeight) {
                                //row height is larger than sheet body height
                                break
                            }
                            scrollY += h
                            sheetScroller.setMinRowIndex(sheetScroller.getMinRowIndex() + 1)
                        }

                        activeArea = ModelUtil.instance().getCellAnchor(this, sheet.getActiveCellRow(), sheet.getActiveCellColumn())

                        var stop = false
                        if (Math.abs(activeArea.left - rowHeader.getRowHeaderWidth()) < 1
                            && activeArea.right >= clipRect.right
                        ) {
                            stop = true
                        } else if (activeArea.right != rowHeader.getRowHeaderWidth().toFloat()
                            && Math.abs(activeArea.right - activeArea.left) < 1
                        ) {
                            sheetScroller.setMinColumnIndex(sheetScroller.getMinColumnIndex() - 1)
                            stop = true
                        }

                        if (Math.abs(activeArea.top - columnHeader.getColumnHeaderHeight()) < 1
                            && activeArea.bottom >= clipRect.bottom
                        ) {
                            stop = true
                        } else if (activeArea.bottom != columnHeader.getColumnHeaderHeight().toFloat()
                            && activeArea.bottom < activeArea.top
                        ) {
                            sheetScroller.setMinRowIndex(sheetScroller.getMinRowIndex() - 1)
                            stop = true
                        }

                        if (stop) {
                            return
                        }
                    }
                }
            }
            sheet.setScroll(scrollX.toInt(), scrollY.toInt())
        }
    }

    fun setZoom(zoom: Float, pointX: Float, pointY: Float) {
        val sheet = this.sheet!!
        val viewWidth = spreadsheet!!.width
        val viewHeight = spreadsheet!!.height

        var normalizedX = (pointX - rowHeader!!.getRowHeaderWidth()) / this.zoom
        var normalizedY = (pointY - columnHeader!!.getColumnHeaderHeight()) / this.zoom

        //width and height to top-left corner of the whole sheet
        normalizedX = Math.min(sheet.getMaxScrollX(), normalizedX + sheet.getScrollX())
        normalizedY = Math.min(sheet.getMaxScrollY(), normalizedY + sheet.getScrollY())

        this.zoom = zoom
        sheet.setZoom(zoom)

        rowHeader!!.calculateRowHeaderWidth(zoom)
        columnHeader!!.calculateColumnHeaderHeight(zoom)

        normalizedX = (normalizedX * zoom - viewWidth / 2) / zoom
        normalizedY = (normalizedY * zoom - viewHeight / 2) / zoom

        scrollTo(normalizedX.toInt().toFloat(), normalizedY.toInt().toFloat())
    }

    /**
     * @return Returns the spreadsheet.
     */
    fun getSpreadsheet(): Spreadsheet? {
        return spreadsheet
    }

    /**
     * @param spreadsheet The spreadsheet to set.
     */
    fun setSpreadsheet(spreadsheet: Spreadsheet?) {
        this.spreadsheet = spreadsheet
    }

    /**
     * @return Returns the rowHeader.
     */
    fun getRowHeader(): RowHeader? {
        return rowHeader
    }

    /**
     *
     * @return
     */
    fun getCurrentMinRow(): Int {
        return sheetScroller!!.getMinRowIndex()
    }

    fun getCurrentMinColumn(): Int {
        return sheetScroller!!.getMinColumnIndex()
    }

    fun selectedCell(rowIndex: Int, colIndex: Int) {
        val sheet = this.sheet!!
        val selecetedCellsRange = this.selecetedCellsRange!!
        val row = sheet.getRow(rowIndex)
        if (row != null && row.getCell(colIndex) != null
            && row.getCell(colIndex)!!.getRangeAddressIndex() >= 0
        ) {
            val cellRangeAddress = sheet.getMergeRange(row.getCell(colIndex)!!.getRangeAddressIndex())!!
            selecetedCellsRange.setFirstRow(cellRangeAddress.getFirstRow())
            selecetedCellsRange.setLastRow(cellRangeAddress.getLastRow())
            selecetedCellsRange.setFirstColumn(cellRangeAddress.getFirstColumn())
            selecetedCellsRange.setLastColumn(cellRangeAddress.getLastColumn())
        } else {
            selecetedCellsRange.setFirstRow(rowIndex)
            selecetedCellsRange.setLastRow(rowIndex)
            selecetedCellsRange.setFirstColumn(colIndex)
            selecetedCellsRange.setLastColumn(colIndex)
        }

        //set to sheet
        getCurrentSheet()!!.setActiveCellRowCol(selecetedCellsRange.getFirstRow(), selecetedCellsRange.getFirstColumn())
    }

    /**
     * set the flag which used to draw moving header dotted line
     * @param draw
     */
    fun setDrawMovingHeaderLine(draw: Boolean) {
        isDrawMovingHeaderLine = draw
    }

    /**
     * change current header information when changing header width or height
     * @param headerInfor
     */
    fun changeHeaderArea(headerInfor: FocusCell?) {
        this.selectedHeaderInfor = headerInfor
    }

    fun goToFindedCell(cell: Cell?) {
        if (cell == null) {
            return
        }

        var col = cell.getColNumber()
        var row = cell.getRowNumber()
        if (cell.getColNumber() > 0) {
            col = cell.getColNumber() - 1
        }

        if (cell.getRowNumber() > 0) {
            row = cell.getRowNumber() - 1
        }

        sheet!!.setActiveCellRowCol(cell.getRowNumber(), cell.getColNumber())
        selectedCell(cell.getRowNumber(), cell.getColNumber())
        goToCell(row, col)

        spreadsheet!!.postInvalidate()
        //
        spreadsheet!!.getControl().actionEvent(EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS, null)
        //
        spreadsheet!!.getControl().actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null)
    }

    fun goToCell(row: Int, column: Int) {
        val area = ModelUtil.instance().getCellAnchor(sheet!!, row, column, true)
        scrollTo(area!!.left.toFloat(), area.top.toFloat())
    }

    /**
     *
     * @param findValue
     * @return true: finded   false: not finded
     */
    fun find(findValue: String?): Boolean {
        if (findingMgr == null) {
            findingMgr = FindingMgr()
        }

        val cell = findingMgr!!.findCell(sheet, findValue)
        if (cell != null) {
            goToFindedCell(cell)
            return true
        } else {
            return false
        }
    }

    fun findAll(findValue: String?, result: Consumer<MutableList<Cell>>): Boolean {
        if (findingMgr == null) {
            findingMgr = FindingMgr()
        }

        val cells = findingMgr!!.findAll(sheet, findValue)
        if (cells != null && !cells.isEmpty()) {
            goToFindedCell(cells[0])
            result.accept(cells)
            return true
        } else {
            return false
        }
    }

    fun findBackward(): Boolean {
        if (findingMgr == null) {
            return false
        }
        val cell = findingMgr!!.findBackward()
        if (cell != null) {
            goToFindedCell(cell)
            return true
        }

        return false
    }

    fun findForward(): Boolean {
        if (findingMgr == null) {
            return false
        }
        val cell = findingMgr!!.findForward()
        if (cell != null) {
            goToFindedCell(cell)
            return true
        }

        return false
    }

    /**
     *
     * @param cell
     * @param x
     * @param y
     * @param content
     */
    fun addExtendCell(cell: Cell?, rect: RectF?, x: Float, y: Float, content: Any?) {
        extendCell!!.add(ExtendCell(cell, rect, x, y, content))
    }

    fun getSheetIndex(): Int {
        return sheet!!.getWorkbook()!!.getSheetIndex(sheet) + 1
    }

    fun dispose() {
        spreadsheet = null
        sheet = null

        if (rowHeader != null) {
            rowHeader!!.dispose()
            rowHeader = null
        }

        if (columnHeader != null) {
            columnHeader!!.dispose()
            columnHeader = null
        }

        if (cellView != null) {
            cellView!!.dispose()
            cellView = null
        }

        if (shapeView != null) {
            shapeView!!.dispose()
            shapeView = null
        }

        if (sheetScroller != null) {
            sheetScroller!!.dispose()
            sheetScroller = null
        }

        if (cellInfor != null) {
            cellInfor!!.dispose()
            cellInfor = null
        }

        if (findingMgr != null) {
            findingMgr!!.dispose()
            findingMgr = null
        }

        if (extendCell != null) {
            extendCell!!.clear()
            extendCell = null
        }

        selectedHeaderInfor = null
        clipRect = null
        effects = null
    }

    fun getFindingMgr(): FindingMgr? {
        return findingMgr
    }

    fun setFindingMgr(findingMgr: FindingMgr?) {
        this.findingMgr = findingMgr
    }

    inner class ExtendCell(cell: Cell?, rect: RectF?, x: Float, y: Float, content: Any?) {
        private var cell: Cell? = null
        private var rect: RectF? = null
        private var x = 0f
        private var y = 0f
        private var content: Any? = null

        init {
            this.cell = cell
            this.setRect(rect)
            this.x = x
            this.y = y
            if (content is String) {
                this.content = content.intern()
            } else {
                this.content = content
            }
        }

        fun getCell(): Cell? {
            return cell
        }

        /**
         * @return Returns the x.
         */
        fun getX(): Float {
            return x
        }

        /**
         * @return Returns the y.
         */
        fun getY(): Float {
            return y
        }

        fun getContent(): Any? {
            return content
        }

        /**
         * @return Returns the rect.
         * (private in Java; the outer class reads it, which Kotlin only allows for non-private members)
         */
        fun getRect(): RectF? {
            return rect
        }

        /**
         * @param rect The rect to set.
         */
        private fun setRect(rect: RectF?) {
            this.rect = rect
        }

        fun dispose() {
            cell = null
            rect = null
            content = null
        }
    }

    companion object {
        const val MAXROW_03 = 65536
        const val MAXCOLUMN_03 = 256

        const val MAXROW_07 = 1048576
        const val MAXCOLUMN_07 = 16384
    }
}
