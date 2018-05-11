package com.android.LGSetupWizard.fragments;

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

public class LGNIAFragment extends Fragment implements RadioButton.OnCheckedChangeListener {
    private static final String TAG = LGNIAFragment.class.getSimpleName();

    private static final int START_TEST = 0x00;
    private static final int NIA_DL_START = 0x01;
    private static final int NIA_DL_FINISHED = 0x02;
    private static final int END_TEST = 0x03;

    // parent View
    private View mView;

    // UI Controls
    private RadioButton mRdoBtnRepeatByCount;
    private RadioButton mRdoBtnRepeatByDuration;
    private LinearLayout mLinearLayoutTestTypeRepeatByCount;
    private LinearLayout mLinearLayoutTestTypeRepeatByDuration;
    private EditText mEditTxtRepeatCount;
    private Spinner mSpinnerTestDuration;
    private Button mBtnStartDl;
    private TextView mTxtViewNIAResult;
    private ScrollView mScrollView;

    // listeners and NIA Client
    private LGNIAClient mLGNIAClient;
    private int mRepeatCount;
    private int mRepeatInterval = 10000;
    private Handler mTargetHandler;

    private static DataPool DATA_POOL = new DataPool();

    public static class DataPool {
        public long totalSize;
        public long totalDuration;
        public float avgTPut;
    }

