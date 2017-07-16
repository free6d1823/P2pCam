package com.breeze.youcam;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jammy.chang on 2017/4/25.
 */

public class EventItem implements Serializable{
    public String name;
    public String note;
    public long time;
    public int type; //event type
    final static int TYPE_ERROR = 0;
    final static int TYPE_WARNING = 1;
    final static int TYPE_INFO = 2;
    final static int TYPE_LOCAL_PHOTO = 3;
    final static int TYPE_LOCAL_VIDEO = 4;
    final static int TYPE_MOTION = 5;
    final static int TYPE_PHOTO = 6; //remote photo
    final static int TYPE_VIDEO = 7;
    public final static int TYPE_MAX = 7;
    public int ctype; //content type
    public final static int CONTENT_UNKNOWN = 0;
    public final static int CONTENT_VIDEO = 1;
    public final static int CONTENT_AUDIO = 2;
    public final static int CONTENT_TEXT = 3;

    final static int TYPE_LAST = 7;
    static int[] typeResourceId = {
            R.drawable.ev_error, R.drawable.ev_warning, R.drawable.ev_inform,
            R.drawable.ev_motion, R.drawable.ev_photo, R.drawable.ev_video,
            R.drawable.ev_local_photo, R.drawable.ev_local_video
    };
    public EventItem(int eType, String device, long tmData, String content, int cType){
        if (eType > TYPE_LAST)
            type =  TYPE_ERROR;
        else
            type = eType;
            name = device; //display name
            note = content; //link, full file path
            time = tmData;
            ctype = cType;
    }
    public int getTypeResourceId() {
        return typeResourceId[type];
    }
    public String timeString() {

        Date tm = new Date();
        tm.setTime(time);
        String text = String.format("%2d:%02d:%02d", tm.getHours(), tm.getMinutes(), tm.getSeconds());
        return text;
    }

}
