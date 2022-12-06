package com.example.demo.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class WhiteListProvider extends ContentProvider {
    //授权，和AndroidManifest.xml中的保持一致
    public static final String AUTHORITY = "com.android.launcher3.contactprovider";
    //匹配Uri的类
    private static UriMatcher uriMatcher;
    public static final String TAG = "StudentCardService";
    //数据改变后指定通知的Uri
    private static final Uri NOTIFY_URI = Uri.parse("content://" + AUTHORITY + "/whitelist");

    private WhiteListDBOpenHelper mDbOpenHelper;

    static final int WHITE_LIST = 1;
    static final int CONTACT_ID = 2;

    private SQLiteDatabase db;

    //初始化
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "whitelist", WHITE_LIST);
    }

    private Context mContext;

    public WhiteListProvider() {
        Log.e(TAG, "WhiteListProvider: ");
    }

    @Override
    public boolean onCreate() {
        Log.e(TAG, "WhiteListProvider: onCreate");
        mContext = getContext();
        mDbOpenHelper = new WhiteListDBOpenHelper(getContext());
        db = mDbOpenHelper.getWritableDatabase();
        return db != null;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.e(TAG, "WhiteListProvider: delete");
        int count = 0;
        if (uriMatcher.match(uri) == WHITE_LIST) {
            count = db.delete(WhiteListDBOpenHelper.DB_TABLE_NAME, selection, selectionArgs);
            //提示数据库的内容变化了
            if (count > 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // at the given URI.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.e(TAG, "WhiteListProvider: insert");
        if (uriMatcher.match(uri) == WHITE_LIST) {
            long rowId = db.insert(WhiteListDBOpenHelper.DB_TABLE_NAME, null, values);
            if (rowId > 0) {
                Uri insertedUri = ContentUris.withAppendedId(NOTIFY_URI, rowId);
                //提示数据库的内容变化了
                getContext().getContentResolver().notifyChange(insertedUri, null);
                return insertedUri;
            }
        }
        return uri;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.e(TAG, "WhiteListProvider: query");
        Cursor cursor = null;
        if (uriMatcher.match(uri) == WHITE_LIST) {
            cursor = db.query(WhiteListDBOpenHelper.DB_TABLE_NAME, projection, selection,
                    selectionArgs, null, null, sortOrder);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.e(TAG, "WhiteListProvider: update");
        int count = 0;
        if (uriMatcher.match(uri) == WHITE_LIST) {
             count = db.update(WhiteListDBOpenHelper.DB_TABLE_NAME, values, selection, selectionArgs);
            //提示数据库的内容变化了
            if (count > 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
        }
        return count;
    }

}