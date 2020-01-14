package com.example.bluet;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bluet.ble.BleClientActivity;
import com.example.bluet.ble.BleServerActivity;
import com.example.bluet.bt.BtClientActivity;
import com.example.bluet.bt.BtServerActivity;
import com.example.bluetooth.util.ToastUtil;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查蓝牙开关
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            ToastUtil.showShortToast(MainActivity.this, "本机没有找到蓝牙硬件或驱动！");
            finish();
            return;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                //直接开启蓝牙
                mBluetoothAdapter.enable();
                //跳转到蓝牙设置界面
                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 112);
            }
        }

        // 检查是否支持BLE蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            ToastUtil.showShortToast(MainActivity.this, "本机不支持BLE低功耗蓝牙！");
            finish();
            return;
        }

        // Android 6.0动态请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE
                    , Manifest.permission.READ_EXTERNAL_STORAGE
                    , Manifest.permission.ACCESS_COARSE_LOCATION};
            for (String str : permissions) {
                if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, 111);
                    break;
                }
            }
        }
    }

    public void btClient(View view) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //防止第一次进入,用户并没有打开蓝牙
            ToastUtil.showShortToast(MainActivity.this, "请打开蓝牙");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 112);
        } else {
            startActivity(new Intent(this, BtClientActivity.class));
        }
    }

    public void btServer(View view) {
        startActivity(new Intent(this, BtServerActivity.class));
    }

    public void bleClient(View view) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            ToastUtil.showShortToast(MainActivity.this, "请打开蓝牙");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 112);
        } else {
            startActivity(new Intent(this, BleClientActivity.class));
        }
    }

    public void bleServer(View view) {
        startActivity(new Intent(this, BleServerActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}
