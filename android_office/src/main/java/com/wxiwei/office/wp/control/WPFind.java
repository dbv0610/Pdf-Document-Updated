/*
 * 文件名称:          WPFind.java
 *  
 * 编译器:            android2.2
 * 时间:              上午11:03:08
 */
package com.wxiwei.office.wp.control;

import androidx.annotation.NonNull;

import com.wxiwei.office.constant.EventConstant;
import com.wxiwei.office.constant.wp.WPViewConstant;
import com.wxiwei.office.java.awt.Rectangle;
import com.wxiwei.office.simpletext.model.IDocument;
import com.wxiwei.office.simpletext.model.IElement;
import com.wxiwei.office.simpletext.view.IView;
import com.wxiwei.office.system.IFind;
import com.wxiwei.office.wp.view.PageRoot;
import com.wxiwei.office.wp.view.PageView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import kotlin.Unit;

/**
 * find feature
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2012-3-22
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:         
 * <p>
 * <p>
 */
public class WPFind implements IFind
{
    /**
     * 
     * @param word
     */
    public WPFind(Word word)
    {
        this.word = word;
        rect = new Rectangle();
    }

    public boolean find(String query)
    {
        if (query == null)
        {
            return false;
        }
        isSetPointToVisible = false;
        this.query = query; 
        float zoom = word.getZoom();
        long offset = 0;
        if (word.getCurrentRootType() == WPViewConstant.PRINT_ROOT)
        {
            IView view = word.getPrintWord().getCurrentPageView();
            while (view != null && view.getType() != WPViewConstant.PARAGRAPH_VIEW)
            {
                view = view.getChildView();
            }
            if (view != null)
            {   
                offset =  view.getStartOffset(null);
            }
        }
        else
        {
            offset = word.viewToModel((int)(word.getScrollX() / zoom), 
                (int)(word.getScrollY() / zoom ), false);
        }
        IDocument doc = word.getDocument();
        findElement = doc.getParagraph(offset);
        while (findElement != null)
        {
            findString = findElement.getText(doc);
            int index = findString.indexOf(query); 
            if (index >= 0)
            {
                addHighlight(index, query.length());
                return true;
            }
            focusCurrentIndex = index;
            findElement = doc.getParagraph(findElement.getEndOffset());
        }
        findString = null;
        return false;
    }

    /**
     * backward to find;
     */
    public boolean findBackward()
    {
        if (query == null)
        {
            return false;
        }
        isSetPointToVisible = false;
        IDocument doc = word.getDocument();
        if (findString != null)
        {
            int index = findString.lastIndexOf(query, relativeParaIndex - query.length() * 2);
            if (index >= 0)
            {
                addHighlight(index, query.length());
                return true;
            }
        }
        findElement = doc.getParagraph(findElement == null ? doc.getLength(0) - 1 : findElement.getStartOffset() - 1);
        while(findElement != null)
        {
            findString = findElement.getText(doc);
            int index = findString.lastIndexOf(query);
            if (index >=0 && isSameSelectPosition(index))
            {
                index = findString.lastIndexOf(query, index - query.length());
            }
            if (index >= 0)
            {
                addHighlight(index, query.length());
                return true;
            }
            focusCurrentIndex = index;
            findElement = doc.getParagraph(findElement.getStartOffset() - 1);
        }
        findString = null;
        return false;
    }



    @Override
    public boolean findForward() {
        if (query == null)
        {
            return false;
        }
        isSetPointToVisible = false;
        IDocument doc = word.getDocument();
        if (findString != null)
        {
            int index = findString.indexOf(query, relativeParaIndex);
            if (index >= 0)
            {
                addHighlight(index, query.length());
                return true;
            }
        }
        findElement = doc.getParagraph(findElement == null ? 0 : findElement.getEndOffset());
        while(findElement != null)
        {
            findString = findElement.getText(doc);
            int index = findString.indexOf(query);
            if (index >=0 && isSameSelectPosition(index))
            {
                index = findString.indexOf(query, index + query.length());
            }
            if (index >= 0)
            {
                addHighlight(index, query.length());
                return true;
            }
            focusCurrentIndex = index;
            findElement = doc.getParagraph(findElement.getEndOffset());
        }
        findString = null;
        return false;
    }
    private int focusCurrentIndex =0;

