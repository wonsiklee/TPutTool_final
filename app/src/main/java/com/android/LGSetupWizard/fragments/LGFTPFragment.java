package com.android.LGSetupWizard.fragments;

import android.content.Context;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.android.LGSetupWizard.data.LGFTPFile;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.adapters.LGFTPFileListViewAdapter;
import com.android.LGSetupWizard.clients.LGFTPClient;
import com.android.LGSetupWizard.clients.LGFTPOperationListener;
import com.android.LGSetupWizard.data.MediaScanning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-06-13.
 */
@Accessors(prefix = "m")
public class LGFTPFragment extends Fragment implements View.OnKeyListener {
    private static final String TAG = LGFTPFragment.class.getSimpleName();

    private View mView;

    private Button mBtnDLULStartStop;
    private Button mBtnConnectDisconnect;
    private EditText mEditTextServerAddress;
    private EditText mEditTextPortNum;
    private EditText mEditTextUserID;
    private EditText mEditTextPassword;

    private ListView mFTPFileListView;
    private LGFTPFileListViewAdapter mFTPFileListVIewAdapter;

    private ProgressBar mProgressBar;
    private LinearLayout.LayoutParams mLayoutParam;

    private LGFTPClient mLGFtpClient;
    private ArrayList<LGFTPFile> mFileList;

    final static int MSG_CONNECT_TO_SERVER_FINISHED = 0x00;
    final static int MSG_FILE_SET_CHANGED = 0x01;
    final static int MSG_DISCONNECT_FROM_SERVER_FINISHED = 0x02;

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

        this.mBtnDLULStartStop = (Button) this.mView.findViewById(R.id.btn_ftp_download);
        this.mBtnDLULStartStop.setOnClickListener(this.mClickListenerStart);

        this.mBtnConnectDisconnect = (Button) this.mView.findViewById(R.id.btn_connect_disconnect);
        this.mBtnConnectDisconnect.setOnClickListener(this.mClickListenerConnect);

        this.mLayoutParam = new LinearLayout.LayoutParams(200,200);

        this.mProgressBar = new ProgressBar(this.getContext(),null,android.R.attr.progressBarStyleLarge);
        this.mProgressBar.setIndeterminate(true);
        this.mProgressBar.setVisibility(View.VISIBLE);

        this.mEditTextServerAddress = (EditText) this.mView.findViewById(R.id.editText_server_addr);
        this.mEditTextPortNum = (EditText) this.mView.findViewById(R.id.editText_port_num);
        this.mEditTextUserID = (EditText) this.mView.findViewById(R.id.editText_user_id);
        this.mEditTextPassword = (EditText) this.mView.findViewById(R.id.editText_password);

        if (!this.isNetworkAvailable()) {
            this.mBtnConnectDisconnect.setEnabled(false);
        }

