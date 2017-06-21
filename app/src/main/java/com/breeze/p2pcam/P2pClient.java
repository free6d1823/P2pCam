package com.breeze.p2pcam;

import android.util.Log;

import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.AVAPIs;
/**
 * Created by jammy.chang on 2017/4/19.
 */

public class P2pClient {
    private static String TAG = "P2pClient";
    public static int state = 0; //global state
    int client_state =0; //device state
    private final static int STATE_INIT = 0;
    private final static int STATE_READY = 1;
    private final int STATE_LOGIN= 2;
    private final int STATE_STARTED= 3;
    public static String lastError = "";
    static public boolean init(){
        int ret;
        int avapi;

        avapi = AVAPIs.avGetAVApiVer();
        if(avapi <0){
            lastError = "AVAPIs.avGetAVApiVer failed!" + avapi;
            return false;
        }
        ret = IOTCAPIs.IOTC_Initialize2(0);

        if (ret != IOTCAPIs.IOTC_ER_NoERROR) {
            lastError = "IOTC_Initialize2 error = " + ret;
            return false;
        }
        if( AVAPIs.avInitialize(3)<0){
            lastError = "AVAPIs.avInitialize(3) failed, exceed AV channels.";
            return false;
        }
        state = STATE_READY;

        return true;
    }
    static public String getVersion(){

        int avapi = AVAPIs.avGetAVApiVer();
        int[] ver = new int[1];
        IOTCAPIs.IOTC_Get_Version(ver);
        if (state == STATE_READY) {
            return
                    String.format("IOTC  ver %d.%d.%d.%d\nAVAPI ver  %d.%d.%d.%d",
                            (ver[0] >> 24) & 0xff, (ver[0] >> 16) & 0xff, (ver[0] >> 8) & 0xff, ver[0] & 0xff,
                            (avapi >> 24) & 0xff, (avapi >> 16) & 0xff, (avapi >> 8) & 0xff, avapi & 0xff);
        } else {
            return String.format("AVAPI ver  %d.%d.%d.%d\n%s", (ver[0] >> 24) & 0xff, (ver[0] >> 16) & 0xff, (ver[0] >> 8) & 0xff, ver[0] & 0xff,
                    lastError);
        }
    }
    int sid = -1;
    int avIndex;
    public boolean login(String uid, String password, String admin){
        int ret;
        lastError = "";
        Log.d(TAG, "login "+ uid + " "+password + " "+ admin);
        if(state < STATE_READY) {
            lastError = "login failed: Wrong state ";
            return false;
        }
        sid = IOTCAPIs.IOTC_Get_SessionID();
        if (sid < 0)
        {
            lastError = "IOTC_Get_SessionID error code " + sid;
            return false;
        }
        admin = "admin";
        //return IOTC session ID if return value >= 0 and equal to the input parameter SID.
        ret = IOTCAPIs.IOTC_Connect_ByUID_Parallel(uid, sid);
        if(ret <0){//-40
            switch(ret){
                case IOTCAPIs.IOTC_ER_NO_PERMISSION:
                    lastError = "IOTC_Connect_ByUID_Parallel error: The specified device does not support advance function.";break;
                default:
                    lastError = "IOTC_Connect_ByUID_Parallel error: " + ret; break;
            }
            Log.d(TAG, "IOTC_Connect_ByUID_Parallel error code " + ret);
            return false;
        }
        int[] srvType = new int[1];
        avIndex = AVAPIs.avClientStart(sid, admin, password, 20000, srvType, 0);

        if (avIndex < 0) {
            lastError = "AVAPIs.avClientStart "+ sid + "error: "+ avIndex;
            Log.d(TAG, lastError);
            return false;
        }
        state = STATE_LOGIN;
        return true;
    }
    VideoThread mVideoThread = null;
    AudioThread mAudioThread = null;
    public boolean start() {
        Log.d("P2pMain", "P2pClient start start ---");
        if(state != STATE_LOGIN)
            return false;
        if (startIpcamStream(avIndex)) {
            mVideoThread = new VideoThread(avIndex);
            Thread videoThread = new Thread(mVideoThread,
                    "Video Thread");
            mAudioThread = new AudioThread(avIndex);
            Thread audioThread = new Thread(mAudioThread,
                    "Audio Thread");
            videoThread.start();
            audioThread.start();

            Log.d("P2pMain", "startIpcamStream start TRUE");
        }
        state = STATE_STARTED;
        Log.d("P2pMain", "startIpcamStream start TRUE 2");
        return true;
    }
    public void stop(){
        if (state != STATE_STARTED)
            return;
        mVideoThread.terminate = 1;
        mAudioThread.terminate = 1;
        AVAPIs.avClientStop(avIndex);
        /* wait for thread terminated
        try {
            videoThread.getThread().join();
        }
        catch (InterruptedException e) {
            Log.d(TAG, e.getMessage());
            return false;
        }
        try {
            audioThread.join();
        }
        catch (InterruptedException e) {
            Log.d(TAG, e.getMessage());
            return false;
        }*/
        state = STATE_LOGIN;
    }
    public void free(){
        IOTCAPIs.IOTC_Session_Close(sid);
        AVAPIs.avDeInitialize();
        IOTCAPIs.IOTC_DeInitialize();
        sid = -1;
        avIndex = -1;
        state = STATE_INIT;
    }
    public boolean startIpcamStream(int avIndex) {
        AVAPIs av = new AVAPIs();
        Log.d("P2pMain", "startIpcamStream start");
        int ret = av.avSendIOCtrl(avIndex, AVAPIs.IOTYPE_INNER_SND_DATA_DELAY,
                new byte[2], 2);
        if (ret < 0) {
            Log.d(TAG, "start_ipcam_stream failed[ "+ ret + "]");
            return false;
        }

        // This IOTYPE constant and its corrsponsing data structure is defined in
        // Sample/Linux/Sample_AVAPIs/AVIOCTRLDEFs.h
        //
        int IOTYPE_USER_IPCAM_START = 0x1FF;
        ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START,
                new byte[8], 8);
        if (ret < 0) {
            Log.d(TAG, "start_ipcam_stream failed " + ret);
            return false;
        }

