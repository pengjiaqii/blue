package com.example.demo.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.telecom.TelecomManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.example.demo.R;
import com.example.demo.db.WhiteListEntity;
import com.example.demo.db.WhiteListUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint({"NewApi", "MissingPermission"})
public class DeviceResponseUtil {

    private static final String TAG = "StudentCardService";

    private static final int MSG_ANSWERCALL = 6;
    private static final int MSG_BLUE = 2;
    private static final int MSG_CALL_ON_SPK = 7;
    private static final int MSG_ENDCALL = 5;
    private static final int MSG_NEXTCALL = 4;
    private static final int MSG_PLAYSOUND = 3;
    private static final int MSG_RED = 1;
    public static final int NOTIFICATION_ID = 2016;
    public static final int SOS_CALLWAIT_TIME = 20000;

    private static volatile DeviceResponseUtil instance;
    private final TelephonyManager telephonyManager;
    private final AudioManager mAudioManager;
    private final StudentCardService mService;
    private final SensorManager mSensorManager;
    private final BatteryManager batteryManager;
    private Context mContext;

    private ContentResolver mResolver;

    private String hhmmss = "";
    private String ddmmyy = "";

    private double latitude;
    private double longitude;

    String serialNum = "TEST0123456789ABCDEF";

    private SubscriptionManager mSubscriptionManager;

    private List<SubscriptionInfo> mSubInfoList;

    private static int mSosListIndex = 0;

    private static int loopTimes = 0;

    private boolean isCallActive = false;

    private String[] sosPhoneNumber = new String[3];

    boolean isIncallFlag = false;
    boolean isIncomcallFlag = false;

    private int currentCall;
    private int currentMusic;

    private static final String[] mNumberKeys = {"sos_number0", "sos_number1", "sos_number2"};


    IntentFilter mFilter = new IntentFilter("telephony.callacvive.screenoff");
    private ContentResolver resolver;

    private int lastSignal;
    private float steps;

    public String handleCmdMessage(String message) {
        serialNum = SystemProperties.get("ro.serialno", "0123456789ABCDEF");
        Log.i(TAG, "serialNum===>" + serialNum);
        //        String android_id=android.provider.Settings.System.getString(mContext.getContentResolver(), "android_id");
        //        Log.e(TAG, "android_id===>" + android_id);
        //        String barcode = SystemProperties.get("gsm.serial","barcode");
        //        Log.i(TAG, "barcode===>" + barcode);

        LocationUtils.getInstance(mContext).getLocation();

        hhmmss = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
        ddmmyy = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());

        latitude = LocationUtils.getInstance(mContext).getLatitude();
        longitude = LocationUtils.getInstance(mContext).getLongitude();

        currentMusic = mAudioManager.getStreamVolume(3);
        currentCall = mAudioManager.getStreamVolume(0);

        mContext.registerReceiver(mReceiver, mFilter);

        String deviceId = telephonyManager.getDeviceId();
        Log.e("IMEI", "deviceId===>" + deviceId);

