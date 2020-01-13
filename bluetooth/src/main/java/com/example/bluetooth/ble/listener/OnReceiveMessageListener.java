package com.example.bluetooth.ble.listener;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/13
 * 功能 :接收消息的监听
 */
public interface OnReceiveMessageListener extends OnDetectResponseListener, IErrorListener, IConnectionLostListener {
    /**
     * call when have some response
     * @param s
     */
    void onNewLine(String s);
}
