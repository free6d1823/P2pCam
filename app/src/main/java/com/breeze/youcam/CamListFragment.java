package com.breeze.youcam;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;


/**
 * A simple {@link Fragment} subclass.
 */
public class CamListFragment extends BaseFragment {
    final String TAG = "P2pMain";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetupFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CamListFragment newInstance(String param1, String param2) {
        CamListFragment fragment = new CamListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public CamListFragment() {
        // Required empty public constructor
    }

    private OnFragmentInteractionListener mListener;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cam_list, container, false);
        initUi(view);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    FrameLayout mProgressBar;
    void initUi(View view) {
        mProgressBar = (FrameLayout) view.findViewById(R.id.progressBarHolder);
        ListView lv = (ListView) view.findViewById(R.id.listDevice);
        lv.setAdapter(MainActivity.mPairedList);
        lv.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                createContextMenu(position, view);
                return false;
            }

        });
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DeviceInfo di = (DeviceInfo)MainActivity.mPairedList.getItem(position);
                if(di.status == di.STATUS_NONE) {
                    Intent intent = new Intent(MainActivity.gApp, PairingActivity.class);
                    intent.putExtra(PairingActivity.ARG_DI_SLOT, position);
                    startActivity(intent);
                }
                else if(di.status >= di.STATUS_CONNECTED) {
                    Intent intent = new Intent(MainActivity.gApp, LiveActivity.class);
                    intent.putExtra(MainActivity.EXTRA_DEVICE_DETAIL, di.uuid);
                    startActivity(intent);
                }//
            }
        });

    }
    private static final int MENU_REMOVE = Menu.FIRST;
    private static final int MENU_DETAIL = Menu.FIRST + 1;
    private static final int MENU_CONNECT = Menu.FIRST + 2;
    private static final int MENU_DISCONNECT = Menu.FIRST + 3;
    private static final int MENU_START = Menu.FIRST + 4;
    private static final int MENU_STOP = Menu.FIRST + 5;

    void createContextMenu(int pos, View v){
        DeviceInfo di = (DeviceInfo)MainActivity.mPairedList.getItem(pos);
        if (di == null)
            return;
        PopupMenu menu = new PopupMenu(MainActivity.gApp, v);

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int act = item.getItemId();
                int k = item.getGroupId();

                DeviceInfo hdi = (DeviceInfo)MainActivity.mPairedList.getItem(k);
                Message msg;
                if(hdi == null)
                    return false;
                switch(act){
                    case MENU_STOP:
                        hdi.stop();
                        msg = new Message();
                        msg.what = MESSAGE_UPDATE_LISTVIEW;
                        mHandler.sendMessage(msg);
                        break;
                    case MENU_REMOVE:
                        hdi.remove(); //set as STATUS_NONE
                        MainActivity.mPairedList.notifyDataSetChanged();
                        msg = new Message();
                        msg.what = MESSAGE_UPDATE_LISTVIEW;
                        mHandler.sendMessage(msg);
                        break;
                    case MENU_CONNECT:
                        connectDevice(hdi);

                        break;
                    case MENU_DISCONNECT:
                        hdi.disconnect();
                        msg = new Message();
                        msg.what = MESSAGE_UPDATE_LISTVIEW;
                        mHandler.sendMessage(msg);
                        break;
                    case MENU_DETAIL:
                        showDeviceDetail(hdi);
                        break;
                }
                return false;
            }
        });
        menu.getMenu().add(pos, MENU_DETAIL, Menu.NONE, R.string.action_detail);
        if (di.status == di.STATUS_PAIRED)
            menu.getMenu().add(pos, MENU_CONNECT, Menu.NONE, R.string.action_connect);
        else {
            menu.getMenu().add(pos, MENU_DISCONNECT, Menu.NONE, R.string.action_disconnect);
            if (di.status == di.STATUS_STARTED)
                menu.getMenu().add(pos, MENU_STOP, Menu.NONE, R.string.action_stop);
            else
                menu.getMenu().add(pos, MENU_START, Menu.NONE, R.string.action_start);
        }
        menu.getMenu().add(pos, MENU_REMOVE, Menu.NONE, R.string.action_remove);

        menu.show();;

    }
    public void showDeviceDetail(DeviceInfo di) {
        Intent intent = new Intent(MainActivity.gApp, DetailActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEVICE_DETAIL, di.uuid);
        startActivity(intent);
    }
    void connectDevice(DeviceInfo di){
        startLogin(di);
        MainActivity.mPairedList.notifyDataSetChanged();

    }
    //the handler for message from other thread
    public final int MESSAGE_UPDATE_LISTVIEW = 1;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage ESSAGE_UPDATE_LISTVIEW - msg = " + msg.what);
            mProgressBar.setVisibility(View.GONE);
            switch(msg.what){
                case MESSAGE_UPDATE_LISTVIEW:
                    MainActivity.mPairedList.notifyDataSetChanged();
                    break;
            }
        }

    };
    public class MyRunnable implements Runnable {
        DeviceInfo di;
        public MyRunnable(DeviceInfo d) {
            di = d;
        }
        @Override
        public void run() {
            di.login();
            Message msg = new Message();
            msg.what = MESSAGE_UPDATE_LISTVIEW;
            mHandler.sendMessage(msg);
        }
    }
    void startLogin(DeviceInfo di) {
        mProgressBar.setVisibility(View.VISIBLE);

        final Thread thread = new Thread( new MyRunnable(di), "logining");
        thread.start();
    }
}
