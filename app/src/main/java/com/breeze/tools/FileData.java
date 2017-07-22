package com.breeze.tools;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by jammy.chang on 2017/6/26.
 */

public class FileData {
    String TAG = "P2pMain";
    protected File file;
    public FileOutputStream fos;
    public long tmStart;
    public String getFileName() { return file.getPath();}

    public FileData(Calendar tmNow, int type, boolean bIsVideo) throws  IOException{

        String date = RecordUtility.getFolderName(tmNow);

        File folder = new File(RecordUtility.getWorkingFolder(), date);
        if(!folder.exists()) {
            if (!folder.mkdirs())
                Log.e(TAG, "folder is exist=" + folder.exists());
        }
        String ext = (bIsVideo)?"h264":"pcm";
        String fileName = String.format("%c%s_%02d%02d%02d.%s",'A'+type, date,
                tmNow.get(Calendar.HOUR_OF_DAY), tmNow.get(Calendar.MINUTE), tmNow.get(Calendar.SECOND),
                ext);

        file = new File(folder, fileName);

        fos = new FileOutputStream(file);
        tmStart = tmNow.getTimeInMillis();
Log.e(TAG, "... Create file "+file.toString());
    }
    public void close() {
        if(fos!= null) try {
            fos.close();
            fos = null;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
    public void write(byte[] b, int length) {
        if( fos != null)
            try {
                fos.write(b, 0, length);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
    }
}
