package com.android.LGSetupWizard.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGIperfClient;

import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

@Accessors(prefix = "m")
public class LGIperfFragment extends Fragment {
    private static final String TAG = LGIperfFragment.class.getSimpleName();
    private LGIperfClient mLGIperfClient;

    private Context mContext;
    //UI component
    private View mView;
    private ToggleButton toggleBtn_iperf_start_n_stop;
    private ToggleButton switch_iperf_version;
    private TextView tv_iperf_output;
    private EditText editText_iperf_option;
    private ScrollView scrollView_output;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_iperf, container, false);
            initUIControls();

            mLGIperfClient = new LGIperfClient(getContext());
            mLGIperfClient.setOnStateChangeListener(mLGIperfOnStateChangeListener);
            mLGIperfClient.loadIperfFile();
            mLGIperfClient.setIperfVersion(LGIperfClient.MODE_IPERF);

        }

        return mView;
    }



    private void initUIControls() {
        toggleBtn_iperf_start_n_stop = (ToggleButton) mView.findViewById(R.id.toggleBtn_iperf_start_n_stop);
        toggleBtn_iperf_start_n_stop.setOnClickListener(mToogleBtn_start_n_stop_listener);

        tv_iperf_output = (TextView) mView.findViewById(R.id.tv_iperf_output);
        editText_iperf_option = (EditText)mView.findViewById(R.id.editText_iperf_option);
        switch_iperf_version = (ToggleButton)mView.findViewById(R.id.switch_iperf_version);
        switch_iperf_version.setOnCheckedChangeListener(mSwitch_iperf_version_listener);

        scrollView_output = (ScrollView)mView.findViewById(R.id.scrollView_output);
    }

    private View.OnClickListener mToogleBtn_start_n_stop_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(toggleBtn_iperf_start_n_stop.isChecked()){
                mLGIperfClient.start(editText_iperf_option.getText().toString());

            }else{
                mLGIperfClient.stop();
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener mSwitch_iperf_version_listener = new CompoundButton.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                mLGIperfClient.setIperfVersion(LGIperfClient.MODE_IPERF3);
            }else{
                mLGIperfClient.setIperfVersion(LGIperfClient.MODE_IPERF);
            }
        }
    };

    private LGIperfClient.OnStateChangeListener mLGIperfOnStateChangeListener = new LGIperfClient.OnStateChangeListener(){

        @Override
        public void onGettingMeesage(String message) {
            if (tv_iperf_output!=null) tv_iperf_output.append(message);
        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onStopped() {
            toggleBtn_iperf_start_n_stop.setChecked(false);
        }
    };
    class ResultRunnable implements Runnable{
        String sResultMessage;
        ResultRunnable(String message){
            sResultMessage = message;
        }
        public void run() {
            tv_iperf_output.append(sResultMessage);
            //scrollView_output.fullScroll(130);
        }
    }

    class ResultAsyncTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            return objects;
        }

        @Override
        protected void onProgressUpdate(Object[] obj){
            String sResultMessage = (String) obj[0];
            tv_iperf_output.append(sResultMessage);
            scrollView_output.post(new ScrollDownRunnable());
        }
    }

    final class ScrollDownRunnable implements Runnable {

        public final void run() {
            scrollView_output.fullScroll(130);
        }
    }
}