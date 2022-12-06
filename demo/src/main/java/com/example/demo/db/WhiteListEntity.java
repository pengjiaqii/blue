package com.example.demo.db;

public class WhiteListEntity {
    private String wl_num;
    private String wl_name;
    private String wl_phone;

    public String getWl_num() {
        return wl_num;
    }

    public void setWl_num(String wl_num) {
        this.wl_num = wl_num;
    }

    public String getWl_name() {
        return wl_name;
    }

    public void setWl_name(String wl_name) {
        this.wl_name = wl_name;
    }

    public String getWl_phone() {
        return wl_phone;
    }

    public void setWl_phone(String wl_phone) {
        this.wl_phone = wl_phone;
    }

    @Override
    public String toString() {
        return "WhiteListEntity{" +
                "wl_num='" + wl_num + '\'' +
                ", wl_name='" + wl_name + '\'' +
                ", wl_phone='" + wl_phone + '\'' +
                '}';
    }
}