    private View.OnClickListener mRepeatByCountClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "RepeatByCountStartClickListener.onClick()");

            try {
                String tmp = LGNIAFragment.this.mEditTxtRepeatCount.getText().toString();
                LGNIAFragment.this.mRepeatCount = Integer.valueOf(tmp);
            } catch (NumberFormatException e) {
                Log.d(TAG, "numberFormatException " + e + "\ntmp");
                Toast.makeText(LGNIAFragment.this.getContext(), "숫자만 됩니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Repeat count : " + mRepeatCount);
            LGNIAFragment.this.mTargetHandler.sendEmptyMessage(START_TEST);
        }
    };

    private View.OnClickListener mRepeatByDurationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "RepeatByDurationClickListener.onClick()");
        }
    };

    private View.OnClickListener mStopTestClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "StopTestClickListener.onClick()");
            LGNIAFragment.this.mRepeatCount = 0;
            LGNIAFragment.this.mTargetHandler.sendEmptyMessage(END_TEST);
        }
    };

    private Handler mGrepAvgTPutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mIsInProgress) {
                this.sendEmptyMessageDelayed(0, 2000);
                mLGNIAClient.publishAvgTPut();
            }
        }
    };
    public boolean mIsInProgress = false;
    private Handler mRepeatByCountTestControlHandler = new Handler() {



        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_TEST:
                    Log.d(TAG, "START_TEST");
                    // switch listener
                    LGNIAFragment.this.mBtnStartDl.setOnClickListener(LGNIAFragment.this.mStopTestClickListener);
                    LGNIAFragment.this.mBtnStartDl.setText(R.string.str_stop_nia_test);
                    this.sendEmptyMessage(NIA_DL_START);
                    mGrepAvgTPutHandler.sendEmptyMessageDelayed(0, 1000);
                    break;

                case NIA_DL_START:
                    Log.d(TAG, "NIA_DL_START - Remaining count : " + mRepeatCount);
                    mLGNIAClient.startNIADownload();
                    mIsInProgress = true;
                    break;

                case NIA_DL_FINISHED:
                    Log.d(TAG, "NIA_DL_FINISHED");
                    mRepeatCount--;
                    Toast.makeText(LGNIAFragment.this.getContext(), "TEST Finished : " + DATA_POOL.totalSize + " bytes received \nfor " + DATA_POOL.totalDuration + ",\nTput : " + DATA_POOL.avgTPut + " Mbps", Toast.LENGTH_LONG).show();
                    if (mRepeatCount > 0) {
                        this.sendEmptyMessageDelayed(NIA_DL_START, LGNIAFragment.this.mRepeatInterval);
                    } else {
                        this.sendEmptyMessage(END_TEST);
                    }
                    break;

                case END_TEST:
                    Log.d(TAG, "END_TEST");
                    mLGNIAClient.stopDownload();
                    mIsInProgress = false;
                    this.removeMessages(START_TEST);
                    this.removeMessages(NIA_DL_START);
                    mGrepAvgTPutHandler.removeMessages(0);
                    LGNIAFragment.this.mBtnStartDl.setOnClickListener(LGNIAFragment.this.mRepeatByCountClickListener);
                    LGNIAFragment.this.mBtnStartDl.setText(R.string.str_start_nia_test);
                    break;

            }
        }
    };

    private Handler mRepeatByDurationTestControlHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO : need to Implement
        }
    };


    private ILGNIADownloadStateChangeListener mNIADownloadStateChangeListener = new ILGNIADownloadStateChangeListener() {
        @Override
        public void onDownloadStarted() {
            Log.d(TAG, "onDownloadStarted()");
        }

        @Override
        public void onDownloadFinished(long totalSize, long totalDuration) {
            Log.d(TAG, "onDownloadFinished()");
            DATA_POOL.totalSize = totalSize;
            DATA_POOL.totalDuration = totalDuration;
            DATA_POOL.avgTPut = (totalSize * 8.0f / 1024 / 1024 / (totalDuration / 1000.0f));

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LGNIAFragment.this.mTxtViewNIAResult.append("\n*********************************\n");
                    LGNIAFragment.this.mTxtViewNIAResult.append("AvgTput : " + DATA_POOL.avgTPut + " Mbps");
                    LGNIAFragment.this.mTxtViewNIAResult.append("\n***********************************\n");
                }
            });

            LGNIAFragment.this.mTargetHandler.sendEmptyMessage(NIA_DL_FINISHED);
        }

        @Override
        public void onTPutPublished(final float tput) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LGNIAFragment.this.mTxtViewNIAResult.append("\nAvgTput : " + tput + " Mbps");
                }
            });
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGNIAFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPuase()");
        this.mLGNIAClient.setOnStateChangedListener(null);
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        this.mTxtViewNIAResult.setText("");
        this.mLGNIAClient.setOnStateChangedListener(this.mNIADownloadStateChangeListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (this.mView == null) {
            this.mView = inflater.inflate(R.layout.fragment_nia, container, false);
            this.initUIControls();

            this.mLGNIAClient = new LGNIAClient();
            this.mLGNIAClient.setOnStateChangedListener(this.mNIADownloadStateChangeListener);
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

    private void initUIControls() {
        this.mRdoBtnRepeatByCount = (RadioButton) this.mView.findViewById(R.id.rdoBtn_test_type_repeat_by_count);
        this.mRdoBtnRepeatByCount.setOnCheckedChangeListener(this);

        this.mLinearLayoutTestTypeRepeatByCount = (LinearLayout) this.mView.findViewById(R.id.ll_test_type_repeat_by_count);
        this.mLinearLayoutTestTypeRepeatByDuration = (LinearLayout) this.mView.findViewById(R.id.ll_test_type_repeat_by_duration);

        this.mBtnStartDl = (Button) this.mView.findViewById(R.id.btn_start_nia_dl_test);

        this.mRdoBtnRepeatByDuration = (RadioButton) this.mView.findViewById(R.id.rdoBtn_test_type_repeat_by_duration);
        this.mRdoBtnRepeatByDuration.setOnCheckedChangeListener(this);
        this.mRdoBtnRepeatByCount.setChecked(true);

        this.mEditTxtRepeatCount = (EditText) this.mView.findViewById(R.id.editTxt_repeat_count);
        this.mSpinnerTestDuration = (Spinner) this.mView.findViewById(R.id.spinner_test_duration_time);
        List<String> spinnerData = new ArrayList<>();
        for (int i = 10; i <= 600; i = i + 10) {
            spinnerData.add(i + "");
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this.getContext(), android.R.layout.simple_spinner_item, spinnerData);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mSpinnerTestDuration.setAdapter(dataAdapter);

        this.mTxtViewNIAResult = (TextView) this.mView.findViewById(R.id.txtView_nia_result);
        this.mTxtViewNIAResult.setMovementMethod(new ScrollingMovementMethod());
        this.mTxtViewNIAResult.setMaxLines(20);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
            switch (compoundButton.getId()) {
                case R.id.rdoBtn_test_type_repeat_by_count:
                    Log.d(TAG, "R.id.rdoBtn_test_type_repeat_by_count checked : " + isChecked);
                    this.mLinearLayoutTestTypeRepeatByDuration.setVisibility(View.GONE);
                    this.mLinearLayoutTestTypeRepeatByCount.setVisibility(View.VISIBLE);
                    this.mBtnStartDl.setOnClickListener(LGNIAFragment.this.mRepeatByCountClickListener);
                    this.mTargetHandler = this.mRepeatByCountTestControlHandler;
                    break;

                case R.id.rdoBtn_test_type_repeat_by_duration:
                    Log.d(TAG, "R.id.rdoBtn_test_type_repeat_by_duration checked : " + isChecked);
                    this.mLinearLayoutTestTypeRepeatByDuration.setVisibility(View.VISIBLE);
                    this.mLinearLayoutTestTypeRepeatByCount.setVisibility(View.GONE);
                    this.mBtnStartDl.setOnClickListener(LGNIAFragment.this.mRepeatByDurationClickListener);
                    this.mTargetHandler = this.mRepeatByDurationTestControlHandler;
                    break;
            }
        }
    }
}