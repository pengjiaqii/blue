//package com.example.demo.util;
//
//import android.annotation.SuppressLint;
//import android.app.Application;
//import android.app.KeyguardManager;
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.graphics.Color;
//import android.location.LocationManager;
//import android.media.AudioManager;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.os.Binder;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.os.PowerManager;
//import android.provider.Settings;
//import android.telecom.PhoneAccountHandle;
//import android.telecom.TelecomManager;
//import android.telephony.PhoneNumberUtils;
//import android.telephony.PhoneStateListener;
//import android.telephony.SmsManager;
//import android.telephony.TelephonyManager;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.ImageButton;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Timer;
//import java.util.TimerTask;
//
//import static android.graphics.PixelFormat.RGBA_8888;
//import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
//
//import android.app.PendingIntent;
//import android.app.ProgressDialog;
//import android.content.BroadcastReceiver;
//
//import com.example.demo.R;
//
//import android.telephony.SubscriptionManager;
//
//public class SOSservice extends Service {
//    private static final int MSG_ANSWERCALL = 6;
//    private static final int MSG_BLUE = 2;
//    private static final int MSG_CALL_ON_SPK = 7;
//    private static final int MSG_ENDCALL = 5;
//    private static final int MSG_NEXTCALL = 4;
//    private static final int MSG_PLAYSOUND = 3;
//    private static final int MSG_RED = 1;
//    public static final int NOTIFICATION_ID = 2016;
//    public static final int SOS_CALLWAIT_TIME = 20000;
//    private static String TAG = "SOS";
//    EndCallListener callListener;
//    private boolean call_goon_flag = true;
//    private int call_index = 0;
//    private boolean cdma_is_call = false;
//    private int current_call;
//    private boolean current_gps;
//    private int current_music;
//    private int[] cycle = {0, 200, 200, 200, 200, 200, 500, 400, 200, 400, 200, 400, 500, 200, 200, 200, 200, 200, 1300};
//    boolean is_incall_flag = false;
//    boolean is_incomcall_flag = false;
//    private boolean is_run = true;
//    private KeyguardManager.KeyguardLock keyguardLock = null;
//    ImageButton kill;
//    private TimerTask killcurrcalltask;
//    AudioManager mAudioManager;
//    IntentFilter mFilter = new IntentFilter("telephony.callacvive.screenoff");
//    RelativeLayout mFloatLayout;
//    // Broadcast for sending sms
//    private static final String ACTION_SENDTO_SEND = "com.mediatek.ct.csm.action.ACTION_SEND";
//    private static final String ACTION_SENDTO_DELIVERED =
//            "com.mediatek.ct.csm.action.ACTION_DELIVERED";
//    @SuppressLint("HandlerLeak")
//    private Handler mHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MSG_RED:
//                    warning_bg.setBackgroundColor(Color.rgb(255, 0, 0));
//                    break;
//                case MSG_BLUE:
//                    warning_bg.setBackgroundColor(Color.rgb(0, 0, 255));
//                    break;
//                case MSG_PLAYSOUND:
//                case MSG_NEXTCALL:
//                    Log.i(SOSservice.TAG, "===go_playsound: handleMessage nextcall ==");
//                    next_call();
//                    break;
//                case MSG_ENDCALL:
//                    break;
//                case MSG_ANSWERCALL:
//                    autoanswerRingingCall();
//                    break;
//                case MSG_CALL_ON_SPK:
//                    setSpeekModle(true);
//                    break;
//                default:
//                    break;
//
//            }
//
//
//        }
//    };
//    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        public void onReceive(Context paramContext, Intent paramIntent) {
//            Log.i(SOSservice.TAG, "===CDMA IN CALL ==onReceive=");
//            cdma_is_call = true;
//        }
//    };
//    TelephonyManager mTM;
//    private Timer mTimer = new Timer();
//    WindowManager mWindowManager;
//    private String[] m_num_sos = new String[3];
//    private MediaPlayer media = null;
//    private TimerTask nextcalltask;
//    private TextView noContacts;
//    private int number = -1;
//    private boolean sms_position_flag = true;
//    private String sms_sos_normal_text;
//    private Thread thread;
//    private PowerManager.WakeLock wakeLock = null;
//    RelativeLayout warning_bg;
//    WindowManager.LayoutParams wmParams;
//
//    private void InitData() {
//        SharedPreferences sp = getSharedPreferences("sos_info", 0);
//        m_num_sos[0] = sp.getString("num1_sos", "17665136602");
//        m_num_sos[1] = sp.getString("num2_sos", "17665136602");
//        m_num_sos[2] = sp.getString("num3_sos", "17665136602");
//        sms_position_flag = sp.getBoolean("positionflag", true);
//    }
//
//
//    private void acquireUnLock() {
//        if (wakeLock == null) {
//            wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(6, getClass().getCanonicalName());
//            wakeLock.acquire();
//        }
//        if (keyguardLock == null) {
//            keyguardLock = ((KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE)).newKeyguardLock("keyguard");
//            keyguardLock.disableKeyguard();
//        }
//    }
//
//    private void enableGpsProvider(Boolean paramBoolean) {
//        Log.v(TAG, "GPS provider enabled :" + paramBoolean);
//        Settings.Secure.setLocationProviderEnabled(getContentResolver(), "gps", paramBoolean.booleanValue());
//    }
//
//    private void endCall() {
//        Log.i(TAG, "===endCall===");
//        TelecomManager tm = getTelecommService();
//        if ((tm != null) && (tm.isInCall()))
//            tm.endCall();
//    }
//
//    private int getActivatedCardID() {
//        return getDefaultSim();
//    }
//
//    private boolean isCDMAPhone() {
//        return this.mTM.getPhoneType() == 2;
//    }
//
//    public static final boolean isOPenGPS(Context paramContext) {
//        LocationManager lm = (LocationManager) paramContext.getSystemService(Context.LOCATION_SERVICE);
//        boolean gpsenable = lm.isProviderEnabled("gps");
//        boolean networkenable = lm.isProviderEnabled("network");
//        Log.i(TAG, "===GPS isOPen===gps=" + gpsenable + ";;network=" + networkenable);
//        return (gpsenable) || (networkenable);
//    }
//
//    //    private void play_waring_sound() {
//    //        Log.i(TAG, "====play_waring_sound=begin=SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//    //        if (mAudioManager.getMode() != 0)
//    //            mAudioManager.setMode(0);
//    //        if (media == null) {
//    //            mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
//    //            if (!mAudioManager.isSpeakerphoneOn()) {
//    //                Log.i(TAG, "===play_waring_sound==OPEN=set=SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//    //                mAudioManager.setSpeakerphoneOn(true);
//    ////                AudioSystem.setForceUse(1, 1);
//    //            }
//    //            mAudioManager.setStreamVolume(3, mAudioManager.getStreamMaxVolume(3), 0);
//    //            media = MediaPlayer.create(this, R.raw.warning);
//    //            media.setLooping(true);
//    //            media.start();
//    //        }
//    //        Log.i(TAG, "====play_waring_sound=end=SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//    //    }
//
//    private void releaseLock() {
//        if ((wakeLock != null) && (wakeLock.isHeld())) {
//            wakeLock.release();
//            wakeLock = null;
//        }
//        if (keyguardLock != null) {
//            keyguardLock.reenableKeyguard();
//            keyguardLock = null;
//        }
//    }
//
//    private void stop_waring_sound() {
//        Log.i(TAG, "===stop_waring_sound==begin=SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//        if (media != null) {
//            media.stop();
//            media.release();
//            media = null;
//            mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
//            if (mAudioManager.isSpeakerphoneOn()) {
//                Log.i(TAG, "===stop_waring_sound==DOWN=set=SP_OFF=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//                mAudioManager.setSpeakerphoneOn(false);
//                //                AudioSystem.setForceUse(1, 0);
//            }
//            mAudioManager.setStreamVolume(3, current_music, 0);
//        }
//        Log.i(TAG, "===stop_waring_sound==end=SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//    }
//
//    void autoanswerRingingCall() {
//        Log.i(TAG, "===AutoanswerComingCall===");
//        TelecomManager tm = getTelecommService();
//        if ((tm != null) && (tm.isRinging()))
//            tm.acceptRingingCall();
//    }
//
//    public int getDefaultSim() {
//        try {
//            int i = ((Integer) Class.forName("android.telephony.TelephonyManager").getMethod("getSlotIndex", new Class[0]).invoke(mTM, new Object[0])).intValue();
//            Log.i(TAG, "getDefaultSim:" + i);
//            return i;
//        } catch (Exception e) {
//            Log.e(TAG, "getDefaultSim " + e.toString());
//            e.printStackTrace();
//        }
//        return 0;
//    }
//
//    public int getSubid() {
//        int subid[] = SubscriptionManager.getSubId(getDefaultSim());
//        if (subid != null) {
//            return subid[0];
//        }
//        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
//    }
//
//    TelecomManager getTelecommService() {
//        return (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
//    }
//
//    public boolean isSimOK() {
//        int simState = mTM.getSimState();
//        Log.i(TAG, "isSimOK===simState===>" + simState);
//        return TelephonyManager.SIM_STATE_READY == simState;
//    }
//
//    public void message_sos_normal_send() {
//        for (int i = 0; i < 3; i++) {
//            if (m_num_sos[i].length() == 0)
//                continue;
//            send_sms(m_num_sos[i], sms_sos_normal_text);
//        }
//    }
//
//    public void next_call() {
//        if (nextcalltask != null)
//            nextcalltask.cancel();
//        cdma_is_call = false;
//        Log.i(TAG, "===call====;;call_index=" + call_index);
//        nextcalltask = new TimerTask() {
//            public void run() {
//                for (int i = call_index; ; i++) {
//                    if (i < 3) {
//                        if ((m_num_sos[i].length() == 0) || ((!PhoneNumberUtils.isEmergencyNumber(m_num_sos[i])) && (!isSimOK())))
//                            continue;
//                        Log.i(TAG, "===next_call==call_goon_flag = " + call_goon_flag);
//                        Log.i(TAG, "====next_call=SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//                        if (!call_goon_flag)
//                            continue;
///* light start
//                        Intent localIntent = new Intent("android.intent.action.CALL_PRIVILEGED", Uri.parse("tel:" +m_num_sos[i]));
//                        localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(localIntent);
//*/
//                        call(SOSservice.this, m_num_sos[i]);
//                        // light end
//                        call_index = i + 1;
//                        //SOSservice.access$1402(SOSservice.this, i + 1);
//                        if (killcurrcalltask != null) {
//                            killcurrcalltask.cancel();
//                            Log.i(TAG, "===killcurrcalltask==cancle=1==newcall==");
//                        }
//                        /*killcurrcalltask= new TimerTask()
//                        {
//                            public void run()
//                            {
//                                Log.i(TAG, "===killcurrcalltask==run==");
//                                if (!isInCall())
//                                {
//                                    endCall();
//                                    return;
//                                }
//                                is_incall_flag=false;
//                            }
//                        };
//                        mTimer.schedule(killcurrcalltask, SOS_CALLWAIT_TIME);*/
//                    }
//                    return;
//                }
//            }
//        };
//        mTimer.schedule(nextcalltask, 5000L);
//    }
//
//    // light start
//    public void call(Context context, String telNum) {
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_CALL);
//        intent.setData(Uri.parse("tel:" + telNum));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
//        if (telecomManager != null) {
//            List<PhoneAccountHandle> phoneAccountHandleList = telecomManager.getCallCapablePhoneAccounts();
//            PhoneAccountHandle phoneAccountHandle = telecomManager.getUserSelectedOutgoingPhoneAccount();
//            if (phoneAccountHandleList != null && phoneAccountHandleList.size() >= 2 && phoneAccountHandle == null) {
//                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandleList.get(0));
//            } else {
//                //intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
//            }
//        }
//        context.startActivity(intent);
//    }
//    // light end
//
//    public IBinder onBind(Intent paramIntent) {
//        return null;
//    }
//
//    public void onCreate() {
//        super.onCreate();
//        Log.i(TAG, "====service=onCreate=====");
//        startForeground(NOTIFICATION_ID, new Notification());
//        acquireUnLock();
//        InitData();
//        mLocationClient = ((LocationApplication) getApplication()).mLocationClient;
//        InitLocation();
//        callListener = new EndCallListener();
//        mTM = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));
//        SubscriptionManager mSubscriptionManager = (SubscriptionManager) getApplicationContext().getSystemService(
//                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
//        mTM.listen(callListener, 32);
//        if (mWindowManager == null) {
//            Application localApplication = getApplication();
//            getApplication();
//            mWindowManager = ((WindowManager) localApplication.getSystemService(Context.WINDOW_SERVICE));
//            Log.i(TAG, "mWindowManager--->" + mWindowManager);
//            if (wmParams == null) {
//                wmParams = new WindowManager.LayoutParams();
//                wmParams.type = TYPE_SYSTEM_ALERT;
//                wmParams.format = RGBA_8888;
//                wmParams.flags = 2622592;
//                wmParams.gravity = 51;
//                wmParams.x = 0;
//                wmParams.y = 0;
//                wmParams.width = -1;
//                wmParams.height = -1;
//            }
//            mFloatLayout = ((RelativeLayout) LayoutInflater.from(getApplication()).inflate(R.layout.warningui, null));
//            mWindowManager.addView(mFloatLayout, wmParams);
//        }
//        kill = ((ImageButton) mFloatLayout.findViewById(R.id.cancle_sos));
//        warning_bg = ((RelativeLayout) mFloatLayout.findViewById(R.id.warning_bg));
//        noContacts = ((TextView) mFloatLayout.findViewById(R.id.no_contacts));
//        for (int i = 0; ; i++) {
//            if (i < 3) {
//                if (!m_num_sos[i].isEmpty())
//                    noContacts.setVisibility(View.GONE);
//            } else {
//                kill.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View paramView) {
//                        stopSelf();
//                    }
//                });
//                thread = new MyThread();
//                thread.start();
//                mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
//                current_music = mAudioManager.getStreamVolume(3);
//                current_call = mAudioManager.getStreamVolume(0);
//                Log.i(TAG, "===play_sound=1=");
//                Log.i(TAG, "===first=play_sound==SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//                Log.i(TAG, "===go_playsound: service oncreate==");
//                play_waring_sound();
//                if (isSimOK())
//                    new Thread() {
//                        public void run() {
//                            message_sos_normal_send();
//                            if (sms_position_flag) {
//
//                                current_gps = isOPenGPS(getApplicationContext());
//                                if (current_gps != true)
//                                    enableGpsProvider(Boolean.valueOf(true));
//                                mLocationClient.start();
//                                Log.i(TAG, "==sms=mLocationClient==");
//                            }
//                        }
//                    }
//                            .start();
//                Log.i(TAG, "===next_call==");
//                new Thread() {
//                    public void run() {
//                        next_call();
//                    }
//                }.start();
//                getApplicationContext().registerReceiver(mReceiver, mFilter);
//                return;
//            }
//            noContacts.setVisibility(View.GONE);
//        }
//    }
//
//    public void onDestroy() {
//        super.onDestroy();
//        getApplicationContext().unregisterReceiver(mReceiver);
//        stopSOSservice();
//    }
//
//    //    public void send_sms(String number, String message) {
//    //        //SmsManager.getDefault();
//    //        Log.i(TAG, "SMS length=" + message.length());
//    //        ComponentName cn = SmsApplication.getDefaultRespondViaMessageApplication(this, true);
//    //        Log.e("SOS", "send_sms cn " + cn + " number " + number);
//    //        Log.i(TAG, "send_sms =" + getSubid());
//    //        if (cn != null) {
//    //
//    //            Intent intent = new Intent("android.intent.action.RESPOND_VIA_MESSAGE", Uri.fromParts("smsto", number, null));
//    //            intent.putExtra("android.intent.extra.TEXT", message);
//    //            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, getSubid());
//    //            intent.setComponent(cn);
//    //            Log.e("SOS", "send sms intent  " + intent);
//    //            startService(intent);
//    //        }
//    //    }
//
//    public void doSendSMSTo(String phoneNumber, String message) {
//
//        PendingIntent sentIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(
//                ACTION_SENDTO_SEND), 0);
//        PendingIntent deliveryIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(
//                ACTION_SENDTO_DELIVERED), 0);
//        SmsManager.getDefault().sendTextMessage(phoneNumber, null,
//                message, sentIntent, deliveryIntent);
//        /*if(PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)){
//            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"+phoneNumber));
//            intent.putExtra("sms_body", message);
//            startActivity(intent);
//        }*/
//    }
//
//    /* private void requestPermission(String phoneNumber,String message) {
//          //判断Android版本是否大于23
//          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//              int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
//              if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
//                  ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS);
//                  return;
//              } else {
//                  doSendSMSTo(phoneNumber,message);
//                  //已有权限
//              }
//          } else {
//              //API 版本在23以下
//          }
//      }*/
//    void setSpeekModle(boolean paramBoolean) {
//        mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
//        mAudioManager.setMode(2);
//        Log.i(TAG, "====setSpeekModle==begin=currentcallVoice=" + current_call + ";;SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//        if ((!mAudioManager.isSpeakerphoneOn()) && (true == paramBoolean)) {
//            mAudioManager.setSpeakerphoneOn(true);
//            Log.i(TAG, "====setSpeekModle==OPEN=set=SP=");
//            mAudioManager.setStreamVolume(0, mAudioManager.getStreamMaxVolume(0), 0);
//        } else {
//            Log.i(TAG, "====setSpeekModle==end=currentcallVoice=" + current_call + ";;SP_ON=" + mAudioManager.isSpeakerphoneOn() + ";;MODE=" + mAudioManager.getMode());
//            mAudioManager.setSpeakerphoneOn(false);
//            Log.i(TAG, "====setSpeekModle==DOWN=set=SP=");
//            mAudioManager.setStreamVolume(0, current_call, 0);
//        }
//    }
//
//    void stopSOSservice() {
//        if (nextcalltask != null)
//            nextcalltask.cancel();
//        if (killcurrcalltask != null) {
//            Log.i(TAG, "===killcurrcalltask==cancle==stopsos=");
//            killcurrcalltask.cancel();
//        }
//        mHandler.removeMessages(3);
//        mHandler.removeMessages(4);
//        mHandler.removeMessages(6);
//        mHandler.removeMessages(7);
//        Log.i(TAG, "===stop-sos===SP_ON=" + this.mAudioManager.isSpeakerphoneOn() + ";;MODE=" + this.mAudioManager.getMode());
//        Log.i(TAG, "===stop-sos===is_incall_flag=" + this.is_incall_flag + ";is_incomcall_flag=" + this.is_incomcall_flag);
//        is_incall_flag = false;
//        is_incomcall_flag = false;
//        stop_waring_sound();
//        if (mAudioManager.getMode() != 0)
//            mAudioManager.setMode(0);
//        mTM.listen(callListener, 0);
//        if (mFloatLayout != null) {
//            mWindowManager.removeView(mFloatLayout);
//            mFloatLayout = null;
//        }
//        if ((isSimOK()) && (sms_position_flag)) {
//            if (current_gps != isOPenGPS(getApplicationContext()))
//                enableGpsProvider(Boolean.valueOf(current_gps));
//        }
//        if (thread.isAlive())
//            is_run = false;
//        releaseLock();
//        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(2016);
//    }
//
//    private class EndCallListener extends PhoneStateListener {
//        private EndCallListener() {
//        }
//
//        public void onCallStateChanged(int state, String paramString) {
//            switch (state) {
//
//                case TelephonyManager.CALL_STATE_IDLE:// 电话挂断
//                    Log.i(TAG, "===CALL_STATE_IDLE===");
//                    if (is_incomcall_flag) {
//                        is_incomcall_flag = false;
//                        Log.i(TAG, "===go_playsound: onCallStateChanged incoming to end ==");
//                        play_waring_sound();
//                    } else {
//                        is_incall_flag = false;
//                        if (killcurrcalltask != null) {
//                            killcurrcalltask.cancel();
//                            Log.i(TAG, "===killcurrcalltask==cancle=0=callend=");
//                        }
//                        setSpeekModle(false);
//                        Log.i(TAG, "===play_sound=2=");
//                        mHandler.sendEmptyMessageDelayed(MSG_NEXTCALL, 2000L);
//                    }
//                    break;
//                case 1:
//                    Log.i(SOSservice.TAG, "===CALL_STATE_RINGING===");
//                    is_incomcall_flag = true;
//                    stop_waring_sound();
//                    mHandler.sendEmptyMessageDelayed(MSG_ENDCALL, 2000L);
//                    break;
//                case 2:
//                    Log.i(TAG, "===CALL_STATE_OFFHOOK===");
//                    is_incall_flag = true;
//                    stop_waring_sound();
//                    mHandler.sendEmptyMessageDelayed(MSG_CALL_ON_SPK, 2000L);
//                    break;
//
//            }
//        }
//    }
//
//    public class MyThread extends Thread {
//        public MyThread() {
//        }
//
//        public void run() {
//            while (is_run)
//                try {
//                    number++;
//                    if (number == cycle.length)
//                        number = 0;
//                    Thread.sleep(cycle[number]);
//                    Message localMessage = new Message();
//                    localMessage.what = (1 + number % 2);
//                    mHandler.sendMessage(localMessage);
//                } catch (InterruptedException localInterruptedException) {
//                    localInterruptedException.printStackTrace();
//                }
//        }
//    }
//
//    class ServiceBinder extends Binder {
//        ServiceBinder() {
//        }
//
//        public SOSservice getService() {
//            return SOSservice.this;
//        }
//    }
//}
