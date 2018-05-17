package com.android.LGSetupWizard.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGIperfClient;

import java.lang.reflect.InvocationTargetException;

import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

@Accessors(prefix = "m")
public class LGIperfFragment extends Fragment {
    private static final String TAG = LGIperfFragment.class.getSimpleName();
    private LGIperfClient mLGIperfClient;

    private View mView;
    private Context mContext;
    //UI component
    private View mainLayoutView;
    private ToggleButton toggleBtn_iperf_start_n_stop;
    private ToggleButton switch_iperf_version;
    private TextView tv_iperf_output;
    private EditText editText_iperf_option;
    private ScrollView scrollView_output;
    private Button btn_iperf_advanced;

    private View menuLayoutView;
    private Button btn_iperf_menu_back;
    private Button btn_iperf_menu_add;
    private Button btn_iperf_menu_delete;
    private Button btn_iperf_menu_select;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        mContext = getContext();
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_iperf, container, false);
            initUIControls();

            mLGIperfClient = new LGIperfClient(getContext());
            mLGIperfClient.setOnStateChangeListener(mLGIperfOnStateChangeListener);
            mLGIperfClient.loadIperfFile();
            mLGIperfClient.setIperfVersion(LGIperfClient.MODE_IPERF);

        }

        return mView;
    }


    private void initUIControls() {

        mainLayoutView = (View)mView.findViewById(R.id.iperf_main);


        toggleBtn_iperf_start_n_stop = (ToggleButton) mainLayoutView.findViewById(R.id.toggleBtn_iperf_start_n_stop);
        toggleBtn_iperf_start_n_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(toggleBtn_iperf_start_n_stop.isChecked()){
                    mLGIperfClient.start(editText_iperf_option.getText().toString());

                }else{
                    mLGIperfClient.stop();
                }
            }
        });

        tv_iperf_output = (TextView) mainLayoutView.findViewById(R.id.tv_iperf_output);
        editText_iperf_option = (EditText)mainLayoutView.findViewById(R.id.editText_iperf_option);
        switch_iperf_version = (ToggleButton)mainLayoutView.findViewById(R.id.switch_iperf_version);
        switch_iperf_version.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mLGIperfClient.setIperfVersion(LGIperfClient.MODE_IPERF3);
                }else{
                    mLGIperfClient.setIperfVersion(LGIperfClient.MODE_IPERF);
                }
            }
        });

        btn_iperf_advanced = (Button)mainLayoutView.findViewById(R.id.btn_iperf_advanced);
        btn_iperf_advanced.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"clicked advandced btn");
                switchLayout(LAYOUT_MODE_MENU);
            }
        });
        scrollView_output = (ScrollView)mainLayoutView.findViewById(R.id.scrollView_output);


        menuLayoutView = (View)mView.findViewById(R.id.iperf_menu);
        FrameLayout padding_bottom = (FrameLayout) menuLayoutView.findViewById(R.id.padding_bottom);
        //padding_bottom.getLayoutParams().height = getNavigationBarHeight(mContext);
        //padding_bottom.requestLayout();
        if(hasSoftMenu()){
            padding_bottom.getLayoutParams().height = getSoftMenuHeight();
            padding_bottom.requestLayout();
        }

        btn_iperf_menu_back = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_back);
        btn_iperf_menu_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"clicked back btn");
                switchLayout(LAYOUT_MODE_MAIN);
            }
        });

        btn_iperf_menu_add = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_add);
        btn_iperf_menu_delete = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_delete);

        btn_iperf_menu_select = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_select);
        btn_iperf_menu_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"clicked selected btn");
                //TODO send Selected data

                switchLayout(LAYOUT_MODE_MAIN);
            }
        });
    }

    private static int LAYOUT_MODE_MAIN = 0;
    private static int LAYOUT_MODE_MENU= 1;
    private void switchLayout(int mode){
        if(mode == LAYOUT_MODE_MAIN){
            mainLayoutView.setVisibility(View.VISIBLE);
            menuLayoutView.setVisibility(View.INVISIBLE);
        }else if(mode == LAYOUT_MODE_MENU){
            mainLayoutView.setVisibility(View.INVISIBLE);
            menuLayoutView.setVisibility(View.VISIBLE);
        }else{
            Log.e(TAG,"Exception! Mode");
            return;
        }
    }
    private LGIperfClient.OnStateChangeListener mLGIperfOnStateChangeListener = new LGIperfClient.OnStateChangeListener(){

        @Override
        public void onGettingMeesage(String message) {
            if (tv_iperf_output!=null) tv_iperf_output.append(message);
            if (scrollView_output!=null) scrollView_output.fullScroll(130);
        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onStopped() {
            toggleBtn_iperf_start_n_stop.setChecked(false);
        }
    };


    private boolean hasSoftMenu() {
        boolean hasMenuKey = ViewConfiguration.get(getContext().getApplicationContext()).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if (!hasMenuKey && !hasBackKey) {
            return true;
        } else {
            return false;
        }
    }

    private int getSoftMenuHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int deviceHeight = 0;

        if (resourceId > 0) {
            deviceHeight = resources.getDimensionPixelSize(resourceId);
        }

        return deviceHeight;
    }

    /*
    public static int getNavigationBarHeight(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize =getRealScreenSize(context);

        // navigation bar on the side
        if (appUsableSize.x < realScreenSize.x) {
            return realScreenSize.x - appUsableSize.x;
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return realScreenSize.y - appUsableSize.y;
        }

        return 0;
    }

    public static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {} catch (InvocationTargetException e) {} catch (NoSuchMethodException e) {}
        }

        return size;
    }
    */

}