        this.mFTPFileListView = (ListView) this.mView.findViewById(R.id.listView_ftpFileList);
        this.mFTPFileListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        this.mFTPFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick() position : " + position + ", id : " + id  );
                final LGFTPFile file = (LGFTPFile) LGFTPFragment.this.mFTPFileListVIewAdapter.getItem(position);
                if (file.isFile()) {
                    LGFTPFragment.this.mFTPFileListVIewAdapter.toggleFileSelectedStatusAt(position);
                } else {
                    // if it's not a file, then should follow the following steps.
                    // 1. disable all touchable UIs and show progress bar
                    showProgressBar();

                    // 2. clear the selected file list
                    LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();

                    // 3. call change working directory method in a new thread and receive the new file list.
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            Log.d(TAG, file.getName());
                            LGFTPFragment.this.mLGFtpClient.changeWorkingDirectory(file.getName());

                            try {
                                Log.d(TAG, "working directory : " + LGFTPFragment.this.mLGFtpClient.printWorkingDirectory());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();

                    // 4. when the directory change is completed, set a new set of file list to the adapter and notify
                    // this step will be done via callback called from LGFTPClient, but still leaving this message just for futuer me,
                    // who won't be able to remember later.
                }
            }
        });

        this.mFTPFileListVIewAdapter = new LGFTPFileListViewAdapter(this.getContext());
        this.mFTPFileListView.setAdapter(this.mFTPFileListVIewAdapter);
        Log.d(TAG, "onResume() completed");
    }

    private void showProgressBar() {
        Log.d(TAG, "show progress bar");
        ((LinearLayout) this.mView).addView(this.mProgressBar, this.mLayoutParam);
    }

    private void hideProgressBar() {
        Log.d(TAG, "hide progress bar");
        ((LinearLayout) this.mView).removeView(this.mProgressBar);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
                mFileList = fileList;
                for (LGFTPFile f : fileList) {
                    Log.d(TAG, f.getName());
                }
                Log.d(TAG, "fileList length : " + mFileList.size());
            }

            mUIControlHandler.sendEmptyMessage(MSG_CONNECT_TO_SERVER_FINISHED);
        }

        @Override
        public void onGetFileListFinished(ArrayList<LGFTPFile> fileList) {
            Log.d(TAG, "onGetFileListFinished(ArrayList<FTPFile>)");
            if (fileList == null) {
                Log.d(TAG, "fileList is null, this could mean either 'login fail' or 'server connection fail'");
            } else {
                mFileList = fileList;
                Log.d(TAG, "fileList length : " + mFileList.size());
            }
            mUIControlHandler.sendEmptyMessage(MSG_FILE_SET_CHANGED);
        }

        @Override
        public void onChangeDirectoryFinished(ArrayList<LGFTPFile> fileList) {
            Log.d(TAG, "onChangeDirectoryFinished()");
            LGFTPFragment.this.mFileList = fileList;
            mUIControlHandler.sendEmptyMessage(MSG_CONNECT_TO_SERVER_FINISHED);
        }

        @Override
        public void onDisconnectToServerFinished() {
            Log.d(TAG, "onDisconnectToServerFinished()");
        }

        @Override
        public void onDownloadProgressPublished(float progress) {
            Log.d(TAG, "onDownloadProgressPublished(float progress) : " + progress);
        }

        @Override
        public void onDownloadStarted() {
            Log.d(TAG, "onDownloadStarted()");
        }

        @Override
        public void onDownloadFinished(boolean result, File file) {
            Log.d(TAG, "onDownloadFinished() " + result);
            new MediaScanning(LGFTPFragment.this.getContext(), file);
            LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();
        }
    };

    // UI Control handler
    private Handler mUIControlHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FILE_SET_CHANGED:
                    debug_printCurrentFileList();

                    // TODO 1 : assign the list to the adapter
                    LGFTPFile[] sortedArray = new LGFTPFile[LGFTPFragment.this.mFileList.size()];
                    sortedArray = LGFTPFragment.this.mFileList.toArray(sortedArray);
                    Arrays.sort(sortedArray);
                    LGFTPFragment.this.mFileList = new ArrayList<>(Arrays.asList(sortedArray));

                    LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(LGFTPFragment.this.mFileList);
                    // TODO 2 : refresh
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
                    break;

                case MSG_CONNECT_TO_SERVER_FINISHED:
                    Log.d(TAG, "MSG_CONNECT_TO_SERVER_FINISHED");
                    hideProgressBar();

                    sendEmptyMessage(MSG_FILE_SET_CHANGED);

                    mBtnConnectDisconnect.setOnClickListener(mClickListenerDisconnect);
                    mBtnConnectDisconnect.setText("끊기");
                    break;

                case MSG_DISCONNECT_FROM_SERVER_FINISHED:
                    Log.d(TAG, "MSG_DISCONNECT_FROM_SERVER_FINISHED");
                    mBtnConnectDisconnect.setOnClickListener(mClickListenerConnect);
                    mBtnConnectDisconnect.setText("연결");
                    LGFTPFragment.this.mFTPFileListVIewAdapter.setFileList(null);
                    LGFTPFragment.this.mFTPFileListVIewAdapter.notifyDataSetChanged();
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
            showProgressBar();
            mFileList = null;
            mLGFtpClient = new LGFTPClient(mLGFTPOperationListener);
            mLGFtpClient.connectToServer(
                    mEditTextServerAddress.getText().toString(), Integer.valueOf(mEditTextPortNum.getText().toString()),
                    mEditTextUserID.getText().toString(), mEditTextPassword.getText().toString());
        }
    };

    // disconnect listener
    private View.OnClickListener mClickListenerDisconnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mFileList = null;
            mLGFtpClient.disconnectFromServer();
            mUIControlHandler.sendEmptyMessage(MSG_DISCONNECT_FROM_SERVER_FINISHED);
        }
    };

    // dl start listener
    private View.OnClickListener mClickListenerStart = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "DL button start onClick()");
            final ArrayList<LGFTPFile> sSelectedFileList = LGFTPFragment.this.mFTPFileListVIewAdapter.getSelectedFile();
            Log.d(TAG, "Selected file count : " + sSelectedFileList.size());
            for (LGFTPFile file: sSelectedFileList) {
                Log.d(TAG, "\t" + file.toString());
            }

            if (sSelectedFileList.size() > 0) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            LGFTPFragment.this.mLGFtpClient.retrieveFileOutputStream(sSelectedFileList.get(0));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    };

    // dl stop listener
    private View.OnClickListener mClickListenerStop = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "DL button stop onClick()");
        }
    };

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (this.mLGFtpClient != null && !"/".equals(this.mLGFtpClient.getCurrentWorkingDirectory())) {
                LGFTPFragment.this.mFTPFileListVIewAdapter.clearSelectedFilePositionList();
                new Thread() {
                    @Override
                    public void run() {
                        LGFTPFragment.this.mLGFtpClient.changeWorkingDirectory("../");
                    }
                }.start();
                return true;
            }
        }
        return false;
    }
}
