package com.breeze.youcam;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.st_SearchDeviceInfo;

import java.io.UnsupportedEncodingException;

public class EventListFragment extends BaseFragment {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetupFragment.
     */
    // TODO: Rename and change types and number of parameters
    EventList mEventList;
    public static EventListFragment newInstance(String param1, String param2) {
        EventListFragment fragment = new EventListFragment();
        //Bundle args = new Bundle();
        return fragment;
    }
    public EventListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_event_list, container, false);
        initUi(view);
        startReflesh();

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();

    }
    FrameLayout mProgressBar;
    void initUi(View view) {
        mProgressBar = (FrameLayout) view.findViewById(R.id.progressBarHolder);
        mEventList = new EventList(MainActivity.gApp);
        ListView lv = (ListView) view.findViewById(R.id.listEvent);
        lv.setAdapter(mEventList);
        lv.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                return false;
            }

        });
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EventItem di = (EventItem)mEventList.getItem(position);
                //if(di.status >= di.STATUS_CONNECTED) {
                //Intent intent = new Intent(MainActivity.gApp, LiveActivity.class);
               // intent.putExtra(MainActivity.EXTRA_DEVICE_DETAIL, di.uuid);
                //startActivity(intent);
                //}//
            }
        });
    }
    public final int MESSAGE_UPDATE_ADDITEM = 100;
    public final int MESSAGE_UPDATE_REFLESH_DONE = 101;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch(msg.what){
                case MESSAGE_UPDATE_ADDITEM:
                    EventItem di = (EventItem) msg.obj;
                    mEventList.addItem(0, di);
                    mEventList.notifyDataSetChanged();
                    break;
                case MESSAGE_UPDATE_REFLESH_DONE:
                    mProgressBar.setVisibility(View.GONE);
                    break;
            }
        }

    };
    void startReflesh(){
        mProgressBar.setVisibility(View.VISIBLE);
        mEventList.resetItem();
        mEventList.notifyDataSetChanged();
        Thread reflesh = new Thread( new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {

                    Message msg = new Message();
                    msg.what = MESSAGE_UPDATE_ADDITEM;
                    msg.obj = new EventItem(i % 8, "name" + i, "AAAA" + i);
                    mHandler.sendMessage(msg);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Message msg = new Message();
                msg.what = MESSAGE_UPDATE_REFLESH_DONE;
                mHandler.sendMessage(msg);
            }
        }, "reflesh Thread");
        reflesh.start();
    }
}
