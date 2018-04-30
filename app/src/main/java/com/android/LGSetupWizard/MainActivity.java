package com.android.LGSetupWizard;

import com.android.LGSetupWizard.adapters.FragmentPagerAdapter;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements BottomNavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName() + " tput";

    private ViewPager mViewPager;
    private FragmentPagerAdapter mFragmentPagerAdapter;

    private BottomNavigationView mNavigation;

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_configuration:
                Log.d(TAG, "onNavigationItemSelected() " + "0");
                mViewPager.setCurrentItem(0);
                return true;
            case R.id.navigation_nia:
                Log.d(TAG, "onNavigationItemSelected() " + "1");
                mViewPager.setCurrentItem(1);
                return true;
            case R.id.navigation_ftp:
                Log.d(TAG, "onNavigationItemSelected() " + "2");
                mViewPager.setCurrentItem(2);
                return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @SuppressLint("ServiceCast")
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mFragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager());

        this.mViewPager = (ViewPager) findViewById(R.id.view_pager);
        this.mViewPager.setAdapter(this.mFragmentPagerAdapter);
        this.mViewPager.addOnPageChangeListener(this);

        this.mNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        this.mNavigation.setOnNavigationItemSelectedListener(this);
    }

    @Override
    public void onPageSelected(int position) {
        this.mNavigation.setSelectedItemId(this.mFragmentPagerAdapter.getItem(position).getId());
        this.mNavigation.getMenu().getItem(position).setChecked(true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // do nothing
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // do nothing
    }

    private boolean isDoubleTouched = false;
    private final static int MSG_RESET_FIRST_TOUCH = 0x1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_RESET_FIRST_TOUCH) {
                isDoubleTouched = false;
            }
        }
    };

    @Override
    public void onBackPressed() {
        Log.d(TAG, "isDoubleTouched " + this.isDoubleTouched);
        if (this.isDoubleTouched) {
            this.mHandler.removeMessages(MSG_RESET_FIRST_TOUCH);
            super.onBackPressed();
            return;
        } else {
            this.isDoubleTouched = true;
            Toast.makeText(this, "종료할라면 한번 더!!", Toast.LENGTH_SHORT).show();
            this.mHandler.sendEmptyMessageDelayed(MSG_RESET_FIRST_TOUCH, 1000);
        }
    }
}
