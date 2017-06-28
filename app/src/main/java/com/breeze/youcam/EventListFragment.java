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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.breeze.tools.RecordUtility;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.st_SearchDeviceInfo;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class EventListFragment extends BaseFragment implements RecordUtility.OnDataFound{

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
    Button mBtnA;
    Button mBtnB;
    TextView mTvDate;
    Date mTmStart;
    long mTmDuration; //in ms
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
        //
        mBtnA = (Button) view.findViewById(R.id.btnPrev);
        mBtnB = (Button) view.findViewById(R.id.btnNext);
        mTvDate = (TextView) view.findViewById(R.id.tvDate);
        mBtnA.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                mTmStart = new Date(mTmStart.getTime() - mTmDuration);

                startReflesh();
            }
        });
        mBtnB.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                mTmStart = new Date(mTmStart.getTime() + mTmDuration);

                startReflesh();
            }
        });
        Calendar now = Calendar.getInstance();

        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        mTmStart = now.getTime();
        mTmDuration = 60*60*24*1000;
        startReflesh();



    }

    void startReflesh(){
        SimpleDateFormat formatter
                = new SimpleDateFormat("yyyy MMMM dd  EEEE");
        String dateString = formatter.format(mTmStart);
        mTvDate.setText(dateString);

        mProgressBar.setVisibility(View.VISIBLE);
        mEventList.resetItem();
        mEventList.notifyDataSetChanged();
        RecordUtility.startReflesh(mTmStart, mTmDuration, this);

    }

    @Override
    public boolean onDataFound(EventItem item) {
        if (item == null)
            mProgressBar.setVisibility(View.GONE);
        else {
            mEventList.addItem(0, item);
            mEventList.notifyDataSetChanged();
        }
        return false;
    }
}
