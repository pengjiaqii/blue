package com.example.demo.util;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentCardService extends Service {
    public static final String TAG = "StudentCardService";
    /**
     * 心跳检测时间
     */
    private static final long HEART_BEAT_RATE = 60 * 1000;
    /**
     * 重试机制
     */
    private static final long RETRY_INIT = 10 * 1000;
    /**
     * IP地址
     */
    //    private static final String HOST = "112.74.176.68";
    private static final String HOST = "192.168.12.98";
    /**
     * 端口号
     */
    //    public static final int PORT = 9090;
    public static final int PORT = 8082;

    private long sendTime = 0L;

    /**
     * 弱引用 在引用对象的同时允许对垃圾对象进行回收
     */
    private WeakReference<Socket> mSocket;

    private ReadThread mReadThread;

    //    private ITcpService.Stub iTcpService = new ITcpService.Stub() {
    //        @Override
    //        public void sendMessage(String message) throws RemoteException {
    //            sendMsg(message);
    //        }
    //    };
    private double latitude;
    private double longitude;
    private SendThread sendThread;


    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "TcpService：onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TcpService：onCreate");

        LocationUtils.getInstance(this).getLocation();

        String hhmmss = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());

        String ddmmyy = new SimpleDateFormat("ddMMyy", Locale.getDefault()).format(new Date());

        String serialNum = "0123456789ABCDE";

        Log.d(StudentCardService.TAG, "hhmmss:" + hhmmss);
        Log.d(StudentCardService.TAG, "ddmmyy:" + ddmmyy);
        latitude = LocationUtils.getInstance(this).getLatitude();
        longitude = LocationUtils.getInstance(this).getLongitude();
        Log.d(StudentCardService.TAG, "latitude:" + latitude);
        Log.d(StudentCardService.TAG, "longitude:" + longitude);

        new InitSocketThread().start();

        registerAllReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
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
                    String v2SIGNAL = DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext()
                            , StudentCardService.this).handleCmdMessage("V2SIGNAL");
                    Log.i(TAG, "heartBeat---v2SIGNAL--->" + v2SIGNAL);
                    sendMsg(v2SIGNAL);
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    private Runnable retryInitRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "TcpService：重试init");
            if (null == mSocket || null == mSocket.get() || !mSocket.get().isConnected()) {
                releaseLastSocket(mSocket);
                new InitSocketThread().start();
            } else {
                mHandler.removeCallbacks(retryInitRunnable);
            }
        }
    };


    public void sendMsg(String msg) {
        Log.i(TAG, "TcpService：sendMsg:" + msg);
        if (null == mSocket || null == mSocket.get() || !mSocket.get().isConnected()) {
            return;
        }
        if(TextUtils.isEmpty(msg)){
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
        Log.d(TAG, "TcpService：initSocket:HOST:" + HOST);
        Log.d(TAG, "TcpService：initSocket:PORT:" + PORT);
        Socket socket = new Socket(HOST, PORT);
        socket.setKeepAlive(true);
        mSocket = new WeakReference<Socket>(socket);
        mReadThread = new ReadThread(socket);
        mReadThread.start();
        //初始化成功之后发一条命令给服务器
        if (null != mSocket && null != mSocket.get() && mSocket.get().isConnected()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    String v2SIGNAL = DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext()
                            , StudentCardService.this).handleCmdMessage("V2SIGNAL");
                    Log.d(TAG, "v2SIGNAL--->" + v2SIGNAL);
                }
            });

        }
        // 初始化成功后，就准备发送心跳包
        mHandler.post(heartBeatRunnable);
        //连接成功后上报应用数据
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String returnUploadAppInfo = DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext()
                        , StudentCardService.this).uploadAppInfo(1, getAppOnLauncher());
                Log.i(TAG, "returnUploadAppInfo--->" + returnUploadAppInfo);
                sendMsg(returnUploadAppInfo);
            }
        });
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
                if (mSocket != null) {
                    mSocket.clear();
                    mSocket = null;
                }
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
                Log.e(TAG, "初始化异常了,开始重启：" + e.getMessage());
                mHandler.postDelayed(retryInitRunnable, RETRY_INIT);
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
                                    String replyMessage =
                                            DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext()
                                                    , StudentCardService.this).handleCmdMessage(message);
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

    private void registerAllReceiver() {
        //上报sos数据
        MyReceiver myReceiver = new MyReceiver();
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction("com.studentcard.tcp.broadcast");
        this.registerReceiver(myReceiver, filter1);

        //应用安装卸载数据上传
        AppReceiver appReceiver = new AppReceiver();
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter2.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter2.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter2.addDataScheme("package");
        this.registerReceiver(appReceiver, filter2);

    }

    public class MyReceiver extends BroadcastReceiver {
        public MyReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "收到广播：" + intent.getAction());
            if ("com.studentcard.tcp.broadcast".equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String text = bundle.getString("is_sos_call");
                    if ("SOSCALL".equals(text)) {
                        DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext()
                                , StudentCardService.this).uploadSOSData();
                    }
                    Toast.makeText(context, "成功接收广播：" + text, Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    public class AppReceiver extends BroadcastReceiver {
        public AppReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_ADDED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Uri uri = intent.getData();
                Log.d(TAG, "----安装成功packageName:" + packageName);
                Log.d(TAG, "----Uri:" + uri);
                ArrayList<AppInfoEntity> appInfoEntities = new ArrayList<>(1);
                List<AppInfoEntity> entities = getPackageAppInfo();
                AppInfoEntity entity = new AppInfoEntity();
                for (AppInfoEntity appInfoEntity : entities) {
                    if(TextUtils.equals(appInfoEntity.getPackageName(),packageName)){
                        entity.setAppName(appInfoEntity.getAppName());
                        break;
                    }else {
                        entity.setAppName("");
                    }
                }
                entity.setPackageName(packageName);
                entity.setAppType("2");
                appInfoEntities.add(entity);
                String returnUploadAppInfo = DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext()
                        , StudentCardService.this).uploadAppInfo(2,appInfoEntities);
                sendMsg(returnUploadAppInfo);
                Toast.makeText(context, "安装成功" + packageName, Toast.LENGTH_LONG).show();
            } else if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_REPLACED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Log.d(TAG, "----替换成功：" + packageName);
                Toast.makeText(context, "替换成功" + packageName, Toast.LENGTH_LONG).show();
            } else if (TextUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_REMOVED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Log.d(TAG, "----卸载成功：" + packageName);
                ArrayList<AppInfoEntity> appInfoEntities = new ArrayList<>(1);
                AppInfoEntity entity = new AppInfoEntity();
                entity.setAppName("");
                entity.setPackageName(packageName);
                entity.setAppType("2");
                appInfoEntities.add(entity);
                String returnUploadAppInfo = DeviceResponseUtil.getInstance(StudentCardService.this.getApplicationContext(),
                        StudentCardService.this).uploadAppInfo(3,appInfoEntities);
                sendMsg(returnUploadAppInfo);
                Toast.makeText(context, "卸载成功" + packageName, Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * 获取所有应用信息
     *
     * @return
     */
    private List<AppInfoEntity> getPackageAppInfo() {
        PackageManager pm = this.getPackageManager();
        @SuppressLint("WrongConstant")
        List<PackageInfo> installList = pm.getInstalledPackages(PackageManager.PERMISSION_GRANTED);
        Log.d("AppPackage", "install_app_size--->" + installList.size());
        ArrayList<AppInfoEntity> appInfoEntities = new ArrayList<>();
        for (int i = 0; i < installList.size(); i++) {
            PackageInfo item = installList.get(i);
            AppInfoEntity appInfoEntity = new AppInfoEntity();
            try {
                String appName = item.applicationInfo.loadLabel(pm).toString();
                String packageName = item.packageName;
                String versionName = item.versionName;
                Drawable appIcon = item.applicationInfo.loadIcon(pm);
                if (isSystemApp(item)) {
                    Log.w("AppPackage", "系统应用---appName===>" + appName + "===packageName===>" + packageName +
                            "===appIcon===>" + appIcon);
                    appInfoEntity.setAppType("1");
                } else {
                    Log.d("AppPackage", "非系统应用---appName===>" + appName + "===packageName===>" + packageName +
                            "===appIcon===>" + appIcon);
                    appInfoEntity.setAppType("2");
                }
                appInfoEntity.setAppName(unicodeEncode(appName));
                appInfoEntity.setPackageName(packageName);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            appInfoEntities.add(appInfoEntity);
        }

        return appInfoEntities;
    }


    private List<AppInfoEntity> getAppOnLauncher(){
        ArrayList<AppInfoEntity> appInfoEntities = new ArrayList<>();

        PackageManager mPackageManager = this.getApplicationContext().getPackageManager();
        LauncherApps mLauncherApps = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager mUserManager = (UserManager) this.getSystemService(Context.USER_SERVICE);
        List<UserHandle> users = mUserManager.getUserProfiles();
        List<UserHandle> profiles= users == null ? Collections.<UserHandle>emptyList() : users;

        //根据手机所有用户获取每个用户下的应用
        for (UserHandle user : profiles) {
            // Query for the set of apps
            final List<LauncherActivityInfo> apps = mLauncherApps.getActivityList(null, user);
            Log.d("AppPackage", "getAppOnLauncher---size--->" + apps.size());
            // Fail if we don't have any apps
            // TODO: Fix this. Only fail for the current user.
            if (apps == null || apps.isEmpty()) {
                continue;
            }
            // Create the ApplicationInfos
            for (int i = 0; i < apps.size(); i++) {
                LauncherActivityInfo app = apps.get(i);
                // This builds the icon bitmaps.
                AppInfoEntity entity = new AppInfoEntity();
                ComponentName componentName = app.getComponentName();
                String packageName = componentName.getPackageName();
                entity.setPackageName(packageName);
                ApplicationInfo applicationInfo = new ApplicationInfo();
                try {
                    applicationInfo = mPackageManager.getApplicationInfo(packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                Drawable appIcon = applicationInfo.loadIcon(mPackageManager);

                //filter system app
                if ((applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ||
                        (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    String applicationName = (String) mPackageManager.getApplicationLabel(applicationInfo);
                    entity.setAppName(unicodeEncode(applicationName));
                    Log.w("AppPackage", "系统应用---appName===>" + applicationName + "===packageName===>" + packageName +
                            "===appIcon===>" + appIcon);
                    entity.setAppType("1");
                }else {
                    String applicationName = (String) mPackageManager.getApplicationLabel(applicationInfo);
                    entity.setAppName(unicodeEncode(applicationName));
                    Log.d("AppPackage", "非系统应用---appName===>" + applicationName + "===packageName===>" + packageName +
                            "===appIcon===>" + appIcon);
                    entity.setAppType("2");
                }
                appInfoEntities.add(entity);
            }
        }

        return appInfoEntities;
    }


    private boolean isSystemApp(PackageInfo pi) {
        boolean isSysApp = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
        boolean isSysUpd = (pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1;
        return isSysApp || isSysUpd;
    }

    /**
     * @Title: unicodeEncode
     * @Description: unicode编码 将中文字符转换成Unicode字符
     * @param string
     * @return
     */
    public String unicodeEncode(String string) {
        char[] utfBytes = string.toCharArray();
        String unicodeBytes = "";
        for (int i = 0; i < utfBytes.length; i++) {
            String hexB = Integer.toHexString(utfBytes[i]);
            if (hexB.length() <= 2) {
                hexB = "00" + hexB;
            }
            unicodeBytes = unicodeBytes + "\\u" + hexB;
        }
        return unicodeBytes;
    }

}