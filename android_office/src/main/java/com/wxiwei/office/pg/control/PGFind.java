/*
 * 文件名称:           PGFind.java
 *
 * 编译器:             android2.2
 * 时间:               下午1:58:09
 */
package com.wxiwei.office.pg.control;

import com.wxiwei.office.common.shape.AbstractShape;
import com.wxiwei.office.common.shape.IShape;
import com.wxiwei.office.common.shape.TableCell;
import com.wxiwei.office.common.shape.TableShape;
import com.wxiwei.office.common.shape.TextBox;
import com.wxiwei.office.constant.EventConstant;
import com.wxiwei.office.java.awt.Rectangle;
import com.wxiwei.office.pg.model.PGSlide;
import com.wxiwei.office.simpletext.model.SectionElement;
import com.wxiwei.office.system.IFind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ppt find
 * <p>
 * <p>
 * Read版本:       Read V1.0
 * <p>
 * 作者:           jhy1790
 * <p>
 * 日期:           2012-3-22
 * <p>
 * 负责人:         jhy1790
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
public class PGFind implements IFind {
    /**
     *
     */
    public PGFind(Presentation presentation) {
        this.presentation = presentation;
        rect = new Rectangle();
    }

    /**
     *
     */
    public boolean find(String query) {
        if (query == null) {
            return false;
        }
        this.query = query;
        startOffset = -1;
        shapeIndex = -1;
        // find
        int slideIndex = presentation.getCurrentIndex();
        do {
            if (findSlideForward(slideIndex)) {
                return true;
            }
            if (++slideIndex == presentation.getRealSlideCount()) {
                slideIndex = 0;
            }
        }
        while (slideIndex != presentation.getCurrentIndex());
        return false;
    }

    /**
     *
     */
    public boolean findBackward() {
        if (query == null) {
            return false;
        }
        int slideIndex = presentation.getCurrentIndex();
        do {
            if (findSlideBackward(slideIndex)) {
                return true;
            }
            startOffset = -1;
            shapeIndex = -1;
        }
        while (--slideIndex >= 0);
        return false;
    }

    /**
     *
     */
    public boolean findForward() {
        if (query == null) {
            return false;
        }
        int slideIndex = presentation.getCurrentIndex();
        do {
            if (findSlideForward(slideIndex)) {
                return true;
            }
            startOffset = -1;
            shapeIndex = -1;
        }
        while (++slideIndex != presentation.getRealSlideCount());
        return false;
    }

