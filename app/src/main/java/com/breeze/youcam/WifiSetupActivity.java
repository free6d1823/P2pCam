package com.breeze.youcam;

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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.breeze.tools.WifiList;
import com.breeze.tools.WifiListActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WifiSetupActivity extends WifiListActivity {

    public static final String ARG_METHOD_ID = "MethodId";
    static String TAG = "P2pMain";
    int mMethodId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_wifi_setup); //must set this before super created
        int[] imageIds = {R.drawable.wifi_n_0, R.drawable.wifi_n_1, R.drawable.wifi_n_2,
                R.drawable.wifi_n_3,R.drawable.wifi_n_4,
                R.drawable.wifi_p_0, R.drawable.wifi_p_1, R.drawable.wifi_p_2,
                R.drawable.wifi_p_3,R.drawable.wifi_p_4,
        };
        mAdapter = new WifiList(this, R.layout.list_wifi, imageIds);
        super.onCreate(savedInstanceState);

        startScan();
    }
    @Override
    public void onBack() {
            navigateUpTo(new Intent(this, SetupFragment.class));
    }

}