        int IOTYPE_USER_IPCAM_AUDIOSTART = 0x300;
        ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_AUDIOSTART,
                new byte[8], 8);
        if (ret < 0) {
            Log.d(TAG, "start_ipcam_stream failed[ " + ret + "]");
            return false;
        }

        return true;
    }

    public static class VideoThread implements Runnable {
        static final int VIDEO_BUF_SIZE = 100000;
        static final int FRAME_INFO_SIZE = 16;
        static public int terminate = 0;
        private int avIndex;
        public VideoThread(int avIndex) {
            this.avIndex = avIndex;
        }

        @Override
        public void run() {
            Log.d(TAG, Thread.currentThread().getName() + " Start...");

            AVAPIs av = new AVAPIs();
            byte[] frameInfo = new byte[FRAME_INFO_SIZE];
            byte[] videoBuffer = new byte[VIDEO_BUF_SIZE];
            while (terminate != 1) {
                int[] frameNumber = new int[1];
                //int avRecvFrameData 	( 	int  	nAVChannelID,
                //char *  	abFrameData,
                //int  	nFrameDataMaxSize,
                //char *  	abFrameInfo,
                //int  	nFrameInfoMaxSize,
                //unsigned int *  	pnFrameIdx
                //)
                //FRAMEINFO_t videoInfo;
                //videoInfo.codec_id = MEDIA_CODEC_VIDEO_H264;
                //videoInfo.flags = IPC_FRAME_FLAG_IFRAME;


                int ret = av.avRecvFrameData(avIndex, videoBuffer,
                        VIDEO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE,
                        frameNumber);
                if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
                    try {
                        Thread.sleep(30);
                        continue;
                    }
                    catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                }
                else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                    Log.d(TAG, Thread.currentThread().getName() + " Lost video frame number " + frameNumber[0]);
                    continue;
                }
                else if (ret == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                    Log.d(TAG, " Incomplete video frame number " + frameNumber[0]);
                    continue;
                }
                else if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                    Log.d(TAG, "AV_ER_SESSION_CLOSE_BY_REMOTE\n");
                    break;
                }
                else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                    Log.d(TAG, "AV_ER_REMOTE_TIMEOUT_DISCONNECT\n");
                    break;
                }
                else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                    Log.d(TAG, "Session cant be used anymore\n");
                    break;
                }
                //typedef struct _FRAMEINFO
                //{
                //    0 unsigned short codec_id;	// Media codec type defined in sys_mmdef.h,
                //    // MEDIA_CODEC_AUDIO_PCMLE16 for audio,
                //    // MEDIA_CODEC_VIDEO_H264		= 0x4E,
                //    2 unsigned char flags;		// IPC_FRAME_FLAG_IFRAME	= 0x01
                //    3 unsigned char cam_index;	// 0 - n

                //    4 unsigned char onlineNum;	// number of client connected this device
                //    5 unsigned char reserve1[3];

                //    8 unsigned int reserve2;	//
                //   12 unsigned int timestamp;	// Timestamp of the frame, in milliseconds

                //}FRAMEINFO_t;
                // Now the data is ready in videoBuffer[0 ... ret - 1]
                // Do something here
                int codec_id = frameInfo[0]+(frameInfo[1]<<8);

                int flags = frameInfo[2];
                long timestamp= ((frameInfo[12]&0x00ff)+((frameInfo[13]<<8)&0x00ff00)+((frameInfo[14]<<16)&0x00ff0000)+((frameInfo[15]<<24)&0x00ff000000));
                String text = String.format(" :codec=%x flag=%x time=%8d.%03d", codec_id, flags, (int)(timestamp/1000), (int)(timestamp%1000));
                //frameInfo[12],frameInfo[13], frameInfo[14], frameInfo[15]);
                Log.d(TAG, "videoData " + frameNumber[0] + text);
            }

            Log.d(TAG, Thread.currentThread().getName() + " Exit\n");
        }
    }

    public static class AudioThread implements Runnable {
        static final int AUDIO_BUF_SIZE = 1024;
        static final int FRAME_INFO_SIZE = 16;
        static public int terminate = 0;
        private int avIndex;

        public AudioThread(int avIndex) {
            this.avIndex = avIndex;
        }

        @Override
        public void run() {
            Log.d(TAG, Thread.currentThread().getName() + " Start\n");

            AVAPIs av = new AVAPIs();
            byte[] frameInfo = new byte[FRAME_INFO_SIZE];
            byte[] audioBuffer = new byte[AUDIO_BUF_SIZE];
            while (terminate != 1) {
                int ret = av.avCheckAudioBuf(avIndex);

                if (ret < 0) {
                    // Same error codes as below
                    Log.d(TAG, "avCheckAudioBuf() failed: " + ret);
                    break;
                }
                else if (ret < 3) {
                    try {
                        Thread.sleep(120);
                        continue;
                    }
                    catch (InterruptedException e) {
                        Log.d(TAG, e.getMessage());
                        break;
                    }
                }

                int[] frameNumber = new int[1];
                ret = av.avRecvAudioData(avIndex, audioBuffer,
                        AUDIO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE,
                        frameNumber);

                if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                    Log.d(TAG, "AudioThread: AV_ER_SESSION_CLOSE_BY_REMOTE\n");
                    break;
                }
                else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                    Log.d(TAG, "AudioThread: AV_ER_REMOTE_TIMEOUT_DISCONNECT\n");
                    break;
                }
                else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                    Log.d(TAG, "AudioThread: Session cant be used anymore\n");
                    break;
                }
                else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                    //System.out.printf("[%s] Audio frame losed\n",
                    //        Thread.currentThread().getName());
                    continue;
                }

                // Now the data is ready in audioBuffer[0 ... ret - 1]
                // Do something here
                Log.d(TAG, "audioData fn=" + frameNumber[0] + "frame info =" + frameInfo[0] + (frameInfo[1] << 8));
            }

            Log.d(TAG, "AudioThread:  Exit\n");
        }
    }
}
