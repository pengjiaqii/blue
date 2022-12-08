package com.example.demo.sos;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.demo.db.WhiteListEntity;
import com.example.demo.db.WhiteListUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("LongLogTag")
public class PowerButtonReceiverService extends Service {
    private static final String TAG = "PowerButtonReceiverService";
    private int mCounter = 0;
    private static final int MSG_OVER_3_SECONDS = 501;
    private static final int REGARD_AS_TIMEOUT = 3000;
    private InternalHandler mTimeoutHandler;
    private Looper mServiceLooper;
    // Start this service as foreground, otherwise it easily been killed by LMK
    private static final int ONGOING_NOTIFICATION_ID = 405;
    private static final String ONGOING_NOTIFICATION = "Emergency Quick Dial Active";

    private EndCallListener callListener;

    private TelephonyManager mTM;

    boolean is_incomcall_flag = false;

    private int callIndex = 0;

    private static final int MSG_ANSWERCALL = 6;
    private static final int MSG_CALL_ON_SPK = 7;
    private static final int MSG_ENDCALL = 5;
    private static final int MSG_NEXTCALL = 4;

    //顺序拨打白名单的三个号码，一直不接听的话，最多循环3次
    private static int loopTimes = 0;

    private AudioManager mAudioManager;

    private Handler mHandler;

    private class sosLoopHandler extends Handler {
        public sosLoopHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NEXTCALL:
                    Log.i(TAG, "===: handleMessage nextcall ==");
                    nextCall();
                    break;
                case MSG_ENDCALL:
                    endCall();
                    break;
                case MSG_CALL_ON_SPK:
                    setSpeakerMode(true);
                    break;
                default:
                    break;
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Screen ON/OFF broadcast, counter=" + mCounter);
            mCounter++;
            if (mCounter == 3) {
                if (true) {
                    /**
                     * SOS:call 1-3 numbers in the whitelist in sequence until the call stops.
                     * If you do not answer the callfor three consecutive times,the call stops.
                     * Don't send messages.
                     */
                    callListener = new EndCallListener();
                    mTM = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));
                    mTM.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);

                    //start sos call
                    mHandler.sendEmptyMessage(MSG_NEXTCALL);

