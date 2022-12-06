package com.example.demo;

import java.io.Serializable;

public class Item implements Serializable {

    public Item() {
    }

    public void Item() {
    }

    public void Item(String s, String s1, String s2) {
        _id = s;
        name = s1;
        number = s2;
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public void setId(String s) {
        _id = s;
    }

    public void setName(String s) {
        name = s;
    }

    public void setNumber(String s) {
        number = s;
    }

    private String _id;
    private String name;
    private String number;
}

