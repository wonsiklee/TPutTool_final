package com.android.LGSetupWizard.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.LGSetupWizard.MainActivity;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.data.LGTestFlowConfigurationInfo;
import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.ui.popup.TestResultPopupWindow;
import com.android.LGSetupWizard.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-06-13.
 */
@Accessors(prefix = "m")
public class LGTestFlowConfigFragment extends Fragment {
    private static final String TAG = LGTestFlowConfigFragment.class.getSimpleName();

    static public final int FRAGMENT_INDEX_AUTO_CONFIG = 0;
    static public final int FRAGMENT_INDEX_FTP = 1;
    static public final int FRAGMENT_INDEX_IPERF = 2;
    static public final int FRAGMENT_INDEX_HTTP = 3;

    static final int TEST_FLOW_CTRL_MSG_START_FLOW = 0x00;
    static final int TEST_FLOW_CTRL_MSG_FETCH_NEXT_AND_LAUNCH = 0x01;
    static final int TEST_FLOW_CTRL_MSG_SET_FRAGMENT_TO_CONFIGURATION = 0x02;
    static final int TEST_FLOW_CTRL_MSG_RESET_ALL_CONFIGURATION = 0x03;
    static final int TEST_FLOW_CTRL_MSG_ABORT = 0x10;

    private Context mContext;


    private View mParentView; // parent View
    private MainActivity mParentActivity;
    private ViewPager mParentViewPager;
    private HashMap<Fragment, Boolean> mTestTargetMap;
    private HashMap<Fragment, LGTestFlowConfigurationInfo> mTargetConfigurationMap;

