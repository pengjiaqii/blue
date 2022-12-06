package com.example.demo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WhiteListDBOpenHelper extends SQLiteOpenHelper {

    //数据库名
    private static final String DB_NAME = "whitelist.db";
    //表名
    public static final String DB_TABLE_NAME = "whitelist";
    //数据可版本号
    private static final int DB_VERSION = 1;

    public WhiteListDBOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建一张用户表
        String sql_create_user = "CREATE TABLE IF NOT EXISTS " +
                DB_TABLE_NAME + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, wl_num TEXT,wl_phone TEXT,wl_name TEXT)";
        db.execSQL(sql_create_user);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
