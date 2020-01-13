package com.example.bluetooth.ble.listener;

import android.bluetooth.BluetoothDevice;

import com.example.bluetooth.ble.bean.SearchResult;

import java.util.List;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/13
 * 功能 :搜索蓝牙设备的监听
 */
public interface OnSearchDeviceListener extends IErrorListener {
    /**
     * Call before discovery devices.
     */
    void onStartDiscovery();

    /**
     * Call when found a new device.
     *
     * @param device the new device
     */
    void onNewDeviceFound(BluetoothDevice device);

    /**
     * Call when the discovery process completed.
     *
     * @param bondedList the remote devices those are bonded(paired).
     * @param newList    the remote devices those are not bonded(paired).
     */
    void onSearchCompleted(List<SearchResult> bondedList, List<SearchResult> newList);
}
