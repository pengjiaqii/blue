package com.example.demo.util;

public class AppInfoEntity {
    private String appName;
    private String packageName;
    //appType:app类型 1:系统app,2应用市场app
    private String appType;


    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    @Override
    public String toString() {
        return "AppInfoEntity{" +
                "appName='" + appName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", appType='" + appType + '\'' +
                '}';
    }
}
