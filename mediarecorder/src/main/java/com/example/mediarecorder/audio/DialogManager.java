package com.example.mediarecorder.audio;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.mediarecorder.R;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/16 13:42
 * 功能 :
 */
public class DialogManager {
    private volatile static DialogManager instance;
    private ImageView ivLoad;
    private TextView tvPrompt;


    public static DialogManager getInstance() {
        if (null == instance) {
            synchronized (DialogManager.class) {
                instance = new DialogManager();
            }
        }
        return instance;
    }

    public AlertDialog recordDialogShow(Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context, R.style.DialogManage);
        dialogBuilder.setCancelable(false);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_voice_speak, null);
        tvPrompt = view.findViewById(R.id.tv_prompt);
        ivLoad = view.findViewById(R.id.iv_load);
        dialogBuilder.setView(view);
        AlertDialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new BitmapDrawable());
        return dialog;
    }

    public void updateUI(int resId, String prompt) {
        if (null != tvPrompt && null != ivLoad) {
            tvPrompt.setText(prompt);
            ivLoad.setImageResource(resId);
        }
    }

}
