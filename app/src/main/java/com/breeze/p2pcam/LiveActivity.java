package com.breeze.p2pcam;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

public class LiveActivity extends Activity {
    final String TAG = "P2pMain";
    DeviceInfo mDi = null;
    FrameLayout mProgressBar;
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

        }
    }

    public class MyRunnable implements Runnable {
        DeviceInfo di;
        public MyRunnable(DeviceInfo d) {
            di = d;
        }
        @Override
        public void run() {
            Log.d(TAG, "Started status was "+ di.statusString()  );
            boolean ret = di.start();
            Log.d(TAG, "Started status="+ di.statusString() + "ret="+ret);
            Message msg = new Message();
            msg.what = MESSAGE_STARTED;
            mHandler.sendMessage(msg);
        }
    }
    void start() {
        mProgressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "start progress on");
        Thread task = new Thread( new MyRunnable(mDi), "live streaming");
        task.start();
    }
}
