package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.MainActivity;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.ILGFTPOperationListener;
import com.android.LGSetupWizard.clients.LGFTPClient;
import com.android.LGSetupWizard.clients.LGTestFlowManager;
import com.android.LGSetupWizard.data.LGFTPFile;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;

import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-06-13.
 */
@Accessors(prefix = "m")
public class LGTestFlowConfigFragment extends Fragment {
    private static final String TAG = LGTestFlowConfigFragment.class.getSimpleName();

    // parent View
    private View mView;

    LGTestFlowManager mLGTestFlowManager;
    //ILGFTPOperationListener m

    private Button mBtnTest;

    private MainActivity mParentActivity;
    private FloatingActionButton mFabFetchFromFragment;
    private ViewPager mViewPager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGTestFlowConfigFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");

        this.mLGTestFlowManager = LGTestFlowManager.getInstance();
        this.mLGTestFlowManager.registerTestController(new LGFTPClient(new ILGFTPOperationListener() {
            @Override
            public void onConnectToServerFinished(boolean result, @Nullable ArrayList<LGFTPFile> fileNames) {

            }

            @Override
            public void onDisconnectToServerFinished() {

            }

            @Override
            public void onDownloadProgressPublished(float tputValue, long downloadedBytes) {

            }

            @Override
            public void onDownloadStarted(LGFTPFile fileName) {

            }

            @Override
            public void onDownloadFinished(boolean wasSuccessful, @NonNull File file, float avgTPut) {

            }

            @Override
            public void onChangeWorkingDirectoryFinished(ArrayList<LGFTPFile> fileList) {

            }
        }));
        //tmp.obtainMessage(LGTestFlowManager.);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPuase()");

        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        this.mParentActivity = ((MainActivity)(LGTestFlowConfigFragment.this.getActivity()));
        this.mFabFetchFromFragment = mParentActivity.getFabFetchInfo();
        this.mFabFetchFromFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ILGTestFlowFragment dd  = (ILGTestFlowFragment) mParentActivity.getFragmentPagerAdapter().getItem(mViewPager.getCurrentItem());
                dd.reportBackToTestFlowConfigurationFragment();
                mFabFetchFromFragment.setVisibility(View.INVISIBLE);
            }
        });

        mViewPager = mParentActivity.getViewPager();
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
        this.mBtnTest = this.mView.findViewById(R.id.btn_test);
        this.mBtnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "test ");

                // WONSIK
                Context context = LGTestFlowConfigFragment.this.getContext();

                int sNavigationBarHeight = 0;

                mFabFetchFromFragment.setVisibility(View.VISIBLE);
                Resources resources = context.getResources();
                int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    sNavigationBarHeight = resources.getDimensionPixelSize(resourceId);
                }
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mFabFetchFromFragment.getLayoutParams();
                params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, sNavigationBarHeight + 100);

                mViewPager.setCurrentItem(2);
            }
        });
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttache(Context)");
    }
}