package com.breeze.youcam;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
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


import com.breeze.qrcode.android.CaptureActivity;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.st_LanSearchInfo;
import com.tutk.IOTC.st_LanSearchInfo2;
import com.tutk.IOTC.st_SearchDeviceInfo;

import java.io.UnsupportedEncodingException;

import static com.tutk.IOTC.IOTCAPIs.IOTC_Lan_Search;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Lan_Search2;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Search_Device_Result;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Search_Device_Start;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends BaseFragment{
    final String TAG = "P2pMain";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    FrameLayout mProgressBar;
    // TODO: Rename and change types of parameters
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
    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public SearchFragment() {
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
        View view = inflater.inflate(R.layout.fragment_search, container, false);

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
    DeviceList mDeviceList;
    private static final int MENU_ADD = Menu.FIRST + 4;

    boolean createContextMenu(int pos, View v) {
        DeviceInfo di = (DeviceInfo) mDeviceList.getItem(pos);
        if (di == null)
            return false;
        if (di.status >= di.STATUS_PAIRED)
            return false;
        PopupMenu menu = new PopupMenu(MainActivity.gApp, v);
        menu.getMenu().add(pos, MENU_ADD,  Menu.NONE, R.string.action_add);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int k = item.getGroupId();
                DeviceInfo sel = (DeviceInfo) mDeviceList.getItem(k);
                if (sel == null)
                    return false;
                int act = item.getItemId();
                if (act == MENU_ADD) {

                    sel.status = DeviceInfo.STATUS_PAIRED;
                    mDeviceList.notifyDataSetChanged();
                    MainActivity.mPairedList.addItem(0, sel);
                }
                return true;
            }
        });
        menu.show();
        return true; //handled
    }

    void initUi(View view) {
        mProgressBar = (FrameLayout) view.findViewById(R.id.progressBarHolder);
        mDeviceList = new DeviceList(getActivity());
        ListView lv = (ListView) view.findViewById(R.id.listDevice);
        lv.setAdapter(mDeviceList);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return createContextMenu(position, view);

            }

        });


        Button btnScan = (Button) view.findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
            }
        });
        Button btnAdd = (Button) view.findViewById(R.id.btnManual);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddDevice(null);
            }
        });
        Button btnAuto = (Button) view.findViewById(R.id.btnAuto);
        btnAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.gApp, CaptureActivity.class);

                startActivity(intent);
            }
        });

    }
    //the handler for message from other thread
    public final int MESSAGE_UPDATE_LISTVIEW = 1;
    public final int MESSAGE_UPDATE_ADDDEVICE = 2;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
             mProgressBar.setVisibility(View.GONE);
            switch(msg.what){
                case MESSAGE_UPDATE_LISTVIEW:
                    mDeviceList.notifyDataSetChanged();
                    break;
                case MESSAGE_UPDATE_ADDDEVICE:
                    DeviceInfo di = (DeviceInfo)msg.obj;
                    mDeviceList.addItem(0, di);
                    mDeviceList.notifyDataSetChanged();
                    break;
            }
        }

    };
    void startScanning() {
        mProgressBar.setVisibility(View.VISIBLE);

        Thread scanning = new Thread( new Runnable() {

            @Override
            public void run() {
                int[] nArray = new int[1];

                int ret = IOTCAPIs.IOTC_Search_Device_Start(3000,1000);
                Log.d(TAG, "IOTC_Search_Device_Start = "+ ret);
                if (ret == IOTCAPIs.IOTC_ER_FAIL_CREATE_SOCKET) {
                    //please enable Wifi
                }

                while(true) {

                    st_SearchDeviceInfo[] ab_LanSearchInfo =
                            IOTCAPIs.IOTC_Search_Device_Result(nArray, 0);
                    if (nArray[0] < 0) {
                        break;
                    }

                    for (int i = 0; i < nArray[0]; i++) {
                        //if already in the list, skip it
                        try {
                            String uuid = new String(ab_LanSearchInfo[i].UID, 0, ab_LanSearchInfo[i].UID.length, "utf-8");
                            if(null != mDeviceList.findByUuid(uuid))
                                continue;

                            DeviceInfo di = new DeviceInfo(new String(ab_LanSearchInfo[i].DeviceName));
                            di.uuid = uuid;
                            di.user = "admin";
                            di.password = "888888";
                            di.ip = new String(ab_LanSearchInfo[i].IP, 0, ab_LanSearchInfo[i].IP.length, "utf-8");
                            di.port = ab_LanSearchInfo[i].port;
                            di.status = di.STATUS_AVAILABLE;
                            if (MainActivity.mPairedList.findByUuid(di.uuid) != null) {
                                di.status = di.STATUS_PAIRED;
                            }
                            Message msg = new Message();
                            msg.what = MESSAGE_UPDATE_ADDDEVICE;
                            msg.obj = di;
                            mHandler.sendMessage(msg);




                        } catch (UnsupportedEncodingException e) {

                            e.printStackTrace();
                        }

                    }
                } //end while
                Message msg = new Message();
                msg.what = MESSAGE_UPDATE_LISTVIEW;
                mHandler.sendMessage(msg);
            }
        }, "Scanning Thread");
        scanning.start();
    }

    void AddDevice(DeviceInfo di) {
        AddDeviceDialog dlg = new AddDeviceDialog(MainActivity.gApp,
                new AddDeviceDialog.OnDialogEventListener() {
                    @Override
                    public void onOk(DeviceInfo dif) {
                        mDeviceList.addItem(0, dif);
                    }
                });
        if (di == null) {
          di = new DeviceInfo("name");

        }
        dlg.mDi = di;
        dlg.show();


    }
}
