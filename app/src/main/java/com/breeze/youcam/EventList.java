package com.breeze.youcam;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jammy.chang on 2017/4/25.
 */

public class EventList extends BaseAdapter {
    private LayoutInflater mLi;
    private ArrayList<EventItem> mEventArray = new ArrayList<EventItem>();
    Lock mLock;
    public EventList(Activity context) {
        mLi = (LayoutInflater)
                context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        mLock = new ReentrantLock();

    }
    @Override
    public int getCount() {
        return mEventArray.size();
    }

    @Override
    public Object getItem(int position) {

        if(position >= mEventArray.size())
            return null;
        else return  mEventArray.get(position);
    }

    @Override
    public long getItemId(int position) {
        if(position >= mEventArray.size())
            return -1;
        else return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        EventItem di;
        LinearLayout itemView = null; //xml is LinearLayout not relativeLayout
        lock();
        try {
            if (convertView == null) {
                itemView = (LinearLayout) mLi.inflate(R.layout.list_event_view, parent, false);
            } else {
                itemView = (LinearLayout) convertView;
            }
            if (position >= mEventArray.size())
                return null;

            di = mEventArray.get(position);
            ImageView imageView = (ImageView) itemView.findViewById(R.id.ivType);
            TextView nameView = (TextView) itemView.findViewById(R.id.tvName);

            TextView timeView = (TextView) itemView.findViewById(R.id.tvTime);
            TextView noteView = (TextView) itemView.findViewById(R.id.tvNote);
            imageView.setImageResource(di.getTypeResourceId());
            nameView.setText(di.name);

            timeView.setText(di.timeString());

            noteView.setText(di.note);
        } finally {
            unlock();
        }

        return itemView;
    }
    public void addItem(int pos, EventItem dev)
    {
        lock();
        mEventArray.add(dev);
        unlock();
    }
    public void removeItem(int pos)
    {
        mEventArray.remove(pos);
    }
    public void resetItem()
    {

        lock();
        mEventArray.clear();
        unlock();

    }
    public void notifyDataSetChanged() {
        lock();
        super.notifyDataSetChanged();
        unlock();
    }
    public void lock() {
        mLock.lock();
    }

    public void unlock() {
        mLock.unlock();
    }

}
