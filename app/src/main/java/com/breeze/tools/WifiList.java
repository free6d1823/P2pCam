package com.breeze.tools;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breeze.youcam.DeviceInfo;
import com.breeze.youcam.R;

import java.util.ArrayList;

/**
 * Created by Benjamin Wang on 2017/7/15.
 */

public class WifiList extends BaseAdapter {
    public static class WifiItem {
        String ssid;
        String security;
        boolean wps;
        boolean open;
        float level;
        public WifiItem(String ss, String cap, float signal){
            ssid = ss;
            security = "";
            if(cap.contains("WPA-"))security += "WPA";
            if(cap.contains("WPA2-")){
                if (!security.isEmpty()) security += "/";
                security += "WPA2";
            }
            open = security.isEmpty();
            wps = cap.contains("WPS");
            level = signal;
        }
    }
    private final LayoutInflater mInflater;
    int mResource;
    int[] mLevelIds;
    public WifiList (Context context, @LayoutRes int resource, @IdRes int[] levels) {
        mResource = resource;
        mLevelIds = levels;//icon id must be size 10

        mInflater =  (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }
    private ArrayList<WifiItem> mDeviceArray = new ArrayList<WifiItem>();
    @Override
    public int getCount() {
        return mDeviceArray.size();
    }
    public void addItem(WifiItem dev)
    {
        mDeviceArray.add(dev);

    }
    public void removeItem(int pos)
    {
        mDeviceArray.remove(pos);
    }
    //level bigger first
    public void sortByLevel() {
        ArrayList<WifiItem> newArray = new ArrayList<WifiItem>();
        float maxLevel;
        int maxId;
        int size = mDeviceArray.size();
        for(int i=0;i< size; i++){
            maxLevel = mDeviceArray.get(0).level;
            maxId = 0;
            for (int j=1; j<mDeviceArray.size(); j++) {
                if(mDeviceArray.get(j).level > maxLevel ) {
                    maxId = j;
                    maxLevel = mDeviceArray.get(j).level;
                }
            }
            WifiItem max = mDeviceArray.get(maxId);
            newArray.add(i, max);
            mDeviceArray.remove(maxId);
        }
        mDeviceArray = null;
        mDeviceArray = newArray;
    }
    public void resetItem()
    {
        mDeviceArray.clear();
    }
    @Override
    public Object getItem(int position) {
        if(position >= mDeviceArray.size())
            return null;
        return mDeviceArray.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView; //xml is LinearLayout not relativeLayout

        if (convertView == null) {
            itemView = mInflater.inflate(mResource, parent, false);
        } else {
            itemView = convertView;
        }
        if (position >= mDeviceArray.size())
            return null;
        WifiItem wi = (WifiItem) mDeviceArray.get(position);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.ivLevel);
        TextView nameView = (TextView) itemView.findViewById(R.id.tvName);
        TextView statusView = (TextView) itemView.findViewById(R.id.tvStatus);
        nameView.setText(wi.ssid);
        String description = "";
        if (!wi.open)
            description = "Secured with " + wi.security;
        if(wi.wps)
            description += " (WPS available)";
        statusView.setText(description);
        int id;
        if(wi.level < -90) id = 0;
        else if(wi.level < -80) id = 1;
        else if(wi.level < -70) id = 2;
        else if(wi.level < -60) id = 3;
        else id = 4;
        if (wi.open == false)
            id +=5;

        imageView.setImageResource(mLevelIds[id]);

        return itemView;
    }


}
