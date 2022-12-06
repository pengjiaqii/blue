package com.example.demo;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.AlarmClock;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import com.example.demo.util.LocationUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HandleCmdUtil {

    private static final String TAG = HandleCmdUtil.class.getSimpleName();

    private volatile static HandleCmdUtil instance;
    private final Context mContext;

    private String hhmmss = "";
    private String ddmmyy = "";

    private double latitude;
    private double longitude;

    String serialNum = "0123456789ABCDEF";

    private HandleCmdUtil(Context context) {
        mContext = context.getApplicationContext();
    }

    public static HandleCmdUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (HandleCmdUtil.class) {
                if (instance == null) {
                    instance = new HandleCmdUtil(context);
                }
            }
        }
        return instance;
    }

    /**
     * 处理消息分发到对应的指令
     *
     * @param message
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public String handleCmdMessage(String message) {
        //获取一些基本信息
        hhmmss = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
        ddmmyy = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());

        latitude = LocationUtils.getInstance(mContext).getLatitude();
        longitude = LocationUtils.getInstance(mContext).getLongitude();

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String line1Number = telephonyManager.getLine1Number();
        //        String deviceId = telephonyManager.getDeviceId();
        CellLocation cellLocation = telephonyManager.getCellLocation();
        //        String simSerialNumber = telephonyManager.getSimSerialNumber();
        //        String subscriberId = telephonyManager.getSubscriberId();

        if (message.contains("WT") && message.contains("GETIMEID")) {
            return handleGetImeidCmd();
        } else if (message.contains("WT") && message.contains("D1")) {
            return handlePositionMonitorD1(message);
        } else if (message.contains("WT") && message.contains("GETPARAM")) {
            return handleDeviceParams(message);
        } else if (message.contains("WT") && message.contains("GETICCID")) {
            return handleSimIccid(message);
        } else if (message.contains("WT") && message.contains("ALARMCLOCK")) {
            return handleSetAlarm(message);
        } else if (message.contains("WT") && message.contains("DELCLOCK")) {
            return handleCancelAlarm(message);
        } else {
            return "";
        }
    }

    /**
     * 1.1 主动定位监控命令 D1
     *
     * @param message
     */
    private String handlePositionMonitorD1(String message) {
        String[] split = message.split(",");
        String lastTime = split[split.length - 1];

        return "*WT," + serialNum + ",V4" + ",D1," + lastTime + "," + hhmmss + ",A," + latitude + "," + longitude + "," + ddmmyy + ",FDFFFFFF";
    }

    /**
     * 1.2 获取设备参数
     *
     * @param message
     */
    private String handleDeviceParams(String message) {
        String[] split = message.split(",");
        String lastTime = split[split.length - 1];

        return "*WT," + serialNum + ",V4" + ",GETPARAM," + lastTime + ",ver:ANDROID," + ddmmyy + ",FFFFDFFF";
    }

    /**
     * 1.3 查询设备中的sim卡号码和iccid指令：GETICCID
     */
    private String handleSimIccid(String message) {
        String[] split = message.split(",");
        String lastTime = split[split.length - 1];

        return "*WT," + serialNum + ",V4" + ",GETICCID," + lastTime + "," + hhmmss + "," + ddmmyy + ",FFFFDFFF";
    }

    /**
     * 1.4 GETIMEID，获取设备机身码（IMEID）信息
     */
    private String handleGetImeidCmd() {

        return "*WT," + serialNum + ",GETIMEID," + hhmmss + ",A," + latitude + "," + longitude + "," + ddmmyy + ",FFFFFFFF";
    }

    /**
     * 2.1 设置临时监控中心 GPRS 服务器 IP 地址、监听端口号、报警设置 ，即时变更服务器指向 S23
     */
    private String handleSetIPAddress() {


        return "*WT," + serialNum + ",GETIMEID," + hhmmss + ",A," + latitude + "," + longitude + "," + ddmmyy + ",FFFFFFFF";
    }

    /**
     * 2.2 工作模式设置：MODE
     */
    private String handleSetWorkMode() {


        return "*WT," + serialNum + ",GETIMEID," + hhmmss + ",A," + latitude + "," + longitude + "," + ddmmyy + ",FFFFFFFF";
    }

    /**
     * 设置闹钟
     *
     * @param message
     * @return
     */
    private String handleSetAlarm(String message) {
        //*WT,IMEI,ALARMCLOCK,HHMMSS,sequence,HAHAHA,12:24,repeatTime,flag#

        String[] splitMeg = message.split(",");
        String lastTime = splitMeg[splitMeg.length - 1];
        String alarmName = splitMeg[5];
        String time = splitMeg[6];
        String[] hourMinutes = time.split(":");

        //获取日历对象
        //        Calendar calendar = Calendar.getInstance();
        ArrayList<Integer> alarmDays = new ArrayList<Integer>();
        alarmDays.add(Calendar.SATURDAY);
        //设置闹钟
        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, alarmName);
        alarmIntent.putExtra(AlarmClock.EXTRA_DAYS, alarmDays);
        alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(hourMinutes[0]));
        alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(hourMinutes[1]));
        alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(alarmIntent);


        return "*WT," + serialNum + ",V4" + ",ALARMCLOCK," + lastTime + "," + hhmmss + ",A," + latitude + "," + longitude + "," + ddmmyy + ",FDFFFFFF";
    }

    private String handleCancelAlarm(String message) {
        AlarmManager manager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);



        Intent alarmIntent = new Intent(AlarmClock.ACTION_DISMISS_ALARM);
        alarmIntent.putExtra(AlarmClock.ALARM_SEARCH_MODE_LABEL, "HAHAHA");
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(alarmIntent);


        return message;
    }

}
