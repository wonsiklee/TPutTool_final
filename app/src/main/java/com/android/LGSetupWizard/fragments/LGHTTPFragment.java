package com.android.LGSetupWizard.fragments;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGHTTPClient;
import com.android.LGSetupWizard.clients.LGHTTPDownloadStateChangeListener;
import com.android.LGSetupWizard.clients.LGOKHTTPClient;

/**
 * Created by yunsik.lee on 2018-05-03.
 */

public class LGHTTPFragment extends Fragment implements RadioButton.OnCheckedChangeListener {
    private static final String TAG = LGHTTPFragment.class.getSimpleName();

    private static final int START_TEST = 0x00;
    private static final int HTTP_DL_START = 0x01;
    private static final int HTTP_DL_FINISHED = 0x02;
    private static final int END_TEST = 0x03;

    // parent View
    private View mView;

    // UI Controls
    private EditText mEditTxtFileAddr;
    private RadioButton mRdoBtnOkHttp;
    private RadioButton mRdoBtnApache;
    private EditText mEditTxtRepeatCount;
    private Button mBtnStartDl;
    private TextView mTxtViewHTTPResult;
    private CheckBox mCheckBoxEnableFileIO;

    // listeners and HTTP Client
    private LGHTTPClient mLGHTTPClient;
    private int mRepeatCount;
    private int mRepeatInterval = 10000;
    private Handler mTargetHandler;

    private static DataPool DATA_POOL = new DataPool();

    public static class DataPool {
        public long totalSize;
        public long totalDuration;
        public float avgTPut;
    }

