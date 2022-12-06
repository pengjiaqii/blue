package com.example.demo.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

public class WhiteListUtil {

    private static final String TAG = "StudentCardService";

    private static volatile WhiteListUtil instance;

    private final Context mContext;
    private final ContentResolver resolver;


    public void insert(String wl_num, String wl_name, String wl_phone) {
        Uri uri = Uri.parse("content://com.android.launcher3.contactprovider/whitelist");
        ContentValues values = new ContentValues();
        values.put("wl_num", wl_num);
        values.put("wl_name", wl_name);
        values.put("wl_phone", wl_phone);
        resolver.insert(uri, values);
    }

    public void delete(String wl_num) {
        Uri uri = Uri.parse("content://com.android.launcher3.contactprovider/whitelist");
        String where = "wl_num=?";
        String[] where_args = {wl_num};
        resolver.delete(uri, where, where_args);
    }


    public ArrayList<WhiteListEntity> query(String query_wl_num) {
        ArrayList<WhiteListEntity> whiteListEntities = new ArrayList<>();
        Uri uri = Uri.parse("content://com.android.launcher3.contactprovider/whitelist");
        String where = "wl_num=?";
        String[] where_args = {query_wl_num};
        Cursor cursor = resolver.query(uri, null, where, where_args, null);
        while (cursor.moveToNext()) {
            WhiteListEntity entity = new WhiteListEntity();
            String wl_num = cursor.getString(cursor.getColumnIndex("wl_num"));
            Log.i(TAG, "queryAll----->" + wl_num);
            entity.setWl_num(wl_num);

            String wl_name = cursor.getString(cursor.getColumnIndex("wl_name"));
            Log.i(TAG, "queryAll----->" + wl_name);
            entity.setWl_name(wl_name);

            String wl_phone = cursor.getString(cursor.getColumnIndex("wl_phone"));
            Log.i(TAG, "queryAll----->" + wl_phone);
            entity.setWl_phone(wl_phone);

            whiteListEntities.add(entity);
        }
        return whiteListEntities;
    }

    public ArrayList<WhiteListEntity> queryAll() {
        ArrayList<WhiteListEntity> whiteListEntities = new ArrayList<>();
        Uri uri = Uri.parse("content://com.android.launcher3.contactprovider/whitelist");
        Cursor cursor = resolver.query(uri, null, null, null, null);
        while (cursor.moveToNext()) {
            WhiteListEntity entity = new WhiteListEntity();
            String wl_num = cursor.getString(cursor.getColumnIndex("wl_num"));
            Log.i(TAG, "queryAll----->" + wl_num);
            entity.setWl_num(wl_num);

            String wl_name = cursor.getString(cursor.getColumnIndex("wl_name"));
            Log.i(TAG, "queryAll----->" + wl_name);
            entity.setWl_name(wl_name);

            String wl_phone = cursor.getString(cursor.getColumnIndex("wl_phone"));
            Log.i(TAG, "queryAll----->" + wl_phone);
            entity.setWl_phone(wl_phone);

            whiteListEntities.add(entity);
        }
        return whiteListEntities;
    }


    private void update(String wl_num, String wl_name, String wl_phone) {
        Uri uri = Uri.parse("content://com.android.launcher3.contactprovider/contacts");
        ContentValues values = new ContentValues();
        values.put("wl_num", wl_num);
        values.put("wl_name", wl_name);
        values.put("wl_phone", wl_phone);
        String where = "wl_num=?";
        String[] where_args = {wl_num};
        resolver.update(uri, values, where, where_args);
    }





    private WhiteListUtil(Context context) {
        mContext = context.getApplicationContext();
        resolver = context.getContentResolver();
    }

    public static WhiteListUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (WhiteListUtil.class) {
                if (instance == null) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    instance = new WhiteListUtil(context);
                }
            }

        }
        return instance;
    }
}