    ArrayList<ILGTestTestFragment> mTestTargetFragmentList;
    ILGTestTestFragment mCurrentTarget;
    ILGTestFlowStateListener mTestStateListener = new ILGTestFlowStateListener() {
        @Override
        public void onTestStarted() {
            Log.d(TAG, "onTestStarted()");
        }

        @Override
        public void onTestFinished() {
            Log.d(TAG, "onTestFinished()");

            mTestFlowHandler.sendEmptyMessage(TEST_FLOW_CTRL_MSG_SET_FRAGMENT_TO_CONFIGURATION);
            if (mTestTargetFragmentList.size() > 0) {
                Log.d(TAG, mTestTargetFragmentList.size() + " tests left");
                mTestFlowHandler.sendEmptyMessage(TEST_FLOW_CTRL_MSG_FETCH_NEXT_AND_LAUNCH);
            } else {
                Log.d(TAG, "no test fragment left, clearing testTargetMap and fragmentList.");
                mTestTargetMap.clear();
                mTestTargetFragmentList.clear();
                mTestFlowHandler.sendEmptyMessage(TEST_FLOW_CTRL_MSG_RESET_ALL_CONFIGURATION);
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mTestFlowHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case TEST_FLOW_CTRL_MSG_SET_FRAGMENT_TO_CONFIGURATION:
                    setFragmentToShow(FRAGMENT_INDEX_AUTO_CONFIG);
                    break;

                case TEST_FLOW_CTRL_MSG_RESET_ALL_CONFIGURATION:
                    resetAllConfigurationInfo();
                    break;

                case TEST_FLOW_CTRL_MSG_START_FLOW:
                    // this is a preparation state.
                    // first need to client fragments, and put them in a list.
                    // this list is going to be the actual test target list.
                    Log.d(TAG, "TEST_FLOW_CTRL_MSG_START_FLOW");
                    LGTestFlowConfigFragment.this.mTestTargetFragmentList = new ArrayList<>();
                    Set<Fragment> elementSet = mTestTargetMap.keySet();
                    Iterator iter = elementSet.iterator();
                    while(iter.hasNext()) {
                        Fragment fragment = (Fragment)iter.next();
                        Log.d(TAG, "fragment.hashCode() " + fragment.hashCode());
                        if (mTestTargetMap.get(fragment)) {
                            mTestTargetFragmentList.add((ILGTestTestFragment) fragment);
                        } else {
                            Log.d(TAG, fragment.getClass().getName());
                        }
                    }
                    Log.d(TAG, "Test size = " + mTestTargetFragmentList.size());
                    if (mTestTargetFragmentList.size() > 0) {
                        sendEmptyMessage(TEST_FLOW_CTRL_MSG_FETCH_NEXT_AND_LAUNCH);
                    } else {
                        Log.d(TAG, "No test tarrgeted fragment found");
                        Toast.makeText(mContext, "설정된 테스트 항목이 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case TEST_FLOW_CTRL_MSG_FETCH_NEXT_AND_LAUNCH:
                    Log.d(TAG, "TEST_FLOW_CTRL_MSG_FETCH_NEXT_AND_LAUNCH");
                    if (mTestTargetFragmentList != null && mTestTargetFragmentList.size() > 0) {
                        mCurrentTarget = mTestTargetFragmentList.remove(0);
                        int index = 0;
                        if (mCurrentTarget instanceof LGFTPFragment) {
                            index = FRAGMENT_INDEX_FTP;
                        } else if (mCurrentTarget instanceof LGIperfFragment) {
                            index = FRAGMENT_INDEX_IPERF;
                        } else {
                            index = FRAGMENT_INDEX_HTTP;
                        }
                        Log.d(TAG, "target index in the adapter is " + index);
                        setFragmentToShow(index);
                        mCurrentTarget.setOnStateChangeListener(mTestStateListener);
                        mCurrentTarget.runTest();
                    }
                    break;

                case TEST_FLOW_CTRL_MSG_ABORT:
                    Log.d(TAG, "TEST_FLOW_CTRL_MSG_ABORT");
                    mCurrentTarget.stopTest();
                    mTestTargetMap.clear();
                    mTestTargetFragmentList.clear();
                    break;

                default:
                    break;
            }
        }
    };

    private Button mBtnStartTestFlow;
    private FloatingActionButton mFabFetchFromFragment;

    private Button mBtnOpenFTPConf;
    private Button mBtnOpeniPerfConf;
    private Button mBtnOpenHttpConf;

    private ImageButton mImgBtnShowHistory;
    private TestResultPopupWindow mTestResultPopupWindow;

    // FTP UI instances.
    private TextView mTxtViewFTPUseFileIO;
    private TextView mTxtViewFTPTCPWMem;
    private TextView mTxtViewFTPPsv;
    private TextView mTxtViewFTPUseEPSV;
    private TextView mTxtViewFTPRepeatCount;
    private TextView mTxtViewFTPRepeatInterval;
    private TextView mTxtViewFTPFileCount;

    // iPerf UI instances.
    // TODO : iPerf UI - hyukbin.ko

    //  HTTP UI instances.
    private TextView mTxtViewHTTPFileAddress;
    private TextView mTxtViewHTTPStack;
    private TextView mTxtViewHTTPUseFileIO;
    private TextView mTxtViewHTTPRepeatCount;
    private TextView mTxtViewHTTPRepeatInterval;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGTestFlowConfigFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (this.mParentView == null) {
            this.mContext = LGTestFlowConfigFragment.this.getContext();

            this.mParentView = inflater.inflate(R.layout.fragment_test_flow_config, container, false);
            this.mBtnStartTestFlow = this.mParentView.findViewById(R.id.btn_start_test_flow);
            this.mBtnStartTestFlow.setOnClickListener(v -> {
                Log.d(TAG, "start test flow.");
                mTestFlowHandler.sendEmptyMessage(TEST_FLOW_CTRL_MSG_START_FLOW);
            });
            this.mBtnOpenFTPConf = this.mParentView.findViewById(R.id.btn_open_ftp_config);
            this.mBtnOpenFTPConf.setOnClickListener(v -> {
                showFetchFabBtn();
                setFragmentToShow(FRAGMENT_INDEX_FTP);
            });

            this.mBtnOpeniPerfConf = this.mParentView.findViewById(R.id.btn_open_iperf_config);
            this.mBtnOpeniPerfConf.setOnClickListener(v -> {
                showFetchFabBtn();
                setFragmentToShow(FRAGMENT_INDEX_IPERF);
            });

            this.mBtnOpenHttpConf = this.mParentView.findViewById(R.id.btn_open_http_config);
            this.mBtnOpenHttpConf.setOnClickListener(v -> {
                showFetchFabBtn();
                setFragmentToShow(FRAGMENT_INDEX_HTTP);
            });

            this.mParentActivity = ((MainActivity)(LGTestFlowConfigFragment.this.getActivity()));
            this.mFabFetchFromFragment = this.mParentActivity.getFabFetchInfo();
            this.mFabFetchFromFragment.setOnClickListener(v -> {
                LGTestFlowConfigurationInfo testConfigurationInfo = ((ILGTestTestFragment) mParentActivity.getFragmentPagerAdapter().getItem(mParentViewPager.getCurrentItem())).getTestConfigurationInfo();
                processConfigurationInfo(testConfigurationInfo);
                mFabFetchFromFragment.setVisibility(View.INVISIBLE);
                setFragmentToShow(FRAGMENT_INDEX_AUTO_CONFIG);
            });
            this.mParentViewPager = mParentActivity.getViewPager();
            this.mTestTargetMap = new HashMap<>();
            this.mTargetConfigurationMap = new HashMap<>();

            this.mImgBtnShowHistory = this.mParentView.findViewById(R.id.imgBtn_history);
            this.mImgBtnShowHistory.setOnClickListener((v) -> {
                mTestResultPopupWindow = new TestResultPopupWindow(LGTestFlowConfigFragment.this.mContext);
                mTestResultPopupWindow.show(LGTestFlowConfigFragment.this.getView(), TestResultDBManager.TestCategory.ALL_TYPE);
            });
            
                    
            // FTP UI instance init.
            this.mTxtViewFTPUseFileIO = this.mParentView.findViewById(R.id.txtView_config_ftp_use_file_IO_value);
            this.mTxtViewFTPTCPWMem = this.mParentView.findViewById(R.id.txtView_config_ftp_tcp_buffer_size_value);
            this.mTxtViewFTPPsv = this.mParentView.findViewById(R.id.txtView_config_ftp_use_PSV_value);
            this.mTxtViewFTPUseEPSV = this.mParentView.findViewById(R.id.txtView_config_ftp_use_IPv4_epsv_value);
            this.mTxtViewFTPRepeatCount = this.mParentView.findViewById(R.id.txtView_config_ftp_repeat_count_value);
            this.mTxtViewFTPRepeatInterval = this.mParentView.findViewById(R.id.txtView_config_ftp_repeat_interval_value);
            this.mTxtViewFTPFileCount = this.mParentView.findViewById(R.id.txtView_config_ftp_file_count_value);

            // iPerf UI instance init.
            // TODO : hyukbin.ko

            // HTTP UI instance init.
            this.mTxtViewHTTPFileAddress = this.mParentView.findViewById(R.id.txtView_config_http_file_address_value);
            this.mTxtViewHTTPStack = this.mParentView.findViewById(R.id.txtView_config_http_stack_value);
            this.mTxtViewHTTPUseFileIO = this.mParentView.findViewById(R.id.txtView_config_http_use_file_IO_value);
            this.mTxtViewHTTPRepeatCount = this.mParentView.findViewById(R.id.txtView_config_http_repeat_count_value);
            this.mTxtViewHTTPRepeatInterval = this.mParentView.findViewById(R.id.txtView_config_http_repeat_interval_value);

            resetAllConfigurationInfo();

            if (isThereAnyLegitimateTestTarget()) {
                this.mBtnStartTestFlow.setEnabled(true);
            } else {
                this.mBtnStartTestFlow.setEnabled(false);
            }
        }
        return mParentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
    }

    private void setFragmentToShow(int position) {
        this.mParentViewPager.setCurrentItem(position);
    }

    private void showFetchFabBtn() {
        int sNavigationBarHeight = 0;

        LGTestFlowConfigFragment.this.mFabFetchFromFragment.setVisibility(View.VISIBLE);
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            sNavigationBarHeight = resources.getDimensionPixelSize(resourceId);
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mFabFetchFromFragment.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, sNavigationBarHeight + 100);
    }