    private View.OnClickListener mStartTestClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "mStartTestClickListener.onClick()");

            try {
                String tmp = LGHTTPFragment.this.mEditTxtRepeatCount.getText().toString();
                LGHTTPFragment.this.mRepeatCount = Integer.valueOf(tmp);
            } catch (NumberFormatException e) {
                Log.d(TAG, "numberFormatException " + e + "\ntmp");
                Toast.makeText(LGHTTPFragment.this.getContext(), "숫자만 됩니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Repeat count : " + mRepeatCount);
            LGHTTPFragment.this.mTargetHandler.sendEmptyMessage(START_TEST);
        }
    };

    private View.OnClickListener mStopTestClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "StopTestClickListener.onClick()");
            LGHTTPFragment.this.mRepeatCount = 0;
            LGHTTPFragment.this.mTargetHandler.sendEmptyMessage(END_TEST);
        }
    };

    private Handler mGrepAvgTPutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mIsInProgress) {
                this.sendEmptyMessageDelayed(0, 2000);
                mLGHTTPClient.publishAvgTPut();
            }
        }
    };
    public boolean mIsInProgress = false;
    private Handler mHttpTestControlHandler = new Handler() {



        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_TEST:
                    Log.d(TAG, "START_TEST");
                    // switch listener
                    LGHTTPFragment.this.mBtnStartDl.setOnClickListener(LGHTTPFragment.this.mStopTestClickListener);
                    LGHTTPFragment.this.mBtnStartDl.setText(R.string.str_stop_nia_test);
                    LGHTTPFragment.this.mRdoBtnOkHttp.setClickable(false);
                    LGHTTPFragment.this.mRdoBtnApache.setClickable(false);
                    this.sendEmptyMessage(HTTP_DL_START);
                    mGrepAvgTPutHandler.sendEmptyMessageDelayed(0, 1000);
                    break;

                case HTTP_DL_START:
                    Log.d(TAG, "HTTP_DL_START - Remaining count : " + mRepeatCount);
                    mLGHTTPClient.startHTTPDownload();
                    mIsInProgress = true;
                    break;

                case HTTP_DL_FINISHED:
                    Log.d(TAG, "HTTP_DL_FINISHED");
                    mRepeatCount--;
                    Toast.makeText(LGHTTPFragment.this.getContext(), "TEST Finished : " + DATA_POOL.totalSize + " bytes received \nfor " + DATA_POOL.totalDuration + ",\nTput : " + DATA_POOL.avgTPut + " Mbps", Toast.LENGTH_LONG).show();
                    if (mRepeatCount > 0) {
                        this.sendEmptyMessageDelayed(HTTP_DL_START, LGHTTPFragment.this.mRepeatInterval);
                    } else {
                        this.sendEmptyMessage(END_TEST);
                    }
                    break;

                case END_TEST:
                    Log.d(TAG, "END_TEST");
                    mLGHTTPClient.stopDownload();
                    mIsInProgress = false;
                    this.removeMessages(START_TEST);
                    this.removeMessages(HTTP_DL_START);
                    mGrepAvgTPutHandler.removeMessages(0);
                    LGHTTPFragment.this.mBtnStartDl.setOnClickListener(LGHTTPFragment.this.mStartTestClickListener);
                    LGHTTPFragment.this.mBtnStartDl.setText(R.string.str_start_nia_test);
                    LGHTTPFragment.this.mRdoBtnOkHttp.setClickable(true);
                    LGHTTPFragment.this.mRdoBtnApache.setClickable(true);
                    break;

            }
        }
    };


    private LGHTTPDownloadStateChangeListener mHTTPDownloadStateChangeListener = new LGHTTPDownloadStateChangeListener() {
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
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("\n*********************************\n");
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("AvgTput : " + DATA_POOL.avgTPut + " Mbps");
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("\n***********************************\n");
                }
            });

            LGHTTPFragment.this.mTargetHandler.sendEmptyMessage(HTTP_DL_FINISHED);
        }

        @Override
        public void onTPutPublished(final float tput) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("\nAvgTput : " + tput + " Mbps");
                }
            });
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGHTTPFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPuase()");
        this.mLGHTTPClient.setOnStateChangedListener(null);
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        this.mTxtViewHTTPResult.setText("");
        this.mLGHTTPClient.setOnStateChangedListener(this.mHTTPDownloadStateChangeListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (this.mView == null) {
            this.mView = inflater.inflate(R.layout.fragment_http, container, false);
            this.initUIControls();

            this.mLGHTTPClient = new LGOKHTTPClient();
            this.mLGHTTPClient.setOnStateChangedListener(this.mHTTPDownloadStateChangeListener);
            this.mTargetHandler = this.mHttpTestControlHandler;
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
        this.mEditTxtFileAddr = (EditText) this.mView.findViewById(R.id.editTxt_file_addr);

        this.mRdoBtnOkHttp = (RadioButton) this.mView.findViewById(R.id.rdoBtn_http_stack_okhttp);
        this.mRdoBtnOkHttp.setOnCheckedChangeListener(this);
        this.mRdoBtnOkHttp.setChecked(true);

        this.mRdoBtnApache = (RadioButton) this.mView.findViewById(R.id.rdoBtn_http_stack_apache);
        this.mRdoBtnApache.setOnCheckedChangeListener(this);

        this.mEditTxtRepeatCount = (EditText) this.mView.findViewById(R.id.editTxt_repeat_count);

        this.mCheckBoxEnableFileIO = (CheckBox) this.mView.findViewById(R.id.checkBox_enable_file_io);

        this.mBtnStartDl = (Button) this.mView.findViewById(R.id.btn_start_http_dl_test);

        this.mTxtViewHTTPResult = (TextView) this.mView.findViewById(R.id.txtView_http_result);
        this.mTxtViewHTTPResult.setMovementMethod(new ScrollingMovementMethod());
        this.mTxtViewHTTPResult.setMaxLines(20);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
            switch (compoundButton.getId()) {
                case R.id.rdoBtn_http_stack_okhttp:
                    Log.d(TAG, "R.id.rdoBtn_http_stack_okhttp checked : " + isChecked);
                    this.mLGHTTPClient = new LGOKHTTPClient();
                    break;

                case R.id.rdoBtn_http_stack_apache:
                    Log.d(TAG, "R.id.rdoBtn_http_stack_apache checked : " + isChecked);
                    break;
            }
        }
    }
}