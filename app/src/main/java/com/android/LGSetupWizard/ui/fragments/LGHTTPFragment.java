package com.android.LGSetupWizard.ui.fragments;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.clients.LGApacheHTTPClient;
import com.android.LGSetupWizard.clients.LGHTTPClient;
import com.android.LGSetupWizard.clients.LGHTTPDownloadStateChangeListener;
import com.android.LGSetupWizard.clients.LGOKHTTPClient;
import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.ui.popup.TestResultPopupWindow;

import lombok.experimental.Accessors;

/**
 * Created by yunsik.lee on 2018-05-03.
 */

@Accessors(prefix = "m")
public class LGHTTPFragment extends Fragment implements RadioButton.OnCheckedChangeListener {
    private static final String TAG = LGHTTPFragment.class.getSimpleName();

    private String testAddr = "http://192.168.1.2/1G";
    //private String testAddr = "http://tool.xcdn.gdms.lge.com/swdata/MOBILESYNC/GO/P5.3.23.20150119/LGPCSuite/Autorun/LGPCSuite_Setup.exe";
    //private String testAddr = null;

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
    private ImageButton mImageButtonShowResult;
    private EditText mEditTxtRepeatCount;
    private Button mBtnStartDl;
    private TextView mTxtViewHTTPResult;
    private TextView mTxtViewHTTPResultHistory;
    private CheckBox mCheckBoxEnableFileIO;
    private ProgressBar mProgressBarHttpProgress;
    private ImageButton mImageButtonClearAddr;

    // listeners and HTTP Client
    private LGHTTPClient mLGHTTPClient;
    private int mRepeatCount;
    private int mMaxCount;
    private int mRepeatInterval = 5000;
    private Handler mTargetHandler;
    private InputMethodManager mInputMethodManager;

    private static DataPool DATA_POOL = new DataPool();
    private boolean mEnableFileIO;

    private TestResultPopupWindow mTestResultPopupWindow;

    public static class DataPool {
        public long totalSize;
        public float totalDuration;
        public float avgTPut;
    }

