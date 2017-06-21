package com.breeze.p2pcam;


import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breeze.opengl.MySurfaceView;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaybackFragment extends BaseFragment {
    private MySurfaceView mGLView;
    String TAG = "PlaybackFragment";
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playback, container, false);

        mGLView = (MySurfaceView)view.findViewById(R.id.viewVideo);
        Log.d(TAG, "mGLView = "+ mGLView);
         return view;
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetupFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlaybackFragment newInstance(String param1, String param2) {
        PlaybackFragment fragment = new PlaybackFragment();
        //Bundle args = new Bundle();
        return fragment;
    }
    public PlaybackFragment() {
        // Required empty public constructor
    }



    @Override
    public void onResume() {
        super.onResume();
        mGLView.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        mGLView.onPause();
    }
}
