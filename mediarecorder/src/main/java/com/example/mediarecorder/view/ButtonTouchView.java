package com.example.mediarecorder.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/16 13:42
 * 功能 :
 */
public class ButtonTouchView extends AppCompatButton {

    public ButtonTouchView(Context context) {
        super(context);
    }

    public ButtonTouchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonTouchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        return true;
    }
}
