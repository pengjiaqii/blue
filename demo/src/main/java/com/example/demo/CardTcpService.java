package com.example.demo;

import android.os.RemoteException;

import java.util.HashMap;

public class CardTcpService extends IStudentCardService.Stub{


    private static HashMap<String,String> mCache = null;
    private static final String TAG="StudentCardService";

    public CardTcpService() {
        mCache = new HashMap<>();

    }


    public void setVal(String key, String value) throws RemoteException {
        mCache.put(key, value);
    }

    public String getVal(String key) throws RemoteException {
        return mCache.get(key);
    }
}
