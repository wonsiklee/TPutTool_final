package com.android.LGSetupWizard.fragments;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.data.LGFTPFile;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.adapters.LGFTPFileListViewAdapter;
import com.android.LGSetupWizard.clients.LGFTPClient;
import com.android.LGSetupWizard.clients.ILGFTPOperationListener;
import com.android.LGSetupWizard.data.MediaScanning;
import com.android.LGSetupWizard.database.TestResultPopupWindow;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-06-13.
 */
@Accessors(prefix = "m")
public class LGFTPFragment extends Fragment implements View.OnKeyListener, AdapterView.OnItemClickListener, Dialog.OnDismissListener {
    private static final String TAG = LGFTPFragment.class.getSimpleName();

    private View mView;

    private Button mBtnConnectDisconnect;
    private EditText mEditTextServerAddress;
    private EditText mEditTextPortNum;
    private EditText mEditTextUserID;
    private EditText mEditTextPassword;

    private LinearLayout mLinearLayoutLoggedInViewGroup;
    private ImageButton mImgBtnTestHistory;
    private Switch mSwitchFileIO;
    private Spinner mSpinnerRepeatCount;
    private Button mBtnDLULStartStop;

    private ListView mFTPFileListView;

    private RadioGroup mRadioGroupMethodType;
    private RadioButton mRadioButtonConventionalMethod;
    private RadioButton mRadioButtonApacheMethod;
    private RadioButton mRadioButtonFileChannelMethod;

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

    final static private String KEY_DOWNLOAD_FILE_NAME = "file_name";
    final static private String KEY_DOWNLOAD_RESULT = "file_size";
    final static private String KEY_AVG_TPUT = "avg_tput";
    final static private String KEY_LOGIN_RESULT = "login_result";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGFTPFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
        mInitialFileCount = Integer.MIN_VALUE;
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

    private static boolean DEBUG = false;