    private void processConfigurationInfo(LGTestFlowConfigurationInfo info) {
        if (info instanceof LGFTPFragment.LGFTPTestFlowConfigurationInfo) {
            Log.d(TAG, "LGFTPTestFlowConfigurationInfo returned");
            Log.d(TAG, "returned info : " + info.isGoodToGo());
            LGFTPFragment.LGFTPTestFlowConfigurationInfo sFtpTestConfig = (LGFTPFragment.LGFTPTestFlowConfigurationInfo) info;
            this.mTxtViewFTPUseFileIO.setText(Utils.getBooleanString(sFtpTestConfig.isUsingFTPFileIO()));
            this.mTxtViewFTPTCPWMem.setText(sFtpTestConfig.getFTPBufferSize() + "");
            this.mTxtViewFTPPsv.setText(Utils.getBooleanString(sFtpTestConfig.isUsingFTPPSV()));
            this.mTxtViewFTPUseEPSV.setText(Utils.getBooleanString(sFtpTestConfig.isUsingIPv4EPSV()));
            this.mTxtViewFTPRepeatCount.setText(sFtpTestConfig.getFTPRepeatCount() + "");
            this.mTxtViewFTPRepeatInterval.setText(sFtpTestConfig.getFTPRepeatInterval() + "");
            this.mTxtViewFTPFileCount.setText(sFtpTestConfig.getFileCount() + "");

        } else if (info instanceof LGHTTPFragment.LGHTTPTestFlowConfigurationInfo) {
            Log.d(TAG, "LGHTTPTestFlowConfigurationInfo returned");
            Log.d(TAG, "returned info : " + info.isGoodToGo());
            LGHTTPFragment.LGHTTPTestFlowConfigurationInfo sHttpTestConfig = (LGHTTPFragment.LGHTTPTestFlowConfigurationInfo) info;
            this.mTxtViewHTTPFileAddress.setText(sHttpTestConfig.getHTTPFileAddress());
            this.mTxtViewHTTPStack.setText(sHttpTestConfig.getHTTPStack());
            this.mTxtViewHTTPUseFileIO.setText(Utils.getBooleanString(sHttpTestConfig.isUsingHTTPFileIO()));
            this.mTxtViewHTTPRepeatCount.setText(sHttpTestConfig.getHTTPRepeatCount() + "");
            this.mTxtViewHTTPRepeatInterval.setText(sHttpTestConfig.getHTTPRepeatInterval() + "");

        } else if (info instanceof LGIperfFragment.LGIperfTestFlowConfiguration) {
            Log.d(TAG, "LGIperfTestFlowConfiguration returned");
            // TODO : add codes here - hyukbin.ko

        }

        Log.d(TAG, info.getFragmentInstance().hashCode() + ", " + info.isGoodToGo());
        this.mTestTargetMap.put(info.getFragmentInstance(), info.isGoodToGo());
        this.mTargetConfigurationMap.put(info.getFragmentInstance(), info);
        Log.d(TAG, "mTargetConfigurationMap size = " + mTargetConfigurationMap.size());

        if (isThereAnyLegitimateTestTarget()) {
            this.mBtnStartTestFlow.setEnabled(true);
        } else {
            this.mBtnStartTestFlow.setEnabled(false);
        }
    }

    private boolean isThereAnyLegitimateTestTarget() {
        return this.mTestTargetMap.values().contains(new Boolean(true)) ? true: false;
    }

    private void resetAllConfigurationInfo() {
        mBtnStartTestFlow.setEnabled(false);

        // FTP configuration reset
        this.mTxtViewFTPUseFileIO.setText("N/A");
        this.mTxtViewFTPTCPWMem.setText("N/A");
        this.mTxtViewFTPPsv.setText("N/A");
        this.mTxtViewFTPUseEPSV.setText("N/A");
        this.mTxtViewFTPRepeatCount.setText("N/A");
        this.mTxtViewFTPRepeatInterval.setText("N/A");
        this.mTxtViewFTPFileCount.setText("N/A");

        // iPerf configuration reset
        // TODO : hyukbin.ko reset code goes here.

        // Http configuration reset
        this.mTxtViewHTTPFileAddress.setText("N/A");
        this.mTxtViewHTTPStack.setText("N/A");
        this.mTxtViewHTTPUseFileIO.setText("N/A");
        this.mTxtViewHTTPRepeatCount.setText("N/A");
        this.mTxtViewHTTPRepeatInterval.setText("N/A");
    }
}