    public  void focusByCurrent(Consumer<Integer> result) {
        if (query == null)
        {
            return ;
        }
        if(findString!=null){
            addHighlight(focusCurrentIndex,query.length());
        }
    }
    private void addHighlight(int index, int queryLen)
    {
        relativeParaIndex = index;
        long findCurrentOffset = findElement.getStartOffset() + index;
        word.getHighlight().addHighlight(findCurrentOffset, findCurrentOffset + queryLen);
        relativeParaIndex += queryLen;
        
        if (word.getCurrentRootType() == WPViewConstant.PRINT_ROOT)
        {
            IView root = word.getRoot(WPViewConstant.PAGE_ROOT);
            boolean invalidate = true;
            if (root != null && root.getType() == WPViewConstant.PAGE_ROOT)
            {
                IView pv = ((PageRoot)root).getViewContainer().getParagraph(findCurrentOffset, false);
                while (pv != null && pv.getType() != WPViewConstant.PAGE_VIEW)
                {
                    pv = pv.getParentView();
                }
                if (pv != null)
                {
                    pageIndex = ((PageView)pv).getPageNumber() - 1;
                    if (pageIndex != word.getCurrentPageNumber() - 1)
                    {
                        word.showPage(pageIndex, -1);
                        isSetPointToVisible = true;
                        invalidate = false;
                    }
                    else
                    {
                        rect.setBounds(0, 0, 0, 0);
                        word.modelToView(findCurrentOffset, rect, false);
                        rect.x -= pv.getX();
                        rect.y -= pv.getY();
                        if (!word.getPrintWord().getListView().isPointVisibleOnScreen(rect.x, rect.y))
                        {
                            word.getPrintWord().getListView().setItemPointVisibleOnScreen(rect.x, rect.y);
                            invalidate = false;
                        }
                        else
                        {
                            word.getPrintWord().exportImage(word.getPrintWord().getListView().getCurrentPageView(), null);
                        }
                    }
                }
            }
            if (invalidate)
            {
                word.postInvalidate();
            }
            return;
        }
        //
        rect.setBounds(0, 0, 0, 0);
        word.modelToView(findCurrentOffset, rect, false);
        Rectangle vRect = word.getVisibleRect();
        float zoom = word.getZoom();
        int x = (int)(rect.x * zoom);
        int y = (int)(rect.y * zoom);
        if (!vRect.contains(x, y))
        {   
            if (x + vRect.width > word.getWordWidth() * zoom)
            {
                x = (int)(word.getWordWidth() * zoom) - vRect.width;
            }
            if (y + vRect.height > word.getWordHeight() * zoom)
            {
                y = (int)(word.getWordHeight() * zoom) - vRect.height;
            }
            word.scrollTo(x, y);
        }
        else
        {
            word.postInvalidate();
        }
        //
        word.getControl().actionEvent(EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS, null);
        //
        if (word.getCurrentRootType() != WPViewConstant.PRINT_ROOT)
        {
            word.getControl().actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null);
        }
    }
    
    
    /**
     * 
     */
    public void resetSearchResult()
    {        
    }
    
    /**
     * 
     */
    public int getPageIndex()
    {
        return pageIndex;
    }
    
    /**
     * 判断是否与当前选中是同一个位置
     */
    private boolean isSameSelectPosition(int index)
    {
        return word.getHighlight().isSelectText() 
            && (findElement.getStartOffset() + index) == word.getHighlight().getSelectStart();
    }
    
    
    /**
     * @return Returns the isSetPointToVisible.
     */
    public boolean isSetPointToVisible()
    {
        return isSetPointToVisible;
    }

    /**
     * @param isSetPointToVisible The isSetPointToVisible to set.
     */
    public void setSetPointToVisible(boolean isSetPointToVisible)
    {
        this.isSetPointToVisible = isSetPointToVisible;
    }
    
    /**
     * 
     */
    public void dispose()
    {
        findElement = null;
        word = null;
        rect = null;
    }

    private final List<Long> allFindOffsets = new ArrayList<>();

    /**
     * Find *all* occurrences of `query` in the document, highlight them,
     * and return a list of their start offsets.
     */
    public List<Long> findAll(String query,Consumer<Integer> result) {
        allFindOffsets.clear();
        if (query == null || query.isEmpty()) return allFindOffsets;
        this.query = query;
        word.getHighlight().removeHighlight();

        IDocument doc = word.getDocument();
        IElement para = doc.getParagraph(0);         // start at beginning
        while (para != null) {
            String text = para.getText(doc);
            int idx = text.indexOf(query);
            while (idx >= 0) {
                long matchOffset = para.getStartOffset() + idx;
                allFindOffsets.add(matchOffset);
                // highlight this occurrence
                word.getHighlight().addHighlight(matchOffset, matchOffset + query.length());
                // look for next in same paragraph
                idx = text.indexOf(query, idx + query.length());
                focusCurrentIndex = idx;
            }
            para = doc.getParagraph(para.getEndOffset());
        }
        if(result!=null){
            result.accept(allFindOffsets.size());
        }
        // after bulk‐highlight, repaint
        word.postInvalidate();
        return new ArrayList<>(allFindOffsets);
    }

    /**
     * Focus on the Nth occurrence (0-based) previously found by findAll().
     * Returns true if successful.
     */
    public boolean focusBy(int occurrenceIndex) {
        if (occurrenceIndex < 0 || occurrenceIndex >= allFindOffsets.size()) {
            return false;
        }
        long offset = allFindOffsets.get(occurrenceIndex);
        IDocument doc = word.getDocument();
        // locate the paragraph containing this offset
        findElement = doc.getParagraph(offset);
        if (findElement == null) return false;
        findString = findElement.getText(doc);
        // compute relative index within paragraph
        int relIdx = (int)(offset - findElement.getStartOffset());
        // clear previous highlights, then highlight & scroll exactly this one
        word.getHighlight().removeHighlight();
        addHighlight(relIdx, query.length());
        return true;
    }



    //
    private boolean isSetPointToVisible;
    //
    protected int pageIndex;
    // Relative to the paragraph start offset index
    protected int relativeParaIndex;
    // find the current offset
    //public long findCurrentOffset;
    // find the paragraph content;
    protected String findString;
    //
    protected String query;
    // find this paragraph element;
    protected IElement findElement;
    //
    protected Word word;
    //
    protected Rectangle rect;


}
