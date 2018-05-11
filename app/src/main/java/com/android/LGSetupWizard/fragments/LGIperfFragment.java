package com.android.LGSetupWizard.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGIperfClient;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

public class LGIperfFragment extends Fragment {
    private static final String TAG = LGIperfFragment.class.getSimpleName();
    private LGIperfClient mLGIperfClient;

    //UI component
    private View mView;
    private ToggleButton toggleBtn_iperf_start_n_stop;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_iperf, container, false);
            initUIControls();

            mLGIperfClient = new LGIperfClient(getContext());
            mLGIperfClient.loadIperfFile();

        }
        return mView;
    }

    private void initUIControls() {
        toggleBtn_iperf_start_n_stop = (ToggleButton) this.mView.findViewById(R.id.toggleBtn_iperf_start_n_stop);
        toggleBtn_iperf_start_n_stop.setOnClickListener(mToogleBtn_start_n_stop_listener);
    }

    private View.OnClickListener mToogleBtn_start_n_stop_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(toggleBtn_iperf_start_n_stop.isChecked()){
                mLGIperfClient.start();
            }else{
                mLGIperfClient.stop();
            }
        }
    };
}