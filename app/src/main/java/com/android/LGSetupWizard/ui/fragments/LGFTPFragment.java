package com.android.LGSetupWizard.ui.fragments;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.LGSetupWizard.clients.LGFTPOperationListener;
import com.android.LGSetupWizard.data.ILGTestFlowConfigurationInfo;
import com.android.LGSetupWizard.data.MediaScanning;
import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.data.LGFTPFile;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.adapters.LGFTPFileListViewAdapter;
import com.android.LGSetupWizard.clients.LGFTPClient;
import com.android.LGSetupWizard.clients.ILGFTPOperationListener;
import com.android.LGSetupWizard.ui.dialog.LGFTPFileDownloadProgressDialog;
import com.android.LGSetupWizard.ui.popup.CounterSettingPopupWindow;
import com.android.LGSetupWizard.ui.popup.TestResultPopupWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-06-13.
 */
@Accessors(prefix = "m")
public class LGFTPFragment extends Fragment implements View.OnKeyListener, AdapterView.OnItemClickListener, Dialog.OnDismissListener, View.OnFocusChangeListener, ILGTestFlowFragment {
    private static boolean DEBUG = false;
    private static final String TAG = LGFTPFragment.class.getSimpleName();

    private View mView;

    // Logged out view
    private LinearLayout mLinearLayoutLoggedOutViewGroup;
    private EditText mEditTextServerAddress;
    private EditText mEditTextPortNum;
    private EditText mEditTextUserID;
    private EditText mEditTextPassword;

    // Logged in view
    private LinearLayout mLinearLayoutLoggedInViewGroup;
    private CheckBox mCheckBoxUseFileIO;
    private RadioGroup mRadioGroupMethodType;
    private RadioButton mRadioButtonConventionalMethod;
    private RadioButton mRadioButtonApacheMethod;
    private RadioButton mRadioButtonFileChannelMethod;
    private EditText mEditTextRepeatCount;
    private EditText mEditTextTestIntervalInSec;
    private Button mBtnDLULStartStop;

    private int mCheckedMethod;

    // always shown views
    private ImageButton mImgBtnTestHistory;
    private CheckBox mCheckBoxUsePassiveMode;
    private CheckBox mCheckBoxUseEPSVforIPv4;
    private Button mBtnConnectDisconnect;
    private ListView mFTPFileListView;

    private LGFTPFileDownloadProgressDialog mLGFTPFileDownloadProgressDialog;

    private ProgressDialog mNetworkOperationProgressDialog;
    private TestResultPopupWindow mTestResultPopupWindow;

    private LGFTPFileListViewAdapter mFTPFileListVIewAdapter;
    private LGFTPClient mLGFtpClient;
    private ArrayList<LGFTPFile> mFileList;

    private String mDownloadingFileName;
    private long mDownloadingFileSize;
    private boolean mDownloadResult;

    private int mInitialFileCount;

    private String mMccMnc;

    final static private String KEY_DOWNLOAD_FILE_NAME = "file_name";
    final static private String KEY_DOWNLOAD_RESULT = "file_size";
    final static private String KEY_AVG_TPUT = "avg_tput";
    final static private String KEY_LOGIN_RESULT = "login_result";
    private EditText mEditTextBufferSize;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGFTPFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
        this.mInitialFileCount = Integer.MIN_VALUE;
        this.mMccMnc = ((TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperator();
        Log.d(TAG, "MccMnc = " + mMccMnc);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideSoftKeyboard();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        if (this.mView == null) {
            this.mView = inflater.inflate(R.layout.fragment_ftp, container, false);
            this.mView.setFocusableInTouchMode(true);
            this.mView.requestFocus();
            this.mView.setOnKeyListener(LGFTPFragment.this);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        new Thread() {
            @Override
            public void run() {
                if (LGFTPFragment.this.mLGFtpClient != null && LGFTPFragment.this.mLGFtpClient.isAvailable()) {
                    LGFTPFragment.this.mLGFtpClient.disconnectFromServer();
                }
            }
        }.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();

        if (this.mLGFtpClient == null) {
            this.mLGFtpClient = new LGFTPClient(this.mILGFTPOperationListener);
        }

        this.mNetworkOperationProgressDialog = new ProgressDialog(this.getContext());
        this.mLGFTPFileDownloadProgressDialog = new LGFTPFileDownloadProgressDialog(this.getContext());
        this.mLGFTPFileDownloadProgressDialog.setOnDismissListener(this);
        this.mTestResultPopupWindow = new TestResultPopupWindow(this.getContext());

        this.initLoggedInViews();
        this.initLoggedOutViews();
        this.initAlwaysShownViews();

        this.mUIControlHandler.sendEmptyMessage(MSG_REFRESH_ALL_UI);
        Log.d(TAG, "onResume() completed");
    }

    private void initAlwaysShownViews() {
        this.mImgBtnTestHistory = (this.mView.findViewById(R.id.imgBtn_history));
        this.mImgBtnTestHistory.setOnClickListener(this.mClickListenerShowHistory);

        this.mCheckBoxUsePassiveMode = this.mView.findViewById(R.id.checkbox_set_passive);
        this.mCheckBoxUsePassiveMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideSoftKeyboard();
                Log.d(TAG, "calling setPassiveMode " + isChecked);
                if (mLGFtpClient.isConnected()) {
                    LGFTPFragment.this.mLGFtpClient.setPassiveMode(isChecked);
                }
            }
        });

