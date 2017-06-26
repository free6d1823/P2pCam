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

public class WifiListActivity extends Activity implements View.OnClickListener{

    static String TAG = "P2pMain";


    WifiManager wifi;
    ListView lv;
    TextView textStatus;
    Button buttonScan;
    int size = 0;
    List<ScanResult> results;

    String ITEM_NAME = "name";
    String ITEM_STATUS = "status";

    ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    SimpleAdapter adapter;
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
        buttonScan = (Button) findViewById(R.id.btnScan);
        buttonScan.setOnClickListener(this);
        lv = (ListView)findViewById(R.id.listWifi);

        wifi = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        this.adapter = new SimpleAdapter(this, arraylist, R.layout.list_wifi,
                new String[] { ITEM_NAME, ITEM_STATUS }, new int[] { R.id.tvName, R.id.tvStatus });
        lv.setAdapter(this.adapter);

        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                results = wifi.getScanResults();
                size = results.size();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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


    public void onClick(View view)
    {
        arraylist.clear();
        wifi.startScan();

        Toast.makeText(this, "Scanning...." + size, Toast.LENGTH_SHORT).show();
        try
        {
            size = size - 1;
            while (size >= 0)
            {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(ITEM_NAME, results.get(size).SSID );
                item.put(ITEM_STATUS, results.get(size).toString());

                arraylist.add(item);
                size--;
                adapter.notifyDataSetChanged();
            }
        }
        catch (Exception e)
        { }
    }
}
