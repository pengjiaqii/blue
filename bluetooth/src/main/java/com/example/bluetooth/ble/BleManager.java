package com.example.bluetooth.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.example.bluetooth.ble.bean.MessageBean;
import com.example.bluetooth.ble.bean.SearchResult;
import com.example.bluetooth.ble.listener.OnConnectListener;
import com.example.bluetooth.ble.listener.OnReceiveMessageListener;
import com.example.bluetooth.ble.listener.OnSearchDeviceListener;
import com.example.bluetooth.ble.listener.OnSendMessageListener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/13
 * 功能 :BLE蓝牙的管理器，封装起来的工具类
 */
public class BleManager {


    private static final String DEVICE_HAS_NOT_BLUETOOTH_MODULE = "该设备没有可用的蓝牙";
    private static final String TAG = "BleManager";
    //LinkedBlockingQueue内部由单链表实现，只能从head(头)取元素，从tail(尾)添加元素。
    // 添加元素和获取元素都有独立的锁，也就是说LinkedBlockingQueue是读写分离的，读写操作可以并行执行。
    // LinkedBlockingQueue采用可重入锁(ReentrantLock)来保证在并发情况下的线程安全。
    private Queue<MessageBean> mMessageBeanQueue = new LinkedBlockingQueue<>();
    //线程池
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();
    private List<SearchResult> mBondedList = new ArrayList<>();
    private HashMap<String, Object> paar = new HashMap<>();
    private List<SearchResult> mNewList = new ArrayList<>();

    private OnSearchDeviceListener mOnSearchDeviceListener;
    private OnConnectListener onConnectListener;
    private OnSendMessageListener onSendMessageListener;
    private OnReceiveMessageListener onReceiveMessageListener;

    private volatile Receiver mReceiver = new Receiver();
    private volatile STATUS mCurrStatus = STATUS.FREE;

    private BluetoothAdapter mBluetoothAdapter;
    private static volatile BleManager blueManager;

    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadRunnable readRunnable;
    private Context mContext;
    private static int DEFAULT_BUFFER_SIZE = 10;
    private volatile boolean mWritable = true;
    private volatile boolean mReadable = true;
    private boolean mNeed2unRegister;
    private boolean what = true;
    private int number = 0;
    private boolean readVersion = true;
    private boolean supportBLE = false;

