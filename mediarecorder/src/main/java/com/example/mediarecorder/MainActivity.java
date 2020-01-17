package com.example.mediarecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MediaRecorderTag";

    private static final int GET_RECODE_AUDIO = 0x55;
    private Button mBtnControl;
    private TextView mFilePathTv;
    private ProgressBar mLoadingPb;
    private boolean isStart = false;
    //MediaRecorder，用来录制音频
    private MediaRecorder mMediaRecorder = null;
    //音频文件保存地址
    private String filePath;
    private long mStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnControl = (Button) findViewById(R.id.btn_control);
        mFilePathTv = (TextView) findViewById(R.id.file_path);
        mLoadingPb = (ProgressBar) findViewById(R.id.loading);
        //请求权限
        requestRecordAudioPermission();

        mBtnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStart) {
                    startRecord();
                    mBtnControl.setText("停止录制");
                    isStart = true;
                } else {
                    stopRecord();
                    mBtnControl.setText("开始录制");
                    isStart = false;
                }
            }
        });
    }

    /**
     * 申请录音权限
     */
    private void requestRecordAudioPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, GET_RECODE_AUDIO);

        }
    }


    /**
     * 开始录制
     */
    private void startRecord() {
        // 开始录音
        // Initial：实例化MediaRecorder对象
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        try {
            //setAudioSource/setVideoSource,这里是设置麦克风
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //
            //设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
            //，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
            //
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // 设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样
            //通用的ARM编码格式
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //所有android系统都支持的适中采样的频率
            mMediaRecorder.setAudioSamplingRate(44100);
            //设置最大录音时间，单位毫秒
            mMediaRecorder.setMaxDuration(15000);
            //设置音质频率
            mMediaRecorder.setAudioEncodingBitRate(1024 * 1024);

            String fileName = DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA)) + ".m4a";
            File file = new File("/sdcard/testRecord");
            if (!file.exists()) {
                file.mkdir();
            }
            filePath = file.getAbsolutePath() + "/" + fileName;
            //准备
            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.prepare();
            // 开始
            mMediaRecorder.start();
            //一些状态文案设置
            mLoadingPb.setVisibility(View.VISIBLE);
            mFilePathTv.setVisibility(View.VISIBLE);
            mFilePathTv.setText("正在录制...");
            //记录开始时间
            mStartTime = System.currentTimeMillis();

        } catch (IllegalStateException | IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * 停止录制，资源释放
     */
    private void stopRecord() {
        long duration = (int) ((System.currentTimeMillis() - mStartTime) / 1000);
        Log.d(TAG, "duration:" + duration);
        if (duration < 1000) {
            return;
        }


        mLoadingPb.setVisibility(View.INVISIBLE);
        mFilePathTv.setText(String.format("文件路径位置：%s", filePath));

        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            filePath = "";
        } catch (RuntimeException e) {
            System.out.println(e.toString());
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            File file = new File(filePath);
            if (file.exists())
                file.delete();

            filePath = "";
        }
    }
}
