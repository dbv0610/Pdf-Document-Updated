package com.wxiwei.office.ss.sheetbar;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wxiwei.office.R;


public class SheetButton extends LinearLayout
{
    private static final int FONT_SIZE = 14;
    //
    private static final int SHEET_BUTTON_MIN_WIDTH = 100;
 
    /**
     * 
     * @param context
     */
    public SheetButton(Context context, String sheetName, int sheetIndex, SheetbarResManager sheetbarResManager)
    {
        super(context);
        setOrientation(HORIZONTAL);
        this.sheetIndex = sheetIndex;
        this.sheetbarResManager = sheetbarResManager;
        
        init(context, sheetName);
    }
    
    /**
     * 
     */
    private void init(Context context, String sheetName)
    {
        //左边图标
        /*left = new View(context);
        
        left.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_LEFT));
        addView(left);*/
        
        // 
        textView = new TextView(context);
        textView.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_MIDDLE));
        textView.setText(sheetName);
        textView.setTextSize(FONT_SIZE);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(getTextColorTab(false));

        int verticalPadding = context.getResources().getDimensionPixelSize(R.dimen.space_10);
        int horizontalPadding = context.getResources().getDimensionPixelSize(R.dimen.space_12);

        textView.setPadding(
           horizontalPadding,
           verticalPadding,
           horizontalPadding,
           verticalPadding
        );

        /*int w = (int)textView.getPaint().measureText(sheetName);
        w = Math.max(w, SHEET_BUTTON_MIN_WIDTH);*/
        textView.setMinWidth(SHEET_BUTTON_MIN_WIDTH);
        addView(textView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        
        // 右边图标
        /*right = new View(context);
        right.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_RIGHT));
        addView(right);*/
    }
    
    /**
     * 
     *
     */
    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getAction();
        switch (action)
        {
            /*case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if(!active)
                {
                    left.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_PUSH_LEFT));
                    textView.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_PUSH_MIDDLE));
                    right.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_PUSH_RIGHT));
                }                
                break;*/

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(!active)
                {
                    //left.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_LEFT));
                    textView.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_MIDDLE));
                    textView.setTextColor(getTextColorTab(false));
                    //right.setBackgroundDrawable(sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_RIGHT));
                }                
                break;
        }
        return super.onTouchEvent(event);
    }
    
    /**
     * 选中或取消选中用到
     */
    public void changeFocus(boolean gainFocus)
    {
        active = gainFocus;
        
        /*left.setBackgroundDrawable(gainFocus ? sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_FOCUS_LEFT) :
            sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_LEFT));*/
        textView.setBackgroundDrawable(gainFocus ? sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_FOCUS_MIDDLE) :
            sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_MIDDLE));


        textView.setTextColor(getTextColorTab(gainFocus));

        /*right.setBackgroundDrawable(gainFocus ? sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_FOCUS_RIGHT) :
            sheetbarResManager.getDrawable(SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_RIGHT));*/
    }

    private int getTextColorTab(boolean gainFocus) {
        Integer colorValue;

        if (gainFocus) {
            colorValue = sheetbarResManager.getColor(SheetbarResConstant.RESID_SHEET_BUTTON_SELECTED_TEXT_COLOR);
        } else {
            colorValue = sheetbarResManager.getColor(SheetbarResConstant.RESID_SHEET_BUTTON_UNSELECTED_TEXT_COLOR);
        }

        if (colorValue == null) {
            colorValue = Color.WHITE;
        }

        return colorValue;
    }

    /**
     * 
     */
    public int getSheetIndex()
    {
        return this.sheetIndex;
    }
    
    /**
     * 
     */
    public void dispose()
    {
        sheetbarResManager = null;
        
        left = null;
        textView = null;
        right = null;
    }
    
    private SheetbarResManager sheetbarResManager;
    //
    private int sheetIndex;;
    // 左边图标
    private View left;
    // 中间文本
    private TextView textView;
    // 右边图标
    private View right;
    
    private boolean active;
}
