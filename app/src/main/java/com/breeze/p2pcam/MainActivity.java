package com.breeze.p2pcam;

import android.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.widget.ImageView;

public class MainActivity extends FragmentActivity
        implements BaseFragment.OnFragmentInteractionListener{
    final String TAG = "P2pMain";
    public static MainActivity gApp;
    ViewPager mPager;
    SectionsPagerAdapter mPagerAdapter;
    public static String EXTRA_DEVICE_DETAIL = "DEVICE_DETAIL";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(0);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int arg0) {
                updateIndicator();
            }
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}
            @Override
            public void onPageSelected(int arg0) {}
        });

        initP2pClient();
        gApp = this;
    }

    @Override
    public void onFragmentInteraction(int command, int data) {

    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        @Override
        public Fragment getItem(int position) {
            if(position == 0)
                return CamListFragment.newInstance("pa", "p2");
            if(position == 1)
                return EventListFragment.newInstance("pa", "p2");
            if(position == 2)
                return PlaybackFragment.newInstance("pa", "p2");
            if(position == 3)
                return SearchFragment.newInstance("pa", "p2");
            if(position == 4)
                return AboutFragment.newInstance("", "");
            return AboutFragment.newInstance("log1", "log2");
        }

        @Override
        public int getCount() {
            return 5;
        }
    }
    public void updateIndicator() {
        int[] titles= {
                R.string.title_page1,R.string.title_page2,
                R.string.title_page3, R.string.title_page4,
                R.string.title_page5
        };
        int[] icons={
                R.drawable.icon1, R.drawable.icon2, R.drawable.icon3,
                R.drawable.icon4, R.drawable.icon5
        };
        int[] ids={R.id.imageView1, R.id.imageView2,R.id.imageView3,
                R.id.imageView4,R.id.imageView5};

        for(int i=0;i<mPagerAdapter.getCount();i++)
        {
            ImageView dot = (ImageView)findViewById(ids[i]);

            if(i==mPager.getCurrentItem())
            {
                dot.setImageResource(R.drawable.page_d);
                setTitle(titles[i]);
                ActionBar actionBar = getActionBar();
                actionBar.setIcon(icons[i]);
            }else
            {
                dot.setImageResource(R.drawable.page_u);
            }

        }

    }
    //////// P2P /////////////////////////////////////////
 /*
AKFH1NVJAZVJY3AW111A

NVUTEM8BNL1UHJWD111A
L9462EM394T6YSYR111A
PBSMU5TYZLDMU6TN111A
PXWRXYE4SGR6P4G5111A
*/

    static public DeviceList mPairedList = null;

    @Override
    protected void onResume() {
        super.onResume();

    }
    @Override
    protected void onStop() {
        super.onStop();
        if( mPairedList != null)
            Config.saveDevices(mPairedList);
    }
    boolean initP2pClient()
    {
        boolean ret;

        new Config (this);
        ret = P2pClient.init();
        if(ret )  {
            mPairedList = new DeviceList(this);
            Config.loadDevices(mPairedList);

        }


        return ret;

    }


}