    private enum STATUS {
        DISCOVERING,
        //连接状态
        CONNECTED,
        //空闲状态
        FREE
    }


    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (!paar.containsKey(device.getAddress())) {
                Log.i(TAG, "device " + device.getAddress() + "   " + device.getName());
                paar.put(device.getAddress(), "mac:" + device.getAddress());
                SearchResult searchResult = new SearchResult(device, rssi, null);
                mNewList.add(searchResult);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (mOnSearchDeviceListener != null)
                        mOnSearchDeviceListener.onSearchCompleted(mBondedList, mNewList);
                }
            }
        }

    };

    /**
     * 单例
     */
    public static BleManager getInstance(Context context) {
        if (blueManager == null) {
            synchronized (BleManager.class) {
                if (blueManager == null)
                    blueManager = new BleManager(context);
            }
        }
        return blueManager;
    }

    /**
     *
     */
    private BleManager(Context context) {
        mContext = context.getApplicationContext();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * {@link OnSearchDeviceListener}
     * 详情请点击@link
     */
    public void setOnSearchDeviceListener(OnSearchDeviceListener mOnSearchDeviceListener) {
        this.mOnSearchDeviceListener = mOnSearchDeviceListener;
    }

    /**
     * {@link OnConnectListener}
     * 详情请点击@link
     */
    public void setOnConnectListener(OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    /**
     * {@link OnSendMessageListener}
     * 详情请点击@link
     */
    public void setOnSendMessageListener(OnSendMessageListener onSendMessageListener) {
        this.onSendMessageListener = onSendMessageListener;
    }

    /**
     * {@link OnReceiveMessageListener}
     * 详情请点击@link
     */
    public void setOnReceiveMessageListener(OnReceiveMessageListener onReceiveMessageListener) {
        this.onReceiveMessageListener = onReceiveMessageListener;
    }


    public void setReadVersion(boolean readVersion) {
        this.readVersion = readVersion;
    }

    /**
     * 请求异步启用设备的蓝牙。
     * 如果设备没有蓝牙模块，抛出NullPointerException
     */
    public void requestEnableBt() {
        if (mBluetoothAdapter == null) {
            throw new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE);
        }
        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();
    }

    /**
     * 搜索蓝牙设备.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void searchBLEDevices() {
        try {
            //先检查搜索蓝牙的监听是否为空
            checkNotNull(mOnSearchDeviceListener);
            if (mBondedList == null) {
                mBondedList = new ArrayList<>();
            } else {
                mBondedList.clear();
            }
            if (mNewList == null) {
                mNewList = new ArrayList<>();
            } else {
                mNewList.clear();
            }
            //说明该设备没有蓝牙这个东西可用
            if (mBluetoothAdapter == null) {
                mOnSearchDeviceListener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                return;
            }
            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            //开始搜索，详情见回调
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 点击开始搜索按钮，开始搜索设备
     */
    public void searchDevices() {
        try {
            if (mCurrStatus == STATUS.FREE) {
                mCurrStatus = STATUS.DISCOVERING;
                checkNotNull(mOnSearchDeviceListener);
                if (mBondedList == null) {
                    mBondedList = new ArrayList<>();
                } else {
                    mBondedList.clear();
                }
                if (mNewList == null) {
                    mNewList = new ArrayList<>();
                } else {
                    mNewList.clear();
                }
                if (mBluetoothAdapter == null) {
                    mOnSearchDeviceListener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                    return;
                }
                if (mReceiver == null)
                    mReceiver = new Receiver();
                // ACTION_FOUND
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                mContext.registerReceiver(mReceiver, filter);
                // ACTION_DISCOVERY_FINISHED
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                //注册广播
                mContext.registerReceiver(mReceiver, filter);
                mNeed2unRegister = true;
                mBondedList.clear();
                mNewList.clear();
                if (mBluetoothAdapter.isDiscovering())
                    mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter.startDiscovery();
                mOnSearchDeviceListener.onStartDiscovery();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 搜索蓝牙的监听广播
     */
    private class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    //开始搜索
                    if (mOnSearchDeviceListener != null)
                        mOnSearchDeviceListener.onStartDiscovery();
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    //找到设备，这个Intent中包含两个extra fields：
                    // EXTRA_DEVICE和EXTRA_CLASS，分别包含BluetooDevice和BluetoothClass。
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        //表示远程设备没有结合（配对）。
                        //有没有共享与远程设备链路密钥，因此通信（如果允许的话）将未认证和未加密的。
                        if (paar != null && !paar.containsKey(device.getAddress())) {
                            paar.put(device.getAddress(), "mac:" + device.getAddress());
                            if (mNewList != null) {
                                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                                SearchResult searchResult = new SearchResult(device, rssi, null);
                                mNewList.add(searchResult);
                            }
                        }
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        //表示绑定
                        if (mBondedList != null) {
                            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                            SearchResult searchResult = new SearchResult(device, rssi, null);
                            mBondedList.add(searchResult);
                        }
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //搜索结束
                    if (mOnSearchDeviceListener != null)
                        mOnSearchDeviceListener.onSearchCompleted(mBondedList, mNewList);
                    //搜索蓝牙设备
                    searchBLEDevices();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 将消息发送到远程设备。 如果本地设备我以前不连接到远程设备，
     * 它将调用connectDevice（），然后发送该消息。 你可以得到一个响应的getInstance远程设备，就像HTTP。
     * 但是，如果没有得到响应的getInstance远程设备就会受阻。
     *
     * @param item         需要发送的消息对象
     * @param needResponse 如果需要获得响应getInstance远程设备
     */
    public void sendMessage(MessageBean item, boolean needResponse) {
        try {
            if (mCurrStatus == STATUS.CONNECTED) {
                if (mBluetoothAdapter == null) {
                    onSendMessageListener.onError(new RuntimeException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                    return;
                }
                mMessageBeanQueue.add(item);
                WriteRunnable writeRunnable = new WriteRunnable();
                mExecutorService.submit(writeRunnable);
                number = 0;
                what = true;
                if (needResponse) {
                    if (readRunnable == null) {
                        readRunnable = new ReadRunnable();
                        mExecutorService.submit(readRunnable);
                    } else {
                        Log.i("blue", "readRunnable is not null !");
                    }
                }
            } else {
                Log.i("blue", "the blue is not connected !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 断开当前连接的蓝牙设备
     */
    public void closeDevice() {
        try {
            if (mCurrStatus == STATUS.CONNECTED) {
                mReadable = false;
                mWritable = false;
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mSocket != null && mSocket.isConnected()) {
                    mSocket.close();
                    mSocket = null;
                    number = 0;
                    what = true;
                    if (readRunnable != null) {
                        readRunnable = null;
                    }
                } else {
                    Log.i("blue", "closeDevice faield please check bluetooth is enable and the mSocket is connected !");
                }
            } else {
                Log.i("blue", "the bluetooth is not conencted ! please connect devices !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接bluetooth
     *
     * @param mac 要连接的对方蓝牙的地址
     */
    public void connectDevice(String mac) {
        try {
            if (mCurrStatus != STATUS.CONNECTED) {
                if (mac == null || TextUtils.isEmpty(mac))
                    throw new IllegalArgumentException("mac地址为null或为空！");
                if (!BluetoothAdapter.checkBluetoothAddress(mac))
                    throw new IllegalArgumentException("mac地址不正确！确保它是大写的！");
                if (mReadable = false) {
                    mReadable = true;
                }
                if (mWritable = false) {
                    mWritable = true;
                }
                if (onConnectListener != null) {
                    onConnectListener.onConnectStart();
                    //连接bluetooth线程
                    ConnectDeviceRunnable connectDeviceRunnable = new ConnectDeviceRunnable(mac);
                    checkNotNull(mExecutorService);
                    mExecutorService.submit(connectDeviceRunnable);
                }
            } else {
                Log.i(TAG, "蓝色已连接！");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停 读写线程
     *
     * @param pauseread
     * @param pausewriter
     */
    public void pauseBlue(boolean pauseread, boolean pausewriter) {
        this.mWritable = pausewriter;
        this.mReadable = pauseread;
    }

    /**
     * 关闭连接并释放与该流关联的系统资源。
     */
    public void close() {
        try {
            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter = null;
            }
            if (mNeed2unRegister) {
                mContext.unregisterReceiver(mReceiver);
                mReceiver = null;
                mNeed2unRegister = !mNeed2unRegister;
            }
            if (mMessageBeanQueue != null) {
                mMessageBeanQueue.clear();
                mMessageBeanQueue = null;
            }
            mWritable = false;
            mReadable = false;

            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
            if (mExecutorService != null) {
                mExecutorService.shutdown();
                mExecutorService = null;
            }
            mNewList = null;
            mBondedList = null;
            mReceiver = null;
            blueManager = null;
            mCurrStatus = STATUS.FREE;
        } catch (Exception e) {
            e.printStackTrace();
            mSocket = null;
        }
    }

    /**
     * 连接bluetooth线程
     */
    private class ConnectDeviceRunnable implements Runnable {
        private String mac;
        private BluetoothDevice mRemoteDevice;

        public ConnectDeviceRunnable(String mac) {
            this.mac = mac;
        }

        @Override
        public void run() {
            try {
                if (onConnectListener == null) {
                    Log.i(TAG, "connectListener为null！");
                    return;
                }
                mRemoteDevice = mBluetoothAdapter.getRemoteDevice(mac);
                mBluetoothAdapter.cancelDiscovery();
                mCurrStatus = STATUS.FREE;
                Log.d(TAG, "prepare to connect: " + mRemoteDevice.getAddress() + " " + mRemoteDevice.getName());
                mSocket = mRemoteDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(Constants.STR_UUID));
                onConnectListener.onConnecting();
                mSocket.connect();
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                mCurrStatus = STATUS.CONNECTED;
                onConnectListener.onConnectSuccess(mac);
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.toString());
                Log.d(TAG, "Exception: " + e.getMessage());
                //                e.printStackTrace();
                //                onConnectListener.onConnectFailed();
                //                try {
                //                    mInputStream.close();
                //                    mOutputStream.close();
                //                } catch (Exception closeException) {
                //                    closeException.printStackTrace();
                //                }
                //                mCurrStatus = STATUS.FREE;

                try {
                    Method m = mRemoteDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    mSocket = (BluetoothSocket) m.invoke(mRemoteDevice, 1);
                    mSocket.connect();
                } catch (Exception ee) {
                    Log.e(TAG, ee.toString());
                    try {
                        mSocket.close();
                    } catch (IOException ie) {
                        Log.w(TAG, "Exception: " + ie.toString());
                        Log.w(TAG, "Exception: " + ie.getMessage());
                    }
                }

            }
        }
    }

    /**
     * 读取bluetooth流线程
     */
    private class ReadRunnable implements Runnable {


        @Override
        public void run() {
            try {
                if (onReceiveMessageListener == null) {
                    Log.i("blue", "receiverMessageListener为null！");
                    return;
                }
                mReadable = true;
                InputStream stream = mInputStream;
                while (mCurrStatus != STATUS.CONNECTED && mReadable)
                    ;
                checkNotNull(stream);
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                StringBuilder builder = new StringBuilder();
                while (mReadable) {
                    int count = 0;
                    while (count == 0) {
                        count = stream.available();//输入流中的数据个数。
                    }
                    if (readVersion) {
                        if (count == 10) {
                            int num = stream.read(buffer);
                            String text2 = TypeConversion.bytesToHexStrings(buffer);
                            builder.append(text2);
                            Log.i("version", text2);
                            if (text2.endsWith("04 ")) {
                                String versionHex = TypeConversion.HexStringSplit(builder.toString());
                                String[] version = TypeConversion.HexStringConversionVesion(versionHex);
                                if (version.length >= 2) {
                                    String sn = version[1];
                                    Log.i("sn", sn);
                                    onReceiveMessageListener.onNewLine("当前设备SN：" + sn);
                                }
                            }
                        } else {
                            if (count >= 10) {
                                int num = stream.read(buffer);
                                String text2 = TypeConversion.bytesToHexStrings(buffer);
                                builder.append(text2);
                                Log.i("append", text2);
                            }
                        }
                    } else {
                        if (count == 10 && what) {
                            int num = stream.read(buffer);
                            String progress = TypeConversion.bytesToHexStrings(buffer);
                            Log.i("progress", progress);
                            onReceiveMessageListener.onProgressUpdate(progress, 0);
                        } else if (count >= 10) {
                            what = false;
                            int num = stream.read(buffer);
                            String detect = TypeConversion.bytesToHexStrings(buffer);
                            builder.append(detect);
                            Log.i("detect", detect);
                            if (detect.endsWith("04 ")) {
                                number++;
                            }
                            if (number == 5) {
                                onReceiveMessageListener.onDetectDataFinish();
                                onReceiveMessageListener.onNewLine(builder.toString().trim());
                                builder.delete(0, builder.length());
                            } else {
                                onReceiveMessageListener.onDetectDataUpdate(detect);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                onReceiveMessageListener.onConnectionLost(e);
                mCurrStatus = STATUS.FREE;
            }
        }
    }

    /**
     * 输入bluetooth流线程
     */
    private class WriteRunnable implements Runnable {

        @Override
        public void run() {
            if (onSendMessageListener == null) {
                Log.i("blue", "onSendMessageListener为null！");
                return;
            }
            mWritable = true;
            while (mCurrStatus != STATUS.CONNECTED && mWritable)
                ;
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mOutputStream));
            while (mWritable) {
                MessageBean item = mMessageBeanQueue.poll();
                if (item.mTYPE != null && MessageBean.TYPE.STRING == item.mTYPE) {
                    try {
                        writer.write(item.text);
                        writer.newLine();
                        writer.flush();
                        Log.d(TAG, "send string message: " + item.text);
                        onSendMessageListener.onSuccess(Constants.STATUS_OK, "发送STRING类型消息的成功回调!");
                    } catch (IOException e) {
                        onSendMessageListener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                        break;
                    }
                } else if (item.mTYPE != null && MessageBean.TYPE.CHAR == item.mTYPE) {
                    try {
                        writer.write(item.data);
                        writer.flush();
                        Log.d(TAG, "send char message: " + item.data);
                        onSendMessageListener.onSuccess(Constants.STATUS_OK, "发送CHAR类型消息的成功回调!");
                    } catch (IOException e) {
                        onSendMessageListener.onConnectionLost(e);
                        mCurrStatus = STATUS.FREE;
                        break;
                    }
                }
            }

        }

    }

    /**
     * 校验
     *
     * @param o
     */
    private void checkNotNull(Object o) {
        if (o == null)
            throw new NullPointerException();
    }


}
