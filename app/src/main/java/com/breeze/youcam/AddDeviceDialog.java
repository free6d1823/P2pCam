package com.breeze.youcam;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class AddDeviceDialog extends Dialog implements
        View.OnClickListener {
    DeviceInfo mDi = null;
    OnDialogEventListener   mListener = null;
    public Context mContext;
    public interface OnDialogEventListener {
        void onOk(DeviceInfo di);
    }
    public AddDeviceDialog(Context c, int a ) {
        super(c,a );
    }
    public AddDeviceDialog(Context c, OnDialogEventListener onDialogEventListener ) {
        super(c);
        mContext = c;
        mListener = onDialogEventListener;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_add_device);

        Button bt1 = (Button)findViewById(R.id.btnYes);
        bt1.setOnClickListener(this);
        Button bt2 = (Button)findViewById(R.id.btnNo);
        bt2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (R.id.btnYes == v.getId()) {
            if( mListener != null )
                mListener.onOk(mDi);
        }
        dismiss();
    }
}
