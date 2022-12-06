package com.example.demo

import android.content.Context
import android.graphics.drawable.Drawable
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlin.jvm.Volatile
import com.example.demo.AlertDialogUtil

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/16 15:28
 * 功能 :
 */
object AlertDialogUtil {
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
    fun showNormalDialog(
        context: Context?,
        drawable: Drawable?,
        title: String?,
        message: String?,
        positiveText: String?,
        negativeText: String?,
        positiveClickListener: DialogInterface.OnClickListener?,
        negativeClickListener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(context!!)
        builder.setIcon(drawable)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveText, positiveClickListener)
        builder.setNegativeButton(negativeText, negativeClickListener)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    /**
     * 列表对话框
     *
     * @param context
     * @param title
     * @param items
     * @param itemsClickListener
     */
    fun showListDialog(
        context: Context?, title: String?, items: Array<String?>?, itemsClickListener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(title)
        builder.setItems(items, itemsClickListener)
        val alertDialog = builder.create()
        alertDialog.show()
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
    fun showSingleChoiceDialog(
        context: Context?,
        title: String?,
        items: Array<String?>?,
        checkedItem: Int,
        singleChoiceItemClickListener: DialogInterface.OnClickListener?,
        positiveClickListener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(title)
        builder.setSingleChoiceItems(items, checkedItem, singleChoiceItemClickListener)
        builder.setPositiveButton("确定", positiveClickListener)
        builder.setNegativeButton("取消") { dialog, which -> dialog.dismiss() }
        val alertDialog = builder.create()
        alertDialog.show()
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
    fun showMultiChoiceDialog(
        context: Context?,
        title: String?,
        items: Array<String?>?,
        checkedItems: BooleanArray?,
        multiChoiceItemsClickListener: OnMultiChoiceClickListener?,
        positiveText: String?,
        positiveClickListener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(title)
        builder.setMultiChoiceItems(items, checkedItems, multiChoiceItemsClickListener)
        builder.setPositiveButton(positiveText, positiveClickListener)
        builder.setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    /**
     * 自定义的对话框
     * @param context
     * @param view
     */
    fun showCustomDialog(context: Context?, view: View?) {
        val builder = AlertDialog.Builder(context!!)
        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.show()
    }

}