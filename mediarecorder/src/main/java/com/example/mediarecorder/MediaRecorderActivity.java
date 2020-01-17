package com.example.mediarecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.mediarecorder.audio.AudioManager;
import com.example.mediarecorder.audio.DialogManager;
import com.example.mediarecorder.view.ButtonTouchView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class MediaRecorderActivity extends AppCompatActivity implements AudioManager.OnVolumeChangeListener, View.OnTouchListener {

    private static final String TAG = "MediaRecorderTag";

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private AlertDialog recordDialogShow;
    private DialogManager dialogManager;
    private AudioManager audioManager;
    private MainHandler mainHander;
    private long time, downTime, number = 10;
    private String filePath = null;
    private float downY;
    private boolean isCanceled = false;
    private boolean isLastTime = false;
    //播放录音
    private Button mBtnPlay;
    //录音时长
    private TextView mTvTime;
    private LinearLayout mRecordFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_recorder);
        //初始化一些工具类和监听还有权限
        init();
        //
        initView();
    }

    /**
     * 初始化一些工具类和监听还有权限
     */
    private void init() {
        dialogManager = DialogManager.getInstance();
        audioManager = AudioManager.getInstance();
        //录音的音量监听
        audioManager.setOnVolumeChangeListener(this);
        //初始化dialog
        recordDialogShow = dialogManager.recordDialogShow(this);
        //避免内存泄漏得handler
        mainHander = new MainHandler(this);
        //动态申请权限
        ActivityCompat.requestPermissions(this, permissions, Constant.REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private void initView() {
        ButtonTouchView btnRecord = findViewById(R.id.btn_record);
        mBtnPlay = findViewById(R.id.btn_play);
        mTvTime = findViewById(R.id.tv_time);
        mRecordFile = findViewById(R.id.recordFile);
        btnRecord.setOnTouchListener(this);
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //                解决点击无效问题
                view.performClick();
                handlerActionDown(motionEvent);
                break;
            case MotionEvent.ACTION_UP:
                if (handlerActionUp()) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                handlerActionMove(motionEvent);
                break;
            case MotionEvent.ACTION_CANCEL:
                handlerActionCancel();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 处理点击操作
     *
     * @param motionEvent 移动事件
     */
    private void handlerActionDown(MotionEvent motionEvent) {
        Log.i(TAG, "  ACTION_DOWN ");
        downY = motionEvent.getY();
        audioManager.stopPlaying();
        dialogManager.updateUI(R.mipmap.listener00, getString(R.string.speaking));
        recordDialogShow.show();
        //记录开始录音时候的时间
        time = System.currentTimeMillis();
        downTime = time;
        //获取文件存放位置
        filePath = getFilePath();
        //开始录音
        audioManager.startRecording(filePath);
        mainHander.sendEmptyMessageDelayed(Constant.WHAT_SECOND_FINISH, Constant.DELAY_TIME_SHORT);
        isLastTime = false;
        number = 10;
    }

    /**
     * 处理抬手操作
     *
     * @return 语音是否超过1秒
     */
    private boolean handlerActionUp() {
        Log.i(TAG, " ACTION_UP ");
        audioManager.stopRecording();
        //1秒误按处理,不录制
        if (System.currentTimeMillis() - time < Constant.DELAY_TIME_SHORT) {
            dialogManager.updateUI(R.mipmap.no_voice, getString(R.string.speak_short));
            mainHander.sendEmptyMessageDelayed(Constant.WHAT_DIALOG_CLOSE, Constant.DELAY_TIME_SHORT);
            filePath = null;
            return true;
        }
        recordDialogShow.dismiss();
        //是否取消发送
        if (!isCanceled && !isLastTime) {
            //不取消发送
            mRecordFile.setVisibility(View.VISIBLE);
            mTvTime.setText(audioManager.getTime(filePath) / 1000 + "秒");
            mBtnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    audioManager.stopPlaying();
                    audioManager.startPlaying(filePath);
                }
            });

//            fileList.add(filePath);
//            recordAdapter.notifyDataSetChanged();
        }
        return false;
    }

    /**
     * 处理上滑取消，下滑发送
     *
     * @param motionEvent 移动事件2
     */
    private void handlerActionMove(MotionEvent motionEvent) {
        float moveY = motionEvent.getY();
        Log.i(TAG, " ACTION_MOVE downY=" + downY + " moveY=" + moveY);
        if (downY - moveY > Constant.VALUE_100) {
            Log.i(TAG, "上滑ing....");
            isCanceled = true;
            dialogManager.updateUI(R.mipmap.no_voice, getString(R.string.cancle_speaking));
        }
        if (downY - moveY < Constant.VALUE_20) {
            Log.i(TAG, "下滑ing....");
            isCanceled = false;
            dialogManager.updateUI(R.mipmap.listener00, getString(R.string.speaking));
        }
    }

    /**
     * 处理权限申请时的弹出框问题
     */
    private void handlerActionCancel() {
        Log.i(TAG, " ACTION_CANCEL ");
        recordDialogShow.dismiss();
        audioManager.stopRecording();
    }


    /**
     * 录音音量监听的回调
     *
     * @param value 分贝值
     */
    @Override
    public void onVolumeChange(double value) {
        Log.i(TAG, "当时获取到的音量为：" + value);
        int volume = (int) (value / Constant.VALUE_10);
        int mipmapId = volume;
        if (volume == Constant.VALUE_9 || volume == Constant.VALUE_10) {
            mipmapId = Constant.VALUE_8;
        }
        mipmapId = getResources().getIdentifier("listener0" + mipmapId, "mipmap", getPackageName());
        if (!isCanceled && !isLastTime) {
            dialogManager.updateUI(mipmapId, getString(R.string.speaking));
        }
    }


    /**
     * 防止handler的内存泄漏问题
     */
    private static class MainHandler extends Handler {
        private final WeakReference<MediaRecorderActivity> weakReference;

        private MainHandler(MediaRecorderActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MediaRecorderActivity activity = weakReference.get();
            if (null == activity) {
                return;
            }
            activity.handlerMessgae(msg);
        }
    }

    /**
     * handler的事件处理
     *
     * @param msg 消息
     */
    public void handlerMessgae(Message msg) {
        switch (msg.what) {
            case Constant.WHAT_DIALOG_CLOSE:
                if (recordDialogShow.isShowing()) {
                    recordDialogShow.dismiss();
                }
                break;
            case Constant.WHAT_SECOND_FINISH:
                if (downTime != time) {
                    return;
                }
                isLastTime = true;
                if (number == 0) {
                    dialogManager.updateUI(R.mipmap.warning, getString(R.string.speak_long));
                    audioManager.stopRecording();
                    recordDialogShow.dismiss();
                    isCanceled = true;
                }

                if (number > 0) {
                    int mipmapId = getResources().getIdentifier("num" + number, "mipmap", getPackageName());
                    dialogManager.updateUI(mipmapId, getString(R.string.speaking));
                    number--;
                    mainHander.sendEmptyMessageDelayed(Constant.WHAT_SECOND_FINISH, Constant.DELAY_TIME_SHORT);
                }

                break;
            default:
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            default:
                break;
        }
        if (!permissionToRecordAccepted) {
            finish();
        }
    }

    /**
     * 文件保存的本地位置
     *
     * @return 资源本地位置 filePath: /data/data/com.example.admin.weixinrecorddemo/cache/1513913361991.3gp
     */
    public String getFilePath() {
        String cachePath = getCacheDir().getAbsolutePath();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            //外部存储可用
            if (null != getExternalCacheDir()) {
                cachePath = getExternalCacheDir().getAbsolutePath();
            }
        }
        return String.format(Locale.getDefault(), "%1$s%2$s%3%d%4$s", cachePath, File.separator, System.currentTimeMillis(), ".mp3");
    }
}
