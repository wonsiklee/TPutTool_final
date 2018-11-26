package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.MainActivity;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGTestFlowManager;

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
    //ILGFTPOperationListener m

    private Button mBtnStartTestFlow;

    private Button mBtnOpenFTPConf;
    private Button mBtnOpeniPerfConf;
    private Button mBtnOpenHttpConf;


    private MainActivity mParentActivity;
    private FloatingActionButton mFabFetchFromFragment;
    private ViewPager mViewPager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGTestFlowConfigFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");

        this.mContext = LGTestFlowConfigFragment.this.getContext();
        this.mLGTestFlowManager = LGTestFlowManager.getInstance();
        this.mParentActivity = ((MainActivity)(LGTestFlowConfigFragment.this.getActivity()));
        this.mFabFetchFromFragment = this.mParentActivity.getFabFetchInfo();
        this.mFabFetchFromFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object obj = ((ILGTestFlowFragment) mParentActivity.getFragmentPagerAdapter().getItem(mViewPager.getCurrentItem())).reportBackToTestFlowConfigurationFragment();
                parseIntoUI(obj);
                mFabFetchFromFragment.setVisibility(View.INVISIBLE);
                mViewPager.setCurrentItem(1);
            }
        });

        this.mViewPager = mParentActivity.getViewPager();
    }

    private void parseIntoUI(Object obj) {

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPuase()");

        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");

        super.onResume();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (this.mView == null) {
            this.mView = inflater.inflate(R.layout.fragment_test_flow_config, container, false);
        }
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        this.mBtnOpenFTPConf = this.mView.findViewById(R.id.btn_open_ftp_config);
        this.mBtnOpenFTPConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFetchFabBtn();
                setFragmentToShow(2);
            }
        });
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttache(Context)");
    }
}