    @Override
    public void onResume() {
        super.onResume();

        this.mNetworkOperationProgressDialog = new ProgressDialog(this.getContext());
        this.mLGFTPFileDownloadProgressDialog = new LGFTPFileDownloadProgressDialog(this.getContext());
        this.mLGFTPFileDownloadProgressDialog.setOnDismissListener(this);

        if (this.mLGFtpClient == null) {
            this.mLGFtpClient = new LGFTPClient(this.mILGFTPOperationListener);
        }

        this.mBtnConnectDisconnect = (Button) this.mView.findViewById(R.id.btn_connect_disconnect);
        if (this.mLGFtpClient != null && this.mLGFtpClient.isConnected()) {
            this.mBtnConnectDisconnect.setOnClickListener(this.mClickListenerDisconnect);
        } else {
            this.mBtnConnectDisconnect.setOnClickListener(this.mClickListenerConnect);
        }

        this.mEditTextServerAddress = (EditText) this.mView.findViewById(R.id.editText_server_addr);
        this.mEditTextPortNum = (EditText) this.mView.findViewById(R.id.editText_port_num);
        this.mEditTextUserID = (EditText) this.mView.findViewById(R.id.editText_user_id);
        this.mEditTextPassword = (EditText) this.mView.findViewById(R.id.editText_password);

        if (!this.isNetworkAvailable()) {
            this.mBtnConnectDisconnect.setEnabled(false);
        }

        this.mFTPFileListView = (ListView) this.mView.findViewById(R.id.listView_ftpFileList);
        this.mFTPFileListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        this.mFTPFileListView.setOnItemClickListener(this);

        if (this.mFTPFileListVIewAdapter == null) {
            this.mFTPFileListVIewAdapter = new LGFTPFileListViewAdapter(this.getContext());
            this.mFTPFileListView.setAdapter(this.mFTPFileListVIewAdapter);
        }

        this.mLinearLayoutLoggedInViewGroup = this.mView.findViewById(R.id.linearLayout_logged_in_view_group);
        this.mImgBtnTestHistory = (this.mView.findViewById(R.id.imgBtn_history));
        this.mImgBtnTestHistory.setOnClickListener(this.mClickListenerShowHistory);
        this.mBtnDLULStartStop = this.mView.findViewById(R.id.btn_ftp_download);
        this.mBtnDLULStartStop.setOnClickListener(this.mClickListenerStart);
        this.mSwitchFileIO = this.mView.findViewById(R.id.switch_file_IO_enabler);

        this.mSpinnerRepeatCount = this.mView.findViewById(R.id.spinner_ftp_download_repeat_count);

        this.mTestResultPopupWindow = new TestResultPopupWindow(this.getContext());

        this.mRadioGroupMethodType = this.mView.findViewById(R.id.radioGroup_method_type);
        this.mRadioButtonConventionalMethod = this.mView.findViewById(R.id.radioButton_method_type_conventional);
        this.mRadioButtonApacheMethod = this.mView.findViewById(R.id.radioButton_method_type_apache);
        this.mRadioButtonFileChannelMethod= this.mView.findViewById(R.id.radioButton_method_type_file_channel);

        this.mRadioGroupMethodType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == mRadioButtonConventionalMethod.getId()) {
                    mCheckedMethod = 0x00;
                } else if (checkedId == mRadioButtonApacheMethod.getId()) {
                    mCheckedMethod = 0x01;
                } else if (checkedId == mRadioButtonFileChannelMethod.getId()) {
                    mCheckedMethod = 0x02;
                }
            }
        });
        this.mUIControlHandler.sendEmptyMessage(MSG_REFRESH_ALL_UI);

        Log.d(TAG, "onResume() completed");
    }

    private int mCheckedMethod;


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
    /* network operation progress bar show/hide [END] */

    private void showDLBtnLayout() {
        LGFTPFragment.this.mLinearLayoutLoggedInViewGroup.animate().translationY(0).alpha(1.0f).setDuration(300).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                LGFTPFragment.this.mLinearLayoutLoggedInViewGroup.setVisibility(View.VISIBLE);
                LGFTPFragment.this.mSpinnerRepeatCount.setDropDownWidth(LGFTPFragment.this.mSpinnerRepeatCount.getWidth());
                try {
                    Field popup = Spinner.class.getDeclaredField("mPopup");
                    popup.setAccessible(true);
                    ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(mSpinnerRepeatCount);
                    popupWindow.setHeight(LGFTPFragment.this.mSpinnerRepeatCount.getHeight() * 16);
                } catch (NoClassDefFoundError | ClassCastException | NoSuchFieldException | IllegalAccessException e) {
                    Log.e(TAG, "" + e.getMessage());
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) { }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
    }

    private void hideDLBtnLayout() {
        LGFTPFragment.this.mLinearLayoutLoggedInViewGroup.animate().translationY(mLinearLayoutLoggedInViewGroup.getHeight()).alpha(0.0f).setDuration(600)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) { }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        LGFTPFragment.this.mLinearLayoutLoggedInViewGroup.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) { }

                    @Override
                    public void onAnimationRepeat(Animator animation) { }
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

    ILGFTPOperationListener mILGFTPOperationListener = new ILGFTPOperationListener() {
        @Override
        public void onConnectToServerFinished(boolean result, ArrayList<LGFTPFile> fileList) {
            Log.d(TAG, "onConnectToServerFinished(boolean, ArrayList<FTPFile>) : " + result);
            Message msg = mUIControlHandler.obtainMessage(MSG_CONNECT_TO_SERVER_FINISHED);
            Bundle b = new Bundle();
            b.putBoolean(KEY_LOGIN_RESULT, result);
            msg.setData(b);
            if (result) {
                if (fileList == null) {
                    Log.d(TAG, "no File list retrieved.");
                } else {
                    LGFTPFragment.this.mFileList = fileList;
                    Log.d(TAG, "fileList length : " + mFileList.size());
                }
            }
            LGFTPFragment.this.mUIControlHandler.sendMessage(msg);
        }

        @Override
        public void onChangeWorkingDirectoryFinished(ArrayList<LGFTPFile> fileList) {
            Log.d(TAG, "onChangeWorkingDirectoryFinished()");
            LGFTPFragment.this.mFileList = fileList;
            LGFTPFragment.this.mUIControlHandler.sendEmptyMessage(MSG_CHANGE_WORKING_DIRECTORY_FINISHED);
        }

        @Override
        public void onDisconnectToServerFinished() {
            Log.d(TAG, "onDisconnectToServerFinished()");
            LGFTPFragment.this.mUIControlHandler.sendEmptyMessage(MSG_DISCONNECT_FROM_SERVER_FINISHED);
        }

        @Override
        public void onDownloadProgressPublished(float tputValue, long downloadedBytes) {
            //Log.d(TAG, "onDownloadProgressPublished(float tputValue, long downloadedBytes) : " + tputValue + ", " + downloadedBytes + " bytes");
            LGFTPFragment.this.mLGFTPFileDownloadProgressDialog.updateProgressValue(((float) downloadedBytes / mDownloadingFileSize) * 100, downloadedBytes, tputValue);
        }

        @Override
        public void onDownloadStarted(LGFTPFile file) {
            Log.d(TAG, "onDownloadStarted() : " + file.getName() + ", size : " + file.getSize());
            LGFTPFragment.this.mDownloadingFileName = file.getName();
            LGFTPFragment.this.mDownloadingFileSize = file.getSize();
        }

        @Override
        public void onDownloadFinished(boolean result, File file, float avgTPut) {
            Log.d(TAG, "onDownloadFinished() " + result + ", " + file.toString());
            if (result) {
                if (LGFTPFragment.this.mSwitchFileIO.isChecked()) {
                    new MediaScanning(LGFTPFragment.this.getContext(), file);
                }
                LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFilePositionList().remove(0);
            }

            TestResultDBManager.getInstance(LGFTPFragment.this.getContext()).insert(mSwitchFileIO.isChecked()? TestResultDBManager.TestCategory.FTP_DL_WITH_FILE_IO : TestResultDBManager.TestCategory.FTP_DL_WITHOUT_FILE_IO, avgTPut, file.getName());

            Message msg = LGFTPFragment.this.mUIControlHandler.obtainMessage(MSG_FILE_DOWNLOAD_FINISHED);
            Bundle b = new Bundle();
            b.putBoolean(KEY_DOWNLOAD_RESULT, result);
            b.putString(KEY_DOWNLOAD_FILE_NAME, file.getName());
            b.putFloat(KEY_AVG_TPUT, avgTPut);
            msg.setData(b);

            LGFTPFragment.this.mUIControlHandler.sendMessage(msg);
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
                            LGFTPFragment.this.mBtnConnectDisconnect.setText("Log out");
                            LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                            LGFTPFragment.this.showDLBtnLayout();
                            LGFTPFragment.this.mSwitchFileIO.setEnabled(true);
                            this.sendEmptyMessage(MSG_FILE_SET_CHANGED);
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
                    LGFTPFragment.this.mBtnConnectDisconnect.setOnClickListener(mClickListenerConnect);
                    LGFTPFragment.this.mBtnConnectDisconnect.setText("Log in");
                    LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                    LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(null);
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    LGFTPFragment.this.dismissNetworkOperationProgressBar();
                    LGFTPFragment.this.hideDLBtnLayout();

                    LGFTPFragment.this.mSwitchFileIO.setEnabled(false);
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
                        LGFTPFragment.this.mDownloadResult = false;
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
                    if (LGFTPFragment.this.mLGFtpClient.isConnected()) {

                    } else {
                        LGFTPFragment.this.mBtnConnectDisconnect.setOnClickListener(mClickListenerConnect);
                        LGFTPFragment.this.mBtnConnectDisconnect.setText("Log in");
                        LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                        LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(null);
                        LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                        LGFTPFragment.this.dismissNetworkOperationProgressBar();
                        LGFTPFragment.this.mSwitchFileIO.setEnabled(false);
                        LGFTPFragment.this.mLinearLayoutLoggedInViewGroup.animate().translationY(mLinearLayoutLoggedInViewGroup.getHeight()).alpha(0.0f).setDuration(600)
                                .setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) { }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        LGFTPFragment.this.mLinearLayoutLoggedInViewGroup.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) { }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) { }
                                });
                    }
                    break;
                case MSG_SHOW_RESULT_POPUP_WINDOW:
                    Log.d(TAG, "MSG_SHOW_RESULT_POPUP_WINDOW");
                    // TODO : need to fetch all the related data (where test category is about (FTP))from DB, and project it to the data.
                    mTestResultPopupWindow.show(LGFTPFragment.this.getView(), mSwitchFileIO.isChecked() ? TestResultDBManager.TestCategory.FTP_DL_WITH_FILE_IO : TestResultDBManager.TestCategory.FTP_DL_WITHOUT_FILE_IO);
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
            LGFTPFragment.this.showNetworkOperationProgressBar("Log in", "Logging in...");
            LGFTPFragment.this.mFileList = null;
            new Thread() {
                @Override
                public void run() {
                    LGFTPFragment.this.mLGFtpClient.connectToServer(
                            mEditTextServerAddress.getText().toString(), Integer.valueOf(mEditTextPortNum.getText().toString()),
                            mEditTextUserID.getText().toString(), mEditTextPassword.getText().toString());
                }
            }.start();
        }
    };

    // disconnect listener
    private View.OnClickListener mClickListenerDisconnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LGFTPFragment.this.mFileList = null;
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

    private View.OnLongClickListener mLongClickListenerStart = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final int sRepeatCount = Integer.valueOf(mSpinnerRepeatCount.getSelectedItem().toString());
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
                            for (int i = 0; i != sRepeatCount; ++i) {
                                LGFTPFragment.this.mFTPFileListVIewAdapter.setSelectedFilePositionList((ArrayList<Integer>) sTmpSelectedFilePositionList.clone());
                                LGFTPFragment.this.mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_STARTED);
                                LGFTPFragment.this.mLGFtpClient.retrieveFile(sTmpSelectedFileList, LGFTPFragment.this.mSwitchFileIO.isChecked(), mCheckedMethod);
                                Log.d(TAG, "one set finished, " + (sRepeatCount -1) + " times left");
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
            return true;
        }
    };

    // dl start listener
    private View.OnClickListener mClickListenerStart = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "mClickListenerStart.onClick()");
            final int sRepeatCount = Integer.valueOf(mSpinnerRepeatCount.getSelectedItem().toString());
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
                            for (int i = 0; i != sRepeatCount; ++i) {
                                LGFTPFragment.this.mFTPFileListVIewAdapter.setSelectedFilePositionList((ArrayList<Integer>) sTmpSelectedFilePositionList.clone());
                                LGFTPFragment.this.mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_STARTED);
                                if (LGFTPFragment.this.mLGFtpClient.retrieveFile(sTmpSelectedFileList, LGFTPFragment.this.mSwitchFileIO.isChecked(), mCheckedMethod)) {
                                    Log.d(TAG, "one set finished, " + (sRepeatCount -1) + " times left, delaying for 3 secs");
                                    Thread.sleep(3000);
                                } else {
                                    Toast.makeText(LGFTPFragment.this.getContext(), "다운로드가 취소되었습니다.", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "onItemClick() position : " + position + ", id : " + id  );
        final LGFTPFile file = (LGFTPFile) LGFTPFragment.this.mFTPFileListVIewAdapter.getItem(position);
        if (file.isFile()) {
            LGFTPFragment.this.mFTPFileListVIewAdapter.toggleFileSelectedStatusAt(position);
        } else {
            // if it's not a file, then should follow the following steps.
            LGFTPFragment.this.changeWorkingDirectory("Changing working directory", "Retrieving file list...", file.getName());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog instanceof LGFTPFileDownloadProgressDialog) {
            Log.d(TAG, "onDismiss()");

            Log.d(TAG, "network operation progress bar is dismissed");

            /* TODO : UI has been handled within the handler message code above
             *  but need to cleanup the download thread.*/
            if (!LGFTPFragment.this.mDownloadResult) { // 여기가 문제 TODO

                boolean ret = LGFTPFragment.this.mLGFtpClient.stopDownloadAndCancelTheRest();
                Log.d(TAG, "cancel result = " + ret);
                if (ret) {
                    mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_CANCELLED);
                }
                /*new Thread() {
                    @Override
                    public void run() {
                        boolean ret = LGFTPFragment.this.mLGFtpClient.stopDownloadAndCancelTheRest();
                        Log.d(TAG, "cancel result = " + ret);
                        if (ret) {
                            mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_CANCELLED);
                        }
                    }
                }.start();*/
            }

        } else {
            Log.d(TAG, "not a LGFTPFileDownloadProgressDialog.");
        }
    }
}
