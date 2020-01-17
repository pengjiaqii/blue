package com.example.alertdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/16 15:28
 * 功能 :
 */
public class AlertDialogUtil {
    //单例
    private volatile static AlertDialogUtil instance = null;

    // 私有化构造方法
    private AlertDialogUtil() {

    }

    public static AlertDialogUtil getInstance() {
        if (instance == null) {
            synchronized (AlertDialogUtil.class) {
                if (instance == null) {
                    instance = new AlertDialogUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 普通的带两个按钮的对话框
     *
     * @param context
     * @param drawable
     * @param title
     * @param message
     * @param positiveText
     * @param negativeText
     * @param rightClickListener
     * @param leftClickListener
     */
    public void showNormalDialog(Context context, Drawable drawable, String title, String message, String positiveText,
                                 String negativeText, DialogInterface.OnClickListener positiveClickListener,
                                 DialogInterface.OnClickListener negativeClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(drawable);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, positiveClickListener);
        builder.setNegativeButton(negativeText, negativeClickListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }


    /**
     * 列表对话框
     *
     * @param context
     * @param title
     * @param items
     * @param itemsClickListener
     */
    public void showListDialog(Context context, String title, String[] items,
                               DialogInterface.OnClickListener itemsClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setItems(items, itemsClickListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }


    /**
     * 列表单选对话框
     *
     * @param context
     * @param title
     * @param items
     * @param checkedItem
     * @param singleChoiceItemClickListener
     * @param positiveClickListener
     */
    public void showSingleChoiceDialog(Context context, String title, String[] items, int checkedItem,
                                       DialogInterface.OnClickListener singleChoiceItemClickListener,
                                       DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setSingleChoiceItems(items, checkedItem, singleChoiceItemClickListener);
        builder.setPositiveButton("确定", positiveClickListener);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    /**
     *
     * @param context
     * @param title
     * @param items
     * @param checkedItems
     * @param multiChoiceItemsClickListener
     * @param positiveText
     * @param positiveClickListener
     */
    public void showMultiChoiceDialog(Context context, String title, String[] items, boolean[] checkedItems,
                         DialogInterface.OnMultiChoiceClickListener multiChoiceItemsClickListener,
                         String positiveText, DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMultiChoiceItems(items, checkedItems, multiChoiceItemsClickListener);
        builder.setPositiveButton(positiveText, positiveClickListener);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * 自定义的对话框
     * @param context
     * @param view
     */
    public void showCustomDialog(Context context, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
