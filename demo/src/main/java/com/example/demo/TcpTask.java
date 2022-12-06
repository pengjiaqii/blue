package com.example.demo;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class TcpTask {
    private static volatile TcpTask instance;
    private static final String TAG = "TcpTask";
    //Socket
    private Socket socket;
    //IP地址
    private String ipAddress;
    //端口号
    private int port;
    private Thread thread;
    //Socket输出流
    private OutputStream outputStream;
    //Socket输入流
    private InputStream inputStream;
    //连接回调
    private OnServerConnectedCallbackBlock connectedCallback;
    //断开连接回调(连接失败)
    private OnServerDisconnectedCallbackBlock disconnectedCallback;
    //接收信息回调
    private OnReceiveCallbackBlock receivedCallback;

    //构造函数私有化
    private TcpTask() {

    }

    //单例
    public static TcpTask sharedCenter() {
        if (instance == null) {
            synchronized (TcpTask.class) {
                if (instance == null) {
                    instance = new TcpTask();
                }
            }
        }
        return instance;
    }

    /**
     * 通过IP地址(域名)和端口进行连接
     *
     * @param ipAddress IP地址(域名)
     * @param port      端口
     */
    public void connect(final String ipAddress, final int port) {

        thread = new Thread(() -> {
            try {
                socket = new Socket(ipAddress, port);

                //socket.setSoTimeout ( 2 * 1000 );//设置超时时间
                Log.d(TAG, "socket 连接状态isConnected：" + isConnected());
                if (isConnected()) {
                    TcpTask.sharedCenter().ipAddress = ipAddress;
                    TcpTask.sharedCenter().port = port;

                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                    Log.d(TAG, "连接成功");
                    if (connectedCallback != null) {
                        connectedCallback.callback();
                    }
                    receive();

                } else {
                    Log.d(TAG, "连接失败");
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback(new IOException("连接失败"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "连接异常:" + e);
                if (disconnectedCallback != null) {
                    disconnectedCallback.callback(e);
                }
            }
        });
        thread.start();
    }

    /**
     * 判断是否连接
     */
    public boolean isConnected() {
        if (socket != null) {
            return socket.isConnected();
        } else {
            return false;
        }
    }

    /**
     * 连接
     */
    public void connect() {
        connect(ipAddress, port);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        Log.d(TAG, "disconnect---isConnected---" + isConnected());
        if (isConnected()) {
            try {
                Log.d(TAG, "disconnect断开连接");
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                    if (socket.isClosed()) {
                        if (disconnectedCallback != null) {
                            disconnectedCallback.callback(new IOException("断开连接"));
                        }
                        socket = null;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "disconnect断开连接出异常啦：" + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * 接收数据
     */
    public synchronized void receive() {
        Log.d(TAG, "开始接收");
        while (isConnected()) {
            try {
                //得到的是16进制数，需要进行解析
                byte[] bt = new byte[1024];
                // 获取接收到的字节和字节数
                int length = inputStream.read(bt);
                Log.d(TAG, "接收到的数据长度length：" + length);
                //没数据的话就return算了
                if (length <= 0) {
                    return;
                }
                //获取正确的字节
                byte[] bs = new byte[length];
                System.arraycopy(bt, 0, bs, 0, length);

                String str = new String(bs, "UTF-8");
                if (str != null) {
                    if (receivedCallback != null) {
                        receivedCallback.callback(str);
                    }
                }
                Log.d(TAG, "接收成功");
            } catch (IOException e) {
                Log.d(TAG, "接收失败：" + e);
                disconnect();
            }
        }
    }

    /**
     * 发送数据
     *
     * @param data 数据
     */
    public void send(final byte[] data) {

        Log.d("TcpTask", "data:" + Arrays.toString(data));
        new Thread(() -> {
            if (socket != null) {
                try {
                    outputStream.write(data);
                    outputStream.flush();
                    Log.d(TAG, "发送成功");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "发送失败:" + e);
                }
            } else {
                //connect();
            }
        }).start();

    }

    /**
     * 回调声明
     */
    public interface OnServerConnectedCallbackBlock {
        void callback();
    }

    public interface OnServerDisconnectedCallbackBlock {
        void callback(IOException e);
    }

    public interface OnReceiveCallbackBlock {
        void callback(String receicedMessage);
    }

    public void setConnectedCallback(OnServerConnectedCallbackBlock connectedCallback) {
        this.connectedCallback = connectedCallback;
    }

    public void setDisconnectedCallback(OnServerDisconnectedCallbackBlock disconnectedCallback) {
        this.disconnectedCallback = disconnectedCallback;
    }

    public void setReceivedCallback(OnReceiveCallbackBlock receivedCallback) {
        this.receivedCallback = receivedCallback;
    }

    /**
     * 移除回调
     */
    private void removeCallback() {
        connectedCallback = null;
        disconnectedCallback = null;
        receivedCallback = null;
    }
}
