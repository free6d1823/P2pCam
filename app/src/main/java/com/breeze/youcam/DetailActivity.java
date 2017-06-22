package com.breeze.youcam;

import android.content.Intent;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DetailActivity extends Activity {
    String TAG = "P2pMain";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Intent intent = getIntent();
        String uuid = intent.getStringExtra(MainActivity.EXTRA_DEVICE_DETAIL);
        // Capture the layout's TextView and set the string as its text


        DeviceInfo di = MainActivity.mPairedList.findByUuid(uuid);

        if (di != null) {
            initUi(di);
        }

    }
    void initUi(DeviceInfo di) {
        TextView tvUuid = (TextView) findViewById(R.id.tvUuid);
        tvUuid.setText(di.uuid);
        EditText etName = (EditText) findViewById(R.id.etName);
        etName.setText(di.name);
        EditText etPassword = (EditText) findViewById(R.id.etPassword);
        etPassword.setText(di.password);
        TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvStatus.setText(di.statusString());
        TextView tvIp = (TextView) findViewById(R.id.tvIp);
        tvIp.setText(di.ip);
        TextView tvPort = (TextView) findViewById(R.id.tvPort);
        tvPort.setText(Integer.toString(di.port));
        final TextView tvTz = (TextView) findViewById(R.id.tvTimeZone);
        Button btnTz = (Button) findViewById(R.id.btnTimeZone);
        btnTz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TimeZone tz = TimeZone.getDefault();
                //tvTz.setText(tz.getDisplayName().toString());

            }
        });
    }

}