    private View.OnClickListener mStartTestClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "mStartTestClickListener.onClick()");

            try {
                String tmp = LGHTTPFragment.this.mEditTxtRepeatCount.getText().toString();
                LGHTTPFragment.this.mMaxCount = Integer.valueOf(tmp);
                LGHTTPFragment.this.mRepeatCount = 0;
            } catch (NumberFormatException e) {
                Log.d(TAG, "numberFormatException " + e + "\ntmp");
                Toast.makeText(LGHTTPFragment.this.getContext(), "숫자만 됩니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Repeat count : " + mMaxCount);
            LGHTTPFragment.this.mTargetHandler.sendEmptyMessage(START_TEST);

            LGHTTPFragment.this.hideKeyboard();
        }
    };

    private View.OnClickListener mShowResultClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "mSaveResultClickListener.onClick()");
            TestResultDBManager.TestCategory sCategory;
            if (LGHTTPFragment.this.mLGHTTPClient instanceof LGOKHTTPClient) {
                if (LGHTTPFragment.this.mCheckBoxEnableFileIO.isChecked()) {
                    sCategory = TestResultDBManager.TestCategory.HTTP_OK_WITH_FILE_IO;
                } else {
                    sCategory = TestResultDBManager.TestCategory.HTTP_OK_WITHOUT_FILE_IO;
                }
            } else {
                if (LGHTTPFragment.this.mCheckBoxEnableFileIO.isChecked()) {
                    sCategory = TestResultDBManager.TestCategory.HTTP_APACHE_WITH_FILE_IO;
                } else {
                    sCategory = TestResultDBManager.TestCategory.HTTP_APACHE_WITHOUT_FILE_IO;
                }
            }
            mTestResultPopupWindow.show(LGHTTPFragment.this.getView(), sCategory);
        }
    };

    private View.OnClickListener mStopTestClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "mStopTestClickListener.onClick()");
            LGHTTPFragment.this.mRepeatCount = 0;
            LGHTTPFragment.this.mMaxCount = 0;
            LGHTTPFragment.this.mTargetHandler.sendEmptyMessage(END_TEST);
        }
    };

    private View.OnClickListener mClearAddrClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "mClearAddrClickListener.onClick()");
            LGHTTPFragment.this.mEditTxtFileAddr.setText("");
        }
    };

    private Handler mGrepAvgTPutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mIsInProgress) {
                mLGHTTPClient.publishCurrentTPut();
                this.sendEmptyMessageDelayed(0, 2000);
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
                    break;

                case HTTP_DL_START:
                    Log.d(TAG, "HTTP_DL_START - Remaining count : " + mRepeatCount);
                    String fileAddr = LGHTTPFragment.this.mEditTxtFileAddr.getText().toString();
                    LGHTTPFragment.this.mEnableFileIO = LGHTTPFragment.this.mCheckBoxEnableFileIO.isChecked();
                    mLGHTTPClient.startHTTPDownload(fileAddr, LGHTTPFragment.this.mEnableFileIO);
                    mIsInProgress = true;
                    mGrepAvgTPutHandler.sendEmptyMessageDelayed(0, 2000);
                    break;

                case HTTP_DL_FINISHED:
                    Log.d(TAG, "HTTP_DL_FINISHED");
                    mRepeatCount++;
                    mIsInProgress = false;
                    Toast.makeText(LGHTTPFragment.this.getContext(),
                            "TEST Finished : " + DATA_POOL.totalSize + " bytes received \nfor " +
                                    String.format("%.2f", DATA_POOL.totalDuration) + " sec,\nTput : " +
                                    String.format("%.2f", DATA_POOL.avgTPut) + " Mbps", Toast.LENGTH_SHORT).show();
                    if (mRepeatCount < mMaxCount) {
                        this.sendEmptyMessageDelayed(HTTP_DL_START, LGHTTPFragment.this.mRepeatInterval);
                    } else {
                        this.sendEmptyMessage(END_TEST);
                    }
                    break;

                case END_TEST:
                    Log.d(TAG, "END_TEST");
                    mLGHTTPClient.stopDownload();
                    this.removeMessages(START_TEST);
                    this.removeMessages(HTTP_DL_START);
                    mGrepAvgTPutHandler.removeMessages(0);
                    LGHTTPFragment.this.mBtnStartDl.setOnClickListener(LGHTTPFragment.this.mStartTestClickListener);
                    LGHTTPFragment.this.mBtnStartDl.setText(R.string.str_start_nia_test);
                    LGHTTPFragment.this.mRdoBtnOkHttp.setClickable(true);
                    LGHTTPFragment.this.mRdoBtnApache.setClickable(true);
                    LGHTTPFragment.this.mTxtViewHTTPResultHistory.append("\n");
                    break;

            }
        }
    };


    private LGHTTPDownloadStateChangeListener mHTTPDownloadStateChangeListener = new LGHTTPDownloadStateChangeListener() {
        @Override
        public void onDownloadStarted() {
            Log.d(TAG, "onDownloadStarted()");
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("OkHttp started, Test no." + (mRepeatCount + 1) + "\n");
                    LGHTTPFragment.this.mProgressBarHttpProgress.setProgress(0);
                }
            });
        }

        @Override
        public void onDownloadFinished(long totalSize, long totalDuration) {
            Log.d(TAG, "onDownloadFinished()");
            DATA_POOL.totalSize = totalSize;
            DATA_POOL.totalDuration = totalDuration;
            DATA_POOL.totalDuration /= 1000;
            DATA_POOL.avgTPut = (totalSize * 8.0f / 1024 / 1024 / (totalDuration / 1000.0f));

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("\n********************************\n");
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("Test no." + (mRepeatCount + 1) + ", AvgTput : " + String.format("%.2f", DATA_POOL.avgTPut) + " Mbps");
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("\n********************************\n\n");

                    LGHTTPFragment.this.mTxtViewHTTPResultHistory.append("#" + (mRepeatCount + 1) + ": " + String.format("%.2f", DATA_POOL.avgTPut) + " Mbps\n");
                }
            });

            LGHTTPFragment.this.mTargetHandler.sendEmptyMessage(HTTP_DL_FINISHED);
            LGHTTPFragment.this.mProgressBarHttpProgress.setProgress(100);
            TestResultDBManager.TestCategory sCategory;
            if (LGHTTPFragment.this.mLGHTTPClient instanceof LGOKHTTPClient) {
                if (LGHTTPFragment.this.mEnableFileIO) {
                    sCategory = TestResultDBManager.TestCategory.HTTP_OK_WITH_FILE_IO;
                } else {
                    sCategory = TestResultDBManager.TestCategory.HTTP_OK_WITHOUT_FILE_IO;
                }
            } else {
                if (LGHTTPFragment.this.mEnableFileIO) {
                    sCategory = TestResultDBManager.TestCategory.HTTP_APACHE_WITH_FILE_IO;
                } else {
                    sCategory = TestResultDBManager.TestCategory.HTTP_APACHE_WITHOUT_FILE_IO;
                }
            }
            TestResultDBManager.getInstance(LGHTTPFragment.this.getContext()).insert(sCategory, DATA_POOL.avgTPut, "Test no." + (mRepeatCount + 1));
            TestResultDBManager.getInstance(LGHTTPFragment.this.getContext()).debug_testQry_DB();
        }

        @Override
        public void onTPutPublished(final float tput, final int progress) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("CurrentTput : " + String.format("%.2f", tput) + " Mbps\n");
                    LGHTTPFragment.this.mProgressBarHttpProgress.setProgress(progress);
                }
            });
        }

        @Override
        public void onError(final String error) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LGHTTPFragment.this.mTxtViewHTTPResult.append("\nerror occured : " + error);
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
        this.mLGHTTPClient.setOnStateChangedListener(this.mHTTPDownloadStateChangeListener);
        this.mTestResultPopupWindow = new TestResultPopupWindow(this.getContext());
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

            mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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
        if (testAddr != null)
            this.mEditTxtFileAddr.setText(testAddr);

        this.mRdoBtnOkHttp = (RadioButton) this.mView.findViewById(R.id.rdoBtn_http_stack_okhttp);
        this.mRdoBtnOkHttp.setOnCheckedChangeListener(this);
        this.mRdoBtnOkHttp.setChecked(true);

        this.mRdoBtnApache = (RadioButton) this.mView.findViewById(R.id.rdoBtn_http_stack_apache);
        this.mRdoBtnApache.setOnCheckedChangeListener(this);

        this.mEditTxtRepeatCount = (EditText) this.mView.findViewById(R.id.editTxt_repeat_count);

        this.mCheckBoxEnableFileIO = (CheckBox) this.mView.findViewById(R.id.checkBox_enable_file_io);

        this.mBtnStartDl = (Button) this.mView.findViewById(R.id.btn_start_http_dl_test);
        this.mBtnStartDl.setOnClickListener(this.mStartTestClickListener);

        this.mImageButtonShowResult = (ImageButton) this.mView.findViewById(R.id.imgBtn_save_result);
        this.mImageButtonShowResult.setOnClickListener(this.mShowResultClickListener);

        this.mProgressBarHttpProgress = (ProgressBar) this.mView.findViewById(R.id.progressBar_http_progress);
        this.mProgressBarHttpProgress.setMax(100);

        this.mTxtViewHTTPResult = (TextView) this.mView.findViewById(R.id.txtView_http_result);
        this.mTxtViewHTTPResult.setMovementMethod(new ScrollingMovementMethod());
        this.mTxtViewHTTPResult.setMaxLines(20);

        this.mTxtViewHTTPResultHistory = (TextView) this.mView.findViewById(R.id.txtView_http_result_history);
        this.mTxtViewHTTPResultHistory.setMovementMethod(new ScrollingMovementMethod());
        this.mTxtViewHTTPResultHistory.setMaxLines(20);

        this.mImageButtonClearAddr = (ImageButton) this.mView.findViewById(R.id.imageButton_clear_addr);
        this.mImageButtonClearAddr.setOnClickListener(this.mClearAddrClickListener);
    }

    private void hideKeyboard()
    {
        mInputMethodManager.hideSoftInputFromWindow(mEditTxtFileAddr.getWindowToken(), 0);
        mInputMethodManager.hideSoftInputFromWindow(mEditTxtRepeatCount.getWindowToken(), 0);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
            switch (compoundButton.getId()) {
                case R.id.rdoBtn_http_stack_okhttp:
                    Log.d(TAG, "R.id.rdoBtn_http_stack_okhttp checked : " + isChecked);
                    this.mLGHTTPClient = new LGOKHTTPClient();
                    this.mLGHTTPClient.setOnStateChangedListener(this.mHTTPDownloadStateChangeListener);
                    break;

                case R.id.rdoBtn_http_stack_apache:
                    Log.d(TAG, "R.id.rdoBtn_http_stack_apache checked : " + isChecked);
                    this.mLGHTTPClient = new LGApacheHTTPClient();
                    this.mLGHTTPClient.setOnStateChangedListener(this.mHTTPDownloadStateChangeListener);
                    break;
            }
        }
    }
}