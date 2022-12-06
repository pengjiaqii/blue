package com.example.demo.util;

public class ContactEntity {
    //白名单编号,取值1-20，展示的位置；取值1-20，对应的编号位置1-3对应3个sos号码。4到6对应亲情号，对应123按键。剩下的序列号对应白名单
    private String wl_num;
    private String name;
    private String phone;
    private String email;
    private String describe;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getWl_num() {
        return wl_num;
    }

    public void setWl_num(String wl_num) {
        this.wl_num = wl_num;
    }

    @Override
    public String toString() {
        return "ContactEntity{" +
                "wl_num='" + wl_num + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", describe='" + describe + '\'' +
                '}';
    }
}
