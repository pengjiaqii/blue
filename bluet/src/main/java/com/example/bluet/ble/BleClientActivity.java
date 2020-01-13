package com.example.bluet.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.example.bluet.R;
import com.example.bluetooth.ble.BleManager;
import com.example.bluetooth.ble.TypeConversion;
import com.example.bluetooth.ble.bean.MessageBean;
import com.example.bluetooth.ble.bean.SearchResult;
import com.example.bluetooth.ble.listener.OnConnectListener;
import com.example.bluetooth.ble.listener.OnReceiveMessageListener;
import com.example.bluetooth.ble.listener.OnSearchDeviceListener;
import com.example.bluetooth.ble.listener.OnSendMessageListener;

import java.util.ArrayList;
import java.util.List;


/**
 * BLE客户端(主机/中心设备/Central)
 */
public class BleClientActivity extends AppCompatActivity {
    public static final String TAG = "BleClientActivity";
    private BleManager bluemanage;
    private int progress = 0;
    //连接状态
    private TextView statusView;
    private TextView contextView;
    private ProgressBar progressBar;
    private StringBuilder stringBuilder;
    //设备数据
    private List<SearchResult> mDevices;
    //设备列表的adapter
    private DeviceListAdapter mAdapter;
    //列表
    private RecyclerView recycleView;
    private RelativeLayout devieslist;
    private RelativeLayout deviesinfo;
    //与某个设备连接的监听
    private OnConnectListener onConnectListener;
    //发送消息的监听
    private OnSendMessageListener onSendMessageListener;
    //搜索蓝牙设备的监听
    private OnSearchDeviceListener onSearchDeviceListener;
    //接收消息的监听
    private OnReceiveMessageListener onReceiveMessageListener;
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        // 0 修改状态  1 更新进度  2 体检完成  3 体检数据进度 4 连接成功
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.obj.toString();
            switch (msg.what) {
                case 0:
                    statusView.setText(message);
                    break;
                case 1:
                    stringBuilder.append(message + " \n");
                    contextView.setText(stringBuilder.toString());
                    progress += 4;
                    progressBar.setProgress(progress);
                    break;
                case 2:
                    progress = 100;
                    progressBar.setProgress(progress);
                    break;
                case 3:
                    statusView.setText("接收完成！");
                    stringBuilder.delete(0, stringBuilder.length());
                    stringBuilder.append(message);
                    contextView.setText(stringBuilder.toString());
                    break;
                case 4:
                    statusView.setText(message);
                    deviesinfo.setVisibility(View.VISIBLE);
                    devieslist.setVisibility(View.GONE);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleclient);

