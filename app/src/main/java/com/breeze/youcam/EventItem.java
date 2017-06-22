package com.breeze.youcam;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by jammy.chang on 2017/4/25.
 */

public class EventItem {
    public String name;
    public String uuid;
    public String note;
    public long time;
    public int type;
    final static int TYPE_ERROR = 0;
    final static int TYPE_WARNING = 1;
    final static int TYPE_INFO = 2;
    final static int TYPE_MOTION = 3;
    final static int TYPE_PHOTO = 4; //remote photo
    final static int TYPE_VIDEO = 5;
    final static int TYPE_LOCAL_PHOTO = 6;
    final static int TYPE_LOCAL_VIDEO = 7;
    final static int TYPE_LAST = 7;
    static int[] typeResourceId = {
            R.drawable.ev_error, R.drawable.ev_warning, R.drawable.ev_inform,
            R.drawable.ev_motion, R.drawable.ev_photo, R.drawable.ev_video,
            R.drawable.ev_local_photo, R.drawable.ev_local_video
    };
    public EventItem(int ty, String device, String id){
        if (ty > TYPE_LAST)
            type =  TYPE_ERROR;
        else
            type = ty;
            name = device;
            uuid = id;
            note = "no comments";
            time = Calendar.getInstance().getTime().getTime();
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