        if (message.contains("WT") && message.contains("SETSOS")) {
            return setSOSNumber(message);
        } else if (message.contains("WT") && message.contains("PBWL")) {
            return setWhiteListNumber(message);
        } else if (message.contains("WT") && message.contains("PBWLALL")) {
            return setAllWhiteListNumber(message);
        } else if (message.contains("WT") && message.contains("D1")) {
            return handlePositionMonitorD1(message);
        } else if (message.contains("WT") && message.contains("APPDOWNLOAD")) {
            return appInstallSwitch(message);
        } else if (message.contains("WT") && message.contains("APPDISABLE")) {
            try {
                return appDisableSwitch(message);
            } catch (JSONException e) {
                e.printStackTrace();
                return "";
            }
        } else if (message.contains("V2SIGNAL")) {
            return generateV2signal(message);
        } else {
            return "";
        }
    }

    /**
     * 设置白名单号码
     *
     * @param message
     * @return
     */
    private String setWhiteListNumber(String message) {
        //*XX,MEID,PBWL,HHMMSS,WL-num,wl-phone,wl_type,wl_meid,wl_name,wl_pic#
        //*WT,MEID,PBWL,140522,WL-num,17665136602,wl_type,wl_meid,\u661f\u671f\u4e09,wl_pic#
        String[] splitMeg = message.split(",");
        String lastTime = splitMeg[3];
        for (int i = 0; i < splitMeg.length; i++) {
            Log.i(TAG, "meg-array：" + splitMeg[i]);
        }

        String wl_phone = splitMeg[5];
        String wl_num = splitMeg[4];
        String wl_name = unicodeDecode(splitMeg[8]);

        //        ContactEntity contactEntity = new ContactEntity();
        //        contactEntity.setWl_num(wl_num);
        //        contactEntity.setName(wl_name);
        //        contactEntity.setPhone(wl_phone);

        //insert data
        //        WhiteListDBOpenHelper helper = new WhiteListDBOpenHelper(mContext);
        //        SQLiteDatabase db = helper.getWritableDatabase();
        //        ContentValues values = new ContentValues();
        //        values.put("wl_name", wl_name);
        //        values.put("wl_phone", wl_phone);
        //        db.insert(WhiteListDBOpenHelper.DB_TABLE_NAME, null, values);
        //        db.close();
        insertWhiteList(wl_num, wl_name, wl_phone);

        //WL-num字段：取值1-20，对应的编号位置1-3对应3个sos号码。4到6对应亲情号，对应123按键。剩下的序列号对应白名单
        if ("1".equals(wl_num) || "2".equals(wl_num) || "3".equals(wl_num)) {
            //sos号码

        }
        WhiteListUtil.getInstance(mContext).query(wl_num);
        WhiteListUtil.getInstance(mContext).queryAll();

        String pbwlReturnMsg = "*WT," + serialNum + ",V4" + ",PBWL," + lastTime + "," + splitMeg[4] + "," + splitMeg[5] +
                "," + splitMeg[6] + "," + splitMeg[7] + "," + splitMeg[8] + "," + hhmmss + ",V," + latitude +
                "," + longitude + "," + ddmmyy + ",FFFFFFFD#";
        Log.i(TAG, " pbwlReturnMsg--->" + pbwlReturnMsg);
        return pbwlReturnMsg;
    }


    /**
     * 设置所有白名单白名单号码，20个
     *
     * @param message
     * @return
     */
    private String setAllWhiteListNumber(String message) {
        /**
         *WT,0000000000,PBWLALL,130305,1,13612345678,1,0,5C0F660E,1,2,13612345678,2,0,5C0F660E,1,3,13612345678,3,0,
         5C0F660E,1,4,13612345678,3,0,5C0F660E,1,5,13612345678,3,0,5C0F660E,1,6,13612345678,3,0,5C0F660E,1,7,13612345678,3,0,5C0F660E,1,8,13612345678,3,0,5C0F660E,1,9,13612345678,3,0,5C0F660E,1,10,13612345678,3,,,#
         */
        String[] splitMeg = message.split(",");
        String lastTime = splitMeg[3];
        for (int i = 0; i < splitMeg.length; i++) {
            Log.i(TAG, "meg-array：" + splitMeg[i]);
        }

        ArrayList<WhiteListEntity> whiteListEntities = new ArrayList<>();

        //4-9位是第一个人信息，10-15位是第二个人信息，16-21位是第三个人信息，依次类推，有20个人
        for (int i = 0; i < 20; i++) {
            WhiteListEntity entity = new WhiteListEntity();
            String wl_num = splitMeg[4 + (6 * i)];
            String wl_phone = splitMeg[5 + (6 * i)];
            String wl_type = splitMeg[6 + (6 * i)];
            String wl_meid = splitMeg[7 + (6 * i)];
            String wl_name = splitMeg[8 + (6 * i)];
            String wl_pic = splitMeg[9 + (6 * i)];

            entity.setWl_num(wl_num);
            entity.setWl_phone(wl_phone);
            entity.setWl_type(wl_type);
            entity.setWl_meid(wl_meid);
            entity.setWl_name(wl_name);
            entity.setWl_pic(wl_pic);
            whiteListEntities.add(entity);
        }

        Log.d(TAG, "whiteListEntities：" + whiteListEntities.size());
        for (WhiteListEntity entity : whiteListEntities) {
            Log.i(TAG, "WhiteListEntity：" + entity.toString());
        }

        String pbwlAllReturnMsg = "*WT," + serialNum + ",V4" + ",PBWLALL," + lastTime + "," + splitMeg[4] + "," + splitMeg[5] +
                "," + splitMeg[6] + "," + splitMeg[7] + "," + hhmmss + ",A," + latitude +
                "," + longitude + "," + ddmmyy + ",FFFFFFFF#";

        Log.i(TAG, " pbwlAllReturnMsg--->" + pbwlAllReturnMsg);

        return pbwlAllReturnMsg;
    }

    /**
     * 设置SOS号码
     *
     * @param message
     * @return
     */
    private String setSOSNumber(String message) {
        //        acquireUnLock();
        //*WT,IMEI,SETSOS,HHMMSS,sequence,name，phone，sequence1,name,phone,sequence2,name,phone#
        String[] splitMeg = message.split(",");
        for (int i = 0; i < splitMeg.length; i++) {
            Log.i(TAG, "meg-array：" + splitMeg[i]);
        }
        String lastTime = splitMeg[3];
        String wl_num0 = splitMeg[4];
        String wl_name0 = unicodeDecode(splitMeg[5]);
        String wl_phone0 = splitMeg[6];

        String wl_num1 = splitMeg[7];
        String wl_name1 = unicodeDecode(splitMeg[8]);
        String wl_phone1 = splitMeg[9];

        String wl_num2 = splitMeg[10];
        String wl_name2 = unicodeDecode(splitMeg[11]);
        String wl_phone2 = splitMeg[12];


        Log.i(TAG, "sosPhone0：" + wl_phone0);
        Log.i(TAG, "sosPhone1：" + wl_phone1);
        Log.i(TAG, "sosPhone2：" + wl_phone2);

        //白名单序号1,2,3对应的是sos的号码
        insertWhiteList(wl_num0, wl_name0, wl_phone0);
        insertWhiteList(wl_num1, wl_name1, wl_phone1);
        insertWhiteList(wl_num2, wl_name2, wl_phone2);


        String setSOSReturnMsg = "*WT," + serialNum + ",V4" + ",SETSOS," + lastTime + "," + hhmmss +
                "," + splitMeg[4] + "," + splitMeg[5] + "," + splitMeg[6] +
                "," + splitMeg[7] + "," + splitMeg[8] + "," + splitMeg[9] +
                "," + splitMeg[10] + "," + splitMeg[11] + "," + splitMeg[12] +
                ",V," + latitude + "," + longitude + "," + ddmmyy + ",FDFFFFFF#";
        Log.i(TAG, " setSOSReturnMeg--->" + setSOSReturnMsg);
        return setSOSReturnMsg;
    }

    /**
     * 往白名单数据库里面插入数据
     *
     * @param wl_num
     * @param wl_name
     * @param wl_phone
     */
    private void insertWhiteList(String wl_num, String wl_name, String wl_phone) {
        if (null == wl_phone || wl_phone.isEmpty()) {
            //删除这条数据
            WhiteListUtil.getInstance(mContext).delete(wl_num);
        } else {
            ArrayList<WhiteListEntity> entities = WhiteListUtil.getInstance(mContext).queryAll();
            entities.stream().forEach(
                    whiteListEntity -> {
                        Log.d(TAG, "WhiteListEntity：" + whiteListEntity.toString());
                        if (wl_num.equals(whiteListEntity.getWl_num())) {
                            //编号相同的时候避免重复也先删除
                            WhiteListUtil.getInstance(mContext).delete(wl_num);
                        }
                    }
            );
            //插入数据，编号，姓名，电话
            WhiteListUtil.getInstance(mContext).insert(wl_num, wl_name, wl_phone);
        }
    }

    private int call_index = 0;

    public void next_call() {
        for (int i = call_index; ; i++) {
            if (i < sosPhoneNumber.length) {
                if ((sosPhoneNumber[i].length() == 0) || !isSimOK())
                    continue;
                Log.i(TAG, "====next_call=SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
                call(sosPhoneNumber[i]);
                call_index = i + 1;
                if (call_index >= sosPhoneNumber.length) {
                    call_index = 0;
                }
            }
            return;
        }
    }

    @SuppressLint("MissingPermission")
    public void call(String telNum) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + telNum));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //        TelecomManager telecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        //        if (telecomManager != null) {
        //            List<PhoneAccountHandle> phoneAccountHandleList = telecomManager.getCallCapablePhoneAccounts();
        //            PhoneAccountHandle phoneAccountHandle = telecomManager.getUserSelectedOutgoingPhoneAccount();
        //            if (phoneAccountHandleList != null && phoneAccountHandleList.size() >= 2 && phoneAccountHandle == null) {
        //                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandleList.get(0));
        //            } else {
        //                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
        //            }
        //        }
        mContext.startActivity(intent);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PLAYSOUND:
                case MSG_NEXTCALL:
                    Log.i(TAG, "===go_playsound: handleMessage nextcall ==");
                    //                    play_waring_sound();
                    next_call();
                    break;
                case MSG_ENDCALL:
                    break;
                case MSG_ANSWERCALL:
                    //                    autoanswerRingingCall();
                    break;
                case MSG_CALL_ON_SPK:
                    setSpeekModle(true);
                    break;
                default:
                    break;

            }


        }
    };

    void setSpeekModle(boolean paramBoolean) {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        Log.i(TAG, "====setSpeekModle==begin=currentcallVoice=" + currentCall + ";;SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
        if ((!mAudioManager.isSpeakerphoneOn()) && (true == paramBoolean)) {
            mAudioManager.setSpeakerphoneOn(true);
            Log.i(TAG, "====setSpeekModle==OPEN=set=SP=");
            mAudioManager.setStreamVolume(0, mAudioManager.getStreamMaxVolume(0), 0);
        } else {
            Log.i(TAG, "====setSpeekModle==end=currentcallVoice=" + currentCall + ";;SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
            mAudioManager.setSpeakerphoneOn(false);
            Log.i(TAG, "====setSpeekModle==DOWN=set=SP=");
            mAudioManager.setStreamVolume(0, currentCall, 0);
        }
    }


    private Runnable loopTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "callSOSNumber");
            next_call();
        }
    };


    public boolean isSimOK() {
        Log.i(TAG, "SIM" + getActivatedCardID() + "===isSimOK====" + telephonyManager.getSimState(getActivatedCardID()));
        return 5 == telephonyManager.getSimState(getActivatedCardID());
    }

    private int getActivatedCardID() {
        return getDefaultSim();
    }

    public int getDefaultSim() {
        try {
            int i = ((Integer) Class.forName("android.telephony.TelephonyManager").getMethod("getSlotIndex", new Class[0]).invoke(telephonyManager, new Object[0])).intValue();
            Log.i(TAG, "getDefaultSim:" + i);
            return i;
        } catch (Exception e) {
            Log.e(TAG, "getDefaultSim " + e.toString());
            e.printStackTrace();
        }
        return 0;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context paramContext, Intent paramIntent) {
            Log.i(TAG, "===CDMA IN CALL ==onReceive=");

        }
    };

    private PowerManager.WakeLock wakeLock = null;
    private KeyguardManager.KeyguardLock keyguardLock = null;

    private void acquireUnLock() {
        if (wakeLock == null) {
            wakeLock = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE)).newWakeLock(6, getClass().getCanonicalName());
            wakeLock.acquire();
        }
        if (keyguardLock == null) {
            keyguardLock = ((KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE)).newKeyguardLock("keyguard");
            keyguardLock.disableKeyguard();
        }
    }

    private void releaseLock() {
        if ((wakeLock != null) && (wakeLock.isHeld())) {
            wakeLock.release();
            wakeLock = null;
        }
        if (keyguardLock != null) {
            keyguardLock.reenableKeyguard();
            keyguardLock = null;
        }
    }

    @SuppressLint("MissingPermission")
    private DeviceResponseUtil(Context context, StudentCardService studentCardService) {
        //获取一些基本信息

        mContext = context.getApplicationContext();
        mResolver = mContext.getContentResolver();

        mSubscriptionManager = (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();

        mService = studentCardService;

        mAudioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        MySensorEventListener mListener = new MySensorEventListener();
        mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL);

        batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        telephonyManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                Log.d(TAG, "响铃号码：" + phoneNumber);
                super.onCallStateChanged(state, phoneNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        //空闲
                        Log.i(TAG, "===CALL_STATE_IDLE===");
                        if (isIncomcallFlag) {
                            isIncomcallFlag = false;
                            Log.d(TAG, "===go_playsound: onCallStateChanged incoming to end ==");
                            //                            play_waring_sound();
                        } else {
                            isIncallFlag = false;
                            setSpeekModle(false);
                            Log.i(TAG, "===play_sound=2=");
                            //                            mHandler.sendEmptyMessageDelayed(MSG_NEXTCALL, 2000L);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        //接通
                        Log.d(TAG, "===CALL_STATE_OFFHOOK===");
                        isIncallFlag = true;
                        //sos 拨打接通之后 移除handler里面loopcall的任务，下次触发sos时再启动
                        mHandler.removeCallbacks(loopTask);
                        //                        mHandler.sendEmptyMessageDelayed(7, 2000L);

                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        //响铃
                        Log.d(TAG, "===CALL_STATE_RINGING===");
                        //1.不在白名单里面的号码响铃立即挂断
                        ArrayList<WhiteListEntity> listEntities = WhiteListUtil.getInstance(mContext).queryAll();
                        ArrayList<String> whitePhoneList = new ArrayList<>();
                        for (WhiteListEntity entity : listEntities) {
                            whitePhoneList.add(entity.getWl_phone());
                        }
                        if (whitePhoneList.isEmpty()) {
                            //白名单没有号码，所有号码都不管

                        } else {
                            if (!whitePhoneList.contains(phoneNumber)) {
                                //挂断
                                endCall();
                            }
                        }
                        isIncomcallFlag = true;
                        //mHandler.sendEmptyMessageDelayed(6, 2000L);
                        //输出来电号码
                        break;
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * 1.1 主动定位监控命令 D1
     *
     * @param message
     */
    private String handlePositionMonitorD1(String message) {
        String[] split = message.split(",");
        String splitRes = split[split.length - 1];
        String lastTime = splitRes.replace("#", "");
        String operator = telephonyManager.getNetworkOperator();
        int mcc = 0;
        int mnc = 0;
        if (!operator.isEmpty()) {
            mcc = Integer.parseInt(operator.substring(0, 3));
            mnc = Integer.parseInt(operator.substring(3));
        }

        int nPhoneType = telephonyManager.getPhoneType();
        CellLocation cel = telephonyManager.getCellLocation();
        int cid = 0;
        int lac = 0;
        if (cel != null) {
            if (nPhoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                //电信
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cel;
                cid = cdmaCellLocation.getBaseStationId();
                lac = cdmaCellLocation.getNetworkId();
            } else {
                //移动和联通
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cel;
                cid = gsmCellLocation.getCid();
                lac = gsmCellLocation.getLac();
            }
        }
        int dbm = 0;
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        if (cellInfoList != null) {
            for (CellInfo cellInfo : cellInfoList) {
                if (cellInfo instanceof CellInfoLte) {
                    //cast to CellInfoLte and call all the CellInfoLte methods you need
                    dbm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                    //                    int asu = ((CellInfoLte) cellInfo).getCellSignalStrength().getAsuLevel();
                    break;
                }
            }
        }


        Log.i(TAG, " MCC = " + mcc + " MNC = " + mnc + " LAC = " + lac + " CID = " + cid);

        // 获取邻区基站信息
        List<NeighboringCellInfo> infos = telephonyManager.getNeighboringCellInfo();
        StringBuffer sb = new StringBuffer("总数 : " + infos.size() + " ");
        for (NeighboringCellInfo info1 : infos) { // 根据邻区总数进行循环
            sb.append(" LAC : " + info1.getLac()); // 取出当前邻区的LAC
            sb.append(" CID : " + info1.getCid()); // 取出当前邻区的CID
            sb.append(" BSSS : " + (-113 + 2 * info1.getRssi()) + " "); // 获取邻区基站信号强度
        }

        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                int asu = signalStrength.getGsmSignalStrength();
                lastSignal = -113 + 2 * asu;
            }
        }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        //基站信息拼接
        String baseStation = mcc + "," + mnc + "," + "0" + "," + infos.size() +
                "," + lac + "," + cid + "," + dbm + "," + lastSignal;

        //电量
        int intBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        String d1ReturnMsg = "*WT," + serialNum + ",V4" + ",D1," + lastTime + "," + hhmmss + ",A," + latitude + "," + longitude +
                "," + baseStation + "," + steps + "," + intBattery + "," + ddmmyy + ",FDFFFFFF#";
        Log.i(TAG, " d1ReturnMsg--->" + d1ReturnMsg);

        return d1ReturnMsg;
    }

    /**
     * @param string
     * @return 转换之后的内容
     * @Title: unicodeDecode
     * @Description: unicode解码 将Unicode的编码转换为中文
     */
    public String unicodeDecode(String string) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(string);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            string = string.replace(matcher.group(1), ch + "");
        }
        return string;
    }

    private void endCall() {
        Log.i(TAG, "===endCall===");
        TelecomManager tm = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        if ((tm != null) && (tm.isInCall())) {
            tm.endCall();
        }
    }

    /**
     * 触发sos的时候，上传sos数据
     */
    @SuppressLint("MissingPermission")
    public void uploadSOSData() {
        String message = "";
        //*WT,866248053277321,SOS,152037,A,2250.2245,N,11391.6189,E,0.11,149,460,11,124968449,30501,5,fc:d7:33:2b:4f:5c,-50,06:1b:6d:c8:3f:85,-74,04:d7:a5:c2:0b:04,-74,52:6b:1c:20:3b:31,-76,d4:68:ba:05:1c:6b,-87,230721,FFFFDFFF#
        String operator = telephonyManager.getNetworkOperator();
        String baseStation = "";
        if (!operator.isEmpty()) {
            int mcc = Integer.parseInt(operator.substring(0, 3));
            int mnc = Integer.parseInt(operator.substring(3));

            int nPhoneType = telephonyManager.getPhoneType();
            CellLocation cel = telephonyManager.getCellLocation();
            int cid = 0;
            int lac = 0;
            if (nPhoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                //电信
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cel;
                cid = cdmaCellLocation.getBaseStationId();
                lac = cdmaCellLocation.getNetworkId();
            } else {
                //移动和联通
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cel;
                cid = gsmCellLocation.getCid();
                lac = gsmCellLocation.getLac();
            }
            int dbm = 0;
            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList != null) {
                for (CellInfo cellInfo : cellInfoList) {
                    if (cellInfo instanceof CellInfoLte) {
                        //cast to CellInfoLte and call all the CellInfoLte methods you need
                        dbm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                        //                    int asu = ((CellInfoLte) cellInfo).getCellSignalStrength().getAsuLevel();
                        break;
                    }
                }
            }


            Log.i(TAG, " MCC = " + mcc + " MNC = " + mnc + " LAC = " + lac + " CID = " + cid);

            // 获取邻区基站信息
            List<NeighboringCellInfo> infos = telephonyManager.getNeighboringCellInfo();
            StringBuffer sb = new StringBuffer("总数 : " + infos.size() + " ");
            for (NeighboringCellInfo info1 : infos) { // 根据邻区总数进行循环
                sb.append(" LAC : " + info1.getLac()); // 取出当前邻区的LAC
                sb.append(" CID : " + info1.getCid()); // 取出当前邻区的CID
                sb.append(" BSSS : " + (-113 + 2 * info1.getRssi()) + " "); // 获取邻区基站信号强度
            }


            //基站信息拼接
            baseStation = mcc + "," + mnc + "," + "0" + "," + infos.size() + "," + lac + "," + cid + "," + dbm;
        }

        message = "*WT," + serialNum + ",SOS," + hhmmss + ",A," + latitude + "," + longitude +
                "," + baseStation + "," + ddmmyy + ",FFFFFFFD";

        Log.i(TAG, " 获取邻区基站信息:" + baseStation);

        mService.sendMsg(message);
    }

    /**
     * 获取应用信息 并上传
     * *WT,imei,UPLOADAPP,HHMMSS,type,groupNum,appName,appType,package,DATE,STATUS#
     * appType:app类型 1:系统app,2应用市场app
     *
     * @param :1:全量上报,2:增加上报,3:删除上报
     * @return
     */
    public String uploadAppInfo(int type, List<AppInfoEntity> appInfoEntities) {
        //*WT,imei,UPLOADAPP,HHMMSS,type,groupNum,appName,package,DATE,STATUS#
        appInfoEntities.stream().forEach(new Consumer<AppInfoEntity>() {
            @Override
            public void accept(AppInfoEntity appInfoEntity) {
                Log.d(TAG, " AppInfoEntity--->" + appInfoEntity.toString());
            }
        });

        StringBuilder allAppInfo = new StringBuilder();
        for (AppInfoEntity info : appInfoEntities) {
            allAppInfo.append(info.getAppName()).append(",").append(info.getAppType())
                    .append(",").append(info.getPackageName());
        }

        String uploadAppInfoMsg = "*WT," + serialNum + ",UPLOADAPP," + hhmmss + "," + type +
                "," + appInfoEntities.size() + "," + allAppInfo + "," + ddmmyy + ",FDFFFFFF#";

        Log.i(TAG, " uploadAppInfoMsg--->" + uploadAppInfoMsg);
        return uploadAppInfoMsg;
    }

    /**
     * 指令关键字：APPDOWNLOAD
     * Switch:0-关闭 1-打开；
     */
    private String appInstallSwitch(String message) {
        //*WT,IMEI,APPDOWNLOAD,seq,switch,date,tracker_status#
        String[] split = message.split(",");
        String appInstallSwitch = split[4];
        String installSwitch = appInstallSwitch.replace("#", "");
        if ("0".equals(installSwitch)) {
            SystemProperties.set("persist.sys.app.install", "0");
        } else if ("1".equals(installSwitch)) {
            SystemProperties.set("persist.sys.app.install", "1");
        }

        String installSwitchReturnMsg = "*WT," + serialNum + "," + "APPDOWNLOAD" + "," + "seq"
                + "," + installSwitch + "," + ddmmyy + ",FFFDFFFF#";
        Log.i(TAG, " installSwitchReturnMsg--->" + installSwitchReturnMsg);
        return installSwitchReturnMsg;
    }

    /**
     * 指令关键字：APPDISABLE
     * Switch:0-关闭 1-打开(打开app禁用，则禁止所有时间段内禁用,关闭禁用,则可限制使用时间)
     */
    private String appDisableSwitch(String message) throws JSONException {
        //*WT,IMEI,APPDISABLE,seq,package,switch,groupNum,startTime,endTime,cycle,activation#
        String noSharp = message.replace("#", "");

        String[] split = noSharp.split(",");
        String appPackage = split[4];
        String appDisableSwitch = split[5];
        String groupNum = split[6];

        String appDisableJsonStr = generateDisableInfo(split, groupNum);
        SharedPreferenceUtil util = new SharedPreferenceUtil(mContext);
        util.putString("com.android.calendar", appDisableJsonStr);

        if ("0".equals(appDisableSwitch)) {
            SystemProperties.set("persist.sys.app.start", "0");
        } else if ("1".equals(appDisableSwitch)) {
            SystemProperties.set("persist.sys.app.start", "1");
        }
        String disableSwitchReturnMsg = "*WT," + serialNum + "," + "APPDISABLE" + "," + "seq"
                + "," + appPackage + "," + appDisableSwitch + "," + ddmmyy + ",FFFDFFFF#";
        Log.i(TAG, " disableSwitchReturnMsg--->" + disableSwitchReturnMsg);
        return disableSwitchReturnMsg;
    }

    /**
     * @param split
     * @param groupNum 最多下发三组
     * @return
     * @throws JSONException
     */
    private String generateDisableInfo(String[] split, String groupNum) throws JSONException {
        String appDisableJsonStr = "";
        JSONStringer appDisable = new JSONStringer();
        JSONStringer jsonStringer = appDisable.object();
        for (int i = 1; i <= Integer.parseInt(groupNum); i++) {
            jsonStringer.key("groupNum").value(groupNum)
                    .key("startTime" + i).value(split[7 + 4 * i])
                    .key("endTime" + i).value(split[8 + 4 * i])
                    .key("cycle" + i).value(split[9 + 4 * i])
                    .key("activation" + i).value(split[10 + 4 * i]);
        }
        appDisableJsonStr = jsonStringer.endObject().toString();


        SharedPreferenceUtil util = new SharedPreferenceUtil(mContext);
        String testString = util.getString("com.android.calendar");

        JSONObject jsonObject = new JSONObject(testString);
        if (jsonObject.length() != 0) {
            String groupNum = jsonObject.getString("groupNum");
            if(TextUtils.equals(groupNum,"1")){
                String startTime1 = jsonObject.getString("startTime1");
                String endTime1 = jsonObject.getString("endTime1");
                String cycle1 = jsonObject.getString("cycle1");
                String activation1 = jsonObject.getString("activation1");

            }else if(TextUtils.equals(groupNum,"2")){
                String startTime1 = jsonObject.getString("startTime1");
                String endTime1 = jsonObject.getString("endTime1");
                String cycle1 = jsonObject.getString("cycle1");
                String activation1 = jsonObject.getString("activation1");

                String startTime2 = jsonObject.getString("startTime2");
                String endTime2 = jsonObject.getString("endTime2");
                String cycle2 = jsonObject.getString("cycle2");
                String activation2 = jsonObject.getString("activation2");

            }else if(TextUtils.equals(groupNum,"3")){
                String startTime1 = jsonObject.getString("startTime1");
                String endTime1 = jsonObject.getString("endTime1");
                String cycle1 = jsonObject.getString("cycle1");
                String activation1 = jsonObject.getString("activation1");

                String startTime2 = jsonObject.getString("startTime2");
                String endTime2 = jsonObject.getString("endTime2");
                String cycle2 = jsonObject.getString("cycle2");
                String activation2 = jsonObject.getString("activation2");

                String startTime3 = jsonObject.getString("startTime3");
                String endTime3 = jsonObject.getString("endTime3");
                String cycle3 = jsonObject.getString("cycle3");
                String activation3 = jsonObject.getString("activation3");
            }
        }

        //        if (TextUtils.equals(groupNum, "1")) {
        //            String startTime1 = split[7];
        //            String endTime1 = split[8];
        //            String cycle1 = split[9];
        //            String activation1 = split[10];
        //
        //            JSONStringer appDisable = new JSONStringer();
        //            appDisableJsonStr = appDisable
        //                    .object()
        //                    .key("groupNum")
        //                    .value(groupNum)
        //                    .key("startTime1")
        //                    .value(startTime1)
        //                    .key("endTime1")
        //                    .value(endTime1)
        //                    .key("cycle1")
        //                    .value(cycle1)
        //                    .key("activation1")
        //                    .value(activation1)
        //                    .endObject()
        //                    .toString();
        //
        //
        //        } else if (TextUtils.equals(groupNum, "2")) {
        //            String startTime1 = split[7];
        //            String endTime1 = split[8];
        //            String cycle1 = split[9];
        //            String activation1 = split[10];
        //
        //            String startTime2 = split[11];
        //            String endTime2 = split[12];
        //            String cycle2 = split[13];
        //            String activation2 = split[14];
        //
        //            JSONStringer appDisable = new JSONStringer();
        //            appDisableJsonStr = appDisable
        //                    .object()
        //                    .key("groupNum")
        //                    .value(groupNum)
        //
        //                    .key("startTime1")
        //                    .value(startTime1)
        //                    .key("endTime1")
        //                    .value(endTime1)
        //                    .key("cycle1")
        //                    .value(cycle1)
        //                    .key("activation1")
        //                    .value(activation1)
        //
        //                    .key("startTime2")
        //                    .value(startTime2)
        //                    .key("endTime2")
        //                    .value(endTime2)
        //                    .key("cycle2")
        //                    .value(cycle2)
        //                    .key("activation2")
        //
        //                    .value(activation2)
        //                    .endObject()
        //                    .toString();
        //
        //
        //        } else if (TextUtils.equals(groupNum, "3")) {
        //            String startTime1 = split[7];
        //            String endTime1 = split[8];
        //            String cycle1 = split[9];
        //            String activation1 = split[10];
        //
        //            String startTime2 = split[11];
        //            String endTime2 = split[12];
        //            String cycle2 = split[13];
        //            String activation2 = split[14];
        //
        //            String startTime3 = split[15];
        //            String endTime3 = split[16];
        //            String cycle3 = split[17];
        //            String activation3 = split[18];
        //
        //            JSONStringer appDisable = new JSONStringer();
        //            appDisableJsonStr = appDisable
        //                    .object()
        //                    .key("groupNum")
        //                    .value(groupNum)
        //
        //                    .key("startTime1")
        //                    .value(startTime1)
        //                    .key("endTime1")
        //                    .value(endTime1)
        //                    .key("cycle1")
        //                    .value(cycle1)
        //                    .key("activation1")
        //                    .value(activation1)
        //
        //                    .key("startTime2")
        //                    .value(startTime2)
        //                    .key("endTime2")
        //                    .value(endTime2)
        //                    .key("cycle2")
        //                    .value(cycle2)
        //                    .key("activation2")
        //                    .value(activation2)
        //
        //                    .key("startTime3")
        //                    .value(startTime3)
        //                    .key("endTime3")
        //                    .value(endTime3)
        //                    .key("cycle3")
        //                    .value(cycle3)
        //                    .key("activation3")
        //                    .value(activation3)
        //
        //                    .endObject()
        //                    .toString();
        //
        //        }
        Log.d(TAG, " appDisableJsonStr--->" + appDisableJsonStr);
        return appDisableJsonStr;
    }


    //    protected boolean endCall() {
    //        boolean ret = false;
    //        // TODO
    //        try {
    //            ITelephony phone =
    //                    ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
    //            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    //
    //            for (int i = 0; i < 4; i++) {
    //                int[] subIds = SubscriptionManager.getSubId(i);
    //                if (subIds != null && subIds.length > 0 && subIds[0] != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
    //                    subId = subIds[0];
    //                    Log.i(TAG,"endCall: subId=" + subId + " from phoneId=" + i);
    //                    break;
    //                }
    //            }
    //
    //            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
    //                ret = phone.endCallForSubscriber(subId);
    //            } else {
    //                Log.i(TAG,"endCall: failed due to no valid subId, stop test directly!");
    //            }
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //        Log.i(TAG,"endCall ret=" + ret);
    //        return ret;
    //    }


    private class MySensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            steps = event.values[0];
            Log.i(TAG, "步数:" + steps);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }


    public static DeviceResponseUtil getInstance(Context context, StudentCardService studentCardService) {
        if (instance == null) {
            synchronized (DeviceResponseUtil.class) {
                if (instance == null) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    instance = new DeviceResponseUtil(context, studentCardService);
                }
            }

        }
        return instance;
    }


    /**
     * 生成v2数据
     *
     * @param message *WT,868976030203477,V2,151744,92,1,56,0,A,2250.2308,N,11391.6231,E,0.11,237,170721,FFFFDFFF#
     * @return
     */
    private String generateV2signal(String message) {
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                int asu = signalStrength.getGsmSignalStrength();
                lastSignal = -113 + 2 * asu;
            }
        }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        int gpsCount = LocationUtils.getInstance(mContext).getCurGpsStatus();
        //电量
        int intBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        String v2Signal = "*WT," + serialNum + ",V2" + "," + hhmmss + "," + lastSignal + "," + gpsCount + "," + intBattery +
                "," + steps + ",A, " + latitude + ",N," + longitude + ",E," + "," + ddmmyy + "," + "FDFFFFFF#";
        return v2Signal;
    }
}