                    Intent uploadSosBroadcast = new Intent("com.studentcard.tcp.broadcast");
                    uploadSosBroadcast.putExtra("is_sos_call", "SOSCALL");
                    sendBroadcast(uploadSosBroadcast);
                } else {
                    try {
                        Intent callIntent = new Intent(Intent.ACTION_CALL_EMERGENCY);
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        callIntent.setData(Uri.parse("tel:112"));
                        context.startActivity(callIntent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
            mTimeoutHandler.sendMessageDelayed(mTimeoutHandler.obtainMessage(
                    MSG_OVER_3_SECONDS), REGARD_AS_TIMEOUT);
        }
    };

    public void nextCall() {
        int count = mNumSos.length;
        String number = "";

        Log.d(TAG, "next_call count = " + count);
        boolean isAirplaneModeOn = false;
        if (Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            isAirplaneModeOn = true;
        }
        //airplane mode,stop call
        if (isAirplaneModeOn) {
            Log.d(TAG, "isAirplaneModeOn = " + isAirplaneModeOn);
            return;
        }
        //no phone number
        if (count <= 0) {
            Log.d(TAG, "count = " + count);
            return;
        }
        //sim status
        if (!isSimOK()) {
            Log.d(TAG, "isSimOK = " + isSimOK());
            return;
        }


        //out of array length
        if (callIndex < count) {
            number = mNumSos[callIndex];
        } else {
            loopTimes += 1;

            callIndex = 0;
        }
        Log.d(TAG, "nextCall callIndex = " + callIndex);
        Log.d(TAG, "nextCall loopTimes = " + loopTimes);

        Log.d(TAG, "nextCall number = " + number);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //        TelecomManager telecomManager = (TelecomManager) this.getSystemService(Context.TELECOM_SERVICE);
        //        if (telecomManager != null) {
        //            List<PhoneAccountHandle> phoneAccountHandleList = telecomManager.getCallCapablePhoneAccounts();
        //            PhoneAccountHandle phoneAccountHandle = telecomManager.getUserSelectedOutgoingPhoneAccount();
        //            if (phoneAccountHandleList != null && phoneAccountHandleList.size() >= 2 && phoneAccountHandle == null) {
        //                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandleList.get(0));
        //            } else {
        //                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
        //            }
        //        }
        this.startActivity(intent);

        callIndex++;
    }

    public boolean isSimOK() {
        int simState = mTM.getSimState();
        Log.i(TAG, "isSimOK===simState===>" + simState);
        return TelephonyManager.SIM_STATE_READY == simState;
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OVER_3_SECONDS:
                    Log.d(TAG, "Reset counter");
                    mCounter = 0;
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "PowerButtonReceiverService, started");
        initData();
        HandlerThread handlerThread = new HandlerThread("OP18PowerButtonReceiverService",
                Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mServiceLooper = handlerThread.getLooper();
        if (mServiceLooper != null) {
            mTimeoutHandler = new InternalHandler(mServiceLooper);
        }

        HandlerThread loopThread = new HandlerThread("SOSLoopHandler");
        loopThread.start();
        mHandler = new sosLoopHandler(loopThread.getLooper());

        Notification.Builder builder = new Notification.Builder(this)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setTicker(ONGOING_NOTIFICATION);
        Notification notification = builder.build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unregisterReceiver(mReceiver);
        mTimeoutHandler.removeCallbacksAndMessages(null);
        mTimeoutHandler = null;
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        loopTimes = 0;
        callIndex = 0;
    }

    private class EndCallListener extends PhoneStateListener {
        private EndCallListener() {

        }

        public void onCallStateChanged(int state, String phoneNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    // 电话挂断
                    Log.i(TAG, "===CALL_STATE_IDLE===");
                    setSpeakerMode(false);
                    Log.i(TAG, "===play_sound=2=");
                    mHandler.removeMessages(MSG_NEXTCALL);
                    mHandler.sendEmptyMessageDelayed(MSG_NEXTCALL, 200L);

                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    //响铃
                    Log.i(TAG, "===CALL_STATE_RINGING===");
                    //1.不在白名单里面的号码响铃立即挂断
                    ArrayList<WhiteListEntity> listEntities =
                            WhiteListUtil.getInstance(PowerButtonReceiverService.this).queryAll();
                    ArrayList<String> whitePhoneList = new ArrayList<>();
                    for (WhiteListEntity entity : listEntities) {
                        whitePhoneList.add(entity.getWl_phone());
                    }
                    if (whitePhoneList.isEmpty()) {
                        //白名单没有号码，所有号码都不管

                    } else {
                        if (!whitePhoneList.contains(phoneNumber)) {
                            //挂断
                            mHandler.sendEmptyMessageDelayed(MSG_ENDCALL, 100L);
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    //接通
                    Log.i(TAG, "===CALL_STATE_OFFHOOK===");
                    mHandler.sendEmptyMessageDelayed(MSG_CALL_ON_SPK, 1000L);
                    break;
            }
        }
    }

    private void endCall() {
        Log.i(TAG, "===endCall===");
        TelecomManager tm = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if ((tm != null) && (tm.isInCall()))
            tm.endCall();
    }

    private void setSpeakerMode(boolean flag) {
        int i = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        AudioManager audiomanager = mAudioManager;
        audiomanager.setMode(flag ? 2 : 0);
        if (!mAudioManager.isSpeakerphoneOn() && flag) {
            Log.i(TAG, "====setSpeekModle==OPEN=set=SP=");
            mAudioManager.setSpeakerphoneOn(true);
            mAudioManager.setStreamVolume(0, mAudioManager.getStreamMaxVolume(0), 0);
        } else if (mAudioManager.isSpeakerphoneOn() && !flag) {
            Log.i(TAG, "====setSpeekModle==DOWN=set=SP=");
            mAudioManager.setSpeakerphoneOn(false);
            mAudioManager.setStreamVolume(0, i, 0);
        }
    }

    private String[] mNumSos = new String[3];

    private void initData() {
        SharedPreferences sp = getSharedPreferences("sos_info", 0);
        mNumSos[0] = sp.getString("num1_sos", "17665136602");
        mNumSos[1] = sp.getString("num2_sos", "17665136602");
        mNumSos[2] = sp.getString("num3_sos", "17665136602");
    }
}
