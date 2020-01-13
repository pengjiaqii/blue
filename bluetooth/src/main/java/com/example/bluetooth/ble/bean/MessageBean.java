package com.example.bluetooth.ble.bean;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/13
 * 功能 :
 */
public class MessageBean {
    public enum TYPE {
        STRING,
        CHAR,
        BYTE
    }

    public String text;
    public char[] data;
    public byte[] bytes;
    public TYPE mTYPE;

    public MessageBean(String text) {
        this.text = text;
        mTYPE = TYPE.STRING;
    }

    public MessageBean(char[] data) {
        this.data = data;
        mTYPE = TYPE.CHAR;
    }

    public MessageBean(byte[] bytes) {
        this.bytes = bytes;
        mTYPE = TYPE.BYTE;
    }
}
