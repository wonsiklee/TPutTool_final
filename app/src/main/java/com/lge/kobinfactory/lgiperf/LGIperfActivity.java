package com.lge.kobinfactory.lgiperf;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.lge.kobinfactory.lgiperf.dialog.EditTextDialog;
import com.lge.kobinfactory.lgiperf.dialog.EditTextUnitDialog;
import com.lge.kobinfactory.lgiperf.dialog.NumberPickerDialog;
import com.lge.kobinfactory.lgiperf.dialog.OnSetDialogListener;
import com.lge.kobinfactory.lgiperf.iperf.LGIperfClient;
import com.lge.kobinfactory.lgiperf.iperf.LGIperfCommand;
import com.lge.kobinfactory.lgiperf.iperf.LGIperfConstants;
import com.lge.kobinfactory.lgiperf.iperf.LGIperfUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

//TODO if press back key in menu view -> go to main view
@Accessors(prefix = "m")
public class LGIperfActivity extends Activity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemClickListener, OnSetDialogListener, LGIperfClient.OnStateChangeListener{
    private static final String TAG = LGIperfActivity.class.getSimpleName();

    private static final String PREF_IPERF = "PREF_IPERF";
    private static final String PREF_IPERF_COMMAND_LIST = "PREF_IPERF_COMMAND_ARRAY_LIST";

    private Context mContext;
    private LGIperfClient mLGIperfClient;
    private List<String> mIperfCommands;
    private LGIperfCommand mSelectedIperfCommand;

    //UI
    private static final int LAYOUT_MODE_MAIN = 0;
    private static final int LAYOUT_MODE_MENU= 1;
    private int currentMode = LAYOUT_MODE_MAIN;

    // MAIN Layout
    private View mainLayoutView;
    private ToggleButton toggleBtn_iperf_start_n_stop;
    private Switch switch_iperf_version;
    private TextView tv_iperf_output;
    private EditText editText_iperf_option;
    private ScrollView scrollView_output;
    private Button btn_iperf_advanced;

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

    @Nullable
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        super.onCreate(savedInstanceState);
        mContext = this;

        initUIControls();

