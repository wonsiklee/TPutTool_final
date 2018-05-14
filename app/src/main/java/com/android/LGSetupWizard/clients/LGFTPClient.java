package com.android.LGSetupWizard.clients;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.LGSetupWizard.data.LGFTPFile;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2018-01-01.
 */
@Accessors(prefix = "m")
public class LGFTPClient {
    static final private String TAG = LGFTPClient.class.getSimpleName();

    private FTPClient mFTPClient;
    private ILGFTPOperationListener mOperationListener;
    private boolean mIsForcedAbortion;

    @Getter private String mCurrentWorkingDirectory = "/";

    public LGFTPClient(ILGFTPOperationListener operationListener) {
        this.mFTPClient = new FTPClient();
        this.mOperationListener = operationListener;
        mIsForcedAbortion = false;
    }

    //
    public void connectToServer(final String serverAddress, final int portNum, final String userID, final String password) {
        ArrayList<LGFTPFile> fileList = null;
        Log.d(TAG, "connectToServer() " + serverAddress);
        try {
            this.mFTPClient.connect(serverAddress, portNum);
            int reply = mFTPClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                mFTPClient.disconnect();
                Log.d(TAG, "connection failed, FTPReply code : " + reply);
            } else {
                Log.d(TAG, "successfully connected");
                if (loginToServer(userID, password)) {
                    Log.d(TAG, "Logged in successfully");
                    fileList = nonThreadedGetFileList();
                    // keep alive 2 mins.
                    LGFTPClient.this.mFTPClient.setKeepAlive(true);
                    LGFTPClient.this.mFTPClient.setControlKeepAliveTimeout(120);
                    // buffer size 25 Mbytes,
                    LGFTPClient.this.mFTPClient.setBufferSize(26214400);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "connection error: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "connection error: " + e.getMessage());
        } finally {
            mOperationListener.onConnectToServerFinished(fileList);
        }
    }

    public void disconnectFromServer() {
        logoutFromServer();
        mOperationListener.onDisconnectToServerFinished();
    }

    private boolean loginToServer(String userID, String password) {
        boolean result;
        Log.d(TAG, "logging in to server " + userID + ", " + password);
        try {
            result = this.mFTPClient.login(userID, password);
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
            Log.d(TAG, "login exception : " + e.getMessage());
        }
        return result;
    }

    private void logoutFromServer() {
        try {
            if (this.mFTPClient.isConnected()) {
                this.mFTPClient.logout();
                this.mFTPClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAvailable() {
        return this.mFTPClient.isAvailable();
    }

    public boolean isConnected() {
        return this.mFTPClient.isConnected();
    }

    private ArrayList<LGFTPFile> nonThreadedGetFileList() {
        ArrayList<LGFTPFile> retArray = new ArrayList<>();

        try {
            for (FTPFile f : this.mFTPClient.listFiles()) {
                retArray.add(new LGFTPFile(f));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retArray;
    }

    public void changeWorkingDirectory(final String path) {

        try {
            mFTPClient.changeWorkingDirectory(path);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mOperationListener.onChangeWorkingDirectoryFinished(nonThreadedGetFileList());
            try {
                mCurrentWorkingDirectory = mFTPClient.printWorkingDirectory();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private long mStartTime;
    private long mElapsedTime;
    private long mDownloadedBytes;
    private float mAvgTPut;

    final static private int MSG_START_TPUT_CALCULATION_LOOP = 0x00;
    final static private int MSG_CALCULATE_TPUT = 0x01;
    final static private int MSG_STOP_TPUT_CALCULATION_LOOP = 0x02;

    final static private String KEY_FILE = "file";
    final static private String KEY_IS_FILE_IO_IN_USE = "file_io_is_in_use";

    private Handler mTputCalculationLoopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_TPUT_CALCULATION_LOOP:
                    Log.d(TAG, "MSG_START_TPUT_CALCULATION_LOOP");
                    sendEmptyMessage(MSG_CALCULATE_TPUT);
                    break;

                case MSG_CALCULATE_TPUT:
                    Log.d(TAG, "MSG_CALCULATE_TPUT");
                    Log.d(TAG, "mDownloadedBytes = " + mDownloadedBytes + ", mElapsedTime = " + ((float) mElapsedTime/ 1000) + " secs");
                    LGFTPClient.this.mAvgTPut = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
                    Log.d(TAG, "Avg TPut : " + LGFTPClient.this.mAvgTPut + " Mbps");
                    LGFTPClient.this.mOperationListener.onDownloadProgressPublished(LGFTPClient.this.mAvgTPut, mDownloadedBytes);
                    sendEmptyMessageDelayed(MSG_CALCULATE_TPUT, 1000);
                    break;

                case MSG_STOP_TPUT_CALCULATION_LOOP:
                    Log.d(TAG, "MSG_STOP_TPUT_CALCULATION_LOOP");
                    if (this.hasMessages(MSG_START_TPUT_CALCULATION_LOOP)) {
                        this.removeMessages(MSG_START_TPUT_CALCULATION_LOOP);
                    }
                    if (this.hasMessages(MSG_CALCULATE_TPUT)) {
                        this.removeMessages(MSG_CALCULATE_TPUT);
                    }
                    LGFTPClient.this.mDownloadedBytes = 0;
                    LGFTPClient.this.mStartTime = 0;
                    LGFTPClient.this.mElapsedTime = 0;
                    LGFTPClient.this.mAvgTPut = 0.0f;
                    break;
                default:
                    Log.d(TAG, "invalid msg delivered");
                    break;
            }
        }
    };

    public boolean retrieveFileAndWrite(ArrayList<LGFTPFile> targetFileList, boolean shouldWrite) throws Exception {
        for (LGFTPFile file: targetFileList) {
            if (!this.retrieveFileAndWrite(file, shouldWrite)) {
                return false;
            }
            Thread.sleep(1000);
        }
        return true;
    }

    private boolean mIsFileIORequried = true;

    public boolean retrieveFileAndWrite(LGFTPFile targetFile, boolean shouldWrite) throws Exception {
        boolean ret = false;
        this.mIsFileIORequried = shouldWrite;
        Log.d(TAG, "retrieve " + targetFile);

        String sRemoteFileName = targetFile.getName();
        Log.d(TAG, "sRemoteFileName " + sRemoteFileName);

        String sDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File sTargetDir = new File(sDirPath + "/TPutMonitor");
        if (!sTargetDir.exists()) {
            Log.d(TAG, sTargetDir.getName() + " does not exist, hence make dir");
            sTargetDir.mkdir();
        }

        if (!sTargetDir.canWrite()) {
            Log.d(TAG, "Cannot write logs to dir");
            throw new Exception("File cannot be written");
        }

        File sDownloadFile = new File(sTargetDir + "/" + sRemoteFileName);
        Log.d(TAG, "downloadFile : " + sDownloadFile);

        OutputStream sOutputStream = null;
        InputStream sInputStream = null;
        try {

            Log.d(TAG, "************************************************************");
            Log.d(TAG, "download started bufferSize = " + this.mFTPClient.getBufferSize() + " bytes");
            sOutputStream = new BufferedOutputStream(new FileOutputStream(sDownloadFile));

            Message msg = LGFTPClient.this.mTputCalculationLoopHandler.obtainMessage(MSG_START_TPUT_CALCULATION_LOOP);
            Bundle b  = new Bundle();
            b.putSerializable(KEY_FILE, targetFile);
            msg.setData(b);


            this.mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
            this.mStartTime = System.currentTimeMillis();

            // 1. start t-put calculation msg loop
            LGFTPClient.this.mTputCalculationLoopHandler.sendMessage(msg);

            // 2. inform the fragment that file DL has been started.
            LGFTPClient.this.mOperationListener.onDownloadStarted((LGFTPFile) msg.getData().getSerializable(KEY_FILE));

            // 3-1. in case we need to write file to sdcard.
            if (shouldWrite) {
                this.mFTPClient.setCopyStreamListener(new CopyStreamListener() {
                    @Override
                    public void bytesTransferred(CopyStreamEvent event) {
                        Log.d(TAG, "event : " + event);
                    }

                    @Override
                    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                        LGFTPClient.this.mDownloadedBytes = totalBytesTransferred;
                        LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - LGFTPClient.this.mStartTime;
                    }
                });

                ret = this.mFTPClient.retrieveFile(sRemoteFileName, sOutputStream);

            } else { // 3-2. in case we do not need to write file to sdcard.
                sInputStream = this.mFTPClient.retrieveFileStream(sRemoteFileName);
                byte[] sBytesArray = new byte[4096];
                int sBytesRead = -1;
                while ((sBytesRead = sInputStream.read(sBytesArray)) != -1) {
                    sOutputStream.write(sBytesArray, 0, sBytesRead);
                    LGFTPClient.this.mDownloadedBytes += sBytesRead;
                    LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - LGFTPClient.this.mStartTime;
                }

                ret = this.mFTPClient.completePendingCommand();
            }

            Log.d(TAG, "mIsForcedAbortion : " + mIsForcedAbortion);
            if (mIsForcedAbortion) {
                ret = false;
            }
            if (ret) {
                Log.d(TAG, "downloaded successfully");
            } else {
                Log.d(TAG, "Download failed");
            }
            this.mIsForcedAbortion = false;
            Log.d(TAG, "************************************************************");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "FileNotFoundException : " + e.getMessage());
        } catch (FTPConnectionClosedException e) {
            e.printStackTrace();
            Log.e(TAG, "FTPConnectionClosedException : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "IOException : " + e.getMessage());
        } finally {
            LGFTPClient.this.mTputCalculationLoopHandler.sendEmptyMessage(MSG_STOP_TPUT_CALCULATION_LOOP);
            if (sOutputStream != null) {
                try {
                    sOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (sInputStream != null) {
                try {
                    sInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mOperationListener.onDownloadFinished(ret, sDownloadFile);
        }
        return ret;
    }

    public void retrieveFileAndDoNotWrite(LGFTPFile targetFile) throws Exception {
        Log.d(TAG, "retrieveFileAndDoNotWrite() " + targetFile.getName());

        String sRemoteFileName = targetFile.getName();
        Log.d(TAG, "sRemoteFileName " + sRemoteFileName);

        String sDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File sTargetDir = new File(sDirPath + "/TPutMonitor");
        if (!sTargetDir.exists()) {
            Log.d(TAG, sTargetDir.getName() + " does not exist, hence make dir");
            sTargetDir.mkdir();
        }

        if (!sTargetDir.canWrite()) {
            Log.d(TAG, "Cannot write logs to dir");
            throw new Exception("File cannot be written");
        }

        File sDownloadFile = new File(sTargetDir + "/" + sRemoteFileName);
        Log.d(TAG, "downloadFile : " + sDownloadFile);

        OutputStream sOutputStream = null;
        InputStream sInputStream = null;
        try {
            sOutputStream = new BufferedOutputStream(new FileOutputStream(sTargetDir));
            sInputStream = this.mFTPClient.retrieveFileStream(sRemoteFileName);
            byte[] sBytesArray = new byte[4096];
            int sBytesRead = -1;
            while ((sBytesRead = sInputStream.read(sBytesArray)) != -1) {
                sOutputStream.write(sBytesArray, 0, sBytesRead);
            }

            boolean sIsSuccess = this.mFTPClient.completePendingCommand();
            if (sIsSuccess) {
                Log.d(TAG,  targetFile.getName() + " has been downloaded successfully.");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (sOutputStream != null) {
                    sOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (sInputStream != null) {
                    sInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String printWorkingDirectory() throws IOException {
        return this.mFTPClient.printWorkingDirectory();
    }

    public boolean stopDownload() {
        try {
            Log.d(TAG, "calling abort()");
            this.mIsForcedAbortion = true;
            return this.mFTPClient.abort();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
