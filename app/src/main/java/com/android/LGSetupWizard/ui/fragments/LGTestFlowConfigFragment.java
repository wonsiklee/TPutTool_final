package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.ILGFTPOperationListener;
import com.android.LGSetupWizard.clients.LGFTPClient;
import com.android.LGSetupWizard.clients.LGTestFlowManager;
import com.android.LGSetupWizard.data.LGFTPFile;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by wonsik.lee on 2017-06-13.
 */

public class LGTestFlowConfigFragment extends Fragment {
    private static final String TAG = LGTestFlowConfigFragment.class.getSimpleName();

    // parent View
    private View mView;

    LGTestFlowManager mLGTestFlowManager;
    //ILGFTPOperationListener m

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
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttache(Context)");
    }
}