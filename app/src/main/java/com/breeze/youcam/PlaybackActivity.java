package com.breeze.youcam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.breeze.nativelib.GL2JNIView;
import com.breeze.nativelib.NativeLib;
import com.breeze.tools.Utility;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.st_SearchDeviceInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PlaybackActivity extends Activity {
    GL2JNIView mGLView;
    EventItem mEventItem = null;
    FileInputStream mInputStream = null;
    int mTotalBytes = 0;
    int mCurrentByte = 0;
    long mStartTime;

    int mFps = 0;
    final int FRAME_HEADER = 8; //length+timestamp
    final int FILE_HEADER = 16; //4CC+fps+time_in_mili
    ProgressBar mProgressBar;
    String TAG = "P2pMain";
    final int STATUS_BIT_NONE = 0;      // b0   0- no media;1- available
    final int STATUS_BIT_READY = 1;
    final int STATUS_BIT_PLAY = 2;      // b1   0- stop     1- playing
    final int STATUS_BIT_BACKWARD = 4;  // b2   0- forward  1- backward
    final int STATUS_BIT_FAST = 8;      // b3   0- normal     1- fast
    final int MASK_PLAYING_STATUS = 0x03; //11 playing 01 stop
    ProgressBar mPositionBar;
    int mStatus = 0;
    public class MediaFile{
        public long startTime;
        public long endTime;
        public long currentTime;
        public FileInputStream is;

    }
    MediaFile mCurrentMedia = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        mGLView = (GL2JNIView)findViewById(R.id.viewVideo);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mEventItem = (EventItem) getIntent().getSerializableExtra("EventItem");
        mStatus = STATUS_BIT_NONE;
        mPositionBar = (ProgressBar) findViewById(R.id.positionBar);

        if(mEventItem != null) {
            startLoadVideo();
            Log.d(TAG, "Now loading .. " + mEventItem.note);
        }

        updateUiButtons();

    }

    @Override
    public void onResume() {
        super.onResume();
        mGLView.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        mGLView.onPause();
    }
    public final int MESSAGE_LOAD_OK = 1;
    public final int MESSAGE_LOAD_FAILED = 2;
    public final int MESSAGE_PLAY_PROGRESS_UPDATE = 3;
    public final int MESSAGE_PLAY_ERROR = 4;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mProgressBar.setVisibility(View.GONE);
            switch(msg.what){
                case MESSAGE_LOAD_OK:
                    mStatus = STATUS_BIT_READY;
                    mPositionBar.setVisibility(View.VISIBLE);
                    break;
                case MESSAGE_LOAD_FAILED:
                    mStatus = STATUS_BIT_NONE;
                    mPositionBar.setVisibility(View.GONE);
                    break;
                case MESSAGE_PLAY_PROGRESS_UPDATE:
                    mPositionBar.setProgress(mCurrentByte);
                    break;
                case MESSAGE_PLAY_ERROR:
                    mStatus = STATUS_BIT_READY; //stop
                    updateUiButtons();
                    break;

            }
            updateUiButtons();
        }

    };

    boolean decodeOneFrame(byte[] buffer, int time){
        ByteBuffer bits = ByteBuffer.allocateDirect(buffer.length);
        bits.put(buffer, 0, buffer.length);
        bits.flip();
        NativeLib.decodeNal(bits, buffer.length, time / 1000);
        return true;
    }
    boolean openFile(String path) {
        byte[] header = new byte[4];
        try {
            mInputStream = new FileInputStream(path);
            mTotalBytes = mInputStream.available();
            if (mTotalBytes <= FILE_HEADER) {
                Log.e(TAG, "File no frame data: "+mTotalBytes);
                mInputStream = null;
                return false;
            }
            //check file header
            mInputStream.read(header);

            if (header[0] != 'h' || header[1] != '2' || header[2] != '6' || header[3] != '4')
            {
                Log.e(TAG, "Not a recognized video file: "+path);
                mInputStream = null;
                return false;
            }
            mInputStream.read(header);
            mFps = Utility.byteArrayToInt(header);
            Log.d(TAG, "fps: "+mFps);
            byte[] data8 = new byte[8];
            mInputStream.read(data8);
            mStartTime = Utility.byteArrayToLong(data8);
            mTotalBytes -= FILE_HEADER;
            mCurrentByte = 0;
            mPositionBar.setMax(mTotalBytes);
            mPositionBar.setProgress(mCurrentByte);
Log.d(TAG, "Open file length = "+mTotalBytes + "bytes");
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }

        return true;
    }
    void startPlay() {
        Thread palayingFile = new Thread( new Runnable() {

            @Override
            public void run() {
                if (mInputStream == null)
                    return;
                byte[] pLength = new byte[4];
                byte[] pTimeStamp = new byte[4];
                try {
                    while (mCurrentByte < mTotalBytes) {
                        mInputStream.read(pLength);
                        mInputStream.read(pTimeStamp);
                        int length = Utility.byteArrayToInt(pLength);
                        int time = Utility.byteArrayToInt(pTimeStamp);
                        byte[] inputBuffer = new byte[length];
                        mInputStream.read(inputBuffer);
                        Log.d(TAG, "decode one frame: " + time + ":" + length);
                        if( ! decodeOneFrame(inputBuffer, time))
                            break;
                        mCurrentByte += (length +FRAME_HEADER);
                        //update position
                        Message msg = new Message();
                        msg.what = MESSAGE_PLAY_PROGRESS_UPDATE;
                        mHandler.sendMessage(msg);
                        //check mode and action
                        if((mStatus & STATUS_BIT_PLAY) == 0)
                            break;
                    }

                } catch(IOException e) {
                    e.printStackTrace();
                    //force stop
                    Message msg = new Message();
                    msg.what = MESSAGE_PLAY_ERROR;
                    mHandler.sendMessage(msg);
                }
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "palayingFile");

        palayingFile.start();
    }
    void startLoadVideo () {
        mProgressBar.setVisibility(View.VISIBLE);

        Thread loadingFile = new Thread( new Runnable() {

            @Override
            public void run() {
                Message msg = new Message();
                msg.what = (openFile(mEventItem.note))?
                        MESSAGE_LOAD_OK: MESSAGE_LOAD_FAILED;
                mHandler.sendMessage(msg);
            }
        }, "loadVideo");

        loadingFile.start();
    }
    //
    private static final byte[] NAL_START_CODE = new byte[] {0, 0, 0, 1};
    /**
     * Finds the next occurrence of the NAL start code from a given index.
     *
     * @param data The data in which to search.
     * @param index The first index to test.
     * @return The index of the first byte of the found start code, or -1.
     */
    private static int findNalStartCode(byte[] data, int index) {
        int endIndex = data.length - NAL_START_CODE.length;
        for (int i = index; i <= endIndex; i++) {
            if (isNalStartCode(data, i)) {
                return i;
            }
        }
        return -1;
    }
    private static boolean isNalStartCode(byte[] data, int index) {
        if (data.length - index <= NAL_START_CODE.length) {
            return false;
        }
        for (int j = 0; j < NAL_START_CODE.length; j++) {
            if (data[index + j] != NAL_START_CODE[j]) {
                return false;
            }
        }
        return true;
    }
    /**
     * Splits an array of NAL units.
     * <p>
     * If the input consists of NAL start code delimited units, then the returned array consists of
     * the split NAL units, each of which is still prefixed with the NAL start code. For any other
     * input, null is returned.
     *
     * @param data An array of data.
     * @return The individual NAL units, or null if the input did not consist of NAL start code
     *     delimited units.
     */
    public static byte[][] splitNalUnits(byte[] data) {
        if (!isNalStartCode(data, 0)) {
            // data does not consist of NAL start code delimited units.
            return null;
        }
        List<Integer> starts = new ArrayList<Integer>();
        int nalUnitIndex = 0;
        do {
            starts.add(nalUnitIndex);
            nalUnitIndex = findNalStartCode(data, nalUnitIndex + NAL_START_CODE.length);
        } while (nalUnitIndex != -1);
        byte[][] split = new byte[starts.size()][];
        for (int i = 0; i < starts.size(); i++) {
            int startIndex = starts.get(i);
            int endIndex = i < starts.size() - 1 ? starts.get(i + 1) : data.length;
            byte[] nal = new byte[endIndex - startIndex];
            System.arraycopy(data, startIndex, nal, 0, nal.length);
            split[i] = nal;
        }
        return split;
    }
    void updateUiButtons() {
        Button btn = (Button) findViewById(R.id.btnPlay);
        btn.setEnabled((mStatus & STATUS_BIT_READY)!=0|| (mStatus!= 0x3) ); //x0
        btn = (Button) findViewById(R.id.btnPause);
        btn.setEnabled( (mStatus & MASK_PLAYING_STATUS) == 0x03);
        btn = (Button) findViewById(R.id.btnBackward);
        btn.setEnabled( mStatus != (STATUS_BIT_BACKWARD|STATUS_BIT_PLAY|STATUS_BIT_READY));
        btn = (Button) findViewById(R.id.btnForward);
        btn.setEnabled( mStatus != (STATUS_BIT_FAST|STATUS_BIT_PLAY|STATUS_BIT_READY));



    }
    public void onClickPlay(View v) {
        mStatus = STATUS_BIT_PLAY|STATUS_BIT_READY;
        mPositionBar.setProgress(0);
        startPlay();
        updateUiButtons();
    }
    public void onClickStop(View v) {
        mStatus = STATUS_BIT_READY;
        //play thread will stop
        updateUiButtons();
    }
    public void onClickBegin(View v) {
        //set pos = 0
        if((mStatus & STATUS_BIT_BACKWARD )!=0)
            mStatus = STATUS_BIT_READY;
        mPositionBar.setProgress(0);
        updateUiButtons();
    }
    public void onClickEnd(View v) {
        //set pos = 0
        if((mStatus & STATUS_BIT_BACKWARD )==0)
            mStatus = STATUS_BIT_READY;
        mPositionBar.setProgress(100);
        updateUiButtons();
    }

    public void onClickBackward(View v) {
        mStatus = STATUS_BIT_BACKWARD|STATUS_BIT_PLAY|STATUS_BIT_READY;
        mPositionBar.setProgress(mPositionBar.getProgress() - 10);
        updateUiButtons();
    }
    public void onClickForward(View v) {
        mStatus = STATUS_BIT_FAST|STATUS_BIT_PLAY|STATUS_BIT_READY;
        mPositionBar.setProgress(mPositionBar.getProgress() + 30);
        updateUiButtons();
    }
}
