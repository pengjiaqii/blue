package com.example.demo.util;

import android.content.Context;
import android.util.Log;

import com.example.demo.IStudentCardService;

public class CardTcpServiceManager {

    private static final String TAG = "StudentCardService";
    private IStudentCardService mService;

    public CardTcpServiceManager(Context context, IStudentCardService service){
        mService = service;
    }

    public void setVal(String key,String value){

        try {
            mService.setVal(key,value);
        } catch(Exception e){
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

    }

    public String getVal(String key){
        try {
            return mService.getVal(key);
        } catch(Exception e){
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return null;
    }

}
