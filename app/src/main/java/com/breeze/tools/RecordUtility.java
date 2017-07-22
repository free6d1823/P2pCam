package com.breeze.tools;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.breeze.youcam.EventItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jammy.chang on 2017/6/26.
 */

public class RecordUtility {
    public static String TAG = "P2pMain";
    public static int mKeepDataDays = 30;
    public static int mFreeSpaceMB = 1000;
    public static int mMaxFileDurationMs = 300000; //5 minutes a file
    public static File mWorkingFolder = null;
    public static File getWorkingFolder() {
        if(mWorkingFolder == null) {
            mWorkingFolder = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), TAG);

            if (null == mWorkingFolder)
                return null;
            if (!mWorkingFolder.exists())
                if (!mWorkingFolder.mkdirs())
                    Log.d(TAG, "mkdirs failed!! " + mWorkingFolder.toString());
        }
        return mWorkingFolder;
    }
    static public String getFolderName(Calendar start){
        String szFile = String.format("%04d%02d%02d", start.get(Calendar.YEAR), start.get(Calendar.MONTH)+1,
                start.get(Calendar.DAY_OF_MONTH));
        return szFile;
    }
    //return time
    //type = event type
    //data = data type
    static public long parserFilename(String name, int type[], int data[]) {
        long time = 0;
        int dataType = EventItem.CONTENT_UNKNOWN;
        char a = name.charAt(0);

        if(a < 'A' || a > EventItem.TYPE_MAX + 'A')
            return time;
        type[0] = a - 'A';

        //name format: H20170626_205857.h264
        String dateRoot = name.substring(1,16);

        if(dateRoot == null )
            return time;
        SimpleDateFormat sdfmt = new SimpleDateFormat("yyyyMMdd_hhmmss");

        try {
            Date tm = sdfmt.parse(dateRoot);
            time = tm.getTime();
        } catch (ParseException e) {
            e.printStackTrace();

        }
        dateRoot = name.substring(17,name.length()).toLowerCase();

        if(dateRoot == null )
            return 0;

        if(dateRoot.equals("h264")) {
            dataType = EventItem.CONTENT_VIDEO;
        }
        else if(dateRoot.equals("pcm"))
            dataType = EventItem.CONTENT_AUDIO;
        else if(dateRoot.equals("txt"))
            dataType = EventItem.CONTENT_TEXT;

        data[0] = dataType;
        return time;
    }
    public static FileData createFile(Calendar time, int type, boolean bIsVideo){
        FileData fd = null;
        try {
            fd = new FileData(time, type, bIsVideo);
            byte header[] = new byte[4];
            if(bIsVideo) {
                header[0]='h';header[1]='2';header[2]='6';header[3]='4';
                fd.write(header, 4);
                //fps
                ByteBuffer ba;
                ba = ByteBuffer.allocate(4);
                ba.putInt(30); //30 fps assumed
                fd.write(ba.array(),4);

            } else {
                header[0]='g';header[1]='7';header[2]='1';header[3]='1';
                fd.write(header, 4);
                //sample rate
                ByteBuffer ba;
                ba = ByteBuffer.allocate(4);
                ba.putInt(8000); //8000 fps assumed
                fd.write(ba.array(), 4);
            }

            ByteBuffer bb;
            bb = ByteBuffer.allocate(8);
            long data = time.getTimeInMillis();
            Log.e(TAG, "putLong = "+ bb.array().length);

            bb.putLong(data);
            Log.e(TAG, "data = "+ data);

            fd.write(bb.array(), 8);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return fd;

    }

    public static FileData checkFileClose(FileData fd) {
        //check file duration
        Calendar tmNow = Calendar.getInstance();
        if (tmNow.getTimeInMillis() - fd.tmStart > mMaxFileDurationMs) {
            fd.close();
        }
        else if (getFreeSapceMb() < mFreeSpaceMB) {
            fd.close();
            checkStorageSpace();
        } else {
                return fd; //keep recording
         }
         return null;
    }
    public static int getFreeSapceMb(){
        File dir = getWorkingFolder();
        long fs = dir.getUsableSpace();
        return (int) (fs/(1024*1024));
    }
    public static int getTotalSapceMb(){
        File dir = getWorkingFolder();
        long fs = dir.getTotalSpace();
        return (int) (fs/(1024*1024));
    }
    //return true if files removed
    public static boolean checkStorageSpace(){
        File dir = getWorkingFolder();
        long fs = dir.getUsableSpace();

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, - mKeepDataDays);
        removeRecordBefore(c);

        //check free space
        long us = dir.getUsableSpace();
        while(us < mFreeSpaceMB*1024){
            c.add(Calendar.DAY_OF_YEAR, -7);
            removeRecordBefore(c);
        }
        //dumpStorage();

        fs = dir.getUsableSpace() -fs;
        return (fs > 0);
    }
    //remove all file before start (included)
    public static int removeRecordBefore(Calendar start){
        int nFiles = 0;
        long target = start.getTime().getTime();
        File dir = getWorkingFolder();

        File file[] = dir.listFiles();
        //	Log.d("Files", "Size: "+ file.length + " to delete = "+ target);

        for (int i=0; i < file.length; i++)
        {
            long tm = file[i].lastModified();

            if (tm <= target ){
                if(file[i].exists()){
                    Log.d(TAG, "removing " + file[i].getName());
                    if(file[i].delete())
                        nFiles ++;
                }
            }
        }
        if (nFiles > 0 )
            Log.d(TAG, nFiles + " files are removed.");

        return nFiles;

    }
    public interface OnDataFound {
        boolean onDataFound(EventItem item);
    }
    static OnDataFound m_searchResultListener = null;

    final static int MESSAGE_FOUND_ADDITEM = 100;
    final static int MESSAGE_FOUND_DONE = 101;
    static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch(msg.what){
                case MESSAGE_FOUND_ADDITEM:
                    m_searchResultListener.onDataFound((EventItem) msg.obj);
                    break;
                case MESSAGE_FOUND_DONE:
                    m_searchResultListener.onDataFound(null);

                    break;
            }
        }

    };
    static Calendar mTmDate = Calendar.getInstance();
    static long mTmEnd;

    public static void startReflesh(Date tmStart, long tmDuration, OnDataFound listener){
        m_searchResultListener = listener;
        mTmDate.setTime(tmStart);
        mTmDate.set(Calendar.HOUR_OF_DAY, 0);
        mTmDate.set(Calendar.MINUTE, 0);
        mTmDate.set(Calendar.SECOND, 0);
        mTmEnd = tmStart.getTime() + tmDuration;

        Thread reflesh = new Thread( new Runnable() {

            @Override
            public void run() {
                while (mTmDate.getTime().getTime() <= mTmEnd) {
                    File folder = new File(RecordUtility.getWorkingFolder(), RecordUtility.getFolderName(mTmDate));
                    Log.d(TAG, "--- search folder = "+ folder.toString());
                    if(folder.exists()){
                        //search file in the folder
                        File file[] = folder.listFiles();
                        for (File f : file){
                            String name = f.getName();
                            int type[] = {0};
                            int kind[] = {0};
                            long date = parserFilename(name, type, kind);
                            if (date < mTmEnd) {

                                EventItem item = new EventItem( type[0], name, date, f.getAbsolutePath(), kind[0]);
                                Message msg2 = new Message();
                                msg2.what = MESSAGE_FOUND_ADDITEM;
                                msg2.obj = item;
                                mHandler.sendMessage(msg2);
                            }
                        }
                    }
                    //
                    mTmDate.set(Calendar.DATE, mTmDate.get(Calendar.DATE)+1);
                }
                Message msg = new Message();
                msg.what = MESSAGE_FOUND_DONE;
                mHandler.sendMessage(msg);

            }
        }, "reflesh Thread");
        reflesh.start();
    }


}