    /**
     *
     */
    private boolean findSlideBackward(int slideIndex) {
        PGSlide slide = presentation.getSlide(slideIndex);
        int i = shapeIndex >= 0 ? shapeIndex : slide.getShapeCountForFind() - 1;
        for (; i >= 0; i--) {
            IShape shape = slide.getShapeForFind(i);
            if (shape != null && shape.getType() == AbstractShape.SHAPE_TEXTBOX) {
                int offset = shapeIndex == i && presentation.getCurrentIndex() == slideIndex ? startOffset : -1;
                SectionElement elem = ((TextBox) shape).getElement();
                if (elem == null || (offset >= 0 && offset < query.length()) || elem.getEndOffset() - elem.getStartOffset() == 0) {
                    continue;
                }
                if (offset >= 0) {
                    offset = elem.getText(presentation.getRenderersDoc()).lastIndexOf(query, Math.max(startOffset - query.length(), 0));
                } else {
                    offset = elem.getText(presentation.getRenderersDoc()).lastIndexOf(query);
                }
                if (offset >= 0) {
                    startOffset = offset;
                    shapeIndex = i;
                    addHighlight(slideIndex, (TextBox) shape);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     */
    private boolean findSlideForward(int slideIndex) {
        PGSlide slide = presentation.getSlide(slideIndex);
        for (int i = Math.max(0, shapeIndex); i < slide.getShapeCountForFind(); i++) {
            IShape shape = slide.getShapeForFind(i);
            if (shape != null && shape.getType() == AbstractShape.SHAPE_TEXTBOX) {
                SectionElement elem = ((TextBox) shape).getElement();
                if (elem == null || elem.getEndOffset() - elem.getStartOffset() == 0) {
                    continue;
                }
                int offset = shapeIndex == i && presentation.getCurrentIndex() == slideIndex ? startOffset : -1;
                if (offset >= 0) {
                    offset = elem.getText(presentation.getRenderersDoc()).indexOf(query, startOffset + query.length());
                } else {
                    offset = elem.getText(presentation.getRenderersDoc()).indexOf(query);
                }
                if (offset >= 0) {
                    startOffset = offset;
                    shapeIndex = i;
                    addHighlight(slideIndex, (TextBox) shape);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * show find, add highlight
     */
    public void removeAllHighlights() {
    }

    public void addHighlight(int slideIndex, TextBox textBox) {
        boolean invalidate = true;
        if (slideIndex != presentation.getCurrentIndex()) {
            presentation.showSlide(slideIndex, true);
            isSetPointToVisible = true;
            invalidate = false;
        } else {
            rect.setBounds(0, 0, 0, 0);
            presentation.getEditor().modelToView(startOffset, rect, false);
            if (!presentation.getPrintMode().getListView().isPointVisibleOnScreen(rect.x, rect.y)) {
                presentation.getPrintMode().getListView().setItemPointVisibleOnScreen(rect.x, rect.y);
                invalidate = false;
            } else {
                presentation.getPrintMode().exportImage(presentation.getPrintMode().getListView().getCurrentPageView(), null);
            }
        }
        if (invalidate) {
            presentation.postInvalidate();
        }
        this.slideIndex = slideIndex;
        presentation.getEditor().setEditorTextBox(textBox);
        presentation.getEditor().getHighlight().addHighlight(startOffset, startOffset + query.length());
        //
        presentation.getControl().actionEvent(EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS, null);
        //
        //presentation.getControl().actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null);
    }

    /**
     *
     */
    public void onConfigurationChanged() {
        PGSlide slide = presentation.getCurrentSlide();
        if (shapeIndex >= 0 && shapeIndex < slide.getShapeCountForFind()) {
            presentation.getEditor().getHighlight().addHighlight(startOffset, startOffset + query.length());
            presentation.postInvalidate();
        }
    }

    /**
     * @return Returns the isSetPointToVisible.
     */
    public boolean isSetPointToVisible() {
        return isSetPointToVisible;
    }

    /**
     * @param isSetPointToVisible The isSetPointToVisible to set.
     */
    public void setSetPointToVisible(boolean isSetPointToVisible) {
        this.isSetPointToVisible = isSetPointToVisible;
    }

    public static class SearchResult {
        public final int slideIndex;
        public final int shapeIndex;
        public final int startOffset;

        public SearchResult(int slideIndex, int shapeIndex, int startOffset) {
            this.slideIndex = slideIndex;
            this.shapeIndex = shapeIndex;
            this.startOffset = startOffset;
        }
    }

    public List<SearchResult> findAll(String query) {
        List<SearchResult> all = new ArrayList<>();
        PGFind.this.query = query;
        if (query == null || query.isEmpty()) return all;
        int slideCount = presentation.getRealSlideCount();
        for (int si = 0; si < slideCount; si++) {
            PGSlide slide = presentation.getSlide(si);
            int shapeCount = slide.getShapeCountForFind();
            for (int sh = 0; sh < shapeCount; sh++) {
                IShape shape = slide.getShapeForFind(sh);
                if (shape != null && shape.getType() == AbstractShape.SHAPE_TEXTBOX) {
                    SectionElement elem = ((TextBox) shape).getElement();
                    if (elem == null) continue;
                    String text = elem.getText(presentation.getRenderersDoc());
                    int idx = text.indexOf(query);
                    while (idx >= 0) {
                        all.add(new SearchResult(si, sh, idx));
                        idx = text.indexOf(query, idx + query.length());
                    }
                }
            }
        }
        return all;
    }

    /**
     * Focus on (i.e. show & highlight) the given result from the last findAll() call.
     *
     * @param resultIndex index into the List<SearchResult> you got from findAll()
     * @param results     the list returned by findAll()
     * @return true if successfully focused, false otherwise
     */
//    public boolean focusCurrent(int resultIndex, List<SearchResult> results) {
//        if (results == null || resultIndex < 0 || resultIndex >= results.size()) return false;
//        SearchResult r = results.get(resultIndex);
//        // set internal state so addHighlight will jump there
//        this.slideIndex = r.slideIndex;
//        this.shapeIndex = r.shapeIndex;
//        this.startOffset = r.startOffset;
//        this.query = presentation.getEditor()
//                .getHighlight()       // assume same query used
//                .getText(startOffset, startOffset + query.length());
//        // call your existing highlight logic:
//        addHighlight(r.slideIndex,
//                (TextBox) presentation.getSlide(r.slideIndex)
//                        .getShapeForFind(r.shapeIndex));
//        return true;
//    }

    /**
     *
     */
    public int getPageIndex() {
        return slideIndex;
    }

    /**
     *
     */
    public void resetSearchResult() {
    }

    public void dispose() {
        presentation = null;
        query = null;
    }

    //
    private boolean isSetPointToVisible;
    //
    private Presentation presentation;
    //
    private String query;
    //
    private int shapeIndex = -1;
    //
    private int slideIndex = -1;
    // 起始位置
    private int startOffset = -1;
    //
    protected Rectangle rect;
}
