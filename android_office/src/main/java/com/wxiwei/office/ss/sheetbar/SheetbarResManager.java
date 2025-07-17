package com.wxiwei.office.ss.sheetbar;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.wxiwei.office.R;


/**
 * TODO: 文件注释
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            jqin
 * <p>
 * 日期:            2012-8-27
 * <p>
 * 负责人:           jqin
 * <p>
 * 负责小组:           
 * <p>
 * <p>
 */
public class SheetbarResManager
{
    public SheetbarResManager(Context context)
    {
        this.context = context;
        
        ClassLoader loader = context.getClassLoader();
        //sheetbar background
        sheetbarBG = new ColorDrawable(ContextCompat.getColor(context, R.color.color_background_sheet_tablayout));
        sheetbarLeftShadow = ContextCompat.getDrawable(context, R.drawable.img_office_ss_sheetbar_shadow_left);
        sheetbarRightShadow = ContextCompat.getDrawable(context, R.drawable.img_office_ss_sheetbar_shadow_right);
        hSeparator = ContextCompat.getDrawable(context, R.drawable.img_office_ss_sheetbar_separated_horizontal);
        //normalLeft = ContextCompat.getDrawable(context, R.drawable.ss_sheetbar_button_normal_left);
        //normalRight = ContextCompat.getDrawable(context, R.drawable.ss_sheetbar_button_normal_right);
        normalMiddle = new ColorDrawable(ContextCompat.getColor(context, R.color.color_background_button_sheet));
        //pushLeft = ContextCompat.getDrawable(context, R.drawable.ss_sheetbar_button_push_left);
        //pushMiddle = ContextCompat.getDrawable(context, R.drawable.ss_sheetbar_button_push_middle);
        //pushRight = ContextCompat.getDrawable(context, R.drawable.ss_sheetbar_button_push_right);
        //focusLeft = ContextCompat.getDrawable(context, R.drawable.ss_sheetbar_button_focus_left);
        focusMiddle = new ColorDrawable(ContextCompat.getColor(context, R.color.clr_excel));
        textSelectedTabColor = ContextCompat.getColor(context, R.color.white);
        textUnselectedTabColor = ContextCompat.getColor(context, R.color.color_unselected_text_button_sheet);

        //focusRight = ContextCompat.getDrawable(context, R.drawable.ss_sheetbar_button_focus_right);

//        sheetbarBG =  Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBAR_BG),
//            SheetbarResConstant.RESNAME_SHEETBAR_BG);
        
        //shadow
//        sheetbarLeftShadow= Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBAR_SHADOW_LEFT),
//            SheetbarResConstant.RESNAME_SHEETBAR_SHADOW_LEFT);
//
//        sheetbarRightShadow= Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBAR_SHADOW_RIGHT),
//            SheetbarResConstant.RESNAME_SHEETBAR_SHADOW_RIGHT);
//
//        //hSeparator
//        hSeparator = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBAR_SEPARATOR_H),
//            SheetbarResConstant.RESNAME_SHEETBAR_SEPARATOR_H);
//
//        //normal state
//        normalLeft = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_LEFT),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_LEFT);
//        normalRight = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_RIGHT),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_RIGHT);
//        normalMiddle = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_MIDDLE),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_MIDDLE);
//
//        //push state
//        pushLeft = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_PUSH_LEFT),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_PUSH_LEFT);
//        pushMiddle = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_PUSH_MIDDLE),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_PUSH_MIDDLE);
//        pushRight = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_PUSH_RIGHT),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_PUSH_RIGHT);
//
//        //focus state
//        focusLeft = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_FOCUS_LEFT),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_FOCUS_LEFT);
//        focusMiddle = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_FOCUS_MIDDLE),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_FOCUS_MIDDLE);
//        focusRight = Drawable.createFromResourceStream(context.getResources(), null,
//            loader.getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_FOCUS_RIGHT),
//            SheetbarResConstant.RESNAME_SHEETBUTTON_FOCUS_RIGHT);
        
    }

    public Integer getColor(int resId) {
        switch (resId) {
            case SheetbarResConstant.RESID_SHEET_BUTTON_SELECTED_TEXT_COLOR:
                return textSelectedTabColor;
            case SheetbarResConstant.RESID_SHEET_BUTTON_UNSELECTED_TEXT_COLOR:
                return textUnselectedTabColor;
        }

        return null;
    }

    public Drawable getDrawable(short resID)
    {
        switch(resID)
        {
            case SheetbarResConstant.RESID_SHEETBAR_BG:
                return sheetbarBG;
                
            case SheetbarResConstant.RESID_SHEETBAR_SHADOW_LEFT:
                return sheetbarLeftShadow;
                
            case SheetbarResConstant.RESID_SHEETBAR_SHADOW_RIGHT:
                return sheetbarRightShadow;
                
            case SheetbarResConstant.RESID_SHEETBAR_SEPARATOR_H:
                return hSeparator;
                
            /*case SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_LEFT:
                return normalLeft;*/
                
            case SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_MIDDLE:
                /*return Drawable.createFromResourceStream(context.getResources(), null,
                    context.getClassLoader().getResourceAsStream(SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_MIDDLE), 
                    SheetbarResConstant.RESNAME_SHEETBUTTON_NORMAL_MIDDLE);*/
                return normalMiddle;
                
            /*case SheetbarResConstant.RESID_SHEETBUTTON_NORMAL_RIGHT:
                return normalRight;
                
            case SheetbarResConstant.RESID_SHEETBUTTON_PUSH_LEFT:
                return pushLeft;*/
                
            /*case SheetbarResConstant.RESID_SHEETBUTTON_PUSH_MIDDLE:
                return pushMiddle;*/
                
            /*case SheetbarResConstant.RESID_SHEETBUTTON_PUSH_RIGHT:
                return pushRight;
                
            case SheetbarResConstant.RESID_SHEETBUTTON_FOCUS_LEFT:
                return focusLeft;*/
                
            case SheetbarResConstant.RESID_SHEETBUTTON_FOCUS_MIDDLE:
                return focusMiddle;
                
            /*case SheetbarResConstant.RESID_SHEETBUTTON_FOCUS_RIGHT:
                return focusRight;*/
        }
        
        return null;
    }
    
    public void dispose()
    {
        sheetbarBG = null;
        
        sheetbarLeftShadow = null;
        sheetbarRightShadow = null;
        
        hSeparator = null;
        
        //normalLeft = null;
        normalMiddle = null;
        //normalRight = null;
        
        //pushLeft = null;
        //pushMiddle =  null;
        //pushRight = null;
        
        //focusLeft = null;
        focusMiddle = null;
        //focusRight = null;
    }
    
    private Context context;
    private Drawable sheetbarBG;
    
    private Drawable sheetbarLeftShadow, sheetbarRightShadow;
    
    private Drawable hSeparator;
    //left
    /*private Drawable normalLeft;
    private Drawable pushLeft;
    private Drawable focusLeft;*/
    
    //middle
    private Drawable normalMiddle;
    //private Drawable pushMiddle;
    private Drawable focusMiddle;

    private int textSelectedTabColor;
    private int textUnselectedTabColor;
    //private int backgroundTabLayoutColor;
    //right
    /*private Drawable normalRight;
    private Drawable pushRight;
    private Drawable focusRight;*/
}