        mDevices = new ArrayList<>();
        mAdapter = new DeviceListAdapter(R.layout.device_list_item, mDevices);
        stringBuilder = new StringBuilder();
        devieslist = findViewById(R.id.parent_r1);
        deviesinfo = findViewById(R.id.parent_r2);
        progressBar = findViewById(R.id.progressbar);
        recycleView = findViewById(R.id.blue_rv);
        recycleView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        contextView = findViewById(R.id.context);
        statusView = findViewById(R.id.status);
        recycleView.setAdapter(mAdapter);
        //蓝牙权限申请，特别注意这个位置权限，6.0以上不添加这个权限搜不到蓝牙
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(BleClientActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS)) {
                    Toast.makeText(BleClientActivity.this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
                }
            }
        }
        //初始化蓝牙管理，设置监听
        initBlueManager();
        //为控件添加点击事件监听
        initListener();
    }

    /**
     * 初始化蓝牙管理，设置监听
     */
    public void initBlueManager() {
        //搜索蓝牙设备的监听
        onSearchDeviceListener = new OnSearchDeviceListener() {
            @Override
            public void onStartDiscovery() {
                sendMessage(0, "正在搜索设备..");
                Log.d(TAG, "onStartDiscovery()");

            }

            @Override
            public void onNewDeviceFound(BluetoothDevice device) {
                Log.d(TAG, "new device: " + device.getName() + " " + device.getAddress());
            }

            @Override
            public void onSearchCompleted(List<SearchResult> bondedList, List<SearchResult> newList) {
                Log.d(TAG, "搜索完成: bondedList" + bondedList.toString());
                Log.d(TAG, "搜索完成: newList" + newList.toString());
                sendMessage(0, "搜索完成,点击列表进行连接！");
                mDevices.clear();
                mDevices.addAll(newList);
                mAdapter.notifyDataSetChanged();
                deviesinfo.setVisibility(View.GONE);
                devieslist.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Exception e) {
                sendMessage(0, "搜索失败");
            }
        };
        //与某个设备连接的监听
        onConnectListener = new OnConnectListener() {
            @Override
            public void onConnectStart() {
                sendMessage(0, "开始连接");
                Log.i("blue", "onConnectStart");
            }

            @Override
            public void onConnecting() {
                sendMessage(0, "正在连接..");
                Log.i("blue", "onConnecting");
            }

            @Override
            public void onConnectFailed() {
                sendMessage(0, "连接失败！");
                Log.i("blue", "onConnectFailed");

            }

            @Override
            public void onConnectSuccess(String mac) {
                sendMessage(4, "连接成功 MAC: " + mac);
                Log.i("blue", "onConnectSuccess");
            }

            @Override
            public void onError(Exception e) {
                sendMessage(0, "连接异常！");
                Log.i("blue", "onError");
            }
        };
        //发送消息的监听
        onSendMessageListener = new OnSendMessageListener() {
            @Override
            public void onSuccess(int status, String response) {
                sendMessage(0, "发送成功！");
                Log.i("blue", "发送消息成功! ");
            }

            @Override
            public void onConnectionLost(Exception e) {
                sendMessage(0, "连接断开！");
                Log.i("blue", "发送消息为onConnectionLost ! ");
            }

            @Override
            public void onError(Exception e) {
                sendMessage(0, "发送失败！");
                Log.i("blue", "发送消息为onError ! ");
            }
        };
        //接收消息的监听
        onReceiveMessageListener = new OnReceiveMessageListener() {


            @Override
            public void onProgressUpdate(String what, int progress) {
                sendMessage(1, what);
            }

            @Override
            public void onDetectDataUpdate(String what) {
                sendMessage(3, what);
            }

            @Override
            public void onDetectDataFinish() {
                sendMessage(2, "接收完成！");
                Log.i("blue", "接收消息为onDetectDataFinish");
            }

            @Override
            public void onNewLine(String s) {
                sendMessage(3, s);
            }

            @Override
            public void onConnectionLost(Exception e) {
                sendMessage(0, "连接断开");
                Log.i("blue", "接收消息为onConnectionLost! ");
            }

            @Override
            public void onError(Exception e) {
                Log.i("blue", "接收消息为onError ! ");
            }
        };
        //BLE蓝牙的管理器，封装起来的工具类
        bluemanage = BleManager.getInstance(getApplicationContext());
        bluemanage.setOnSearchDeviceListener(onSearchDeviceListener);
        bluemanage.setOnConnectListener(onConnectListener);
        bluemanage.setOnSendMessageListener(onSendMessageListener);
        bluemanage.setOnReceiveMessageListener(onReceiveMessageListener);
        bluemanage.requestEnableBt();
    }

    /**
     * 为控件添加事件监听
     */
    public void initListener() {
        //蓝牙设备列表的点击事件
        mAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                String mac = mDevices.get(position).getAddress();
                bluemanage.connectDevice(mac);
            }
        });
        //搜索设备的点击事件
        findViewById(R.id.btn_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluemanage.setReadVersion(false);
                //开始搜索
                bluemanage.searchDevices();
            }
        });

        findViewById(R.id.get_sn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageBean item = new MessageBean(TypeConversion.getDeviceVersion());
                bluemanage.setReadVersion(true);
                bluemanage.sendMessage(item, true);
            }
        });

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluemanage.closeDevice();
                contextView.setText(null);
                devieslist.setVisibility(View.VISIBLE);
                deviesinfo.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluemanage.setReadVersion(false);
                progress = 0;
                progressBar.setProgress(progress);
                stringBuilder.delete(0, stringBuilder.length());
                contextView.setText("");
                MessageBean item = new MessageBean(TypeConversion.startDetect());
                bluemanage.sendMessage(item, true);
            }
        });
    }

    /**
     * @param type    0 修改状态  1 更新进度  2 体检完成  3 体检数据进度
     * @param context
     */
    public void sendMessage(int type, String context) {
        if (handler != null) {
            Message message = handler.obtainMessage();
            message.what = type;
            message.obj = context;
            handler.sendMessage(message);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 2) {
            if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.
                        permission.ACCESS_COARSE_LOCATION)) {
                    return;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluemanage != null) {
            bluemanage.close();
            bluemanage = null;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

    }

}