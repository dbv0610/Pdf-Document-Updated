package com.wxiwei.office.officereader.beans;


import android.content.Context;
import android.widget.ListView;

public class SingleChoiceList extends ListView {
    /**
     * @param context
     * @param itemsResID
     */
    public SingleChoiceList(Context context, int itemsResID) {
        super(context);
//        setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//        String[] items = context.getResources().getStringArray(itemsResID);
//        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
//            context, R.layout.select_dialog_singlechoice, android.R.id.text1, items);
//        setAdapter(adapter);
    }

    /**
     *
     */
    public void dispose() {
    }
}
