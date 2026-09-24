/*
 * 文件名称:          MinRowAndColumnInformation.java
 *
 * 编译器:            android2.2
 * 时间:              下午2:22:31
 */
package com.wxiwei.office.ss.other

import com.wxiwei.office.ss.model.baseModel.Row
import com.wxiwei.office.ss.model.baseModel.Sheet
import com.wxiwei.office.ss.model.baseModel.Workbook

/**
 * min row and column information of current sheet after scrolling
 *
 * Read版本:        Read V1.0
 *
 * 作者:            jqin
 *
 * 日期:            2011-12-27
 *
 * 负责人:           jqin
 *
 * 负责小组:
 */
class SheetScroller {
    //current min row and column index
    private var minRowIndex = 0
    private var minColumnIndex = 0

    //current min row height and column width(zoom = 1)
    private var columnWidth = 0f
    private var rowHeight = 0f

    private var isRowAllVisible = true
    private var isColumnAllVisible = true

    //current min visible row height and visible column width(zoom = 1)
    private var visibleRowHeight = 0.0
    private var visibleColumnWidth = 0.0

    fun reset() {
        setMinRowIndex(0)
        setMinColumnIndex(0)

        setRowHeight(0f)
        setColumnWidth(0f)

        setVisibleRowHeight(0.0)
        setVisibleColumnWidth(0.0)

        setRowAllVisible(true)
        setColumnAllVisible(true)
    }

    fun update(sheet: Sheet, scrollX: Int, scrollY: Int) {
        reset()

        setVisibleRowHeight(scrollY.toDouble())
        setVisibleColumnWidth(scrollX.toDouble())

        val paneInfo = sheet.getPaneInformation()
        if (paneInfo != null) {
            setMinRowIndex(paneInfo.getHorizontalSplitTopRow().toInt())
            setMinColumnIndex(paneInfo.getVerticalSplitLeftColumn().toInt())
        }

        val maxSheetRows = if (sheet.getWorkbook()!!.isBefore07Version()) Workbook.MAXROW_03 else Workbook.MAXROW_07
        val maxSheetColumns = if (sheet.getWorkbook()!!.isBefore07Version()) Workbook.MAXCOLUMN_03 else Workbook.MAXCOLUMN_07
        var row: Row?
        if (scrollY > 0) {
            val firstRow = sheet.getFirstRowNum()
            val lastRow = sheet.getLastRowNum()
            val defaultRowHeight = sheet.getDefaultRowHeight()

            //update row information
            while (visibleRowHeight >= 1 && minRowIndex <= maxSheetRows) {
                // Rows after the last physically defined row all use the
                // default height. Skip them as a block instead of walking up
                // to Excel's 1,048,576-row limit on every touch/frame.
                if (minRowIndex > lastRow) {
                    val skippedRows = (visibleRowHeight / defaultRowHeight).toInt()
                    if (skippedRows > 0) {
                        minRowIndex += skippedRows
                        visibleRowHeight -= skippedRows * defaultRowHeight
                    }
                    if (visibleRowHeight < 1) break
                }
                if (minRowIndex >= firstRow && minRowIndex <= lastRow) {
                    row = sheet.getRow(minRowIndex)
                } else {
                    row = null
                }

                if (row == null || (row != null && !row.isZeroHeight())) {
                    rowHeight = (if (row == null) defaultRowHeight.toFloat() else row.getRowPixelHeight())
                    visibleRowHeight = visibleRowHeight - rowHeight
                }
                minRowIndex++
            }

            if (minRowIndex != maxSheetRows) {
                minRowIndex--
                setVisibleRowHeight(Math.abs(getVisibleRowHeight()))
                if (getVisibleRowHeight() < 1) {
                    minRowIndex++
                    setVisibleRowHeight(0.0)
                } else {
                    setRowAllVisible(false)
                }
            } else {
                minRowIndex--
                row = sheet.getRow(minRowIndex)
                while (row != null && row.isZeroHeight()) {
                    minRowIndex--
                    row = sheet.getRow(getMinRowIndex())
                }

                setVisibleRowHeight(0.0)
            }
        }

        if (scrollX > 0) {
            //update column information
            while (visibleColumnWidth >= 1 && minColumnIndex <= maxSheetColumns) {
                if (!sheet.isColumnHidden(minColumnIndex)) {
                    columnWidth = sheet.getColumnPixelWidth(minColumnIndex)
                    visibleColumnWidth -= columnWidth.toDouble()
                }
                minColumnIndex++
            }

            if (minColumnIndex != maxSheetColumns) {
                minColumnIndex--
                setVisibleColumnWidth(Math.abs(getVisibleColumnWidth()))
                if (getVisibleColumnWidth() < 1) {
                    minColumnIndex++
                    setVisibleColumnWidth(0.0)
                } else {
                    setColumnAllVisible(false)
                }
            } else {
                minColumnIndex--
                while (sheet.isColumnHidden(minColumnIndex)) {
                    minColumnIndex--
                }
                setVisibleColumnWidth(0.0)
            }
        }
    }

