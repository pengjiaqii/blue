package com.example.demo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.demo.ITcpService;

public class StudentCardService extends Service {
    public static final String TAG = "StudentCardService";
    /**
     * 心跳检测时间
     */
    private static final long HEART_BEAT_RATE = 60 * 1000;
    /**
     * 主机IP地址
     */
    private static final String HOST = "192.168.12.98";
    /**
     * 端口号
     */
    public static final int PORT = 8081;

    private long sendTime = 0L;

    /**
     * 弱引用 在引用对象的同时允许对垃圾对象进行回收
     */
    private WeakReference<Socket> mSocket;

    private ReadThread mReadThread;

    private ITcpService.Stub iTcpService = new ITcpService.Stub() {
        @Override
        public void sendMessage(String message) throws RemoteException {
            sendMsg(message);
        }
    };
    private double latitude;
    private double longitude;
    private SendThread sendThread;


    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "TcpService：onBind");
        return (IBinder) iTcpService;
    }

    String v2Signal = "";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TcpService：onCreate");

        LocationUtils.getInstance(this).getLocation(this);

        String hhmmss = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());

        String ddmmyy = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());

        String serialNum = "0123456789ABCDEF";

        Log.d(StudentCardService.TAG, "hhmmss:" + hhmmss);
        Log.d(StudentCardService.TAG, "ddmmyy:" + ddmmyy);
        latitude = LocationUtils.getInstance(this).getLatitude();
        longitude = LocationUtils.getInstance(this).getLongitude();
        Log.d(StudentCardService.TAG, "latitude:" + latitude);
        Log.d(StudentCardService.TAG, "longitude:" + longitude);

        v2Signal = "*WT," + serialNum + ",V2," + hhmmss + ",A," + latitude + "," + longitude + "," + ddmmyy + ",FFFFDFFF";

        new InitSocketThread().start();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "TcpService：onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "TcpService：onDestroy");
        //releaseLastSocket(mSocket);
        super.onDestroy();
    }

    // 发送心跳包
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "TcpService：heartBeatRunnable");
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                if (null == mSocket || null == mSocket.get() || !mSocket.get().isConnected()) {
                    mHandler.removeCallbacks(heartBeatRunnable);
                    mReadThread.release();
                    releaseLastSocket(mSocket);
                    new InitSocketThread().start();
                } else {
                    sendMsg("heart_beat");
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    public void sendMsg(String msg) {
        Log.i(TAG, "TcpService：sendMsg:" + msg);
        if (null == mSocket || null == mSocket.get() || !mSocket.get().isConnected()) {
            return;
        }
        //防止启动多次
        if (sendThread != null) {
            sendThread = null;
        }
        sendThread = new SendThread(msg);
        sendThread.start();
    }

    // 初始化socket
    private void initSocket() throws UnknownHostException, IOException {
        Log.d(TAG, "TcpService：initSocket");
        Socket socket = new Socket(HOST, PORT);
        socket.setKeepAlive(true);
        mSocket = new WeakReference<Socket>(socket);
        mReadThread = new ReadThread(socket);
        mReadThread.start();
        //初始化成功之后发一条命令给服务器
        if (null != mSocket && null != mSocket.get() && mSocket.get().isConnected()) {
            sendMsg(v2Signal);
        }
        // 初始化成功后，就准备发送心跳包
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);
    }

    // 释放socket
    private void releaseLastSocket(WeakReference<Socket> socket) {
        Log.d(TAG, "TcpService：releaseLastSocket");
        try {
            if (null != socket) {
                Socket sk = socket.get();
                if (null != sk && !sk.isClosed()) {
                    sk.close();
                    sk = null;
                }
                mSocket.clear();
                mSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class SendThread extends Thread {

        private String msg = "";

        public SendThread(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            Log.i(TAG, "TcpService：SendThread msg:" + msg);
            Socket soc = mSocket.get();
            try {
                if (null != soc && soc.isConnected()) {
                    OutputStream os = soc.getOutputStream();
                    String message = msg + "\r\n";
                    os.write(message.getBytes());
                    os.flush();
                    // 每次发送成功数据，就改一下最后成功发送的时间，节省心跳间隔时间
                    sendTime = System.currentTimeMillis();
                    Log.i(TAG, "发送成功的时间：" + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
                } else {
                    Log.e(TAG, "SendThread：sendMsg  soc.isConnected():" + soc.isConnected());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "SendThread   Exception：" + e);
                //重启
                mHandler.removeCallbacks(heartBeatRunnable);
                mReadThread.release();
                releaseLastSocket(mSocket);
                new InitSocketThread().start();
            }
        }
    }

    class InitSocketThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "TcpService：InitSocketThread");
            super.run();
            try {
                initSocket();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "TcpService：initSocket Exception：" + e);
            }
        }
    }

    public class ReadThread extends Thread {
        private WeakReference<Socket> mWeakSocket;
        private boolean isStart = true;

        public ReadThread(Socket socket) {
            mWeakSocket = new WeakReference<Socket>(socket);
        }

        public void release() {
            isStart = false;
            releaseLastSocket(mWeakSocket);
        }

        @SuppressLint("NewApi")
        @Override
        public void run() {
            Log.v(TAG, "TcpService：ReadThread");
            super.run();
            Socket socket = mWeakSocket.get();
            if (null != socket) {
                try {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int length = 0;
                    while (!socket.isClosed() && !socket.isInputShutdown() && isStart && ((length = is.read(buffer)) != -1)) {
                        Log.v(TAG, "服务器消息长度length：" + length);
                        if (length > 0) {
                            String message = new String(Arrays.copyOf(buffer, length)).trim();
                            Log.v(TAG, "收到服务器发送来的消息：" + message);
                            // 这里可以用来处理message,并回复消息
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    String replyMessage = DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext()).handleCmdMessage(message);
                                    Log.v(TAG, "replyMessage:" + replyMessage);
                                    sendMsg(replyMessage);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ShutdownBroadcastReceiver extends BroadcastReceiver {
        private static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";

        @Override
        public void onReceive(Context context, Intent intent) {  //即将关机时，要做的事情
            if (intent.getAction().equals(ACTION_SHUTDOWN)) {
                Log.i(TAG, "ShutdownBroadcastReceiver onReceive(), Do thing!");
                sendMsg("关机关机");
            }
        }
    }

    //    @SuppressLint("MissingPermission")
    //    private void getLocation() {
    //        //1.获取位置管理器
    //        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    //        // 查找到服务信息
    //        Criteria criteria = new Criteria();
    //        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
    //
    //        criteria.setAltitudeRequired(false);
    //        criteria.setBearingRequired(false);
    //        criteria.setCostAllowed(true);
    //        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗
    //
    //        String locationProvider = locationManager.getBestProvider(criteria, true);// 获取GPS信息
    //
    //        Location location = locationManager.getLastKnownLocation(locationProvider);// 通过GPS获取位置
    //
    //        latitude = location.getLatitude();//纬度
    //        longitude = location.getLongitude();//经度
    //        Log.i("TcpService", "location.latitude: " + latitude);
    //        Log.i("TcpService", "location.longitude:  " + longitude);
    //
    //        // 设置每2秒获取一次GPS的定位信息
    //        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 8f, new LocationListener() {
    //            // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
    //            @Override
    //            public void onStatusChanged(String provider, int status, Bundle arg2) {
    //            }
    //
    //            // Provider被enable时触发此函数，比如GPS被打开
    //            @Override
    //            public void onProviderEnabled(String provider) {
    //            }
    //
    //            // Provider被disable时触发此函数，比如GPS被关闭
    //            @Override
    //            public void onProviderDisabled(String provider) {
    //            }
    //
    //            //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
    //            @Override
    //            public void onLocationChanged(Location loc) {
    //                Log.i("TcpService", "onLocationChanged");
    //                //            location = loc;
    //                //            showLocation();
    //            }
    //        });
    //    }
}