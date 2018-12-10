package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.MainActivity;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.data.LGTestFlowConfigurationInfo;
import com.android.LGSetupWizard.utils.Utils;

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
import android.widget.TextView;
import android.widget.Toast;

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

    static private final int FRAGMENT_INDEX_TEST_FLOW = 1;
    static private final int FRAGMENT_INDEX_FTP = 2;
    static private final int FRAGMENT_INDEX_IPERF = 3;
    static private final int FRAGMENT_INDEX_HTTP = 4;

    static final int TEST_FLOW_CTRL_MSG_START_FLOW = 0x00;
    static final int TEST_FLOW_CTRL_MSG_FETCH_NEXT_AND_LAUNCH = 0x01;
    static final int TEST_FLOW_CTRL_MSG_SET_FRAGMENT_TO_CONFIGURATION = 0x02;
    static final int TEST_FLOW_CTRL_MSG_DISABLE_START_FLOW_BTN = 0x03;
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
                mTestFlowHandler.sendEmptyMessage(TEST_FLOW_CTRL_MSG_DISABLE_START_FLOW_BTN);
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mTestFlowHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case TEST_FLOW_CTRL_MSG_SET_FRAGMENT_TO_CONFIGURATION:
                    setFragmentToShow(FRAGMENT_INDEX_TEST_FLOW);
                    break;

                case TEST_FLOW_CTRL_MSG_DISABLE_START_FLOW_BTN:
                    mBtnStartTestFlow.setEnabled(false);
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
                        mCurrentTarget.runTest(mTargetConfigurationMap.get(mCurrentTarget));
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
    // TODO : HTTP UI - yunsik.lee

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
                mParentViewPager.setCurrentItem(1);
            });
            this.mParentViewPager = mParentActivity.getViewPager();
            this.mTestTargetMap = new HashMap<>();
            this.mTargetConfigurationMap = new HashMap<>();

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
            // TODO : yunsik.



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
            // TODO : add codes here - yunsik.lee

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
}