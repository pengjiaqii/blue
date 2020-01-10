package com.example.bluet;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

public class BlueToothApplication extends Application {
    private static final Handler sHandler = new Handler();
    /**
     * 系统上下文
     */
    private static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();

    }

    /**
     * 获取系统上下文
     */
    public static Context getAppContext() {
        return mAppContext;
    }

    public static void runUi(Runnable runnable) {
        sHandler.post(runnable);
    }
}
