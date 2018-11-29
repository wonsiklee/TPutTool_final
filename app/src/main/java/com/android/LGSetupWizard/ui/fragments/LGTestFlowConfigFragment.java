package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.MainActivity;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGTestFlowManager;
import com.android.LGSetupWizard.data.ILGTestFlowConfigurationInfo;
import com.android.LGSetupWizard.utils.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
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

import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-06-13.
 */
@Accessors(prefix = "m")
public class LGTestFlowConfigFragment extends Fragment {
    private static final String TAG = LGTestFlowConfigFragment.class.getSimpleName();

    // parent View
    private View mView;

    private LGTestFlowManager mLGTestFlowManager;
    private Context mContext;

    private Button mBtnStartTestFlow;


    private MainActivity mParentActivity;
    private FloatingActionButton mFabFetchFromFragment;
    private ViewPager mViewPager;

    private Button mBtnOpenFTPConf;
    private Button mBtnOpeniPerfConf;
    private Button mBtnOpenHttpConf;

    private TextView mTxtViewFTPUseFileIO;
    private TextView mTxtViewFTPTCPWMem;
    private TextView mTxtViewFTPPsv;
    private TextView mTxtViewFTPUseEPSV;
    private TextView mTxtViewFTPRepeatCount;
    private TextView mTxtViewFTPRepeatInterval;
    private TextView mTxtViewFTPFileCount;

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
        if (this.mView == null) {
            this.mView = inflater.inflate(R.layout.fragment_test_flow_config, container, false);
            this.mBtnStartTestFlow = this.mView.findViewById(R.id.btn_start_test_flow);
            this.mBtnStartTestFlow.setOnClickListener(v -> {
                Log.d(TAG, "start test flow.");
            });
            this.mBtnOpenFTPConf = this.mView.findViewById(R.id.btn_open_ftp_config);
            this.mBtnOpenFTPConf.setOnClickListener(v -> {
                showFetchFabBtn();
                setFragmentToShow(2);
            });

            this.mBtnOpeniPerfConf = this.mView.findViewById(R.id.btn_open_iperf_config);
            this.mBtnOpeniPerfConf.setOnClickListener(v -> {
                showFetchFabBtn();
                setFragmentToShow(3);
            });

            this.mBtnOpenHttpConf = this.mView.findViewById(R.id.btn_open_http_config);
            this.mBtnOpenHttpConf.setOnClickListener(v -> {
                showFetchFabBtn();
                setFragmentToShow(4);
            });

            this.mContext = LGTestFlowConfigFragment.this.getContext();
            this.mLGTestFlowManager = LGTestFlowManager.getInstance();
            this.mParentActivity = ((MainActivity)(LGTestFlowConfigFragment.this.getActivity()));
            this.mFabFetchFromFragment = this.mParentActivity.getFabFetchInfo();
            this.mFabFetchFromFragment.setOnClickListener(v -> {
                ILGTestFlowConfigurationInfo testConfigurationInfo = ((ILGTestFlowFragment) mParentActivity.getFragmentPagerAdapter().getItem(mViewPager.getCurrentItem())).getTestConfigurationInfo();
                processConfigurationInfo(testConfigurationInfo);
                mFabFetchFromFragment.setVisibility(View.INVISIBLE);
                mViewPager.setCurrentItem(1);
            });
            this.mViewPager = mParentActivity.getViewPager();

            this.mTxtViewFTPUseFileIO = this.mView.findViewById(R.id.txtView_config_ftp_use_file_IO_value);
            this.mTxtViewFTPTCPWMem = this.mView.findViewById(R.id.txtView_config_ftp_tcp_buffer_size_value);
            this.mTxtViewFTPPsv = this.mView.findViewById(R.id.txtView_config_ftp_use_PSV_value);
            this.mTxtViewFTPUseEPSV = this.mView.findViewById(R.id.txtView_config_ftp_use_IPv4_epsv_value);
            this.mTxtViewFTPRepeatCount = this.mView.findViewById(R.id.txtView_config_ftp_repeat_count_value);
            this.mTxtViewFTPRepeatInterval = this.mView.findViewById(R.id.txtView_config_ftp_repeat_interval_value);
            this.mTxtViewFTPFileCount = this.mView.findViewById(R.id.txtView_config_ftp_file_count_value);
        }
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
    }

    private void setFragmentToShow(int position) {
        this.mViewPager.setCurrentItem(position);
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

    private void processConfigurationInfo(ILGTestFlowConfigurationInfo info) {
        if (info instanceof LGFTPFragment.LGFTPTestFlowConfigurationInfo) {
            Log.d(TAG, "LGFTPTestFlowConfigurationInfo returned");
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
    }
}