package com.breeze.p2pcam;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by jammy.chang on 2017/4/19.
 */

public class DeviceList extends BaseAdapter {
    final String TAG = "P2pMain";
    private LayoutInflater mLi;
    private ArrayList<DeviceInfo> mDeviceArray = new ArrayList<DeviceInfo>();
    public DeviceList(Activity context) {
        mLi = (LayoutInflater)
                context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }
    public DeviceInfo findByUuid(String uuid) {
        Log.d(TAG, "searching "+ uuid);
        for(int i=0; i< getCount(); i++){
            DeviceInfo di = (DeviceInfo) getItem(i);

            if(di.uuid.equals(uuid)) {

                return di;
            }
        }
        return null;

    }
    public void addItem(int pos, DeviceInfo dev)
    {
        mDeviceArray.add(dev);

    }
    public void removeItem(int pos)
    {
        mDeviceArray.remove(pos);

    }
    public void resetItem()
    {
        mDeviceArray.clear();
    }
    @Override
    public int getCount() {
        return  mDeviceArray.size();
    }

    @Override
    public Object getItem(int position) {
        if(position >= mDeviceArray.size())
            return null;
        else return  mDeviceArray.get(position);
    }

    @Override
    public long getItemId(int position) {
        if(position >= mDeviceArray.size())
            return -1;
        else return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DeviceInfo di;
        LinearLayout itemView; //xml is LinearLayout not relativeLayout

        if (convertView == null) {
            itemView = (LinearLayout) mLi.inflate(R.layout.list_device_view, parent, false);
        } else {
            itemView = (LinearLayout) convertView;
        }
        if (position >= mDeviceArray.size())
            return null;

        di = mDeviceArray.get(position);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.ivDevice);
        TextView nameView = (TextView) itemView.findViewById(R.id.tvDeviceName);
        TextView uuidView = (TextView) itemView.findViewById(R.id.tvUuid);
        TextView ipView = (TextView) itemView.findViewById(R.id.tvIp);
        TextView statusView = (TextView) itemView.findViewById(R.id.tvStatus);
        if(di.status == di.STATUS_PAIRED)
            imageView.setImageResource(R.drawable.status_paired);
        else
            imageView.setImageResource(R.drawable.status_available);
        nameView.setText(di.name);
        uuidView.setText(di.uuid);
        ipView.setText(di.ip + ":" + di.port);

        statusView.setText(di.statusString());

        return itemView;

    }


}