    /**
     * @return Returns the minRowIndex.
     */
    fun getMinRowIndex(): Int {
        return minRowIndex
    }

    /**
     * @param minRowIndex The minRowIndex to set.
     */
    fun setMinRowIndex(minRowIndex: Int) {
        this.minRowIndex = minRowIndex
    }

    /**
     *
     */
    fun dispose() {
    }

    /**
     * @return Returns the minColumnIndex.
     */
    fun getMinColumnIndex(): Int {
        return minColumnIndex
    }

    /**
     * @param minColumnIndex The minColumnIndex to set.
     */
    fun setMinColumnIndex(minColumnIndex: Int) {
        this.minColumnIndex = minColumnIndex
    }

    /**
     * @return Returns the columnWidth.
     */
    fun getColumnWidth(): Float {
        return columnWidth
    }

    /**
     * @param columnWidth The columnWidth to set.
     */
    fun setColumnWidth(columnWidth: Float) {
        this.columnWidth = columnWidth
    }

    /**
     * @return Returns the rowHeight.
     */
    fun getRowHeight(): Float {
        return rowHeight
    }

    /**
     * @param rowHeight The rowHeight to set.
     */
    fun setRowHeight(rowHeight: Float) {
        this.rowHeight = rowHeight
    }

    /**
     * @return Returns the isRowAllVisible.
     */
    fun isRowAllVisible(): Boolean {
        return isRowAllVisible
    }

    /**
     * @param isRowAllVisible The isRowAllVisible to set.
     */
    fun setRowAllVisible(isRowAllVisible: Boolean) {
        this.isRowAllVisible = isRowAllVisible
    }

    /**
     * @return Returns the isColumnAllVisible.
     */
    fun isColumnAllVisible(): Boolean {
        return isColumnAllVisible
    }

    /**
     * @param isColumnAllVisible The isColumnAllVisible to set.
     */
    fun setColumnAllVisible(isColumnAllVisible: Boolean) {
        this.isColumnAllVisible = isColumnAllVisible
    }

    /**
     * @return Returns the visibleRowHeight.
     */
    fun getVisibleRowHeight(): Double {
        return visibleRowHeight
    }

    /**
     * @param visibleRowHeight The visibleRowHeight to set.
     */
    fun setVisibleRowHeight(visibleRowHeight: Double) {
        this.visibleRowHeight = visibleRowHeight
    }

    /**
     * @return Returns the visibleColumnWidth.
     */
    fun getVisibleColumnWidth(): Double {
        return visibleColumnWidth
    }

    /**
     * @param visibleColumnWidth The visibleColumnWidth to set.
     */
    fun setVisibleColumnWidth(visibleColumnWidth: Double) {
        this.visibleColumnWidth = visibleColumnWidth
    }
}
