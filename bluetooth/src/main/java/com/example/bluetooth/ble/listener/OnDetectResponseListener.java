package com.example.bluetooth.ble.listener;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/13
 * 功能 :
 */
public interface OnDetectResponseListener {

    /**
     * call when blue have some reponse and need update progressbar .
     *
     * @param what
     * @param progress
     */
    void onProgressUpdate(String what, int progress);

    /**
     * call when blue have some detectreponse .
     *
     * @param response
     */
    void onDetectDataUpdate(String response);

    /**
     * call wen blue detectreponse is finish .
     */
    void onDetectDataFinish();

}
