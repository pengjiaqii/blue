package com.example.demo.util;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;


import com.example.demo.db.WhiteListEntity;
import com.example.demo.db.WhiteListUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("NewApi")
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
    private Context mContext;

    private ContentResolver mResolver;

    private String hhmmss = "";
    private String ddmmyy = "";

    private double latitude;
    private double longitude;

    String serialNum = "0123456789ABCDEF";

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

    public String handleCmdMessage(String message) {

        currentMusic = mAudioManager.getStreamVolume(3);
        currentCall = mAudioManager.getStreamVolume(0);

        mContext.registerReceiver(mReceiver, mFilter);

        if (message.contains("WT") && message.contains("SETSOS")) {
            return setSOSNumber(message);
        } else if (message.contains("WT") && message.contains("PBWL")) {
            return setWhiteListNumber(message);
        } else if (message.contains("WT") && message.contains("PBWLALL")) {
            return setAllWhiteListNumber(message);
        } else if (message.contains("WT") && message.contains("D1")) {
            return handlePositionMonitorD1(message);
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


        //        WhiteContactsUtil.addContact(mContext, contactEntity);
        //
        //        WhiteContactsUtil.getSOSContactsList(mContext);
        //WL-num字段：取值1-20，对应的编号位置1-3对应3个sos号码。4到6对应亲情号，对应123按键。剩下的序列号对应白名单
        if ("1".equals(wl_num) || "2".equals(wl_num) || "3".equals(wl_num)) {
            //sos号码

        }
        WhiteListUtil.getInstance(mContext).query(wl_num);
        WhiteListUtil.getInstance(mContext).queryAll();

        return "*WT," + serialNum + ",V4" + ",PBWL," + lastTime + "," + splitMeg[4] + "," + splitMeg[5] +
                "," + splitMeg[6] + "," + splitMeg[7] + "," + hhmmss + ",V," + latitude +
                "," + longitude + "," + ddmmyy + ",FFFFFFFD";
    }


    /**
     * 设置所有白名单白名单号码，20个
     *
     * @param message
     * @return
     */
    private String setAllWhiteListNumber(String message) {
        //*XX,MEID,PBWL,HHMMSS,WL-num,wl-phone,wl_type,wl_meid,wl_name,wl_pic#
        //*WT,MEID,PBWL,140522,WL-num,17665136602,wl_type,wl_meid,\u661f\u671f\u4e09,wl_pic#
        /**
         *WT,0000000000,PBWLALL,130305,1,13612345678,1,0,5C0F660E,1,2,13612345678,2,0,5C0F660E,1,3,13612345678,3,0,
         5C0F660E,1,4,13612345678,3,0,5C0F660E,1,5,13612345678,3,0,5C0F660E,1,6,13612345678,3,0,5C0F660E,1,7,13612345678,3,0,5C0F660E,1,8,13612345678,3,0,5C0F660E,1,9,13612345678,3,0,5C0F660E,1,10,13612345678,3,,,#
         */
        String[] splitMeg = message.split(",");
        String lastTime = splitMeg[3];
        for (int i = 0; i < splitMeg.length; i++) {
            Log.i(TAG, "meg-array：" + splitMeg[i]);
        }

        ArrayList<ContactEntity> contactEntities = new ArrayList<>();

        //4-9位是第一个人信息，10-15位是第二个人信息，16-21位是第三个人信息，依次类推，有20个人
        for (int i = 0; i < 20; i++) {
            ContactEntity contactEntity = new ContactEntity();
            String wl_num = splitMeg[4 + (6 * i)];
            String wl_phone = splitMeg[5 + (6 * i)];
            String wl_type = splitMeg[6 + (6 * i)];
            String wl_meid = splitMeg[7 + (6 * i)];
            String wl_name = splitMeg[8 + (6 * i)];
            String wl_pic = splitMeg[9 + (6 * i)];

            contactEntity.setWl_num(wl_num);
            contactEntity.setName(wl_name);
            contactEntity.setPhone(wl_phone);
            contactEntities.add(contactEntity);
        }

        Log.d(TAG, "contactEntities：" + contactEntities.size());

        return "*WT," + serialNum + ",V4" + ",PBWLALL," + lastTime + "," + splitMeg[4] + "," + splitMeg[5] +
                "," + splitMeg[6] + "," + splitMeg[7] + "," + hhmmss + ",V," + latitude +
                "," + longitude + "," + ddmmyy + ",FFFFFFFD";
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
        //        Settings.Global.putString(mContext.getContentResolver(), "sosphone1", "17665136602");
        //        String sosphone1 = Settings.Global.getString(mContext.getContentResolver(), "sosphone1");
        String[] splitMeg = message.split(",");
        for (int i = 0; i < splitMeg.length; i++) {
            Log.i(TAG, "meg-array：" + splitMeg[i]);
        }
        String lastTime = splitMeg[3];
        String sosPhone0 = splitMeg[6];
        String sosPhone1 = splitMeg[9];
        String sosPhone2 = splitMeg[12];

        Log.i(TAG, "sosPhone0：" + sosPhone0);
        Log.i(TAG, "sosPhone1：" + sosPhone1);
        Log.i(TAG, "sosPhone2：" + sosPhone2);


        SharedPreferences sp = mContext.getSharedPreferences("sos_phone", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("sosPhone0", sosPhone0);
        editor.putString("sosPhone1", sosPhone1);
        editor.putString("sosPhone2", sosPhone2);
        editor.apply();
        mHandler.post(loopTask);

        //putSOSNumber("number1", "17665136602");
        return "*WT," + serialNum + ",V4" + ",SETSOS," + lastTime + "," + hhmmss +
                "," + splitMeg[4] + "," + splitMeg[5] + "," + splitMeg[6] +
                "," + splitMeg[7] + "," + splitMeg[8] + "," + splitMeg[9] +
                "," + splitMeg[10] + "," + splitMeg[11] + "," + splitMeg[12] +
                ",V," + latitude + "," + longitude + "," + ddmmyy + ",FDFFFFFF";
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
        TelecomManager telecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            List<PhoneAccountHandle> phoneAccountHandleList = telecomManager.getCallCapablePhoneAccounts();
            PhoneAccountHandle phoneAccountHandle = telecomManager.getUserSelectedOutgoingPhoneAccount();
            if (phoneAccountHandleList != null && phoneAccountHandleList.size() >= 2 && phoneAccountHandle == null) {
                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandleList.get(0));
            } else {
                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
            }
        }
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
    private DeviceResponseUtil(Context context) {
        //获取一些基本信息
        hhmmss = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
        ddmmyy = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());

        latitude = LocationUtils.getInstance(context).getLatitude();
        longitude = LocationUtils.getInstance(context).getLongitude();

        mContext = context.getApplicationContext();
        mResolver = mContext.getContentResolver();

        mSubscriptionManager = (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mResolver = mContext.getContentResolver();
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();


        mAudioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
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
                        ArrayList<ContactEntity> contacts = WhiteContactsUtil.getContacts(mContext);
                        ArrayList<String> whitePhoneList = new ArrayList<>();
                        for (ContactEntity contact : contacts) {
                            whitePhoneList.add(contact.getPhone());
                        }
                        if (!whitePhoneList.contains(phoneNumber)) {
                            //挂断
                            //mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
                            //mTelecomManager.endCall();
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
        String lastTime = split[split.length - 1];

        return "*WT," + serialNum + ",V4" + ",D1," + lastTime + "," + hhmmss + ",A," + latitude + "," + longitude + "," + ddmmyy + ",FDFFFFFF";
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


    public static DeviceResponseUtil getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceResponseUtil.class) {
                if (instance == null) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    instance = new DeviceResponseUtil(context);
                }
            }

        }
        return instance;
    }

}
