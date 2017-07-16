package com.breeze.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActionBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.breeze.youcam.R;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WifiListActivity extends Activity{

    static String TAG = "P2pMain";

    WifiManager wifi;
    ListView lv;
    TextView textStatus;
    Button buttonScan;
    int size = 0;
    List<ScanResult> results;

    public WifiList mAdapter = null; //must be set  by inherit class
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setIcon(
                    new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = getIntent();
        textStatus = (TextView) findViewById(R.id.tvStatus);

        lv = (ListView)findViewById(R.id.listWifi);

        wifi = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled. Now making it enabled", Toast.LENGTH_LONG).show();
            //wifi.setWifiEnabled(true);
        }
        if(mAdapter == null) {
            Log.e(TAG, "You must set adaptor before call super.onCreate.");
            return;
        }
        lv.setAdapter(mAdapter);

        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                results = wifi.getScanResults();
                size = results.size();

                updateList();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                int state = wifi.getWifiState();
                switch (state) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        lv.setVisibility(View.GONE);
                        textStatus.setText("TO SEE AVAILABLE NETWORKS, \nTURN WI-FI ON.");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        lv.setVisibility(View.VISIBLE);
                        textStatus.setText("Tap on the network name to connect.");
                        break;
                };
                Log.d(TAG, "WiFi state = "+ state);
            }
        }, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

    }



    void updateList() {
        //Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();
        try
        {
            mAdapter.resetItem();
            size = size - 1;
            while (size >= 0)
            {
                if(!results.get(size).SSID.isEmpty()) {
                    WifiList.WifiItem item = new WifiList.WifiItem(results.get(size).SSID,
                            results.get(size).capabilities, results.get(size).level);
                    mAdapter.addItem(item);
                    //Log.d("P2pMain", "scan item "+results.get(size).toString());
                }

                size--;

             }
            mAdapter.sortByLevel();
            mAdapter.notifyDataSetChanged();

        }
        catch (Exception e)
        { }
    }
    public void onBack() {
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void startScan()
    {
        wifi.startScan();


    }
}
