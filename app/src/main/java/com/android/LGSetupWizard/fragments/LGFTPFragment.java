package com.android.LGSetupWizard.fragments;

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
import android.widget.ListView;
import android.widget.Toast;

import com.android.LGSetupWizard.data.LGFTPFile;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.adapters.LGFTPFileListViewAdapter;
import com.android.LGSetupWizard.clients.LGFTPClient;
import com.android.LGSetupWizard.clients.LGFTPOperationListener;
import com.android.LGSetupWizard.data.MediaScanning;

import java.io.File;
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

    private Button mBtnDLULStartStop;
    private Button mBtnConnectDisconnect;
    private EditText mEditTextServerAddress;
    private EditText mEditTextPortNum;
    private EditText mEditTextUserID;
    private EditText mEditTextPassword;

    private ListView mFTPFileListView;

    private FileDownloadProgressDialog mFileDownloadProgressDialog;

    private ProgressDialog mNetworkOperationProgressDialog;

    private LGFTPFileListViewAdapter mFTPFileListVIewAdapter;
    private LGFTPClient mLGFtpClient;
    private ArrayList<LGFTPFile> mFileList;



    final static private String KEY_DOWNLOAD_FILE_NAME = "file_name";
    final static private String KEY_DOWNLOAD_RESULT = "file_size";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LGFTPFragment instance hashCode : " + this.hashCode());
        Log.d(TAG, "onCreate()");
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

    @Override
    public void onResume() {
        super.onResume();

        this.mNetworkOperationProgressDialog = new ProgressDialog(this.getContext());
        this.mFileDownloadProgressDialog = new FileDownloadProgressDialog(this.getContext());
        this.mFileDownloadProgressDialog.setOnDismissListener(this);

        if (this.mLGFtpClient == null) {
            this.mLGFtpClient = new LGFTPClient(this.mLGFTPOperationListener);
        }

        this.mBtnDLULStartStop = (Button) this.mView.findViewById(R.id.btn_ftp_download);
        this.mBtnDLULStartStop.setOnClickListener(this.mClickListenerStart);

        this.mBtnConnectDisconnect = (Button) this.mView.findViewById(R.id.btn_connect_disconnect);
        if (this.mLGFtpClient.isConnected()) {
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

        Log.d(TAG, "onResume() completed");
    }

    /* file download progress dialog show/hide [START] */
    private void showDownloadProgressBar() {
        this.mFileDownloadProgressDialog.show();
    }

    private void dismissDownloadProgressBar() {
        if (this.mFileDownloadProgressDialog.isShowing()) {
            this.mFileDownloadProgressDialog.dismiss();
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

    LGFTPOperationListener mLGFTPOperationListener = new LGFTPOperationListener() {
        @Override
        public void onConnectToServerFinished(ArrayList<LGFTPFile> fileList) {
            Log.d(TAG, "onConnectToServerFinished(ArrayList<FTPFile>)");

            if (fileList == null) {
                Log.d(TAG, "fileList is null, this could mean login fail or server connection fail.");
            } else {
                LGFTPFragment.this.mFileList = fileList;
                Log.d(TAG, "fileList length : " + mFileList.size());
            }

            LGFTPFragment.this.mUIControlHandler.sendEmptyMessage(MSG_CONNECT_TO_SERVER_FINISHED);
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
            mUIControlHandler.sendEmptyMessage(MSG_DISCONNECT_FROM_SERVER_FINISHED);
        }

        @Override
        public void onDownloadProgressPublished(float tputValue, long downloadedBytes) {
            Log.d(TAG, "onDownloadProgressPublished(float tputValue, long downloadedBytes) : " + tputValue + ", " + downloadedBytes + " bytes");
        }

        @Override
        public void onDownloadStarted(LGFTPFile file) {
            Log.d(TAG, "onDownloadStarted() : " + file.getName() + ", size : " + file.getSize());

        }

        @Override
        public void onDownloadFinished(boolean result, File file) {
            Log.d(TAG, "onDownloadFinished() " + result + ", " + file.toString());
            if (result) {
                new MediaScanning(LGFTPFragment.this.getContext(), file);
            }

            Message msg = mUIControlHandler.obtainMessage(MSG_FILE_DOWNLOAD_FINISHED);
            Bundle b = new Bundle();
            b.putBoolean(KEY_DOWNLOAD_RESULT, result);
            b.putString(KEY_DOWNLOAD_FILE_NAME, file.getName());
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
    final static int MSG_CLEAR_SELECTED_FILE_LIST = 0x08;


    // UI Control handler
    private Handler mUIControlHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECT_TO_SERVER_FINISHED:
                    Log.d(TAG, "MSG_CONNECT_TO_SERVER_FINISHED");
                    LGFTPFragment.this.mBtnConnectDisconnect.setOnClickListener(mClickListenerDisconnect);
                    LGFTPFragment.this.mBtnConnectDisconnect.setText("Log out");
                    LGFTPFragment.this.dismissNetworkOperationProgressBar();
                    this.sendEmptyMessage(MSG_FILE_SET_CHANGED);
                    break;

                case MSG_DISCONNECT_FROM_SERVER_FINISHED:
                    Log.d(TAG, "MSG_DISCONNECT_FROM_SERVER_FINISHED");
                    LGFTPFragment.this.mBtnConnectDisconnect.setOnClickListener(mClickListenerConnect);
                    LGFTPFragment.this.mBtnConnectDisconnect.setText("Log in");
                    LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(null);
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    LGFTPFragment.this.dismissNetworkOperationProgressBar();
                    break;

                case MSG_CHANGE_WORKING_DIRECTORY_FINISHED:
                    Log.d(TAG, "MSG_CHANGE_WORKING_DIRECTORY_FINISHED");
                    LGFTPFragment.this.dismissNetworkOperationProgressBar();
                    this.sendEmptyMessage(MSG_FILE_SET_CHANGED);
                    break;

                case MSG_FILE_SET_CHANGED:
                    Log.d(TAG, "MSG_FILE_SET_CHANGED");
                    // 1 : sort first
                    LGFTPFile[] sortedArray = new LGFTPFile[LGFTPFragment.this.mFileList.size()];
                    sortedArray = LGFTPFragment.this.mFileList.toArray(sortedArray);
                    Arrays.sort(sortedArray);

                    // 2 : assign the sorted list to the adapter
                    LGFTPFragment.this.mFileList = new ArrayList<>(Arrays.asList(sortedArray));
                    LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(LGFTPFragment.this.mFileList);

                    // 3 : clear selected file index
                    LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();

                    // 4 : refresh
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    break;

                case MSG_FILE_DOWNLOAD_STARTED:
                    Log.d(TAG, "MSG_FILE_DOWNLOAD_STARTED");
                    LGFTPFragment.this.mBtnDLULStartStop.setEnabled(false);
                    LGFTPFragment.this.showDownloadProgressBar();
                    break;

                case MSG_FILE_DOWNLOAD_CANCELLED:
                    Log.d(TAG, "MSG_FILE_DOWNLOAD_CANCELLED");
                    LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                    LGFTPFragment.this.dismissDownloadProgressBar();
                    break;

                case MSG_FILE_DOWNLOAD_FINISHED:
                    Log.d(TAG, "MSG_FILE_DOWNLOAD_FINISHED");
                    Bundle b = msg.getData();
                    boolean sDownloadResult = b.getBoolean(KEY_DOWNLOAD_RESULT);
                    String sDownloadFileName = b.getString(KEY_DOWNLOAD_FILE_NAME);
                    Toast.makeText(LGFTPFragment.this.getContext(),
                            "FileName : " + sDownloadFileName + "\nDownload " + ((sDownloadResult) ? " completed" : " FAILED!!!"),
                            Toast.LENGTH_LONG).show();

                    LGFTPFragment.this.dismissDownloadProgressBar();
                    LGFTPFragment.this.mBtnDLULStartStop.setEnabled(true);
                    break;

                case MSG_CLEAR_SELECTED_FILE_LIST:
                    Log.d(TAG, "MSG_CLEAR_SELECTED_FILE_LIST");
                    LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();
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
            LGFTPFragment.this.showNetworkOperationProgressBar("Login", "Logging in...");
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

    // dl start listener
    private View.OnClickListener mClickListenerStart = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "mClickListenerStart.onClick()");
            final ArrayList<LGFTPFile> sSelectedFileList = LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFile();
            Log.d(TAG, "Selected file count : " + sSelectedFileList.size());

            if (sSelectedFileList.size() > 0) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            LGFTPFragment.this.mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_STARTED);
                            LGFTPFragment.this.mLGFtpClient.retrieveFileOutputStream(sSelectedFileList);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
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
        if (dialog instanceof FileDownloadProgressDialog) {
            Log.d(TAG, "onDismiss()");

            Log.d(TAG, "network operation progress bar is dismissed");

            /* TODO : UI has been handled within the handler message code above
             *  but need to cleanup the download thread.*/
            new Thread() {
                @Override
                public void run() {
                    if (LGFTPFragment.this.mLGFtpClient.stopDownload()) {
                        mUIControlHandler.sendEmptyMessage(MSG_FILE_DOWNLOAD_CANCELLED);
                    }
                }
            }.start();
        } else {
            Log.d(TAG, "not a FileDownloadProgressDialog.");
        }
    }
}
