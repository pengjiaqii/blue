package com.example.bluetooth;

import java.util.List;
import java.util.UUID;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/6 15:06
 * 功能 :
 */
public class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;

    @Override
    public String toString() {
        return "BleAdvertisedData{" +
                "mUuids=" + mUuids +
                ", mName='" + mName + '\'' +
                '}';
    }

    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }

}
