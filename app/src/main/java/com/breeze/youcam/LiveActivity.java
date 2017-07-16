package com.breeze.youcam;

import android.content.Intent;
import android.icu.text.AlphabeticIndex;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.breeze.tools.FileData;
import com.breeze.tools.RecordUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class LiveActivity extends Activity implements P2pClient.OnP2pDataListener{
    final String TAG = "P2pMain";
    DeviceInfo mDi = null;
    FrameLayout mProgressBar;
    TextView mTvVbps;
    TextView mTvAbps;
    TextView mTvFps;
    TextView mTvStatus;

    boolean mIsRecordOn = false;
    boolean mIsSnapshotOn = false;
    boolean mIsMicOn = false;
    boolean mIsSpeakerOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_live);

        Intent intent = getIntent();
        String uuid = intent.getStringExtra(MainActivity.EXTRA_DEVICE_DETAIL);
        mDi = MainActivity.mPairedList.findByUuid(uuid);
        initUi();
        start();
    }
    void initUi() {
        mProgressBar = (FrameLayout) findViewById(R.id.progressBarHolder);
        mTvVbps = (TextView)findViewById(R.id.tvVbps);
        mTvAbps = (TextView)findViewById(R.id.tvAbps);
        mTvFps = (TextView)findViewById(R.id.tvFps);
        mTvStatus = (TextView)findViewById(R.id.tvStatus);
    }
    //the handler for message from other thread
    public final int MESSAGE_STARTED = 3;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage MESSAGE_STARTED - msg = " + msg.what);
            mProgressBar.setVisibility(View.GONE);
            switch(msg.what){
                case MESSAGE_STARTED:
                    MainActivity.mPairedList.notifyDataSetChanged();
                    break;
            }
        }

    };


    public void onClick(View v) {
        int id = v.getId();
        Log.d (TAG, "onClick "+ id);
        switch (id) {
            case R.id.btnQuality:
                Log.d(TAG, " -- setQuality");
                break;
            case R.id.btnResolution:
                Log.d(TAG, " -- setResolution");
                break;
            case R.id.btnBitrate:
                break;
            case R.id.btnMirror:
                break;
            case R.id.btnMode:
                break;
            case R.id.tvStatus:
                if(mDi != null) {
                    if (mDi.status == mDi.STATUS_STARTED) {
                        mDi.stop();

                    }
                    else {
                        mDi.start(this);

                    }
                }
                break;

        }
    }

    public class MyRunnable implements Runnable {
        DeviceInfo di;
        int command;
        public MyRunnable(DeviceInfo d, int cmd) {
            di = d;command = cmd;
        }
        Long tmStart;
        long mLastFrame;
        long mLastVBytes;
        long mLastABytes;
        @Override
        public void run() {
            mTvStatus.setText(di.statusString());

            switch(command){
                case R.id.cmd_start_stream:
                    tmStart =  System.currentTimeMillis();
                    if (di.status == di.STATUS_PAIRED){
                        di.login();
                        command = R.id.cmd_start_stream;
                    } else if (di.status == di.STATUS_CONNECTED) {
                        di.start(LiveActivity.this);
                        mProgressBar.setVisibility(View.GONE);
                        MainActivity.mPairedList.notifyDataSetChanged();

                        mLastFrame = 0;
                        mLastVBytes = 0;
                        mLastABytes = 0;
                        command = R.id.cmd_none;

                    } else if (di.status == di.STATUS_STARTED) {
                        P2pClient p2p = di.getP2pClient();
                        mLastFrame = p2p.mTotalFrames;
                        mLastVBytes = p2p.mVideoTotalBytes;
                        mLastABytes = p2p.mAudioTotalBytes;
                        command = R.id.cmd_none;
                    }
                    mHandler.postDelayed(mRunnable, 1000);
                    break;
                case R.id.cmd_stop_stream:
                    command = R.id.cmd_none;
                    di.stop();
                    mTvStatus.setText(di.statusString());
                    break;
                default: //keep streaming
                    if (di.status >= di.STATUS_STARTED){

                        P2pClient p2p = di.getP2pClient();
                        Long now = System.currentTimeMillis();
                        Long dT = now - tmStart;
                        tmStart = now;

                        long bps;
                        bps = (p2p.mVideoTotalBytes - mLastVBytes) /dT;//Kbps
                        mTvVbps.setText("V:"+bps+"kbps");

                        mLastVBytes = p2p.mVideoTotalBytes;

                        bps = (p2p.mAudioTotalBytes- mLastABytes)*1000 /dT;//Kbps
                        mTvAbps.setText("A:"+bps+"bps");
                        mLastABytes = p2p.mAudioTotalBytes;

                        bps = (p2p.mTotalFrames- mLastFrame)*1000 /dT;//fps
                        mTvFps.setText(bps+" f/s");
                        mLastFrame = p2p.mTotalFrames;


                        mHandler.postDelayed(mRunnable, 3000);
                    }

                    break;
            }

        }
    }
    MyRunnable mRunnable = null;
    void start() {
        mProgressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "start progress on");
        mHandler = new Handler();
        mHandler.removeCallbacks(mRunnable);
        mRunnable = new MyRunnable(mDi, R.id.cmd_start_stream);
        mHandler.post(mRunnable);

        //Thread task = new Thread( new MyRunnable(mDi), "live streaming");
        //task.start();
    }
    void stop() {
        mHandler.removeCallbacks(mRunnable);
        mRunnable = new MyRunnable(mDi, R.id.cmd_stop_stream);
        mHandler.post(mRunnable);
    }

    FileData mVideoRecordFile = null;
    FileData mAudioRecordFile = null;
    public void onClickSnapshot(View v) {
        mIsSnapshotOn = !mIsSnapshotOn;

        v.setBackgroundResource(mIsSnapshotOn? R.drawable.btn_snapshot_on: R.drawable.btn_snapshot_off);

    }
    public void onClickRecord(View v) {
        mIsRecordOn = !mIsRecordOn;
        if (mIsRecordOn == false){
            if (mVideoRecordFile != null) {
                mVideoRecordFile.close();
                mVideoRecordFile = null;
            }
            if (mAudioRecordFile != null) {
                mAudioRecordFile.close();
                mAudioRecordFile = null;
            }
        }
        v.setBackgroundResource(mIsRecordOn? R.drawable.btn_record_on: R.drawable.btn_record_off);
    }
    public void onClickMic(View v) {
        mIsMicOn = !mIsMicOn;

        v.setBackgroundResource(mIsMicOn? R.drawable.btn_mic_on: R.drawable.btn_mic_off);

    }
    public void onClickSpeaker(View v) {
        mIsSpeakerOn = !mIsSpeakerOn;

        v.setBackgroundResource(mIsSpeakerOn? R.drawable.btn_speaker_on: R.drawable.btn_speaker_off);

    }

    @Override
    public void onVideoCallback(byte[] buffer, int length, int timestamp) {
        if (mIsRecordOn) {
            if (mVideoRecordFile == null) {
                Calendar tmNow = Calendar.getInstance();

                mVideoRecordFile = RecordUtility.createFile(tmNow, EventItem.TYPE_LOCAL_VIDEO, true);
                if (mVideoRecordFile == null)
                    return;
                Log.d(TAG, "Create video file: "+mVideoRecordFile.getFileName());
                //open audio
                if(mAudioRecordFile != null)
                    mAudioRecordFile.close();
                mAudioRecordFile = RecordUtility.createFile(tmNow, EventItem.TYPE_LOCAL_VIDEO, false);
                if (mAudioRecordFile == null)
                    Log.e(TAG, "Failed to create audio file: "+mAudioRecordFile.getFileName());
            }
            mVideoRecordFile.write(buffer);
            //check if need close
            mVideoRecordFile = RecordUtility.checkFileClose(mVideoRecordFile);
            if(null == mVideoRecordFile){
                Log.d(TAG, "Video file closed.");
                //check if need close
                mAudioRecordFile.close();
                mAudioRecordFile = null;
                Log.d(TAG, "Audio file closed too.");

            }
        }
        //decode and display
    }

    @Override
    public void onAudioCallback(byte[] buffer, int length, int timestamp) {
        if (mIsRecordOn) {
            if (mAudioRecordFile == null) {
                return; //audio must always follow video
            }
            mAudioRecordFile.write(buffer);

        }
    }

}
