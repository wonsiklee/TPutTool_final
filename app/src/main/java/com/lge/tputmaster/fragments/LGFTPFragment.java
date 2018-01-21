package com.lge.tputmaster.fragments;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lge.tputmaster.R;
import com.lge.tputmaster.adapters.FileListViewAdapter;
import com.lge.tputmaster.clients.LGFTPClient;
import com.lge.tputmaster.clients.LGFTPOperationListener;

import org.apache.commons.net.ftp.FTPFile;

import java.util.ArrayList;

import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-06-13.
 */
@Accessors(prefix = "m")
public class LGFTPFragment extends Fragment {
    private static final String TAG = LGFTPFragment.class.getSimpleName();

    private View mView;

    private Button mBtnDLULStartStop;
    private Button mBtnConnectDisconnect;
    private EditText mEditTextServerAddress;
    private EditText mEditTextPortNum;
    private EditText mEditTextUserID;
    private EditText mEditTextPassword;

    private ListView mFTPFileListView;
    private FileListViewAdapter mFTPFileListVIewAdapter;

    private ProgressBar mProgressBar;
    private LinearLayout.LayoutParams mLayoutParam;

    private LGFTPClient mFtpClient;
    private ArrayList<FTPFile> mFileList;

    final static int MSG_CONNECT_TO_SERVER_FINISHED = 0x00;
    final static int MSG_GET_FILE_LIST_FINISHED = 0x01;
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

        //this.mFileList = null;
        this.mFileList = new ArrayList<>();
        FTPFile ff = new FTPFile();
        ff.setName("dd");
        this.mFileList.add(ff);
        ff = new FTPFile();
        ff.setName("ee");
        ff = new FTPFile();
        ff.setName("ff");

        this.mFTPFileListView = (ListView) this.mView.findViewById(R.id.listView_ftpFileList);
        this.mFTPFileListVIewAdapter = new FileListViewAdapter();
        this.mFTPFileListVIewAdapter.setFileList(this.mFileList);
        this.mFTPFileListVIewAdapter.notifyDataSetChanged();
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
        if (this.mFileList != null) {
            for (FTPFile ftpFile : mFileList) {
                Log.d(TAG, (ftpFile.isDirectory() ? "Directory : " : "File : ") + ftpFile);
            }
        } else {
            Log.d(TAG, "no files to print.");
        }
        Log.d(TAG, "************* debug_printCurrentFileList() ENDS ***************");
    }

    LGFTPOperationListener mLGFTPOperationListener = new LGFTPOperationListener() {
        @Override
        public void onConnectToServerFinished(ArrayList<FTPFile> fileList) {
            Log.d(TAG, "onConnectToServerFinished(ArrayList<FTPFile>)");

            if (fileList == null) {
                Log.d(TAG, "fileList is null, this could mean login fail or server connection fail.");
            } else {
                mFileList = fileList;
                Log.d(TAG, "fileList length : " + mFileList.size());
            }

            mUIHandler.sendEmptyMessage(MSG_CONNECT_TO_SERVER_FINISHED);
        }

        @Override
        public void onGetFileListFinished(ArrayList<FTPFile> fileList) {
            Log.d(TAG, "onGetFileListFinished(ArrayList<FTPFile>)");
            if (fileList == null) {
                Log.d(TAG, "fileList is null, this could mean login fail or server connection fail.");
            } else {
                mFileList = fileList;
                Log.d(TAG, "fileList length : " + mFileList.size());
            }
            mUIHandler.sendEmptyMessage(MSG_GET_FILE_LIST_FINISHED);
        }

        @Override
        public void onChangeDirectoryFinished() {
            Log.d(TAG, "onChangeDirectoryFinished()");
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
        public void onDownloadFinished() {
            Log.d(TAG, "onDownloadFinished()");
        }
    };

    // UI Control handler
    private Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_FILE_LIST_FINISHED:
                    hideProgressBar();
                    break;
                case MSG_CONNECT_TO_SERVER_FINISHED:
                    hideProgressBar();
                    debug_printCurrentFileList();
                    mBtnConnectDisconnect.setOnClickListener(mClickListenerDisconnect);
                    mBtnConnectDisconnect.setText("끊기");
                    break;
                case MSG_DISCONNECT_FROM_SERVER_FINISHED:
                    mBtnConnectDisconnect.setOnClickListener(mClickListenerConnect);
                    mBtnConnectDisconnect.setText("연결");
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
            mFtpClient = new LGFTPClient(mLGFTPOperationListener);
            mFtpClient.connectToServer(
                    mEditTextServerAddress.getText().toString(), Integer.valueOf(mEditTextPortNum.getText().toString()),
                    mEditTextUserID.getText().toString(), mEditTextPassword.getText().toString());
        }
    };

    // disconnect listener
    private View.OnClickListener mClickListenerDisconnect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mFileList = null;
            mFtpClient.disconnectFromServer();
            mUIHandler.sendEmptyMessage(MSG_DISCONNECT_FROM_SERVER_FINISHED);
        }
    };

    // dl start listener
    private View.OnClickListener mClickListenerStart = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    // dl stop listener
    private View.OnClickListener mClickListenerStop = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };
}
