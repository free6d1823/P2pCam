package com.breeze.youcam;


import android.app.Activity;
import android.content.Context;
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
import com.tutk.IOTC.st_SearchDeviceInfo;

import java.io.UnsupportedEncodingException;


/**
 * A simple {@link Fragment} subclass.
 */
public class SetupFragment extends BaseFragment{
    final String TAG = "P2pMain";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

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
    public static SetupFragment newInstance(String param1, String param2) {
        SetupFragment fragment = new SetupFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    public SetupFragment() {
        // Required empty public constructor
    }

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
        View view = inflater.inflate(R.layout.fragment_setup, container, false);

        initUi(view);

        return view;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    void initUi(View view) {
        Button btnMethod1 = (Button) view.findViewById(R.id.btnMethod1);

        btnMethod1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                onMethod1(v);
            }
        });
        Button btnMethod2 = (Button) view.findViewById(R.id.btnMethod2);
        btnMethod2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMethod2(v);
            }
        });
        Button btnMethod3 = (Button) view.findViewById(R.id.btnMethod3);
        btnMethod3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMethod3(v);
            }
        });
    }
    void onMethod1(View v) {
        Context context = v.getContext();
        Intent intent = new Intent(context, WifiSetupActivity.class);
        intent.putExtra(WifiSetupActivity.ARG_METHOD_ID, 1);

        context.startActivity(intent);

    }
    void onMethod2(View v) {

    }
    void onMethod3(View v) {

    }
}
