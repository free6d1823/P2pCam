package com.breeze.youcam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupMenu;

import com.breeze.qrcode.android.CaptureActivity;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.st_SearchDeviceInfo;

import java.io.UnsupportedEncodingException;

public class PairingActivity extends Activity {
    final String TAG = "P2pMain";
    public static final String ARG_DI_SLOT = "DiSlotNum"; //index of desired paired slot id
    public int mDiSlotNum = 0;
    FrameLayout mProgressBar;
    DeviceList mDeviceList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        Intent intent = getIntent();
        mDiSlotNum = intent.getIntExtra(ARG_DI_SLOT, -1);
        Log.d(TAG, "PairingActivity Di Slot is "+ mDiSlotNum);

        mProgressBar = (FrameLayout) findViewById(R.id.progressBarHolder);
        mDeviceList = new DeviceList(this);
        ListView lv = (ListView) findViewById(R.id.listDevice);
        lv.setAdapter(mDeviceList);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return createContextMenu(position, view);

            }

        });


        Button btnScan = (Button) findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
            }
        });
        Button btnAdd = (Button) findViewById(R.id.btnManual);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddDevice(null);
            }
        });
        Button btnAuto = (Button) findViewById(R.id.btnAuto);
        btnAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQrCodeScanning();

            }
        });
    }
    void startQrCodeScanning() {
        Intent intent = new Intent(PairingActivity.this, CaptureActivity.class);
        startActivity(intent);
    }
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
                    DeviceInfo di = (DeviceInfo) MainActivity.mPairedList.getItem(mDiSlotNum);
                    if(di != null) {
                        sel.status = DeviceInfo.STATUS_PAIRED;
                        di.copy(sel);
                        mDeviceList.notifyDataSetChanged();
                        MainActivity.mPairedList.notifyDataSetChanged();
                    }
                }
                return true;
            }
        });
        menu.show();
        return true; //handled
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
        AddDeviceDialog dlg = new AddDeviceDialog(this,
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
