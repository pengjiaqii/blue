package com.example.bluetooth.ble.listener;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/13
 * 功能 :与某个设备连接的监听
 */
public interface OnConnectListener extends IErrorListener {

    void onConnectStart();

    void onConnecting();

    void onConnectFailed();

    void onConnectSuccess(String mac);
}
