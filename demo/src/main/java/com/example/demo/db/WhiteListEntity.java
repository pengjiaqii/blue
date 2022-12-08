package com.example.demo.db;

public class WhiteListEntity {
    private String wl_num;
    private String wl_name;
    private String wl_phone;
    private String wl_type;
    private String wl_meid;
    private String wl_pic;

    public String getWl_type() {
        return wl_type;
    }

    public void setWl_type(String wl_type) {
        this.wl_type = wl_type;
    }

    public String getWl_meid() {
        return wl_meid;
    }

    public void setWl_meid(String wl_meid) {
        this.wl_meid = wl_meid;
    }

    public String getWl_pic() {
        return wl_pic;
    }

    public void setWl_pic(String wl_pic) {
        this.wl_pic = wl_pic;
    }

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
                ", wl_type='" + wl_type + '\'' +
                ", wl_meid='" + wl_meid + '\'' +
                ", wl_pic='" + wl_pic + '\'' +
                '}';
    }
}
