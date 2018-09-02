package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGNIAClient;
import com.android.LGSetupWizard.clients.ILGNIADownloadStateChangeListener;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wonsik.lee on 2017-06-13.
 */

public class TestFlowConfigFragment extends Fragment {
    private static final String TAG = TestFlowConfigFragment.class.getSimpleName();

    private Handler mFullTestControlHandler;

    private static final int START_TEST = 0x00;
    private static final int NIA_DL_START = 0x01;
    private static final int NIA_DL_FINISHED = 0x02;
    private static final int END_TEST = 0x03;

    // parent View
    private View mView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "TestFlowConfigFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
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