        mLGIperfClient = new LGIperfClient(mContext);
        mLGIperfClient.loadIperfFile();
        mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION2);
        mLGIperfClient.setOnStateChangeListener(this);

        initDialog();
    }

    private boolean isDoubleTouched = false;
    private final static int MSG_RESET_FIRST_TOUCH = 0x1;
    private final static int MSG_SAVE_RESULT = 0x2;

    private final static String KEY_RESULT  = "KEY_RESULT";

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_RESET_FIRST_TOUCH) {
                isDoubleTouched = false;
            }
            else if(msg.what == MSG_SAVE_RESULT){
                Bundle b = msg.getData();
                float avg = b.getFloat(KEY_RESULT);
                saveIperfResult(avg);
                Toast.makeText(mContext, "avg = "+avg, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void saveIperfResult(float result){
        Intent intent = new Intent("com.android.LGSetupWizard.LGIperf.save_result");
        intent.putExtra("avg", result);
        sendBroadcast(intent);
    }

    @Override
    public void onBackPressed() {
        if (currentMode == LAYOUT_MODE_MENU){
            switchLayout();
            return;
        }

        if (this.isDoubleTouched) {
            this.mHandler.removeMessages(MSG_RESET_FIRST_TOUCH);
            super.onBackPressed();
            return;
        } else {
            this.isDoubleTouched = true;
            Toast.makeText(this, "press back key one more if you want to exit", Toast.LENGTH_SHORT).show();
            this.mHandler.sendEmptyMessageDelayed(MSG_RESET_FIRST_TOUCH, 1000);
        }
    }

    private void initUIControls() {
        setContentView(R.layout.fragment_iperf);
        FrameLayout padding_bottom = (FrameLayout) findViewById(R.id.iperf_padding_bottom);
        /* only use Fragment
        if(hasSoftMenu()){
            padding_bottom.getLayoutParams().height = getSoftMenuHeight();
            padding_bottom.requestLayout();
        }*/

        mainLayoutView = (View)findViewById(R.id.iperf_main);
        initUIMainView(mainLayoutView);

        menuLayoutView = (View)findViewById(R.id.iperf_menu);
        initUIMenuView(menuLayoutView);

        setEnableMenu(false);
    }

    private void initUIMainView(View view){
        toggleBtn_iperf_start_n_stop = (ToggleButton) view.findViewById(R.id.toggleBtn_iperf_start_n_stop);
        toggleBtn_iperf_start_n_stop.setOnClickListener(this);

        tv_iperf_output = (TextView) view.findViewById(R.id.tv_iperf_output);
        editText_iperf_option = (EditText)view.findViewById(R.id.editText_iperf_option);

        switch_iperf_version = (Switch) view.findViewById(R.id.switch_iperf_version);
        switch_iperf_version.setOnCheckedChangeListener(this);

        btn_iperf_advanced = (Button)view.findViewById(R.id.btn_iperf_advanced);
        btn_iperf_advanced.setOnClickListener(this);

        scrollView_output = (ScrollView)view.findViewById(R.id.scrollView_output);
    }

    private void initUIMenuView(View view){
        btn_iperf_menu_back = (Button)view.findViewById(R.id.btn_iperf_menu_back);
        btn_iperf_menu_back.setOnClickListener(this);

        btn_iperf_menu_add = (Button)view.findViewById(R.id.btn_iperf_menu_add);
        btn_iperf_menu_add.setOnClickListener(this);

        btn_iperf_menu_delete = (Button)view.findViewById(R.id.btn_iperf_menu_delete);
        btn_iperf_menu_delete.setOnClickListener(this);

        btn_iperf_menu_select = (Button)view.findViewById(R.id.btn_iperf_menu_select);
        btn_iperf_menu_select.setOnClickListener(this);

        listView_iperf_comnnad = (ListView) view.findViewById(R.id.listView_iperf_comnnad);
        mIperfMenuListAdapter = new LGIperfMenuListAdapter(mContext);
        listView_iperf_comnnad.setAdapter(mIperfMenuListAdapter);
        listView_iperf_comnnad.setOnItemClickListener(this);

        switch_iperf_menu_version = (Switch)view.findViewById(R.id.switch_iperf_menu_version);
        switch_iperf_menu_server_client = (Switch)view.findViewById(R.id.switch_iperf_menu_server_client);

        btn_iperf_menu_host = (Button)view.findViewById(R.id.btn_iperf_menu_host);
        btn_iperf_menu_host.setOnClickListener(this);

        btn_iperf_menu_port = (Button)view.findViewById(R.id.btn_iperf_menu_port);
        btn_iperf_menu_port.setOnClickListener(this);

        swtich_iperf_menu_tcp_udp = (Switch)view.findViewById(R.id.swtich_iperf_menu_tcp_udp);
        btn_iperf_menu_duration = (Button)view.findViewById(R.id.btn_iperf_menu_duration);
        btn_iperf_menu_duration.setOnClickListener(this);

        btn_iperf_menu_interval = (Button)view.findViewById(R.id.btn_iperf_menu_interval);
        btn_iperf_menu_interval.setOnClickListener(this);

        btn_iperf_menu_rate = (Button)view.findViewById(R.id.btn_iperf_menu_rate);
        btn_iperf_menu_rate.setOnClickListener(this);

        btn_iperf_menu_steams = (Button)view.findViewById(R.id.btn_iperf_menu_steams);
        btn_iperf_menu_steams.setOnClickListener(this);

        btn_iperf_menu_others =(Button) view.findViewById(R.id.btn_iperf_menu_others);
        btn_iperf_menu_others.setOnClickListener(this);
    }
    private void initDialog(){
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
    }

    private void drawMenu(){

        switch_iperf_menu_version.setOnCheckedChangeListener(null);
        switch_iperf_menu_version.setChecked(mSelectedIperfCommand.getVersion() == LGIperfConstants.IPERF_VERSION3);
        switch_iperf_menu_version.setOnCheckedChangeListener(this);

        switch_iperf_menu_server_client.setOnCheckedChangeListener(null);
        switch_iperf_menu_server_client.setChecked(mSelectedIperfCommand.getMode()==LGIperfConstants.IPERF_MODE_CLIENT);
        switch_iperf_menu_server_client.setOnCheckedChangeListener(this);

        btn_iperf_menu_host.setEnabled(switch_iperf_menu_server_client.isChecked());

        swtich_iperf_menu_tcp_udp.setOnCheckedChangeListener(null);
        swtich_iperf_menu_tcp_udp.setChecked(mSelectedIperfCommand.isUDPmode());
        swtich_iperf_menu_tcp_udp.setOnCheckedChangeListener(this) ;
        btn_iperf_menu_rate.setEnabled(swtich_iperf_menu_tcp_udp.isChecked());
    }

    private void setEnableMenu(boolean enable){
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

    private void refreshList(){
        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
        mIperfMenuListAdapter.notifyDataSetChanged();
    }


    private void switchLayout(){
        if(currentMode == LAYOUT_MODE_MENU){
            currentMode = LAYOUT_MODE_MAIN;
            mainLayoutView.setVisibility(View.VISIBLE);
            menuLayoutView.setVisibility(View.INVISIBLE);
            saveIperfCommand();
        }else if(currentMode == LAYOUT_MODE_MAIN){
            currentMode = LAYOUT_MODE_MENU;
            mainLayoutView.setVisibility(View.INVISIBLE);
            menuLayoutView.setVisibility(View.VISIBLE);
            loadIperfCommand();
        }
    }

    private void loadIperfCommand(){
        List<String> gotCommandList = LGIperfUtils.getSharedPreferencesArrayList(mContext, PREF_IPERF, PREF_IPERF_COMMAND_LIST);
        if ( gotCommandList == null ||  gotCommandList.isEmpty()){
            gotCommandList = new ArrayList<String>();
            gotCommandList.add("iperf -s -i 1");
            gotCommandList.add("iperf -c bouygues.iperf.fr -p 5200 -i 1 -t 10");
            gotCommandList.add("iperf3 -s -i 1");
            gotCommandList.add("iperf3 -c bouygues.iperf.fr -p 5200 -i 1 -t 10");
        }

        if(mIperfCommands == null ||
                !(mIperfCommands.containsAll(gotCommandList) && mIperfCommands.size()== gotCommandList.size()) )
        {
            Log.d(TAG,"loadIperfCommand - changed mIperfCommands");
            mIperfCommands = gotCommandList;
            mIperfMenuListAdapter.setCommandList(mIperfCommands);
            mIperfMenuListAdapter.notifyDataSetChanged();
        }
    }

    private void saveIperfCommand(){
        LGIperfUtils.setSharedPreferencesArrayList(mContext, PREF_IPERF, PREF_IPERF_COMMAND_LIST, mIperfMenuListAdapter.getCommandList() );
    }

    private boolean isReadyIpAddress(){
        String ipAddr = LGIperfUtils.getIPAddress();
        if(ipAddr == null){
            tv_iperf_output.append("Device has no IP addr\n");
            scrollView_output.fullScroll(130);
            return false;
        }
        tv_iperf_output.append("Device IP: "+ipAddr+"\n");
        scrollView_output.fullScroll(130);
        return true;

    }

    private boolean hasSoftMenu() {
        boolean hasMenuKey = ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey();
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

    /*====================================================
        L G I p e r f C l i e n t    I n t e r f a c e
    =====================================================*/
    @Override
    public void onGettingMeesage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tv_iperf_output!=null) tv_iperf_output.append(message);
                if (scrollView_output!=null) scrollView_output.fullScroll(130);
            }
        });
    }

    @Override
    public void onStarted() {

    }

    @Override
    public void onResultSave(float avg) {
        Message message = mHandler.obtainMessage(MSG_SAVE_RESULT);
        Bundle b = new Bundle();
        b.putFloat(KEY_RESULT,avg);
        message.setData(b);
        this.mHandler.sendMessage(message);
    }

    @Override
    public void onStopped() {
        toggleBtn_iperf_start_n_stop.setChecked(false);
    }
    /*====================================================
              B u t t o n    I n t e r f a c e
    =====================================================*/
    @Override
    public void onClick(View v) {
        if(v == toggleBtn_iperf_start_n_stop){
            if(toggleBtn_iperf_start_n_stop.isChecked()){
                Log.i(TAG,"start iperf");
                mLGIperfClient.start(editText_iperf_option.getText().toString());
            }else{
                Log.i(TAG,"stop iperf");
                mLGIperfClient.stop();
            }
        }
        else if(v == btn_iperf_advanced || v == btn_iperf_menu_back){
            Log.d(TAG,"clicked advandced btn");
            switchLayout();
        }
        else if(v== btn_iperf_menu_add){
            mIperfMenuListAdapter.addCommand();
            mIperfMenuListAdapter.setCurrentSelectedPosition(0);
            mIperfMenuListAdapter.notifyDataSetChanged();
        }
        else if(v== btn_iperf_menu_delete) {
            int sCurrentSelectedPoistion = mIperfMenuListAdapter.getCurrentSelectedPosition();
            if (sCurrentSelectedPoistion < 0) return;
            mIperfMenuListAdapter.deleteCommand(sCurrentSelectedPoistion);
            mIperfMenuListAdapter.setCurrentSelectedPosition(-1);
            setEnableMenu(false);
            mIperfMenuListAdapter.notifyDataSetChanged();
        }
        else if(v== btn_iperf_menu_select){
            String selectedCommand = (String)mIperfMenuListAdapter.getItem(mIperfMenuListAdapter.getCurrentSelectedPosition());
            switchLayout();

            if(selectedCommand != null ){
                Log.e(TAG,"selected !! "+selectedCommand);
                if (selectedCommand.startsWith(LGIperfConstants.IPERF3_NAME)){
                    Log.e(TAG,"started  !! "+LGIperfConstants.IPERF3_NAME);
                    switch_iperf_version.setOnCheckedChangeListener(null);
                    switch_iperf_version.setChecked(true);
                    switch_iperf_version.setOnCheckedChangeListener(LGIperfActivity.this);
                    mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION3);
                    if(selectedCommand.length() > LGIperfConstants.IPERF3_NAME.length() )
                        editText_iperf_option.setText(selectedCommand.substring(LGIperfConstants.IPERF3_NAME.length()+1));
                }
                else if ( selectedCommand.startsWith(LGIperfConstants.IPERF_NAME)){
                    Log.e(TAG,"started  !! "+LGIperfConstants.IPERF_NAME);
                    switch_iperf_version.setOnCheckedChangeListener(null);
                    switch_iperf_version.setChecked(false);
                    switch_iperf_version.setOnCheckedChangeListener(LGIperfActivity.this);
                    mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION2);
                    if(selectedCommand.length() > LGIperfConstants.IPERF_NAME.length() )
                        editText_iperf_option.setText(selectedCommand.substring(LGIperfConstants.IPERF_NAME.length()+1));
                }
                else{
                    Log.e(TAG,"error!! uncorrect command");
                }
            }
        }
        else if ( v == btn_iperf_menu_host){
            mHostDialog.setDefaultValue((mSelectedIperfCommand.getHost()!=null)?mSelectedIperfCommand.getHost():"");
            mHostDialog.show();
        }
        else if ( v == btn_iperf_menu_port ){
            mPortDialog.setDefaultValue(""+(mSelectedIperfCommand.getPort()!=LGIperfConstants.IPERF_NOT_SET? mSelectedIperfCommand.getPort() : 0));
            mPortDialog.show();
        }
        else if ( v == btn_iperf_menu_duration){
            mDurationDialog.setDefaultValue(mSelectedIperfCommand.getDuration());
            mDurationDialog.show();
        }

        else if ( v == btn_iperf_menu_interval){
            mIntervalDialog.setDefaultValue(mSelectedIperfCommand.getInterval());
            mIntervalDialog.show();
        }
        else if ( v== btn_iperf_menu_rate){
            mRateDialog.setDefaultValue(""+(mSelectedIperfCommand.getRate()!=LGIperfConstants.IPERF_NOT_SET? mSelectedIperfCommand.getRate() : 0));
            mRateDialog.setDefaultUnitPosition(mSelectedIperfCommand.getRateUnit()!=LGIperfConstants.IPERF_NOT_SET? mSelectedIperfCommand.getRateUnit() : 0);
        }
        else if ( v == btn_iperf_menu_steams){
            mStreamDialog.setDefaultValue(mSelectedIperfCommand.getStream());
            mStreamDialog.show();
        }
        else if(v == btn_iperf_menu_others){
            mOtherDialog.setDefaultValue(mSelectedIperfCommand.getOtherOptions());
            mOtherDialog.show();
        }
    }

    /*====================================================
              S w i t c h    I n t e r f a c e
    =====================================================*/
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if( buttonView == switch_iperf_version){
            Log.d(TAG,"switch_iperf_version checked changed:" +isChecked);
            if(isChecked){
                mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION3);
            }else{
                mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION2);
            }
        }
        else if ( buttonView == switch_iperf_menu_server_client){
            if(isChecked){
                mSelectedIperfCommand.setMode(LGIperfConstants.IPERF_MODE_CLIENT);
            }else {
                mSelectedIperfCommand.setMode(LGIperfConstants.IPERF_MODE_SERVER);
            }
            btn_iperf_menu_host.setEnabled(isChecked);
        }
        else if ( buttonView == swtich_iperf_menu_tcp_udp){
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
        if(parent == listView_iperf_comnnad) {
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
        if(dialog == mHostDialog){
            mSelectedIperfCommand.setHost((String)selectedValue);
        }
        else if(dialog == mPortDialog) {
            mSelectedIperfCommand.setPort(Integer.valueOf((String) selectedValue));
        }
        else if(dialog == mDurationDialog){
            mSelectedIperfCommand.setDuration((int)selectedValue);
        }
        else if(dialog == mIntervalDialog){
            mSelectedIperfCommand.setInterval((int)selectedValue);
        }
        else if(dialog == mStreamDialog ){
            mSelectedIperfCommand.setStream((int)selectedValue);
        }
        else if (dialog == mOtherDialog){
            mSelectedIperfCommand.setOtherOptions((String)selectedValue);
        }

        refreshList();
    }

    @Override
    public void onSelect(Dialog dialog, Object selectedValue, int selectedPosition) {
        if(dialog == mRateDialog){
            mSelectedIperfCommand.setRate(Integer.valueOf((String)selectedValue));
            mSelectedIperfCommand.setRateUnit(selectedPosition);
        }
        refreshList();
    }

    @Override
    public void onCancel(Dialog dialog) {

    }

    @Override
    public void onDelete(Dialog dialog) {
        if(dialog == mHostDialog){
            mSelectedIperfCommand.setHost("");
        }
        else if(dialog == mPortDialog){
            mSelectedIperfCommand.setPort(-1);
        }
        else if(dialog == mDurationDialog){
            mSelectedIperfCommand.setDuration(-1);
        }
        else if(dialog == mIntervalDialog){
            mSelectedIperfCommand.setInterval(-1);
        }
        else if(dialog == mRateDialog){
            mSelectedIperfCommand.setRate(-1);
            mSelectedIperfCommand.setRateUnit(-1);
        }
        else if(dialog == mStreamDialog){
            mSelectedIperfCommand.setStream(-1);
        }
        else if (dialog == mOtherDialog){
            mSelectedIperfCommand.setOtherOptions("");
        }

        refreshList();
    }
}