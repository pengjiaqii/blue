package com.example.mediarecorder.audio;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/16 13:41
 * 功能 :
 */
public class AudioManager {

    private final static String TAG = AudioManager.class.getSimpleName();
    private static AudioManager instance;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;


    public static AudioManager getInstance() {
        if (null == instance) {
            synchronized (AudioManager.class) {
                instance = new AudioManager();
            }
        }
        return instance;
    }

    /**
     * 开始录音
     *
     * @param filePath 录音地址
     */
    public void startRecording(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            Log.w(TAG, "录音保存地址不存在");
            return;
        }
        Log.i(TAG, "录音保存地址" + filePath);
        mRecorder = new MediaRecorder();
        //setAudioSource/setVideoSource,这里是设置麦克风
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
        //，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(filePath);
        //设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样
        //通用的ARM编码格式
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        updateMicStatus();
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "MediaRecorder prepare() failed");
        }
        try {
            mRecorder.start();
        } catch (IllegalStateException e) {
            Log.e(TAG, " mRecorder start is failed");
        }
    }

    /**
     * 停止录音
     */
    public void stopRecording() {
        if (null == mRecorder) {
            return;
        }
        mRecorder.release();
        mRecorder = null;
    }


    private final Handler mHandler = new Handler();
    private Runnable mUpdateMicStatusTimer = new Runnable() {
        @Override
        public void run() {
            updateMicStatus();
        }
    };

    /**
     * 更新话筒状态
     */
    private void updateMicStatus() {
        int base = 1, space = 100;
        if (mRecorder != null) {
            double ratio = (double) mRecorder.getMaxAmplitude() / base;
            // 分贝
            double db = 0;
            if (ratio > 1) {
                db = 20 * Math.log10(ratio);
            }
            Log.i(TAG, "音量值：" + db);
            onVolumeChangeListener.onVolumeChange(db);
            mHandler.postDelayed(mUpdateMicStatusTimer, space);
        }
    }

    /**
     * 开始播放
     *
     * @param url 播放地址
     */
    public void startPlaying(String url) {
        if (TextUtils.isEmpty(url)) {
            Log.w(TAG, "资源地址不存在");
            return;
        }
        Log.i(TAG, "播放地址为：" + url);
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(url);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
    }

    /**
     * 停止播放
     */
    public void stopPlaying() {
        if (null == mPlayer) {
            return;
        }
        mPlayer.release();
        mPlayer = null;
    }


    /**
     * 获取录音的时长
     *
     * @param fileName 录音的文件
     * @return 时长的毫秒值
     */
    public int getTime(String fileName) {
        int duration = 1000;
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fileName);
            mPlayer.prepare();
            duration = mPlayer.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "getTime is error");
        }
        stopPlaying();
        return duration;
    }


    public interface OnVolumeChangeListener {

        /**
         * 音量变化的监听回调
         *
         * @param value 分贝值
         */
        void onVolumeChange(double value);
    }

    private OnVolumeChangeListener onVolumeChangeListener;

    public void setOnVolumeChangeListener(OnVolumeChangeListener onVolumeChangeListener) {
        this.onVolumeChangeListener = onVolumeChangeListener;
    }

}
