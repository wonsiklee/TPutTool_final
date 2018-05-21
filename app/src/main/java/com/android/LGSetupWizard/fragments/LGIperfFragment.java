package com.android.LGSetupWizard.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.Util.LGIperfUtils;
import com.android.LGSetupWizard.adapters.LGIperfMenuListAdapter;
import com.android.LGSetupWizard.clients.LGIperfClient;
import com.android.LGSetupWizard.clients.LGIperfConstants;
import com.android.LGSetupWizard.data.LGIperfCommand;
import com.android.LGSetupWizard.fragments.Dialog.EditTextDialog;
import com.android.LGSetupWizard.fragments.Dialog.NumberPickerDialog;
import com.android.LGSetupWizard.fragments.Dialog.OnSetDialogListener;

import java.util.ArrayList;
import java.util.List;

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


    private static int LAYOUT_MODE_MAIN = 0;
    private static int LAYOUT_MODE_MENU= 1;
    private static final String PREF_IPERF = "PREF_IPERF";
    private static final String PREF_IPERF_COMMAND_LIST = "PREF_IPERF_COMMAND_ARRAY_LIST";

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
    private ListView listView_iperf_comnnad;
    private LGIperfMenuListAdapter mIperfMenuListAdapter;

    private List<String> mIperfCommands;

    private LGIperfCommand mSelectedIperfCommand;

    //Menu
    private ToggleButton switch_iperf_menu_version;
    private ToggleButton switch_iperf_menu_server_client;
    private Button btn_iperf_menu_host;
    //private ToggleButton toggleBtn_iperf_menu_reverse;
    private Button btn_iperf_menu_port;
    private ToggleButton swtich_iperf_menu_tcp_udp;
    private Button btn_iperf_menu_duration;
    private Button btn_iperf_menu_interval;
    private Button btn_iperf_menu_steams;
    private Button btn_iperf_menu_others;



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
            mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION2);

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
        switch_iperf_version.setOnCheckedChangeListener(mIperfVersionCheckChangeListener);

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
        btn_iperf_menu_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIperfMenuListAdapter.addCommand();
                mIperfMenuListAdapter.setCurrentSelectedPosition(0);
                mIperfMenuListAdapter.notifyDataSetChanged();
            }
        });

        btn_iperf_menu_delete = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_delete);
        btn_iperf_menu_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int sCurrentSelectedPoistion = mIperfMenuListAdapter.getCurrentSelectedPosition();

                if ( sCurrentSelectedPoistion < 0 ) return;

                mIperfMenuListAdapter.deleteCommand(sCurrentSelectedPoistion);
                mIperfMenuListAdapter.setCurrentSelectedPosition(-1);
                mIperfMenuListAdapter.notifyDataSetChanged();
            }
        });

        btn_iperf_menu_select = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_select);
        btn_iperf_menu_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO send Selected data
                String selectedCommand = (String)mIperfMenuListAdapter.getItem(mIperfMenuListAdapter.getCurrentSelectedPosition());
                switchLayout(LAYOUT_MODE_MAIN);

                if(selectedCommand != null ){
                    Log.e(TAG,"selected !! "+selectedCommand);
                    if (selectedCommand.startsWith(LGIperfConstants.IPERF3_NAME)){
                        Log.e(TAG,"started  !! "+LGIperfConstants.IPERF3_NAME);
                        switch_iperf_version.setOnCheckedChangeListener(null);
                        switch_iperf_version.setChecked(true);
                        switch_iperf_version.setOnCheckedChangeListener(mIperfVersionCheckChangeListener);
                        editText_iperf_option.setText(selectedCommand.substring(LGIperfConstants.IPERF3_NAME.length()+1));
                    }
                    else if ( selectedCommand.startsWith(LGIperfConstants.IPERF_NAME)){
                        Log.e(TAG,"started  !! "+LGIperfConstants.IPERF_NAME);
                        switch_iperf_version.setOnCheckedChangeListener(null);
                        switch_iperf_version.setChecked(false);
                        switch_iperf_version.setOnCheckedChangeListener(mIperfVersionCheckChangeListener);
                        editText_iperf_option.setText(selectedCommand.substring(LGIperfConstants.IPERF_NAME.length()+1));
                    }
                    else{
                        Log.e(TAG,"error!! uncorrect command");
                    }
                }
            }
        });

        listView_iperf_comnnad = (ListView) menuLayoutView.findViewById(R.id.listView_iperf_comnnad);
        mIperfMenuListAdapter = new LGIperfMenuListAdapter(mContext);
        listView_iperf_comnnad.setAdapter(mIperfMenuListAdapter);
        listView_iperf_comnnad.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG,"listView_iperf_comnnad onItemclicked - "+position);
                mIperfMenuListAdapter.setCurrentSelectedPosition(position);
                mIperfMenuListAdapter.notifyDataSetChanged();

                // Selected Item...

                mSelectedIperfCommand = new LGIperfCommand((String)mIperfMenuListAdapter.getItem(position));
                Log.d(TAG,"Selected Item = " +mSelectedIperfCommand.toString());

                drawMenu();
            }
        });


        switch_iperf_menu_version = (ToggleButton)menuLayoutView.findViewById(R.id.switch_iperf_menu_version);
        switch_iperf_menu_server_client = (ToggleButton)menuLayoutView.findViewById(R.id.switch_iperf_menu_server_client);
        btn_iperf_menu_host = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_host);
        btn_iperf_menu_host.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditTextDialog dialog = new EditTextDialog(getContext(),
                        getResources().getString(R.string.dialog_host_title),
                        getResources().getString(R.string.dialog_host_description),
                        mSelectedIperfCommand.getHost());
                dialog.setOnSetDialogListener(new OnSetDialogListener() {
                    @Override
                    public void onSelect(Object selectedValue) {
                        mSelectedIperfCommand.setHost((String)selectedValue);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onDelete() {
                        mSelectedIperfCommand.setHost("");
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            }
        });

        btn_iperf_menu_port = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_port);
        btn_iperf_menu_port.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditTextDialog dialog = new EditTextDialog(getContext(),
                        getResources().getString(R.string.dialog_port_title),
                        getResources().getString(R.string.dialog_port_description),
                        ""+mSelectedIperfCommand.getPort());
                dialog.setInputType(InputType.TYPE_CLASS_NUMBER);
                dialog.setOnSetDialogListener(new OnSetDialogListener() {
                    @Override
                    public void onSelect(Object selectedValue) {
                        mSelectedIperfCommand.setPort(Integer.valueOf((String)selectedValue));
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onDelete() {
                        mSelectedIperfCommand.setPort(-1);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            }
        });



        swtich_iperf_menu_tcp_udp = (ToggleButton)menuLayoutView.findViewById(R.id.swtich_iperf_menu_tcp_udp);
        btn_iperf_menu_duration = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_duration);
        btn_iperf_menu_duration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPickerDialog dialog = new NumberPickerDialog(getContext(),
                        getResources().getString(R.string.dialog_duration_title),
                        getResources().getString(R.string.dialog_duration_description),
                        1,
                        100,
                        mSelectedIperfCommand.getDuration());
                dialog.setOnSetDialogListener(new OnSetDialogListener() {
                    @Override
                    public void onSelect(Object selectedValue) {
                        mSelectedIperfCommand.setDuration((int)selectedValue);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onDelete() {
                        mSelectedIperfCommand.setDuration(-1);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            }
        });

        btn_iperf_menu_interval = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_interval);
        btn_iperf_menu_interval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPickerDialog dialog = new NumberPickerDialog(getContext(),
                        getResources().getString(R.string.dialog_interval_title),
                        getResources().getString(R.string.dialog_interval_description),
                        1,
                        100,
                        mSelectedIperfCommand.getInterval());
                dialog.setOnSetDialogListener(new OnSetDialogListener() {
                    @Override
                    public void onSelect(Object selectedValue) {
                        mSelectedIperfCommand.setInterval((int)selectedValue);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onDelete() {
                        mSelectedIperfCommand.setInterval(-1);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            }
        });


        btn_iperf_menu_steams = (Button)menuLayoutView.findViewById(R.id.btn_iperf_menu_steams);
        btn_iperf_menu_steams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberPickerDialog dialog = new NumberPickerDialog(getContext(),
                        getResources().getString(R.string.dialog_streams_title),
                        getResources().getString(R.string.dialog_streams_description),
                        1,
                        10,
                        mSelectedIperfCommand.getStream());
                dialog.setOnSetDialogListener(new OnSetDialogListener() {
                    @Override
                    public void onSelect(Object selectedValue) {
                        mSelectedIperfCommand.setStream((int)selectedValue);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onDelete() {
                        mSelectedIperfCommand.setStream(-1);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            }
        });



        btn_iperf_menu_others =(Button) menuLayoutView.findViewById(R.id.btn_iperf_menu_others);
        btn_iperf_menu_others.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditTextDialog dialog = new EditTextDialog(getContext(),
                        getResources().getString(R.string.dialog_other_title),
                        getResources().getString(R.string.dialog_other_description),
                        mSelectedIperfCommand.getOtherOptions());
                dialog.setOnSetDialogListener(new OnSetDialogListener() {
                    @Override
                    public void onSelect(Object selectedValue) {
                        mSelectedIperfCommand.setOtherOptions((String)selectedValue);
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onDelete() {
                        mSelectedIperfCommand.setOtherOptions("");
                        mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                        mIperfMenuListAdapter.notifyDataSetChanged();
                    }
                });
                dialog.show();
            }
        });
    }



    private void drawMenu(){
        switch_iperf_menu_version.setOnCheckedChangeListener(null);
        switch_iperf_menu_version.setChecked(mSelectedIperfCommand.getVersion() == LGIperfConstants.IPERF_VERSION3);
        switch_iperf_menu_version.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mSelectedIperfCommand.setVersion(LGIperfConstants.IPERF_VERSION3);
                }else{
                    mSelectedIperfCommand.setVersion(LGIperfConstants.IPERF_VERSION2);
                }
                mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                mIperfMenuListAdapter.notifyDataSetChanged();
            }
        });

        switch_iperf_menu_server_client.setOnCheckedChangeListener(null);
        switch_iperf_menu_server_client.setChecked(mSelectedIperfCommand.getMode()==LGIperfConstants.IPERF_MODE_CLIENT);
        switch_iperf_menu_server_client.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mSelectedIperfCommand.setMode(LGIperfConstants.IPERF_MODE_CLIENT);
                }else {
                    mSelectedIperfCommand.setMode(LGIperfConstants.IPERF_MODE_SERVER);
                }
                mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                mIperfMenuListAdapter.notifyDataSetChanged();

            }
        });

        swtich_iperf_menu_tcp_udp.setOnCheckedChangeListener(null);
        swtich_iperf_menu_tcp_udp.setChecked(mSelectedIperfCommand.isUDPmode());
        swtich_iperf_menu_tcp_udp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSelectedIperfCommand.setUDPmode(isChecked);
                mIperfMenuListAdapter.modifyCommand(mSelectedIperfCommand.toString());
                mIperfMenuListAdapter.notifyDataSetChanged();

            }
        });

    }

    private void switchLayout(int mode){
        if(mode == LAYOUT_MODE_MAIN){
            mainLayoutView.setVisibility(View.VISIBLE);
            menuLayoutView.setVisibility(View.INVISIBLE);

            saveIperfCommand();

        }else if(mode == LAYOUT_MODE_MENU){
            mainLayoutView.setVisibility(View.INVISIBLE);
            menuLayoutView.setVisibility(View.VISIBLE);

            loadIperfCommand();
        }else{
            Log.e(TAG,"Exception! Mode");
            return;
        }
    }

    private CompoundButton.OnCheckedChangeListener mIperfVersionCheckChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(TAG,"switch_iperf_version checked changed:" +isChecked);
            if(isChecked){
                mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION3);
            }else{
                mLGIperfClient.setIperfVersion(LGIperfConstants.IPERF_VERSION2);
            }
        }
    };

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


}