        this.mCheckBoxUseEPSVforIPv4 = this.mView.findViewById(R.id.checkbox_set_epsv_for_ipv4);
        this.mCheckBoxUseEPSVforIPv4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            boolean mIsPassivePreviouslyChecked = mCheckBoxUsePassiveMode.isChecked();
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideSoftKeyboard();
                boolean tmp = mCheckBoxUsePassiveMode.isChecked();
                if (isChecked) {
                    mCheckBoxUsePassiveMode.setEnabled(false);
                    mCheckBoxUsePassiveMode.setChecked(true);
                } else {
                    mCheckBoxUsePassiveMode.setEnabled(true);
                    mCheckBoxUsePassiveMode.setChecked(mIsPassivePreviouslyChecked);
                }

                Log.d(TAG, "calling setEPSVforIPv4 " + isChecked);
                if (mLGFtpClient.isConnected()) {
                    LGFTPFragment.this.mLGFtpClient.setEPSVforIPv4(isChecked);
                }

                mIsPassivePreviouslyChecked = tmp;
            }
        });



        this.mBtnConnectDisconnect = (Button) this.mView.findViewById(R.id.btn_connect_disconnect);
        if (this.mLGFtpClient != null && this.mLGFtpClient.isConnected()) {
            this.mBtnConnectDisconnect.setOnClickListener(this.mClickListenerDisconnect);
        } else {
            this.mBtnConnectDisconnect.setOnClickListener(this.mClickListenerConnect);
        }

        if (!this.isNetworkAvailable()) {
            this.mBtnConnectDisconnect.setEnabled(false);
        }

        this.mFTPFileListView = this.mView.findViewById(R.id.listView_ftpFileList);
        this.mFTPFileListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        this.mFTPFileListView.setOnItemClickListener(this);

        if (this.mFTPFileListVIewAdapter == null) {
            this.mFTPFileListVIewAdapter = new LGFTPFileListViewAdapter(this.getContext());
            this.mFTPFileListView.setAdapter(this.mFTPFileListVIewAdapter);
        }
    }

    private static String PREF_KEY_SERVER_CONFIG = "server_config";
    private static String PREF_KEY_SERVER_ADDRESS = "server_address";
    private static String PREF_KEY_PORT_NUM = "port_num";
    private static String PREF_KEY_ID = "ftp_id";
    private static String PREF_KEY_PASSWORD = "ftp_password";

    @SuppressLint("SetTextI18n")
    private void initLoggedOutViews() {
        this.mLinearLayoutLoggedOutViewGroup = this.mView.findViewById(R.id.ll_logged_out_view_group);
        this.mLinearLayoutLoggedInViewGroup.setMinimumHeight(mLinearLayoutLoggedOutViewGroup.getHeight());

        this.mEditTextServerAddress = this.mView.findViewById(R.id.editText_server_addr);
        this.mEditTextPortNum = this.mView.findViewById(R.id.editText_port_num);
        this.mEditTextUserID = this.mView.findViewById(R.id.editText_user_id);
        this.mEditTextPassword = this.mView.findViewById(R.id.editText_password);

        SharedPreferences pref = this.getActivity().getPreferences(Context.MODE_PRIVATE);

        if (pref.contains("server_config")) {
            this.mEditTextServerAddress.setText(pref.getString(PREF_KEY_SERVER_ADDRESS, "192.168.1.2"));
            this.mEditTextPortNum.setText(pref.getString(PREF_KEY_PORT_NUM, "21"));
            this.mEditTextUserID.setText(pref.getString(PREF_KEY_ID, "user"));
            this.mEditTextPassword.setText(pref.getString(PREF_KEY_PASSWORD, "@lge1234"));
        } else {
            if (this.mMccMnc.equals("45005")) {
                this.mEditTextServerAddress.setText("sdftp.nate.com");
                this.mEditTextPortNum.setText("21");
                this.mEditTextUserID.setText("suser");
                this.mEditTextPassword.setText("s!sdqns2121");
            } else if (this.mMccMnc.contains("450")) {
                this.mEditTextServerAddress.setText("203.229.247.254");
                this.mEditTextPortNum.setText("21");
                this.mEditTextUserID.setText("testbed01");
                this.mEditTextPassword.setText("gprs@tbed!01");
            } else {
                this.mEditTextServerAddress.setText("192.168.1.2");
                this.mEditTextPortNum.setText("21");
                this.mEditTextUserID.setText("user");
                this.mEditTextPassword.setText("@lge1234");
            }
        }
    }

    private InputFilter mNumberInputFilter = new InputFilter() {

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());

                if (source.length() == 0) { // in case deletion
                    if (dest.length() == 1) {
                        return "1";
                    } else {
                        return null;
                    }
                }
                if (isInRange(1, 100, input)) {
                    Log.d(TAG, "still in range");
                    return source;
                }
            } catch (NumberFormatException nfe) {

            }
            return "";
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private void initLoggedInViews() {
        // Logged in views init start.
        this.mLinearLayoutLoggedInViewGroup = this.mView.findViewById(R.id.ll_logged_in_view_group);
        this.mLinearLayoutLoggedInViewGroup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "onTouch() " + event.getAction());
                    hideSoftKeyboard();
                }
                return true;
            }
        });

        this.mCheckBoxUseFileIO = this.mView.findViewById(R.id.checkbox_use_file_IO);
        this.mCheckBoxUseFileIO.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    LGFTPFragment.this.mRadioButtonConventionalMethod.setEnabled(true);
                    LGFTPFragment.this.mRadioButtonFileChannelMethod.setEnabled(true);
                } else {
                    LGFTPFragment.this.mRadioButtonConventionalMethod.setEnabled(false);
                    LGFTPFragment.this.mRadioButtonFileChannelMethod.setEnabled(false);
                }
            }
        });
        this.mCheckBoxUseFileIO.setOnFocusChangeListener(this);

        this.mRadioGroupMethodType = this.mView.findViewById(R.id.radioGroup_method_type);
        this.mRadioGroupMethodType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                hideSoftKeyboard();
                Log.d(TAG , "mRadioGroupMethodType onCheckedChanged()");
                if (checkedId == mRadioButtonConventionalMethod.getId()) {
                    mCheckedMethod = LGFTPClient.METHOD_TYPE_CONVENTIONAL;
                } else if (checkedId == mRadioButtonApacheMethod.getId()) {
                    mCheckedMethod = LGFTPClient.METHOD_TYPE_APACHE;
                } else if (checkedId == mRadioButtonFileChannelMethod.getId()) {
                    mCheckedMethod = LGFTPClient.METHOD_TYPE_FILE_CHANNEL;
                }
            }
        });

        this.mRadioButtonApacheMethod = this.mView.findViewById(R.id.radioButton_method_type_apache);
        this.mRadioButtonApacheMethod.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mEditTextRepeatCount.clearFocus();
                mEditTextTestIntervalInSec.clearFocus();
                hideSoftKeyboard();
                return false;
            }
        });
        this.mRadioButtonFileChannelMethod = this.mView.findViewById(R.id.radioButton_method_type_file_channel);
        this.mRadioButtonApacheMethod.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mEditTextRepeatCount.clearFocus();
                mEditTextTestIntervalInSec.clearFocus();
                hideSoftKeyboard();
                return false;
            }
        });

        this.mRadioButtonConventionalMethod = this.mView.findViewById(R.id.radioButton_method_type_conventional);
        this.mRadioButtonConventionalMethod.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mEditTextRepeatCount.clearFocus();
                mEditTextTestIntervalInSec.clearFocus();
                hideSoftKeyboard();
                return false;
            }
        });

        this.mEditTextBufferSize = this.mView.findViewById(R.id.editText_buf_size);
        this.mEditTextBufferSize.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouch " + event.getAction());
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    CounterSettingPopupWindow c = new CounterSettingPopupWindow(LGFTPFragment.this.getContext(), LGFTPFragment.this.mEditTextBufferSize);
                    c.show((int) LGFTPFragment.this.mEditTextBufferSize.getX(), (int) LGFTPFragment.this.mEditTextBufferSize.getY() + 200);
                }
                return false;
            }
        });


        this.mEditTextRepeatCount = this.mView.findViewById(R.id.editTxt_ftp_download_repeat_count);
        this.mEditTextRepeatCount.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouch " + event.getAction());
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    CounterSettingPopupWindow c = new CounterSettingPopupWindow(LGFTPFragment.this.getContext(), LGFTPFragment.this.mEditTextRepeatCount);
                    c.show((int) LGFTPFragment.this.mEditTextRepeatCount.getX(), (int) LGFTPFragment.this.mEditTextRepeatCount.getY() + mLinearLayoutLoggedInViewGroup.getHeight());
                }
                return false;
            }
        });
        this.mEditTextRepeatCount.setOnFocusChangeListener(this);
        this.mEditTextRepeatCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "afterTextChanged " + s);
                if (s.toString().equals("1")) {
                    mEditTextTestIntervalInSec.setEnabled(false);
                } else {
                    mEditTextTestIntervalInSec.setEnabled(true);
                }
            }
        });

        this.mEditTextTestIntervalInSec = this.mView.findViewById(R.id.editTxt_ftp_download_repeat_interval);
        this.mEditTextTestIntervalInSec.setOnFocusChangeListener(this);
        this.mEditTextTestIntervalInSec.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouch " + event.getAction());
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    CounterSettingPopupWindow c = new CounterSettingPopupWindow(LGFTPFragment.this.getContext(), LGFTPFragment.this.mEditTextTestIntervalInSec);
                    c.show((int) LGFTPFragment.this.mEditTextTestIntervalInSec.getX(), (int) LGFTPFragment.this.mEditTextTestIntervalInSec.getY() + mLinearLayoutLoggedInViewGroup.getHeight());
                }
                return false;
            }
        });

        this.mBtnDLULStartStop = this.mView.findViewById(R.id.btn_ftp_download);
        this.mBtnDLULStartStop.setOnClickListener(this.mClickListenerStart);
    }

    private void hideSoftKeyboard() {
        Context sContext = this.getContext();
        if (sContext != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) sContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        mEditTextRepeatCount.clearFocus();
        mEditTextTestIntervalInSec.clearFocus();
    }

    /* file download progress dialog show/hide [START] */
    private void showFileDownloadProgressBar() {
        this.mLGFTPFileDownloadProgressDialog.updateProgressValue(0, 0, 0);
        this.mLGFTPFileDownloadProgressDialog.show();
    }

    private void dismissFileDownloadProgressBar() {
        if (this.mLGFTPFileDownloadProgressDialog.isShowing()) {
            this.mLGFTPFileDownloadProgressDialog.dismiss();
        }
    }
    /* file download progress dialog show/hide [END] */

    /* network operation progress bar show/hide [START] */
    private void showNetworkOperationProgressBar(String title, String dialogMessage) {
        this.mNetworkOperationProgressDialog.setTitle(title);
        this.mNetworkOperationProgressDialog.setMessage(dialogMessage);
        this.mNetworkOperationProgressDialog.show();
    }

    private void dismissNetworkOperationProgressBar() {
        if (this.mNetworkOperationProgressDialog.isShowing()) {
            this.mNetworkOperationProgressDialog.dismiss();
        }
    }

    private void setLoggedInLayout() {
        Log.d(TAG, "setLoggedInLayout() ");

        this.mLinearLayoutLoggedOutViewGroup.animate()
                .alpha(0.0f)
                .setDuration(600)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLinearLayoutLoggedOutViewGroup.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) { }

                    @Override
                    public void onAnimationRepeat(Animator animation) { }
                });
        this.mLinearLayoutLoggedInViewGroup.setAlpha(0.0f);
        this.mLinearLayoutLoggedInViewGroup.animate()
                .alpha(1.0f)
                .setDuration(600)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mLinearLayoutLoggedInViewGroup.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
    }

    private void setLoggedOutLayout() {
        Log.d(TAG, "setLoggedOutLayout() ");
        this.mLinearLayoutLoggedOutViewGroup.animate()
                .alpha(1.0f)
                .setDuration(600)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mLinearLayoutLoggedOutViewGroup.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) { }

                    @Override
                    public void onAnimationRepeat(Animator animation) { }
                });

        this.mLinearLayoutLoggedInViewGroup.animate()
                .alpha(0.0f)
                .setDuration(600)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLinearLayoutLoggedInViewGroup.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void changeWorkingDirectory(String title, String dialogMessage, final String path) {
        // 1. disable all touchable UIs and show progress bar
        showNetworkOperationProgressBar(title, dialogMessage);

        // 2. clear the selected file list
        LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();

        // 3. call change working directory method in a new thread and receive the new file list.
        new Thread() {
            @Override
            public void run() {
                super.run();

                LGFTPFragment.this.mLGFtpClient.changeWorkingDirectory(path);
                Log.d(TAG, "Working directory has changed to " + path);
            }
        }.start();

        // 4. when the directory change is completed, set a new set of file list to the adapter and notify
        // this step will be done via callback called from LGFTPClient, but still leaving this message just for futuer me,
        // who won't be able to remember later.
    }

    private void debug_printCurrentFileList() {
        Log.d(TAG, "************* debug_printCurrentFileList() STARTS *************");
        Log.d(TAG, "mFileList length : " + this.mFileList.size());
        if (this.mFileList != null) {
            for (LGFTPFile ftpFile : mFileList) {
                Log.d(TAG, (ftpFile.isDirectory() ? "Directory : " : "File : ") + ftpFile);
            }
        } else {
            Log.d(TAG, "no files to print.");
        }
        Log.d(TAG, "************* debug_printCurrentFileList() ENDS ***************");
    }

    // ILGFTPOperationListener mILGFTPOperationListener = new LGFTPOperationListener(this);
    ILGFTPOperationListener mILGFTPOperationListener = new ILGFTPOperationListener() {

        @Override
        public void onConnectToServerFinished(boolean result, ArrayList<LGFTPFile> fileList) {
            Log.d(LGFTPFragment.TAG, "onConnectToServerFinished(boolean, ArrayList<FTPFile>) : " + result);
            Message msg = mUIControlHandler.obtainMessage(LGFTPFragment.MSG_CONNECT_TO_SERVER_FINISHED);
            Bundle b = new Bundle();
            b.putBoolean(LGFTPFragment.KEY_LOGIN_RESULT, result);
            msg.setData(b);
            if (result) {
                if (fileList == null) {
                    Log.d(LGFTPFragment.TAG, "no File list retrieved.");
                } else {
                    mFileList = fileList;
                    Log.d(LGFTPFragment.TAG, "fileList length : " + mFileList.size());
                }
            }
            mUIControlHandler.sendMessage(msg);
        }

        @Override
        public void onChangeWorkingDirectoryFinished(ArrayList<LGFTPFile> fileList) {
            Log.d(LGFTPFragment.TAG, "onChangeWorkingDirectoryFinished()");
            mFileList = fileList;
            mUIControlHandler.sendEmptyMessage(LGFTPFragment.MSG_CHANGE_WORKING_DIRECTORY_FINISHED);
        }

        @Override
        public void onDisconnectToServerFinished() {
            Log.d(LGFTPFragment.TAG, "onDisconnectToServerFinished()");
            mUIControlHandler.sendEmptyMessage(LGFTPFragment.MSG_DISCONNECT_FROM_SERVER_FINISHED);
        }

        @Override
        public void onDownloadProgressPublished(float tputValue, long downloadedBytes) {
            //Log.d(TAG, "onDownloadProgressPublished(float tputValue, long downloadedBytes) : " + tputValue + ", " + downloadedBytes + " bytes");
            mLGFTPFileDownloadProgressDialog.updateProgressValue(((float) downloadedBytes / mDownloadingFileSize) * 100, downloadedBytes, tputValue);
        }

        @Override
        public void onDownloadStarted(LGFTPFile file) {
            Log.d(LGFTPFragment.TAG, "onDownloadStarted() : " + file.getName() + ", size : " + file.getSize());
            mDownloadingFileName = file.getName();
            mDownloadingFileSize = file.getSize();
        }

        @Override
        public void onDownloadFinished(boolean result, File file, float avgTPut) {
            Log.d(LGFTPFragment.TAG, "onDownloadFinished() " + result + ", " + file.toString());
            if (result) {
                if (mCheckBoxUseFileIO.isChecked()) {
                    new MediaScanning(getContext(), file);
                }
                mFTPFileListVIewAdapter.getSelectedFilePositionList().remove(0);
            }

            TestResultDBManager.getInstance(getContext()).insert(mCheckBoxUseFileIO.isChecked()? TestResultDBManager.TestCategory.FTP_DL_WITH_FILE_IO : TestResultDBManager.TestCategory.FTP_DL_WITHOUT_FILE_IO, avgTPut, file.getName());

            Message msg = mUIControlHandler.obtainMessage(LGFTPFragment.MSG_FILE_DOWNLOAD_FINISHED);
            Bundle b = new Bundle();
            b.putBoolean(LGFTPFragment.KEY_DOWNLOAD_RESULT, result);
            b.putString(LGFTPFragment.KEY_DOWNLOAD_FILE_NAME, file.getName());
            b.putFloat(LGFTPFragment.KEY_AVG_TPUT, avgTPut);
            msg.setData(b);

            mUIControlHandler.sendMessage(msg);
            mUIControlHandler.sendEmptyMessage(LGFTPFragment.MSG_REFRESH_ALL_UI);
        }
    };
    
    final static int MSG_CONNECT_TO_SERVER_FINISHED = 0x01;
    final static int MSG_DISCONNECT_FROM_SERVER_FINISHED  = 0x02;
    final static int MSG_CHANGE_WORKING_DIRECTORY_FINISHED = 0x03;
    final static int MSG_FILE_SET_CHANGED = 0x04;
    final static int MSG_FILE_DOWNLOAD_STARTED = 0x05;
    final static int MSG_FILE_DOWNLOAD_CANCELLED = 0x06;
    final static int MSG_FILE_DOWNLOAD_FINISHED = 0x07;
    final static int MSG_FILE_DOWNLOAD_UPDATE_DIALOG_INFO = 0x08;
    final static int MSG_SELECTED_FILES_CHANGED = 0x09;
    final static int MSG_CLEAR_SELECTED_FILES_CHANGED = 0x10;
    final static int MSG_REFRESH_ALL_UI = 0x11;
    final static int MSG_SHOW_RESULT_POPUP_WINDOW = 0x12;


    // UI Control handler
    @Getter
    @SuppressLint("HandlerLeak")
    private Handler mUIControlHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECT_TO_SERVER_FINISHED: {
                        Log.d(TAG, "MSG_CONNECT_TO_SERVER_FINISHED");
                        Bundle b = msg.getData();
                        boolean sResult = b.getBoolean(KEY_LOGIN_RESULT);
                        LGFTPFragment.this.dismissNetworkOperationProgressBar();
                        if (sResult) {
                            LGFTPFragment.this.mBtnConnectDisconnect.setOnClickListener(mClickListenerDisconnect);
                            LGFTPFragment.this.mBtnConnectDisconnect.setText("Log\nout");
                            LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                            // ******************************* TODO
                            LGFTPFragment.this.setLoggedInLayout();

                            this.sendEmptyMessage(MSG_FILE_SET_CHANGED);
                            if (LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileList().size() > 0) {
                                LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                            } else {
                                LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                            }
                        } else {
                            AlertDialog.Builder alertDiBuilder = new AlertDialog.Builder(LGFTPFragment.this.getContext());
                            alertDiBuilder
                                    .setIcon(R.drawable.alert_icon)
                                    .setTitle("문제 발생!!!")
                                    .setMessage("Login 실패!!!")
                                    .setCancelable(false)
                                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                        }
                    }

                    break;

                case MSG_DISCONNECT_FROM_SERVER_FINISHED:
                    Log.d(TAG, "MSG_DISCONNECT_FROM_SERVER_FINISHED");
                    // ******************************* TODO
                    LGFTPFragment.this.hideSoftKeyboard();
                    LGFTPFragment.this.mBtnConnectDisconnect.setOnClickListener(mClickListenerConnect);
                    LGFTPFragment.this.mBtnConnectDisconnect.setText("Log in");
                    LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                    LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(null);
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    LGFTPFragment.this.dismissNetworkOperationProgressBar();
                    LGFTPFragment.this.setLoggedOutLayout();
                    break;

                case MSG_CHANGE_WORKING_DIRECTORY_FINISHED:
                    Log.d(TAG, "MSG_CHANGE_WORKING_DIRECTORY_FINISHED");
                    LGFTPFragment.this.dismissNetworkOperationProgressBar();
                    this.sendEmptyMessage(MSG_FILE_SET_CHANGED);
                    break;

                case MSG_FILE_SET_CHANGED:
                    Log.d(TAG, "MSG_FILE_SET_CHANGED");

                    if (LGFTPFragment.this.mFileList != null) {
                        // 1 : sort first
                        LGFTPFile[] sortedArray = new LGFTPFile[LGFTPFragment.this.mFileList.size()];
                        sortedArray = LGFTPFragment.this.mFileList.toArray(sortedArray);
                        Arrays.sort(sortedArray);

                        // 2 : assign the sorted list to the adapter
                        LGFTPFragment.this.mFileList = new ArrayList<>(Arrays.asList(sortedArray));
                        LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(LGFTPFragment.this.mFileList);
                    } else {
                        Log.d(TAG, "mFileList is null, clearing out currently showing file list.");
                    }

                    // 3 : clear selected file index
                    LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();

                    // 4 : refresh
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();

                    break;

                case MSG_FILE_DOWNLOAD_STARTED:
                    Log.d(TAG, "MSG_FILE_DOWNLOAD_STARTED");
                    mShouldTerminateTestLoop = false;
                    LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                    Log.d(TAG, "mInitialFildCount  = " + mInitialFileCount);
                    Log.d(TAG, "selectedFileCount = " + (LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileCount()));
                    Log.d(TAG, "the result = " + (mInitialFileCount - LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileCount() + 1));
                    LGFTPFragment.this.mLGFTPFileDownloadProgressDialog.updateFileCount(mInitialFileCount, mInitialFileCount - LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileCount() + 1);
                    Log.d(TAG, "mDownloadingFileName : " + LGFTPFragment.this.mDownloadingFileName);
                    LGFTPFragment.this.mLGFTPFileDownloadProgressDialog.setDownloadingFileName(LGFTPFragment.this.mDownloadingFileName);
                    LGFTPFragment.this.showFileDownloadProgressBar();
                    LGFTPFragment.this.mDownloadResult = false;
                    break;

                case MSG_FILE_DOWNLOAD_CANCELLED:
                    Log.d(TAG, "MSG_FILE_DOWNLOAD_CANCELLED");
                    LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                    LGFTPFragment.this.dismissFileDownloadProgressBar();
                    LGFTPFragment.this.mDownloadResult = false;
                    mShouldTerminateTestLoop = true;
                    break;

                case MSG_FILE_DOWNLOAD_FINISHED: {
                        Log.d(TAG, "MSG_FILE_DOWNLOAD_FINISHED");
                        Bundle b = msg.getData();
                        LGFTPFragment.this.mDownloadResult = b.getBoolean(KEY_DOWNLOAD_RESULT);
                        String sDownloadFileName = b.getString(KEY_DOWNLOAD_FILE_NAME);

                        if (mDownloadResult) {
                            Toast.makeText(LGFTPFragment.this.getContext(),
                                    "FileName : " + sDownloadFileName + "\nDownload completed !!!\nAvgTPut : " + b.getFloat(KEY_AVG_TPUT) + " Mbps",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LGFTPFragment.this.getContext(),
                                    "FileName : " + sDownloadFileName + "\nDownload " + " FAILED!!!",Toast.LENGTH_LONG).show();
                        }

                        if (LGFTPFragment.this.mDownloadResult && !LGFTPFragment.this.mFTPFileListVIewAdapter.isSelectedFileListEmpty()) {
                            Log.d(TAG, "download finished, but still got " + mFTPFileListVIewAdapter.getSelectedFileCount() + " files left");
                            this.sendEmptyMessage(MSG_FILE_DOWNLOAD_UPDATE_DIALOG_INFO);
                        } else if (!LGFTPFragment.this.mDownloadResult) {
                            Log.d(TAG, "download result : " + mDownloadResult);
                            LGFTPFragment.this.dismissFileDownloadProgressBar();
                            LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                            LGFTPFragment.this.mInitialFileCount = Integer.MIN_VALUE;
                        } else {
                            Log.d(TAG, "all selected files have have been downloaded successfully.");
                            LGFTPFragment.this.dismissFileDownloadProgressBar();
                            LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                        }
                        LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    }
                    break;

                case MSG_FILE_DOWNLOAD_UPDATE_DIALOG_INFO:
                    Log.d(TAG, "MSG_FILE_DOWNLOAD_UPDATE_DIALOG_INFO");
                    Log.d(TAG, "file count : " + mInitialFileCount + ", " + (mInitialFileCount - LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileCount() + 1));
                    LGFTPFragment.this.mLGFTPFileDownloadProgressDialog.updateFileCount(mInitialFileCount, mInitialFileCount - LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileCount() + 1);
                    LGFTPFragment.this.mLGFTPFileDownloadProgressDialog.setDownloadingFileName(LGFTPFragment.this.mDownloadingFileName);
                    break;

                case MSG_SELECTED_FILES_CHANGED:
                    Log.d(TAG, "MSG_SELECTED_FILES_CHANGED");
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    break;

                case MSG_CLEAR_SELECTED_FILES_CHANGED:
                    Log.d(TAG, "MSG_CLEAR_SELECTED_FILES_CHANGED");
                    LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    break;

                case MSG_REFRESH_ALL_UI:
                    Log.d(TAG, "MSG_REFRESH_ALL_UI");
                    Log.d(TAG, "isConnected : " + LGFTPFragment.this.mLGFtpClient.isConnected());

                    if (LGFTPFragment.this.mLGFtpClient.isConnected()) {
                        Log.d(TAG, "selected file size : " + LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileList().size());
                        if (LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileList().size() == 0) {
                            LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                        } else {
                            LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                        }
                    } else {
                        LGFTPFragment.this.mBtnConnectDisconnect.setOnClickListener(mClickListenerConnect);
                        LGFTPFragment.this.mBtnConnectDisconnect.setText("Log in");
                        LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                        LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(null);
                        LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                        LGFTPFragment.this.dismissNetworkOperationProgressBar();
                    }
                    break;
                case MSG_SHOW_RESULT_POPUP_WINDOW:
                    Log.d(TAG, "MSG_SHOW_RESULT_POPUP_WINDOW");
                    // TODO : need to fetch all the related data (where test category is about (FTP))from DB, and project it to the data.
                    mTestResultPopupWindow.show(LGFTPFragment.this.getView(), mCheckBoxUseFileIO.isChecked() ? TestResultDBManager.TestCategory.FTP_DL_WITH_FILE_IO : TestResultDBManager.TestCategory.FTP_DL_WITHOUT_FILE_IO);
                    break;

                default:
                    break;
            }
        }
    };

    /* btn click Listeners */
    // connect btn
    private View.OnClickListener mClickListenerConnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideSoftKeyboard();
            LGFTPFragment.this.showNetworkOperationProgressBar("Log in", "Logging in...");
            LGFTPFragment.this.mFileList = null;
            new Thread() {
                @Override
                public void run() {
                    LGFTPFragment.this.mLGFtpClient.connectToServer(
                            mEditTextServerAddress.getText().toString(), Integer.valueOf(mEditTextPortNum.getText().toString()),
                            mEditTextUserID.getText().toString(), mEditTextPassword.getText().toString(), mCheckBoxUsePassiveMode.isChecked(), mCheckBoxUseEPSVforIPv4.isChecked());
                }
            }.start();
        }
    };

    // disconnect listener
    private View.OnClickListener mClickListenerDisconnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideSoftKeyboard();
            LGFTPFragment.this.mFileList = null;
            LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();
            LGFTPFragment.this.showNetworkOperationProgressBar("Log out", "Disconnecting...");
            new Thread() {
                @Override
                public void run() {
                    Log.d(TAG, "disconnecting....");
                    LGFTPFragment.this.mLGFtpClient.disconnectFromServer();
                }
            }.start();
        }
    };

    private boolean mShouldTerminateTestLoop = false;

    // dl start listener
    private View.OnClickListener mClickListenerStart = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideSoftKeyboard();
            Log.d(TAG, "mClickListenerStart.onClick()");
            final int sRepeatCount = Integer.valueOf(mEditTextRepeatCount.getText().toString());
            final ArrayList<LGFTPFile> sSelectedFileList = LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFileList();
            Log.d(TAG, "Selected file count : " + sSelectedFileList.size());
            LGFTPFragment.this.mInitialFileCount = sSelectedFileList.size();

            if (sSelectedFileList.size() > 0) {
                new Thread() {
                    @Override
                    public void run() {
                        ArrayList<LGFTPFile> sTmpSelectedFileList = (ArrayList<LGFTPFile>) sSelectedFileList.clone();
                        ArrayList<Integer> sTmpSelectedFilePositionList = (ArrayList<Integer>) mFTPFileListVIewAdapter.getSelectedFilePositionList().clone();
                        try {
                            int sInterval = Integer.valueOf(mEditTextTestIntervalInSec.getText().toString());
                            for (int i = 0; i != sRepeatCount; ++i) {
                                LGFTPFragment.this.mFTPFileListVIewAdapter.setSelectedFilePositionList((ArrayList<Integer>) sTmpSelectedFilePositionList.clone());
                                LGFTPFragment.this.mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_STARTED);
                                if (LGFTPFragment.this.mLGFtpClient.retrieveFile(sTmpSelectedFileList, LGFTPFragment.this.mCheckBoxUseFileIO.isChecked(), mCheckedMethod, sInterval * 1000, Integer.valueOf(mEditTextBufferSize.getText().toString()))) {
                                    Log.d(TAG, "one set finished, " + (sRepeatCount -1) + " times left");
                                    if (mEditTextTestIntervalInSec.isEnabled()) {
                                        Log.d(TAG, "waiting for " + sInterval + " seconds");
                                        Thread.sleep(sInterval * 1000);
                                    } else {
                                        Log.d(TAG, "no need to wait");
                                    }
                                } else {
                                    Log.d(TAG, "Download has been cancelled.");
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    };


    // history img btn click listener
    private View.OnClickListener mClickListenerShowHistory = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick() show history");
            hideSoftKeyboard();
            // TODO : DB list show.

            // test code for DB creation, insertion, delegation
            //TestResultDBManager.getInstance(LGFTPFragment.this.getContext()).debug_testQry_DB();

            mUIControlHandler.sendEmptyMessage(MSG_SHOW_RESULT_POPUP_WINDOW);
        }
    };

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d(TAG, "onKey() called + " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (this.mLGFtpClient != null && !"/".equals(this.mLGFtpClient.getCurrentWorkingDirectory())) {
                LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();
                LGFTPFragment.this.changeWorkingDirectory("Changing working directory", "Retrieving file list...", "../");

                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideSoftKeyboard();
        Log.d(TAG, "onItemClick() position : " + position + ", id : " + id  );
        final LGFTPFile file = (LGFTPFile) LGFTPFragment.this.mFTPFileListVIewAdapter.getItem(position);
        if (file.isFile()) {
            this.mFTPFileListVIewAdapter.toggleFileSelectedStatusAt(position);

        } else {
            // if it's not a file, then should follow the following steps.
            LGFTPFragment.this.changeWorkingDirectory("Changing working directory", "Retrieving file list...", file.getName());
        }

        if (this.mFTPFileListVIewAdapter.getSelectedFileList().size() > 0) {
            this.mBtnDLULStartStop.setEnabled(true);
        } else {
            this.mBtnDLULStartStop.setEnabled(false);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog instanceof LGFTPFileDownloadProgressDialog) {
            Log.d(TAG, "onDismiss()");

            Log.d(TAG, "network operation progress bar is dismissed");

            /* TODO : UI has been handled within the handler message code above
             *  but need to cleanup the download thread.*/
            Log.d(TAG, "mDownloadResult : " + mDownloadResult);
            if (!LGFTPFragment.this.mDownloadResult) { // 여기가 문제 TODO

                boolean ret = LGFTPFragment.this.mLGFtpClient.stopDownloadAndCancelTheRest();
                Log.d(TAG, "cancel result = " + ret);
                if (ret) {
                    mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_CANCELLED);
                }
            }
        } else {
            Log.d(TAG, "not a LGFTPFileDownloadProgressDialog.");
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        Log.d(TAG, "onFocusChange() " + v.getTransitionName() + ", " + hasFocus);
        if (mEditTextRepeatCount.getId() != v.getId() && mEditTextTestIntervalInSec.getId() != v.getId()){
            Log.d(TAG, "hiding softKeyboard");
            hideSoftKeyboard();
        }
    }

    @Override
    public LGFTPTestFlowConfigurationInfo getTestConfigurationInfo() {
        Log.d(TAG, "LGFTPFragment getTestConfigurationInfo()");
        LGFTPTestFlowConfigurationInfo info = new LGFTPTestFlowConfigurationInfo();
        // TODO : need to implement to put all the info into 'info'
        info.setFileCount(10);
        return info;
    }

    public class LGFTPTestFlowConfigurationInfo implements ILGTestFlowConfigurationInfo {
        // TODO : need to implement class that can hold all the info.
        @Getter @Setter private boolean mUsingFTPFileIO;
        @Getter @Setter private int mFTPBufferSize;
        @Getter @Setter private int mFTPRepeatCount;
        @Getter @Setter private int mFTPRepeatInterval;
        @Getter @Setter private boolean mUsingFTPPSV;
        @Getter @Setter private boolean mUsingIPv4EPSV;
        @Getter @Setter private int mFileCount;
    }
}