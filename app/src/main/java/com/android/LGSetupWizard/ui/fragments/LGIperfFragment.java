package com.android.LGSetupWizard.ui.fragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.adapters.LGIperfMenuListAdapter;
import com.android.LGSetupWizard.data.ILGTestFlowConfigurationInfo;
import com.android.LGSetupWizard.data.LGIperfCommand;
import com.android.LGSetupWizard.data.LGIperfConstants;
import com.android.LGSetupWizard.data.LGIperfPackageManager;
import com.android.LGSetupWizard.data.LGIperfServiceHelper;
import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.ui.dialog.EditTextDialog;
import com.android.LGSetupWizard.ui.dialog.EditTextUnitDialog;
import com.android.LGSetupWizard.ui.dialog.NumberPickerDialog;
import com.android.LGSetupWizard.ui.dialog.OnSetDialogListener;
import com.android.LGSetupWizard.ui.popup.TestResultPopupWindow;
import com.android.LGSetupWizard.utils.LGIperfUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

@Accessors(prefix = "m")
public class LGIperfFragment extends Fragment
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemClickListener, OnSetDialogListener, ILGTestFlowFragment  {

    private static final String TAG = LGIperfFragment.class.getSimpleName();

    private static final String PREF_IPERF = "PREF_IPERF";
    private static final String PREF_IPERF_COMMAND_LIST = "PREF_IPERF_COMMAND_ARRAY_LIST";

    private Context mContext;

    private List<String> mIperfCommands;
    private LGIperfCommand mSelectedIperfCommand;

    //UI
    private static final int LAYOUT_MODE_MAIN = 0;
    private static final int LAYOUT_MODE_MENU = 1;
    private int currentMode = LAYOUT_MODE_MAIN;

    // MAIN Layout
    private View mView;
    private View mainLayoutView;
    private ToggleButton toggleBtn_iperf_start_n_stop;
    private Switch switch_iperf_version;
    private TextView tv_iperf_output;
    private EditText editText_iperf_option;
    private ScrollView scrollView_output;
    private Button btn_iperf_advanced;

    private TextView tv_repeat_count,tv_repeat_interval;
    private NumberPickerDialog mTestRepeatDialog;
    private NumberPickerDialog mTestIntervalDialog;
    private ImageButton imageButton_iperf_result;

    // Menu Layout
    private View menuLayoutView;
    private Button btn_iperf_menu_back;
    private Button btn_iperf_menu_add;
    private Button btn_iperf_menu_delete;
    private Button btn_iperf_menu_select;
    private ListView listView_iperf_comnnad;
    private LGIperfMenuListAdapter mIperfMenuListAdapter;

    private Switch switch_iperf_menu_version;
    private Switch switch_iperf_menu_server_client;
    private Button btn_iperf_menu_host;
    private Button btn_iperf_menu_port;
    private Switch swtich_iperf_menu_tcp_udp;
    private Button btn_iperf_menu_duration;
    private Button btn_iperf_menu_interval;
    private Button btn_iperf_menu_rate;
    private Button btn_iperf_menu_steams;
    private Button btn_iperf_menu_others;

    // Menu Dialog
    private EditTextUnitDialog mRateDialog;
    private EditTextDialog mPortDialog;
    private EditTextDialog mHostDialog;
    private EditTextDialog mOtherDialog;
    private NumberPickerDialog mDurationDialog;
    private NumberPickerDialog mIntervalDialog;
    private NumberPickerDialog mStreamDialog;


    private LGIperfPackageManager mLGIperfPackageManager;
    private LGIperfServiceHelper mIperfSserviceHelper;


    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        mContext = context;
        Log.i(TAG,"onAttach - regi - filter ");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LGIperfConstants.ACTION_RESULT_INSTALL_IPERF);
        intentFilter.addAction(LGIperfConstants.ACTION_RESULT_COMMAND);
        intentFilter.addAction(LGIperfConstants.ACTION_SAVE_AVG);
        intentFilter.addAction(LGIperfConstants.ACTION_STOP_IPERF);
        context.registerReceiver(mReceiver,intentFilter);

        if ( mIperfSserviceHelper == null ) {
            mIperfSserviceHelper = new LGIperfServiceHelper(context);
        }

        if(mLGIperfPackageManager == null){
            try {
                mLGIperfPackageManager = new LGIperfPackageManager(context);
                mLGIperfPackageManager.setOnInstalledPackaged(new LGIperfPackageManager.OnInstalledPackaged() {
                    @Override
                    public void packageInstalled(String packageName, int returnCode) {
                        if (returnCode == LGIperfPackageManager.INSTALL_SUCCEEDED) {
                            Log.d(TAG, "Install succeeded");
                            requestLoadIperfService();
                        } else {
                            Log.d(TAG, "Install failed: " + returnCode);
                            //TODO fail pacakge...how to?!
                        }
                    }
                });
            } catch (NoSuchMethodException e) {
                //TODO need check error case.
                Log.e(TAG, "LGIperfPacakge not created!!"+e.toString());
                return;
            }

            if(mLGIperfPackageManager.needIstallIperfPackage()){
                mLGIperfPackageManager.requestInstallIperfPackage();
            }
            else {
                requestLoadIperfService();
            }
        }

        initDialog();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "broadcastreceiver got action:"+action);

            if (LGIperfConstants.ACTION_RESULT_INSTALL_IPERF.equals(action)) {
                Log.d(TAG,"checkin - iperf installed...="+intent.getBooleanExtra(LGIperfConstants.EXTRA_RESULT_INSTALL_IPERF,false));
            }
            else if(LGIperfConstants.ACTION_RESULT_COMMAND.equals(action)) {
                onResultUpdated(intent.getStringExtra(LGIperfConstants.EXTRA_RESULT_COMMAND));
            }
            else if(LGIperfConstants.ACTION_SAVE_AVG.equals(action)) {
                float avg = intent.getFloatExtra(LGIperfConstants.EXTRA_RESULT_COMMAND,-1);
                if (avg < 0 ) {
                    Log.d(TAG, "goit invalid avg!!");
                    return;
                }
                Log.d(TAG,"avg = "+avg);
                saveIperfTput(avg);
            }
            else if(LGIperfConstants.ACTION_STOP_IPERF.equals(action)) {
                onIperfStoped();
            }
        }
    };
    private void onResultUpdated(String message) {
        Log.d(TAG,"onResultUpdate="+message);
        this.mHandler.sendMessage(Message.obtain(null,MSG_RESULT_COMMAND,message));
    }

    public void onIperfStoped() {
        mCurrentRepeatCount--;

        if ( mCurrentRepeatCount==0) {
            toggleBtn_iperf_start_n_stop.setChecked(false);
            lockComponent(false);
        }else{
            this.mHandler.sendEmptyMessageDelayed(MSG_REPEAT_COMMAND, mRepeatInterval*1000);
        }
    }

    private boolean isDoubleTouched = false;
    private final static int MSG_RESET_FIRST_TOUCH = 0x1;
    private final static int MSG_REPEAT_COMMAND = 0x2;
    private final static int MSG_RESULT_COMMAND = 0x3;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_REPEAT_COMMAND){
                mIperfSserviceHelper.startCommand(mSelectedIperfCommand.toString());
            } else if(msg.what == MSG_RESULT_COMMAND){
                final String message = (String)msg.obj;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tv_iperf_output != null) tv_iperf_output.append(message);
                        if (scrollView_output != null) scrollView_output.fullScroll(130);
                    }
                });

            }
        }
    };

    public boolean changeLayoutIfNeed() {
        if (currentMode == LAYOUT_MODE_MENU) {
            switchLayout();
            return true;
        }
        return false;
    }


    //TODO need Move the below method to IperfServiceHelper?
    private void requestLoadIperfService() {
        if( !mIperfSserviceHelper.isBound() && mIperfSserviceHelper.doBindService() ){
            //TODO can't bind Service

            return;
        }
        mIperfSserviceHelper.checkIperf();
    }

    //TODO need Move the below method to IperfServiceHelper?
    private void requestReleaseIperfService(){
        if( mIperfSserviceHelper!=null && mIperfSserviceHelper.isBound()){
            mIperfSserviceHelper.unBindService();
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_iperf, container, false);

            Log.d(TAG, "onCreateView() - loaded mView");
            FrameLayout padding_bottom = (FrameLayout) mView.findViewById(R.id.iperf_padding_bottom);
            /* only use Fragment */
            if(hasSoftMenu()){
                padding_bottom.getLayoutParams().height = getSoftMenuHeight();
                padding_bottom.requestLayout();
            }

            mainLayoutView = (View) mView.findViewById(R.id.iperf_main);
            initUIMainView(mainLayoutView);

            menuLayoutView = (View) mView.findViewById(R.id.iperf_menu);
            initUIMenuView(menuLayoutView);

            setEnableMenu(false);
        }

        return mView;
    }


    @Override
    public void onDetach(){
        super.onDetach();
        Log.i(TAG,"onDetach");
        mContext.unregisterReceiver(mReceiver);

        requestReleaseIperfService();
    }

    private static final int PACKAGE_INSTALLER_STATUS_UNDEFINED = -1000;


    private void saveIperfTput(float avg){
        TestResultDBManager.getInstance(getContext()).insert(TestResultDBManager.TestCategory.iPerf, avg,null);
    }

    private static final String IPERF_APP= "com.lge.kobinfactory.lgiperf";
    private boolean hasIperfApp() {
        boolean isExist = false;

        PackageManager pkgMgr = getContext().getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith(IPERF_APP)) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    private void initUIMainView(View view) {
        toggleBtn_iperf_start_n_stop = (ToggleButton) view.findViewById(R.id.toggleBtn_iperf_start_n_stop);
        toggleBtn_iperf_start_n_stop.setOnClickListener(this);

        tv_iperf_output = (TextView) view.findViewById(R.id.tv_iperf_output);
        editText_iperf_option = (EditText) view.findViewById(R.id.editText_iperf_option);
        editText_iperf_option.setOnFocusChangeListener(mEditFocusOutHideKeboardListener);

        switch_iperf_version = (Switch) view.findViewById(R.id.switch_iperf_version);
        switch_iperf_version.setOnCheckedChangeListener(this);

        btn_iperf_advanced = (Button) view.findViewById(R.id.btn_iperf_advanced);
        btn_iperf_advanced.setOnClickListener(this);

        scrollView_output = (ScrollView) view.findViewById(R.id.scrollView_output);

        tv_repeat_count = (TextView) view.findViewById(R.id.tv_repeat_count);
        tv_repeat_count.setOnClickListener(this);

        mRepeatCount = Integer.valueOf(tv_repeat_count.getText().toString());

        tv_repeat_interval = (TextView) view.findViewById(R.id.tv_repeat_interval);
        tv_repeat_interval.setOnClickListener(this);
        mRepeatInterval = Integer.valueOf(tv_repeat_interval.getText().toString());

        imageButton_iperf_result = (ImageButton) view.findViewById(R.id.imageButton_iperf_result);
        imageButton_iperf_result.setOnClickListener(this);
        swtichRepeatInterval();
    }
    private int mRepeatCount, mRepeatInterval;

    private void swtichRepeatInterval(){
        boolean enable = !tv_repeat_count.getText().toString().equals("1");
        Log.d(TAG, "swtichRepeatInterval="+enable  );
        tv_repeat_interval.setEnabled(enable);
    }

    private void drawMenu() {

        switch_iperf_menu_version.setOnCheckedChangeListener(null);
        switch_iperf_menu_version.setChecked(mSelectedIperfCommand.getVersion() == LGIperfConstants.IPERF_VERSION3);
        switch_iperf_menu_version.setOnCheckedChangeListener(this);

        switch_iperf_menu_server_client.setOnCheckedChangeListener(null);
        switch_iperf_menu_server_client.setChecked(mSelectedIperfCommand.getMode() == LGIperfConstants.IPERF_MODE_CLIENT);
        switch_iperf_menu_server_client.setOnCheckedChangeListener(this);

        btn_iperf_menu_host.setEnabled(switch_iperf_menu_server_client.isChecked());

        swtich_iperf_menu_tcp_udp.setOnCheckedChangeListener(null);
        swtich_iperf_menu_tcp_udp.setChecked(mSelectedIperfCommand.isUDPmode());
        swtich_iperf_menu_tcp_udp.setOnCheckedChangeListener(this);
        btn_iperf_menu_rate.setEnabled(swtich_iperf_menu_tcp_udp.isChecked());
    }

    private void setEnableMenu(boolean enable) {
        switch_iperf_menu_version.setEnabled(enable);
        switch_iperf_menu_server_client.setEnabled(enable);
        btn_iperf_menu_host.setEnabled(enable);
        btn_iperf_menu_port.setEnabled(enable);
        swtich_iperf_menu_tcp_udp.setEnabled(enable);
        btn_iperf_menu_duration.setEnabled(enable);
        btn_iperf_menu_interval.setEnabled(enable);
        btn_iperf_menu_rate.setEnabled(enable);
        btn_iperf_menu_steams.setEnabled(enable);
        btn_iperf_menu_others.setEnabled(enable);
    }

    private void refreshList() {
        if (mSelectedIperfCommand != null) {
            mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
            mIperfMenuListAdapter.notifyDataSetChanged();
        }
    }


    private void switchLayout() {
        if (currentMode == LAYOUT_MODE_MENU) {
            currentMode = LAYOUT_MODE_MAIN;
            mainLayoutView.setVisibility(View.VISIBLE);
            menuLayoutView.setVisibility(View.INVISIBLE);
            saveIperfCommand();
        } else if (currentMode == LAYOUT_MODE_MAIN) {
            currentMode = LAYOUT_MODE_MENU;
            mainLayoutView.setVisibility(View.INVISIBLE);
            menuLayoutView.setVisibility(View.VISIBLE);
            loadIperfCommand();
        }
    }
    private void initUIMenuView(View view) {
        btn_iperf_menu_back = (Button) view.findViewById(R.id.btn_iperf_menu_back);
        btn_iperf_menu_back.setOnClickListener(this);

        btn_iperf_menu_add = (Button) view.findViewById(R.id.btn_iperf_menu_add);
        btn_iperf_menu_add.setOnClickListener(this);

        btn_iperf_menu_delete = (Button) view.findViewById(R.id.btn_iperf_menu_delete);
        btn_iperf_menu_delete.setOnClickListener(this);

        btn_iperf_menu_select = (Button) view.findViewById(R.id.btn_iperf_menu_select);
        btn_iperf_menu_select.setOnClickListener(this);

        listView_iperf_comnnad = (ListView) view.findViewById(R.id.listView_iperf_comnnad);
        mIperfMenuListAdapter = new LGIperfMenuListAdapter(mContext);
        listView_iperf_comnnad.setAdapter(mIperfMenuListAdapter);
        listView_iperf_comnnad.setOnItemClickListener(this);

        switch_iperf_menu_version = (Switch) view.findViewById(R.id.switch_iperf_menu_version);
        switch_iperf_menu_server_client = (Switch) view.findViewById(R.id.switch_iperf_menu_server_client);

        btn_iperf_menu_host = (Button) view.findViewById(R.id.btn_iperf_menu_host);
        btn_iperf_menu_host.setOnClickListener(this);

        btn_iperf_menu_port = (Button) view.findViewById(R.id.btn_iperf_menu_port);
        btn_iperf_menu_port.setOnClickListener(this);

        swtich_iperf_menu_tcp_udp = (Switch) view.findViewById(R.id.swtich_iperf_menu_tcp_udp);
        btn_iperf_menu_duration = (Button) view.findViewById(R.id.btn_iperf_menu_duration);
        btn_iperf_menu_duration.setOnClickListener(this);

        btn_iperf_menu_interval = (Button) view.findViewById(R.id.btn_iperf_menu_interval);
        btn_iperf_menu_interval.setOnClickListener(this);

        btn_iperf_menu_rate = (Button) view.findViewById(R.id.btn_iperf_menu_rate);
        btn_iperf_menu_rate.setOnClickListener(this);

        btn_iperf_menu_steams = (Button) view.findViewById(R.id.btn_iperf_menu_steams);
        btn_iperf_menu_steams.setOnClickListener(this);

        btn_iperf_menu_others = (Button) view.findViewById(R.id.btn_iperf_menu_others);
        btn_iperf_menu_others.setOnClickListener(this);
    }

    private void initDialog() {
        mRateDialog = new EditTextUnitDialog(mContext,
                getResources().getString(R.string.dialog_rate_title),
                getResources().getString(R.string.dialog_rate_description),
                "0",
                0,
                Arrays.asList(getResources().getStringArray(R.array.rate_unit)));
        mRateDialog.setInputType(InputType.TYPE_CLASS_NUMBER);
        mRateDialog.setOnSetDialogListener(this);

        mHostDialog = new EditTextDialog(mContext,
                getResources().getString(R.string.dialog_host_title),
                getResources().getString(R.string.dialog_host_description),
                "");
        mHostDialog.setOnSetDialogListener(this);

        mPortDialog = new EditTextDialog(mContext,
                getResources().getString(R.string.dialog_port_title),
                getResources().getString(R.string.dialog_port_description),
                "");
        mPortDialog.setInputType(InputType.TYPE_CLASS_NUMBER);
        mPortDialog.setOnSetDialogListener(this);

        mOtherDialog = new EditTextDialog(mContext,
                getResources().getString(R.string.dialog_other_title),
                getResources().getString(R.string.dialog_other_description),
                "");
        mOtherDialog.setOnSetDialogListener(this);

        mDurationDialog = new NumberPickerDialog(mContext,
                getResources().getString(R.string.dialog_duration_title),
                getResources().getString(R.string.dialog_duration_description),
                1,
                100,
                0);
        mDurationDialog.setOnSetDialogListener(this);

        mIntervalDialog = new NumberPickerDialog(mContext,
                getResources().getString(R.string.dialog_duration_title),
                getResources().getString(R.string.dialog_duration_description),
                1,
                100,
                0);
        mIntervalDialog.setOnSetDialogListener(this);

        mStreamDialog = new NumberPickerDialog(mContext,
                getResources().getString(R.string.dialog_streams_title),
                getResources().getString(R.string.dialog_streams_description),
                1,
                10,
                0);
        mStreamDialog.setOnSetDialogListener(this);


        mTestRepeatDialog = new NumberPickerDialog(mContext,
                "Test Repeat Count",
                null,
                1,
                100,
                1);
        mTestRepeatDialog.setOnSetDialogListener(this);
        mTestRepeatDialog.setHideDelete(true);
        mTestIntervalDialog = new NumberPickerDialog(mContext,
                "Test Repeat Interval",
                null,
                1,
                100,
                5);;
        mTestIntervalDialog.setOnSetDialogListener(this);
        mTestIntervalDialog.setHideDelete(true);

    }
    private void loadIperfCommand() {
        List<String> gotCommandList = LGIperfUtils.getSharedPreferencesArrayList(mContext, PREF_IPERF, PREF_IPERF_COMMAND_LIST);
        if (gotCommandList == null || gotCommandList.isEmpty()) {
            gotCommandList = new ArrayList<String>();
            gotCommandList.add("iperf -s -i 1");
            gotCommandList.add("iperf -s -w 4M -i 1");
            gotCommandList.add("iperf -c 192.168.2.2 -w 4M -t 10 -i 1");
            gotCommandList.add("iperf3 -c 192.168.2.2 -w 4M -t 10 -i 1 -R");
            gotCommandList.add("iperf -c bouygues.iperf.fr -p 5200 -i 1 -t 10");
            gotCommandList.add("iperf3 -c bouygues.iperf.fr -p 5200 -i 1 -t 10");
            gotCommandList.add("iperf3 -c bouygues.iperf.fr -p 5200 -i 1 -t 10 -R");
        }

        if (mIperfCommands == null ||
                !(mIperfCommands.containsAll(gotCommandList) && mIperfCommands.size() == gotCommandList.size())) {
            Log.d(TAG, "loadIperfCommand - changed mIperfCommands");
            mIperfCommands = gotCommandList;
            mIperfMenuListAdapter.setCommandList(mIperfCommands);
            mIperfMenuListAdapter.notifyDataSetChanged();
        }
    }

    private void saveIperfCommand() {
        LGIperfUtils.setSharedPreferencesArrayList(mContext, PREF_IPERF, PREF_IPERF_COMMAND_LIST, mIperfMenuListAdapter.getCommandList());
    }
    private boolean hasSoftMenu() {
        boolean hasMenuKey = ViewConfiguration.get(getActivity().getApplicationContext()).hasPermanentMenuKey();
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

    private void lockComponent(boolean isLocking){
        boolean isNotLocking = !isLocking;
        editText_iperf_option.setEnabled(isNotLocking);
        btn_iperf_advanced.setEnabled(isNotLocking);
        tv_repeat_interval.setEnabled(isNotLocking);
        tv_repeat_count.setEnabled(isNotLocking);

        if(isNotLocking){
            swtichRepeatInterval();
        }
    }
    private int mCurrentRepeatCount;
    /*====================================================
                  B u t t o n    I n t e r f a c e
        =====================================================*/
    @Override
    public void onClick(View v) {
        if (v == toggleBtn_iperf_start_n_stop) {
            if (toggleBtn_iperf_start_n_stop.isChecked()) {

                Log.i(TAG, "start iperf");

                checkCurrentCommand();
                mCurrentRepeatCount = mRepeatCount;
                mIperfSserviceHelper.startCommand(mSelectedIperfCommand.toString());

                lockComponent(true);
            } else {
                Log.i(TAG, "stop iperf");
                mIperfSserviceHelper.stopCommand();
                lockComponent(false);
            }
            editText_iperf_option.clearFocus();
        } else if (v == tv_repeat_count) {
            mTestRepeatDialog.setDefaultValue(Integer.valueOf(tv_repeat_count.getText().toString()));
            mTestRepeatDialog.show();
        } else if (v == tv_repeat_interval) {
            mTestIntervalDialog.setDefaultValue(Integer.valueOf(tv_repeat_interval.getText().toString()));
            mTestIntervalDialog.show();
        } else if (v == btn_iperf_advanced || v == btn_iperf_menu_back) {
            Log.d(TAG, "clicked advandced btn");
            switchLayout();
        } else if (v == btn_iperf_menu_add) {
            mIperfMenuListAdapter.addCommand();
            mIperfMenuListAdapter.setCurrentSelectedPosition(0);
            mIperfMenuListAdapter.notifyDataSetChanged();
        } else if (v == btn_iperf_menu_delete) {
            int sCurrentSelectedPoistion = mIperfMenuListAdapter.getCurrentSelectedPosition();
            if (sCurrentSelectedPoistion < 0) return;
            mIperfMenuListAdapter.deleteCommand(sCurrentSelectedPoistion);
            mIperfMenuListAdapter.setCurrentSelectedPosition(-1);
            setEnableMenu(false);
            mIperfMenuListAdapter.notifyDataSetChanged();
        } else if (v == btn_iperf_menu_select) {
            String selectedCommand = (String) mIperfMenuListAdapter.getItem(mIperfMenuListAdapter.getCurrentSelectedPosition());
            switchLayout();

            if (selectedCommand != null) {
                Log.e(TAG, "selected !! " + selectedCommand);
                if (selectedCommand.startsWith(LGIperfConstants.IPERF3_NAME)) {
                    Log.e(TAG, "!! " + LGIperfConstants.IPERF3_NAME);
                    switch_iperf_version.setOnCheckedChangeListener(null);
                    switch_iperf_version.setChecked(true);
                    switch_iperf_version.setOnCheckedChangeListener(this);

                    if (selectedCommand.length() > LGIperfConstants.IPERF3_NAME.length())
                        editText_iperf_option.setText(selectedCommand.substring(LGIperfConstants.IPERF3_NAME.length() + 1));
                } else if (selectedCommand.startsWith(LGIperfConstants.IPERF_NAME)) {
                    Log.e(TAG, "!! " + LGIperfConstants.IPERF_NAME);
                    switch_iperf_version.setOnCheckedChangeListener(null);
                    switch_iperf_version.setChecked(false);
                    switch_iperf_version.setOnCheckedChangeListener(this);

                    if (selectedCommand.length() > LGIperfConstants.IPERF_NAME.length())
                        editText_iperf_option.setText(selectedCommand.substring(LGIperfConstants.IPERF_NAME.length() + 1));
                } else {
                    Log.e(TAG, "error!! uncorrect command");
                }
            }
        } else if (v == btn_iperf_menu_host) {
            mHostDialog.setDefaultValue((mSelectedIperfCommand.getHost() != null) ? mSelectedIperfCommand.getHost() : "");
            mHostDialog.show();
        } else if (v == btn_iperf_menu_port) {
            mPortDialog.setDefaultValue("" + (mSelectedIperfCommand.getPort() != LGIperfConstants.IPERF_NOT_SET ? mSelectedIperfCommand.getPort() : 0));
            mPortDialog.show();
        } else if (v == btn_iperf_menu_duration) {
            mDurationDialog.setDefaultValue(mSelectedIperfCommand.getDuration());
            mDurationDialog.show();
        } else if (v == btn_iperf_menu_interval) {
            mIntervalDialog.setDefaultValue(mSelectedIperfCommand.getInterval());
            mIntervalDialog.show();
        } else if (v == btn_iperf_menu_rate) {
            mRateDialog.setDefaultValue("" + (mSelectedIperfCommand.getRate() != LGIperfConstants.IPERF_NOT_SET ? mSelectedIperfCommand.getRate() : 0));
            mRateDialog.setDefaultUnitPosition(mSelectedIperfCommand.getRateUnit() != LGIperfConstants.IPERF_NOT_SET ? mSelectedIperfCommand.getRateUnit() : 0);
            mRateDialog.show();
        } else if (v == btn_iperf_menu_steams) {
            mStreamDialog.setDefaultValue(mSelectedIperfCommand.getStream());
            mStreamDialog.show();
        } else if (v == btn_iperf_menu_others) {
            Log.d(TAG,"menu_others-"+mSelectedIperfCommand.getOtherOptions());
            mOtherDialog.setDefaultValue(mSelectedIperfCommand.getOtherOptions());
            mOtherDialog.show();
        } else if (v == imageButton_iperf_result){
            new TestResultPopupWindow(getContext()).show(getView(),TestResultDBManager.TestCategory.iPerf);
        }

    }

    private void checkCurrentCommand() {
        String current_cmd = (switch_iperf_version.isChecked())? "iperf3":"iperf";
        String option = editText_iperf_option.getText().toString();

        if(option != null && !option.isEmpty()){
            current_cmd = current_cmd+" "+option;
        }

        if ( mSelectedIperfCommand== null || !current_cmd.equals(mSelectedIperfCommand.toString())){
            mSelectedIperfCommand = new LGIperfCommand(current_cmd);
        }
    }
    /*====================================================
             S w i t c h    I n t e r f a c e
   =====================================================*/
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == switch_iperf_version) {
            Log.d(TAG, "switch_iperf_version checked changed:" + isChecked);
            editText_iperf_option.clearFocus();
        } else if (buttonView == switch_iperf_menu_server_client) {
            if (isChecked) {
                mSelectedIperfCommand.setMode(LGIperfConstants.IPERF_MODE_CLIENT);
            } else {
                mSelectedIperfCommand.setMode(LGIperfConstants.IPERF_MODE_SERVER);
            }
            btn_iperf_menu_host.setEnabled(isChecked);
        } else if (buttonView == swtich_iperf_menu_tcp_udp) {
            mSelectedIperfCommand.setUDPmode(isChecked);
            btn_iperf_menu_rate.setEnabled(isChecked);
        }
        refreshList();
    }

    /*====================================================
              M e n u L i s t   I n t e r f a c e
    =====================================================*/
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView_iperf_comnnad) {
            Log.d(TAG, "listView_iperf_comnnad onItemclicked - " + position);
            mIperfMenuListAdapter.setCurrentSelectedPosition(position);
            mIperfMenuListAdapter.notifyDataSetChanged();

            mSelectedIperfCommand = new LGIperfCommand((String) mIperfMenuListAdapter.getItem(position));
            setEnableMenu(true);
            Log.d(TAG, "Selected Item = " + mSelectedIperfCommand.toString());

            drawMenu();
        }
    }

    /*====================================================
                D i a l o g    I n t e r f a c e
    =====================================================*/
    @Override
    public void onSelect(Dialog dialog, Object selectedValue) {
        if (dialog == mHostDialog) {
            mSelectedIperfCommand.setHost((String) selectedValue);
        } else if (dialog == mPortDialog) {
            mSelectedIperfCommand.setPort(Integer.valueOf((String) selectedValue));
        } else if (dialog == mDurationDialog) {
            mSelectedIperfCommand.setDuration((int) selectedValue);
        } else if (dialog == mIntervalDialog) {
            mSelectedIperfCommand.setInterval((int) selectedValue);
        } else if (dialog == mStreamDialog) {
            mSelectedIperfCommand.setStream((int) selectedValue);
        } else if (dialog == mOtherDialog) {
            mSelectedIperfCommand.setOtherOptions((String) selectedValue);
        } else if (dialog == mTestIntervalDialog) {
            mRepeatInterval = (int)selectedValue;
            Log.d(TAG,"TestIntervalDialog="+mRepeatInterval);
            tv_repeat_interval.setText(""+mRepeatInterval);
            return;
        } else if (dialog == mTestRepeatDialog) {
            mRepeatCount = (int)selectedValue;
            Log.d(TAG,"mTestRepeatDialog="+mRepeatCount);
            tv_repeat_count.setText(""+mRepeatCount);
            swtichRepeatInterval();
            return;
        }

        refreshList();
    }

    @Override
    public void onSelect(Dialog dialog, Object selectedValue, int selectedPosition) {
        if (dialog == mRateDialog) {
            mSelectedIperfCommand.setRate(Integer.valueOf((String) selectedValue));
            mSelectedIperfCommand.setRateUnit(selectedPosition);
        }
        refreshList();
    }

    @Override
    public void onCancel(Dialog dialog) {

    }

    @Override
    public void onDelete(Dialog dialog) {
        if (dialog == mHostDialog) {
            mSelectedIperfCommand.setHost("");
        } else if (dialog == mPortDialog) {
            mSelectedIperfCommand.setPort(-1);
        } else if (dialog == mDurationDialog) {
            mSelectedIperfCommand.setDuration(-1);
        } else if (dialog == mIntervalDialog) {
            mSelectedIperfCommand.setInterval(-1);
        } else if (dialog == mRateDialog) {
            mSelectedIperfCommand.setRate(-1);
            mSelectedIperfCommand.setRateUnit(-1);
        } else if (dialog == mStreamDialog) {
            mSelectedIperfCommand.setStream(-1);
        } else if (dialog == mOtherDialog) {
            mSelectedIperfCommand.setOtherOptions("");
        }

        refreshList();
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),0);
    }

    private View.OnFocusChangeListener mEditFocusOutHideKeboardListener= new View.OnFocusChangeListener(){

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(!hasFocus){
                hideKeyboard(v);
            }
        }
    };

    @Override
    public LGIperfTestFlowConfiguration getTestConfigurationInfo() {
        Log.d(TAG, "LGIPerfFragment getTestConfigurationInfo()");
        LGIperfTestFlowConfiguration info = new LGIperfTestFlowConfiguration();
        // TODO : need to implement to put all the info into 'info'
        return info;
    }

    public class LGIperfTestFlowConfiguration implements ILGTestFlowConfigurationInfo {
        // TODO : need to implement class that can hold all the info.
    }
}