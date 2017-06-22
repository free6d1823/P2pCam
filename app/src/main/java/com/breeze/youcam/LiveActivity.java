package com.breeze.youcam;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

public class LiveActivity extends Activity {
    final String TAG = "P2pMain";
    DeviceInfo mDi = null;
    FrameLayout mProgressBar;
    TextView mTvVbps;
    TextView mTvAbps;
    TextView mTvFps;
    TextView mTvStatus;
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
            case R.id.btnSnapshot:
                break;
            case R.id.btnSkeaker:
                break;
            case R.id.btnMic:
                break;
            case R.id.btnRecord:
                break;
            case R.id.btnMode:
                break;
            case R.id.tvStatus:
                if(mDi != null) {
                    if (mDi.status == mDi.STATUS_STARTED)
                        mDi.stop();
                    else
                        mDi.start();
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
                        di.start();
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

                        bps = (p2p.mAudioTotalBytes- mLastABytes) /dT;//Kbps
                        mTvAbps.setText("A:"+bps+"kbps");
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
}
