//package com.example.demo.util;
//
//import android.app.AlarmManager;
//import android.app.AlertDialog;
//import android.app.AlertDialog.Builder;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.ContentResolver;
//import android.content.DialogInterface;
//import android.content.DialogInterface.OnClickListener;
//import android.content.DialogInterface.OnDismissListener;
//import android.content.SharedPreferences;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.database.ContentObserver;
//import android.media.AudioManager;
//import android.media.SoundPool;
//import android.net.Uri;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.telecom.PhoneAccountHandle;
//import android.telecom.TelecomManager;
//import android.telephony.PhoneStateListener;
//import android.telephony.SmsManager;
//import android.telephony.SubscriptionInfo;
//import android.telephony.SubscriptionManager;
//import android.telephony.PhoneNumberUtils;
//import android.telephony.TelephonyManager;
//import android.text.TextUtils;
//import android.util.Log;
//import android.widget.Toast;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//import android.provider.Settings;
//
//import com.example.demo.R;
//
//
//public class SOSBackgroundService extends Service {
//    static final String ACTION_MAKE_SOS_CALL = "ACTION_MAKE_SOS_CALL";
//    static final String ACTION_SEND_SMS = "ACTION_SEND_SMS";
//    static final String ACTION_SEND_SMS_DELIVER = "ACTION_SEND_SMS_DELIVER";
//    static final String ACTION_SEND_SOS_SMS = "ACTION_SEND_SOS_SMS";
//    static final String ACTION_SOS_START = "ACTION_SOS_START";
//    static final String ACTION_SOS_STOP = "ACTION_SOS_STOP";
//    static final String ACTION_SOS_SOUND_STOP = "ACTION_SOS_SOUND_STOP";
//    static final String CALL_ALIVE_ACTION = "ACTION_CALL_ALIVE";
//    private static final int DEFAULT_CALL_DELAY_TIME = 10000;
//    private static final int DEFAULT_CALL_WAITING_DELAY_TIME = 60000;
//    private static final int DEFAULT_SMS_DELAY_TIME = 5000;
//    static final int MSG_TYPE_NEGATIVE = -100;
//    static final int MSG_TYPE_START_TIMER = -101;
//    static final String SOS_CALL_WAITING_ACTION = "ACTION_SOS_CALL_WAITING";
//    static final String SOS_SOUND_ALERT = "SOS_SOUND_ALERT";
//    static final String TAG = "SOS/Service";
//    static AudioManager mAudioManager;
//    private static boolean mSosCallDialing = false;
//    private static String mSosCallNumber = null;
//    private static int mSosListIndex = 0;
//    private boolean bSosMakeCall = false;
//    private boolean isCalling = false;
//    private boolean bSosSoundPlaying = false;
//    private boolean mSOSStarting = false;
//    private AlarmManager mAM;
//    private DbHelper mDBHelper = null;
//    private AlertDialog mDialog;
//    private String mLocation;
//    private LocationHelper mLocationHelper;
//    private PendingIntent mPendingIntent;
//    private PhoneListener mPhoneStateListener[];
//    private SOSReceiver mSosReceiver;
//    private int mSteamId = -1;
//    private ArrayList sosItemList = null;
//    HashMap soundMap = new HashMap();
//    SoundPool soundPool;
//    private static final String empty_number = "";
//
//    private ContentResolver mResolver;
//    private SubscriptionManager mSubscriptionManager;
//    private List<SubscriptionInfo> mSubInfoList;
//    private boolean isCallActive = false;
//    private Handler handler = new Handler();
//
//    public static final int NEW = 0;
//    public static final int CONNECTING = 1;
//    public static final int SELECT_PHONE_ACCOUNT = 2;
//    public static final int DIALING = 3;
//    public static final int RINGING = 4;
//    public static final int ACTIVE = 5;
//    public static final int ON_HOLD = 6;
//    public static final int DISCONNECTED = 7;
//    public static final int ABORTED = 8;
//    public static final int DISCONNECTING = 9;
//
//    private LocationHelper.ReceivedCallback mLocationCallback = new LocationHelper.ReceivedCallback() {
//        public void onReceived(int i, String s) {
//            if (i == 0) {
//                mLocation = " " + s;
//                Log.d("loop_call", "mLocation =  " + mLocation);
//                if (mDialog != null) {
//                    mDialog.dismiss();
//                }
//                mLocationHelper.stopGetLocation();
//            }
//        }
//    };
//
//
//    private Handler dialogHandler = new Handler() {
//        private int count = 60;
//
//        public void handleMessage(Message message) {
//            switch (message.what) {
//                default:
//                    return;
//                case -100:
//                    int i = count;
//                    count = i - 1;
//                    if (i > 0) {
//                        if (mDialog != null)
//                            mDialog.getButton(-2).setText(getString(android.R.string.cancel) + "(" + count + ")");
//                        sendEmptyMessageDelayed(-100, 1000L);
//                    } else {
//                        mDialog.dismiss();
//                        mLocationHelper.stopGetLocation();
//                    }
//                    return;
//                case -101:
//                    count = 60;
//                    sendEmptyMessageDelayed(-100, 1000L);
//                    return;
//            }
//        }
//    };
//
//    public SOSBackgroundService() {
//
//    }
//
//    public void onCreate() {
//        super.onCreate();
//        mAM = (AlarmManager) getSystemService("alarm");
//        mAudioManager = (AudioManager) getSystemService("audio");
//        mLocationHelper = new LocationHelper(this);
//        Log.d(TAG, "@@@@@@@@@@@onCreate");
//        initSound();
//        mSosReceiver = new SOSReceiver();
//        IntentFilter intentfilter = new IntentFilter();
//        intentfilter.addAction(ACTION_SOS_START);
//        intentfilter.addAction(ACTION_SOS_STOP);
//        intentfilter.addAction(ACTION_MAKE_SOS_CALL);
//        intentfilter.addAction(ACTION_SEND_SOS_SMS);
//        intentfilter.addAction(ACTION_SOS_SOUND_STOP);
//        intentfilter.addAction(SOS_CALL_WAITING_ACTION);
//        intentfilter.addAction(CALL_ALIVE_ACTION);
//        intentfilter.addAction(ACTION_SEND_SMS);
//        intentfilter.addAction(ACTION_SEND_SMS_DELIVER);
//        intentfilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
//        intentfilter.addAction("com.android.intent.fastcall");
//        registerReceiver(mSosReceiver, intentfilter);
//        mDBHelper = new DbHelper(this);
//
//        //qyl add loop
//        mSubscriptionManager = (SubscriptionManager) this.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//        mResolver = getContentResolver();
//        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
//        mResolver.registerContentObserver(Settings.Global.getUriFor("mCallState"), true, mCallStateObserver);
//        //qyl end
//
//    }
//
//    //qyl add loop
//    private ContentObserver mCallStateObserver = new ContentObserver(new Handler()) {
//        public void onChange(boolean selfChange) {
//            int mCallState = Settings.Global.getInt(mResolver, "mCallState", 0);
//            int mSosState = getActiveSOSState();
//            Log.d("loop_call", "mCallStateObserver mCallState = " + mCallState);
//            Log.d("loop_call", "mCallStateObserver mSosState = " + mSosState);
//            if (mCallState == ACTIVE) {
//                if (sosItemList != null) {
//                    mSosListIndex = sosItemList.size();
//                } else {
//                    if (mDBHelper != null) {
//                        sosItemList = mDBHelper.getItemList();
//                        mSosListIndex = sosItemList.size();
//                    }
//                }
//                isCallActive = true;
//                handler.removeCallbacks(stopService);
//                handler.postDelayed(stopService, 200);
//            } else if (mCallState == DISCONNECTED) {
//                int count = 0;
//                if (sosItemList != null) {
//                    count = sosItemList.size();
//                } else {
//                    if (mDBHelper != null) {
//                        sosItemList = mDBHelper.getItemList();
//                        count = sosItemList.size();
//                    }
//                }
//				/*if(mSosListIndex >= count) {
//					handler.removeCallbacks(stopService);
//					handler.postDelayed(stopService, 200);
//				} else*/
//                if (mSosState == 1) {
//                    if (mSosListIndex >= count) {
//                        resetCallNumber();
//                    }
//                    handler.removeCallbacks(loopTask);
//                    handler.postDelayed(loopTask, 2000);
//                } else if (mSosState == 0 && mSOSStarting) {
//                    sosSendSms();
//                    handler.removeCallbacks(stopService);
//                    handler.postDelayed(stopService, 0);
//                }
//            } else if (mCallState == RINGING || mCallState == DIALING) {
//                //stopSosSound();
//            }
//        }
//    };
//
//    private Runnable loopTask = new Runnable() {
//        @Override
//        public void run() {
//            Log.d("loop_call", "doCallLoop");
//            doCallLoop();
//        }
//    };
//
//    private Runnable stopService = new Runnable() {
//        @Override
//        public void run() {
//            stopSpeedSMSService();
//        }
//    };
//
//    private void stopSpeedSMSService() {
//        Log.d("loop_call", "stopSpeedSMSService");
//        if (handler != null) {
//            handler.removeCallbacks(loopTask);
//        }
//        turnOffSpeaker();
//        unRegistetPhoneListener();
//        Log.d("loop_call", "stopSpeedSMSService setActiveSOSState(0)");
//        setActiveSOSState(0);
//        bSosMakeCall = false;
//        mSosCallDialing = false;
//        mSOSStarting = false;
//		/*if(mResolver != null && mCallStateObserver != null) {
//			mResolver.unregisterContentObserver(mCallStateObserver);
//		}*/
//        //stopSelf();
//    }
//
//
//    public void setActiveSOSState(int activeSate) {
//        Settings.Global.putInt(mResolver, "active_sos_loop", activeSate);
//    }
//
//    public int getActiveSOSState() {
//        int sosState = Settings.Global.getInt(mResolver, "active_sos_loop", 0);
//        return sosState;
//    }
//
//    public PhoneAccountHandle getPhoneAccountBySlotId(Context context, int slotid) {
//        if (mSubInfoList != null) {
//            final int subInfoLength = mSubInfoList.size();
//
//            for (int i = 0; i < subInfoLength; ++i) {
//                final SubscriptionInfo sir = mSubInfoList.get(i);
//                if (sir.getSimSlotIndex() == slotid) {
//                    int subid = sir.getSubscriptionId();
//
//                    return getPhoneAccountById(context, Integer.toString(subid));
//                }
//            }
//        }
//
//        return null;
//    }
//
//    public PhoneAccountHandle getPhoneAccountById(Context context, String id) {
//        if (!TextUtils.isEmpty(id)) {
//            final TelecomManager telecomManager = (TelecomManager) context
//                    .getSystemService(Context.TELECOM_SERVICE);
//            final List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
//            for (PhoneAccountHandle account : accounts) {
//                if (id.equals(account.getId())) {
//                    return account;
//                }
//            }
//        }
//        return null;
//    }
//    //qyl end
//
//    private void StartSosCallWaitingTimer() {
//        long now = System.currentTimeMillis();
//        mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(SOS_CALL_WAITING_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
//        mAM.set(0, 60000L + now, mPendingIntent);
//        Log.d(TAG, "Start Sos Call Waiting Timer");
//    }
//
//    private void cancelTimer() {
//        if (mPendingIntent != null) {
//            Log.d(TAG, "Cancel Timer,PendingIntent=" + mPendingIntent.toString());
//            mAM.cancel(mPendingIntent);
//            mPendingIntent = null;
//        }
//    }
//
//    private void getCallnumber() {
//        if (sosItemList != null && sosItemList.size() > 0) {
//            mSosListIndex = 1 + mSosListIndex;
//            if (mSosListIndex < sosItemList.size()) {
//                mSosCallNumber = ((Item) sosItemList.get(mSosListIndex)).getNumber();
//                return;
//            }
//        }
//
//        resetCallNumber();
//    }
//
//    private int getSoundId(String s) {
//        if (soundMap != null && soundMap.containsKey(s)) {
//            return ((Integer) soundMap.get(s)).intValue();
//        } else {
//            return -1;
//        }
//    }
//
//    private void initSound() {
//        soundPool = new SoundPool(10, 1, 5);
//        soundMap.put("SOS_SOUND_ALERT", Integer.valueOf(soundPool.load(this, R.raw.sos, 1)));
//    }
//
//    private boolean isSpeakerOn() {
//        boolean flag = mAudioManager.isSpeakerphoneOn();
//        Log.d(TAG, "**************** Speaker state =" + flag);
//        return flag;
//    }
//
//    private void notificateUser(int i) {
//        Toast.makeText(this, getResources().getText(i).toString(), 1).show();
//    }
//
//    private void notificateUser(String s) {
//        Toast.makeText(this, s, 1).show();
//    }
//
//
//    private void registetPhoneListener() {
//        SubscriptionManager mSubscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
//        if (subInfoList == null) {
//            return;
//        }
//        TelephonyManager telephonymanager = (TelephonyManager) getSystemService("phone");
//
//        int size = subInfoList.size();
//        mPhoneStateListener = new PhoneListener[size];
//        int index = 0;
//        for (final SubscriptionInfo record : subInfoList) {
//            mPhoneStateListener[index] = new PhoneListener();
//            telephonymanager.listen(mPhoneStateListener[index], PhoneStateListener.LISTEN_CALL_STATE);
//            index++;
//        }
//    }
//
//    private void resetCallNumber() {
//        mSosCallNumber = null;
//        mSosListIndex = 0;
//    }
//
//    private void showSmsDialog() {
//        mDialog = (new Builder(this)).setMessage(R.string.sms_dialog_msg).setNegativeButton(android.R.string.cancel,
//                new OnClickListener() {
//                    public void onClick(DialogInterface dialoginterface, int i) {
//                        //
//                    }
//                }).setCancelable(false).setOnDismissListener(
//                new OnDismissListener() {
//                    public void onDismiss(DialogInterface dialoginterface) {
//                        mLocationHelper.stopGetLocation();
//                    }
//                }).create();
//
//        mDialog.getWindow().setType(2003);
//        mDialog.show();
//        dialogHandler.sendEmptyMessageDelayed(-101, 1000L);
//    }
//
//    private boolean sosMakeCall() {
//        getCallnumber();
//        Log.d(TAG, "start making call,number=" + mSosCallNumber);
//        String s = mSosCallNumber;
//        boolean flag = false;
//        if (s != null) {
//            turnOnSpeaker();
//            flag = true;
//            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mSosCallNumber));
//            intent.putExtra("is_sos_call", true);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//            Log.d(TAG, "making call successed!");
//        }
//        return flag;
//    }
//
//    private boolean doCallLoop() {
//        boolean isSuccess = false;
//        int count = sosItemList.size();
//        String number = empty_number;
//
//
//        SharedPreferences sp = getSharedPreferences("sos_config", 0);
//        boolean isCallEnable = sp.getBoolean("is_sos_call_enable", true);
//
//        Log.d("loop_call", "doCallLoop count = " + count);
//        Log.d("loop_call", "doCallLoop isCallEnable = " + isCallEnable);
//
//        boolean isAirplaneModeOn = false;
//        if (Settings.Global.getInt(mResolver,
//                Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
//            isAirplaneModeOn = true;
//        }
//
//        if (!isCallEnable) {
//            handler.removeCallbacks(stopService);
//            handler.postDelayed(stopService, 0);
//            return false;
//        }
//
//        if (isAirplaneModeOn) {
//            //飞行模式启动啦
//            handler.removeCallbacks(stopService);
//            handler.postDelayed(stopService, 0);
//            return false;
//        }
//
//
//        if (count <= 0) {
//            handler.removeCallbacks(stopService);
//            handler.postDelayed(stopService, 0);
//            return false;
//        }
//
//        for (int i = 0; i < count; i++) {
//            String num = ((Item) sosItemList.get(i)).getNumber();
//            if (!num.equals(empty_number)) {
//                isSuccess = true;
//            }
//        }
//
//        Log.d("loop_call", "doCallLoop isSuccess = " + isSuccess);
//
//        if (!isSuccess) {
//            notificateUser(R.string.str_make_call_fail_hint);
//            handler.removeCallbacks(stopService);
//            handler.postDelayed(stopService, 0);
//            return false;
//        }
//
//        Log.d("loop_call", "doCallLoop mSosListIndex = " + mSosListIndex);
//
//        if (mSosListIndex < count) {
//            number = ((Item) sosItemList.get(mSosListIndex)).getNumber();
//        } else {
//            handler.removeCallbacks(stopService);
//            handler.postDelayed(stopService, 0);
//            return false;
//        }
//
//        Log.d("loop_call", "doCallLoop number = " + number);
//        handler.removeCallbacks(loopTask);
//
//        if (number.equals(empty_number)) {
//            mSosListIndex++;
//
//            doCallLoop();
//            return false;
//        }
//
//        int mSIMCount = mSubInfoList != null ? mSubInfoList.size() : 0;
//
//        final boolean isPotentialEmergencyNumber =
//                (number != null) && PhoneNumberUtils.isPotentialLocalEmergencyNumber(this, number);
//
//
//        turnOnSpeaker();
//        Intent intent = new Intent();
//        if (isPotentialEmergencyNumber) {
//            intent.setAction(Intent.ACTION_CALL_EMERGENCY);
//        } else {
//            intent.setAction(Intent.ACTION_CALL);
//        }
//        intent.setData(Uri.parse("tel:" + number));
//
//        if (mSIMCount >= 2) {
//            PhoneAccountHandle account = getPhoneAccountBySlotId(this, 0);
//            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account);
//        }
//
//        if (mSIMCount <= 0) {
//            Toast.makeText(this, R.string.callFailed_simError, Toast.LENGTH_SHORT).show();
//            handler.removeCallbacks(stopService);
//            handler.postDelayed(stopService, 0);
//            return false;
//        } else {
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//        }
//        isCalling = true;
//
//        mSosListIndex++;
//
//        return isSuccess;
//    }
//
//    private void sosSendSms() {
//        //String message = mDBHelper.getSMSContent("0");
//        SharedPreferences sp = getSharedPreferences("sos_config", 0);
//        String message = sp.getString("sos_message", getString(R.string.default_sos_msg));
//        if (true /*DbHelper.isInsertLocation(this)*/) {
//            if (mLocation == null || "".equals(mLocation)) {
//                mLocation = getString(R.string.str_can_not_get_location);
//            }
//
//            message = message + mLocation;
//        }
//
//        SubscriptionManager mSubscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
//        if (subInfoList == null) {
//            return;
//        }
//
//        if (message == null || "".equals(message)) {
//            notificateUser(R.string.str_set_sms_content);
//        } else if (sosItemList.size() > 0) {
//            Log.d(TAG, "Start send sms ......");
//            for (int i = 0; i < sosItemList.size(); i++) {
//                String phoneNumber = ((Item) sosItemList.get(i)).getNumber();
//                String name = ((Item) sosItemList.get(i)).getName();
//
//                if (subInfoList.size() > 0) {
//                    final SubscriptionInfo sir = subInfoList.get(0);
//                    int subid = sir.getSubscriptionId();
//
//                    Log.d("loop_call", "sir =  " + sir);
//                    Log.d("loop_call", "subid =  " + subid);
//
//                    //SmsManager smsmanager = SmsManager.getDefault();
//                    SmsManager smsmanager = SmsManager.getSmsManagerForSubscriptionId(subid);
//
//                    PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SEND_SMS), 0);
//
//                    Intent intent = new Intent(ACTION_SEND_SMS_DELIVER);
//                    if (name == null || "".equals(name)) {
//                        intent.putExtra("NAME", "<" + phoneNumber + ">");
//                    } else {
//                        intent.putExtra("NAME", "<" + name + ">");
//                    }
//                    PendingIntent deliveryIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
//
//
//                    if (smsmanager == null) {
//                        Log.d("loop_call", "send sms failed smsmanager get error!s");
//                        return;
//                    }
//
//                    if (message.length() > 70) {
//                        ArrayList<String> msgs = smsmanager.divideMessage(message);
//                        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
//                        ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();
//                        for (int j = 0; j < msgs.size(); j++) {
//                            sentIntents.add(sentIntent);
//                            deliveryIntents.add(deliveryIntent);
//                        }
//                        smsmanager.sendMultipartTextMessage(phoneNumber, null, msgs, sentIntents, deliveryIntents);
//                    } else {
//                        smsmanager.sendTextMessage(phoneNumber, null, message, sentIntent, deliveryIntent);
//                    }
//
//                }
//
//                Log.d("loop_call", "send sms num." + i + " ,dest addr=" + phoneNumber);
//            }
//            Log.d("loop_call", "send sms complete.");
//            return;
//        }
//    }
//
//    private void startSosMakeCallTimer() {
//        long now = System.currentTimeMillis();
//        mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_MAKE_SOS_CALL), PendingIntent.FLAG_CANCEL_CURRENT);
//        mAM.set(0, now + 2000L, mPendingIntent);
//        Log.d(TAG, "start Sos Make Call Timer");
//    }
//
//    private void startSosSendSmsTimer() {
//        long now = System.currentTimeMillis();
//        mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SEND_SOS_SMS), PendingIntent.FLAG_CANCEL_CURRENT);
//        mAM.set(0, now, mPendingIntent);
//        Log.d(TAG, "start Sos Send Sms Timer");
//    }
//
//    private void stopSosSound() {
//        Log.d(TAG, "stopSosSound,play state=" + bSosSoundPlaying);
//        if (bSosSoundPlaying && soundPool != null && soundMap != null) {
//            Log.d(TAG, "stopSosSound,stop");
//            if (mSteamId != -1) {
//                soundPool.stop(mSteamId);
//                mSteamId = -1;
//            }
//            bSosSoundPlaying = false;
//        }
//    }
//
//    private void turnOffSpeaker() {
//        setSpeakModle(false);
//    }
//
//    private void turnOnSpeaker() {
//        setSpeakModle(true);
//    }
//
//    private void unRegistetPhoneListener() {
//        TelephonyManager telephonymanager = (TelephonyManager) getSystemService("phone");
//        if (mPhoneStateListener != null) {
//            for (int index = 0; index < mPhoneStateListener.length; index++) {
//                telephonymanager.listen(mPhoneStateListener[index], PhoneStateListener.LISTEN_NONE);
//                mPhoneStateListener[index] = null;
//            }
//        }
//    }
//
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    public void onDestroy() {
//        Log.e(TAG, "onDestroy");
//        super.onDestroy();
//        //qyl add loop
//        setActiveSOSState(0);
//        if (mResolver != null && mCallStateObserver != null) {
//            mResolver.unregisterContentObserver(mCallStateObserver);
//        }
//        //qyl end
//        unregisterReceiver(mSosReceiver);
//        unRegistetPhoneListener();
//        bSosMakeCall = false;
//        mSOSStarting = false;
//        stopSosSound();
//        mLocationHelper.stopGetLocation();
//    }
//
//    public void onStart(Intent intent, int i) {
//        super.onStart(intent, i);
//    }
//
//    public int onStartCommand(Intent intent, int i, int j) {
//        Log.d(TAG, "onStartCommand");
//        return super.onStartCommand(intent, 1, j);
//    }
//
//    void setSpeakModle(boolean flag) {
//        int i = mAudioManager.getStreamVolume(0);
//        AudioManager audiomanager = mAudioManager;
//        audiomanager.setMode(flag ? 2 : 0);
//        if (!mAudioManager.isSpeakerphoneOn() && flag) {
//            mAudioManager.setSpeakerphoneOn(true);
//            mAudioManager.setStreamVolume(0, mAudioManager.getStreamMaxVolume(0), 0);
//        } else if (mAudioManager.isSpeakerphoneOn() && !flag) {
//            mAudioManager.setSpeakerphoneOn(false);
//            mAudioManager.setStreamVolume(0, i, 0);
//        }
//    }
//
//    class PhoneListener extends PhoneStateListener {
//
//        PhoneListener() {
//            super();
//        }
//
//        @Override
//        public void onCallStateChanged(int state, String incomingNumber) {
//            Log.e(TAG, "state--->" + state);
//            switch (state) {
//                case TelephonyManager.CALL_STATE_IDLE: {
//                    //if(isCalling == false){
//                    //	doCallLoop();
//                    //}
//                }
//                break;
//                case TelephonyManager.CALL_STATE_OFFHOOK: {
//                }
//                break;
//                case TelephonyManager.CALL_STATE_RINGING: {
//                }
//                break;
//            }
//        }
//    }
//
//    public class SOSReceiver extends BroadcastReceiver {
//
//        public SOSReceiver() {
//            super();
//        }
//
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Log.e("loop_call", "onReceive action = " + action);
//            Log.e("loop_call", "onReceive bSosMakeCall = " + bSosMakeCall);
//            if (action.equals(ACTION_SOS_START)) {
//                if (!bSosMakeCall) {
//                    if (sosItemList != null) {
//                        sosItemList.clear();
//                    }
//                    sosItemList = testData();
//                    Log.e("loop_call", "onReceive sosItemList = " + sosItemList);
//                    //resetCallNumber();
//                    bSosMakeCall = true;
//                    isCalling = false;
//                    mSOSStarting = true;
//                    mSosCallDialing = false;
//                    playSosSound();
//                    startSosMakeCallTimer();
//
////                    Intent i = new Intent(context, SOSPopupActivity.class);
////                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                    startActivity(i);
//
//                    //if(DbHelper.isInsertLocation(SOSBackgroundService.this)) {
//                    mLocationHelper.setReceivedCallback(mLocationCallback);
//                    mLocationHelper.startGetLocation();
//                    //}
//
//                } else {
//                    Log.d(TAG, "@@@@@@@@@@@@@SOS working,not allow!!!");
//                    return;
//                }
//            } else if (action.equals(ACTION_SOS_STOP)) {
//                if (bSosMakeCall) {
//                    cancelTimer();
//                    stopSosSound();
//                    mLocationHelper.stopGetLocation();
//                    bSosMakeCall = false;
//                    mSOSStarting = false;
//                    //resetCallNumber();
//                    mSosCallDialing = false;
//                    unRegistetPhoneListener();
//                    return;
//                }
//            } else if (action.equals(ACTION_SEND_SOS_SMS)) {
//                cancelTimer();
//                //startSosMakeCallTimer();
//                return;
//            } else if (action.equals(ACTION_MAKE_SOS_CALL)) {
//                stopSosSound();
//                cancelTimer();
//
//                /*if(!bSosMakeCall) {
//			if(sosItemList != null) {
//				sosItemList.clear();
//			}
//			sosItemList = mDBHelper.getItemList();
//			Log.e("loop_call","onReceive sosItemList = " + sosItemList);
//			bSosMakeCall = true;
//			isCalling = false;
//			mSosCallDialing = false;
//
//		} else {
//					Log.d(TAG, "@@@@@@@@@@@@@SOS working,not allow!!!");
//					return;
//		}*/
//
//                if (sosItemList == null || sosItemList.size() == 0) {
//                    bSosMakeCall = false;
//                    mSosCallDialing = false;
//                    mSOSStarting = false;
//                    //notificateUser(R.string.str_make_call_fail_hint);
//                    return;
//                }
//                Settings.Global.putString(getContentResolver(), "is_call_connected", "disconnected");
//
//                //setActiveSOSState(1);
//                mSosListIndex = 0;
//                //qyl end
//                if (!doCallLoop()) {
//                    bSosMakeCall = false;
//                    mSosCallDialing = false;
//                    mSOSStarting = false;
//                    turnOffSpeaker();
//                    unRegistetPhoneListener();
//                    Log.d(TAG, "SOSReceiver,stop call");
//                    return;
//                } else {
//                    sendBroadcast(new Intent("com.android.intent.ACTION_CLOSESOS"));
//                }
//            } else if (action.equals(ACTION_SOS_SOUND_STOP)) {
//                stopSosSound();
//            } else if (action.equals(CALL_ALIVE_ACTION)) {
//                if (bSosMakeCall && mSosCallDialing) {
//                    Log.d(TAG, "call connected! need stop make any call... ");
//                    cancelTimer();
//                    bSosMakeCall = false;
//                    mSosCallDialing = false;
//                    unRegistetPhoneListener();
//                    Log.d(TAG, "SOSReceiver,stop call");
//                    return;
//                }
//            } else if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
//                if (bSosMakeCall) {
//                    isCalling = true;
//                    registetPhoneListener();
//                    return;
//                }
//            } else if (action.equals(ACTION_SEND_SMS)) {
//                switch (getResultCode()) {
//                    case -1:
//                        notificateUser(R.string.str_sms_sent_success);
//                        return;
//                    case 0:
//                        return;
//                    case 1:
//                        try {
//                            notificateUser(R.string.str_sms_sent_failed);
//                        } catch (Exception ex) {
//                            ex.getStackTrace();
//                        }
//                        return;
//                    case 2:
//                        return;
//                    default:
//                        return;
//                }
//            } else if (action.equals(ACTION_SEND_SMS_DELIVER)) {
//                Log.d(TAG, "sos sms is received !");
//                String s2 = intent.getStringExtra("NAME");
//                switch (getResultCode()) {
//                    case -1:
//                        notificateUser(String.format(getResources().getString(R.string.str_sms_rec_success), new Object[]{
//                                s2
//                        }));
//                        return;
//                    case 0:
//                        return;
//                    case 1:
//                        try {
//                            notificateUser(String.format(getResources().getString(R.string.str_sms_rec_failed), new Object[]{
//                                    s2
//                            }));
//                        } catch (Exception ex) {
//                            ex.getStackTrace();
//                        }
//                        return;
//                    case 2:
//                        return;
//                    default:
//                        return;
//                }
//            } else if ("com.android.intent.fastcall".equals(action)) {
//                ArrayList arraylist;
//                char c = '\u03E7';
//                arraylist = mDBHelper.getRelativesList();
//                int i = intent.getIntExtra("keyCode", 0);
//                switch (i) {
//                    case 8:
//                        c = '\0';
//                        break;
//                    case 9:
//                        c = '\001';
//                        break;
//                    case 10:
//                        c = '\002';
//                        break;
//                    default:
//                        break;
//                }
//
//                if (arraylist != null
//                        && c < arraylist.size()
//                        && !((Item) arraylist.get(c)).getNumber().equals("")) {
//                    String s1 = ((Item) arraylist.get(c)).getNumber();
//                    Intent intent1 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + s1));
//                    intent1.putExtra("is_sos_call", true);
//                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent1);
//                } else {
//                    notificateUser(R.string.str_make_call_fail_hint);
//                }
//                arraylist.clear();
//            }
//        }
//    }
//
//
//    private ArrayList testData(){
//        sosItemList.add("17665136602");
//        sosItemList.add("17665136602");
//        sosItemList.add("17665136602");
//        return sosItemList;
//    }
//}
