package com.android.LGSetupWizard.clients;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.LGSetupWizard.data.LGFTPFile;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

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

import static org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;

/**
 * Created by wonsik.lee on 2018-01-01.
 */
@Accessors(prefix = "m")
public class LGFTPClient {
    static final private String TAG = LGFTPClient.class.getSimpleName();

    private FTPClient mFTPClient;
    private ILGFTPOperationListener mOperationListener;
    private boolean mIsForcedAbort;

    @Getter private String mCurrentWorkingDirectory = "/";

    public LGFTPClient(ILGFTPOperationListener operationListener) {
        this.mFTPClient = new FTPClient();
        //this.mFTPClient = new FTPSClient();
        this.mFTPClient.setControlEncoding("euc-kr");

        this.mOperationListener = operationListener;
        this.mIsForcedAbort = false;
    }

    //
    public void connectToServer(final String serverAddress, final int portNum, final String userID, final String password) {
        boolean sResult = false;
        ArrayList<LGFTPFile> fileList = null;
        Log.d(TAG, "connectToServer() " + serverAddress);
        try {
            this.mFTPClient.connect(serverAddress, portNum);
            int reply = mFTPClient.getReplyCode();
            Log.d(TAG, "server connect reply : " + reply);
            if (!FTPReply.isPositiveCompletion(reply)) {
                mFTPClient.disconnect();
                Log.d(TAG, "connection failed, FTPReply code : " + reply);
            } else {
                Log.d(TAG, "successfully connected");
                sResult = true;
                LGFTPClient.this.mFTPClient.setFileType(BINARY_FILE_TYPE);
                // keep alive 2 mins.
                LGFTPClient.this.mFTPClient.setKeepAlive(true);
                LGFTPClient.this.mFTPClient.setControlKeepAliveTimeout(120);
                LGFTPClient.this.mFTPClient.setSoTimeout(1000);

                if (loginToServer(userID, password)) {
                    Log.d(TAG, "Logged in successfully");
                    fileList = getFileList();

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
            this.mOperationListener.onConnectToServerFinished(sResult, fileList);
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

    private ArrayList<LGFTPFile> getFileList() {
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
            mOperationListener.onChangeWorkingDirectoryFinished(getFileList());
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

    private Handler mTPutCalculationLoopHandler = new Handler() {
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

    public boolean retrieveFile(ArrayList<LGFTPFile> targetFileList, boolean shouldWrite) throws Exception {
        for (LGFTPFile file: targetFileList) {
            if (!this.retrieveFile(file, shouldWrite)) {
                return false;
            }
            Thread.sleep(1000);
        }
        return true;
    }

    /*
          Connect and login to the server.
          Enter local passive mode for data connection.
          Set file type to be transferred to binary.
          Construct path of the remote file to be downloaded.
          Create a new OutputStream for writing the file to disk.

          If using the first method (retrieveFile):
            Pass the remote file path and the OutputStream as arguments of the method retrieveFile().
            Close the OutputStream.
            Check return value of retrieveFile() to verify success.

          If using the second method (retrieveFileStream):
            Retrieve an InputStream returned by the method retrieveFileStream().
            Repeatedly a byte array from the InputStream and write these bytes into the OutputStream, until the InputStream is empty.
            Call completePendingCommand() method to complete transaction.
            Close the opened OutputStream the InputStream.
            Check return value of completePendingCommand() to verify success.

          Logout and disconnect from the server.

          http://www.codejava.net/java-se/networking/ftp/java-ftp-file-download-tutorial-and-example
    * */
    public boolean retrieveFile(LGFTPFile targetFile, boolean shouldWrite) throws Exception {
        boolean ret = false;

        Log.d(TAG, "setUseEPSVwithIPv4(");
        this.mFTPClient.setUseEPSVwithIPv4(true);

        Log.d(TAG, "enterLocalPassiveMode()");
        this.mFTPClient.enterLocalPassiveMode();

        Log.d(TAG, "setFileType(BINARY_FILE_TYPE)");
        this.mFTPClient.setFileType(BINARY_FILE_TYPE);

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
            if (shouldWrite) {
                sOutputStream = new BufferedOutputStream(new FileOutputStream(sDownloadFile));
            }

            sInputStream = this.mFTPClient.retrieveFileStream(sRemoteFileName);

            Message msg = LGFTPClient.this.mTPutCalculationLoopHandler.obtainMessage(MSG_START_TPUT_CALCULATION_LOOP);
            Bundle b  = new Bundle();
            b.putSerializable(KEY_FILE, targetFile);
            msg.setData(b);

            // 1. initialize control variables.
            LGFTPClient.this.mStartTime = System.currentTimeMillis();
            LGFTPClient.this.mDownloadedBytes = 0;
            LGFTPClient.this.mElapsedTime = 0;
            LGFTPClient.this.mIsForcedAbort = false;

            // 2. start t-put calculation msg loop
            LGFTPClient.this.mTPutCalculationLoopHandler.sendMessage(msg);

            // 3. inform the fragment that file DL has been started.
            //LGFTPClient.this.mOperationListener.onDownloadStarted((LGFTPFile) msg.getData().getSerializable(KEY_FILE));
            LGFTPClient.this.mOperationListener.onDownloadStarted(targetFile);

            byte[] sBytesArray = new byte[20971520]; // 20 MBytes
            int sBytesRead = -1;
            Log.d(TAG, "shouldWrite : " + shouldWrite);
            while ((sBytesRead = sInputStream.read(sBytesArray)) != -1) {
                if (shouldWrite) {
                    sOutputStream.write(sBytesArray, 0, sBytesRead);
                }
                LGFTPClient.this.mDownloadedBytes += sBytesRead;
                LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - LGFTPClient.this.mStartTime;
            }
            ret = this.mFTPClient.completePendingCommand();

            Log.d(TAG, "mIsForcedAbort : " + mIsForcedAbort);
            if (mIsForcedAbort) {
                ret = false;
            }
            if (ret) {
                Log.d(TAG, "downloaded successfully");
            } else {
                Log.d(TAG, "Download failed");
            }
            this.mIsForcedAbort = false;
            Log.d(TAG, "************************************************************");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "FileNotFoundException : " + e.getMessage());
        } catch (FTPConnectionClosedException e) {
            e.printStackTrace();
            Log.e(TAG, "FTPConnectionClosedException : " + e.getMessage());
            Log.e(TAG, "CODE : " + this.mFTPClient.getReplyCode() + ", " + this.mFTPClient.getReplyString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "IOException : " + e.getMessage());
        } finally {
            // 4. invoke callback
            LGFTPClient.this.mOperationListener.onDownloadFinished(ret, sDownloadFile, ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000));

            // 5. initialize variables.
            LGFTPClient.this.mDownloadedBytes = 0;
            LGFTPClient.this.mElapsedTime = 0;
            LGFTPClient.this.mAvgTPut = 0;

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

            Log.d(TAG, "sending MSG_STOP_TPUT_CALCULATION_LOOP");
            LGFTPClient.this.mTPutCalculationLoopHandler.sendEmptyMessage(MSG_STOP_TPUT_CALCULATION_LOOP);
        }
        return ret;
    }

    public String printWorkingDirectory() throws IOException {
        return this.mFTPClient.printWorkingDirectory();
    }

    public boolean stopDownload() {
        try {
            Log.d(TAG, "calling abort()");
            this.mIsForcedAbort = true;
            return this.mFTPClient.abort();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
