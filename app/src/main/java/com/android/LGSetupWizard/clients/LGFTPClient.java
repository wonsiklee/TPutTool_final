package com.android.LGSetupWizard.clients;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.LGSetupWizard.data.LGFTPFile;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
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

    final static private int MSG_START_TPUT_CALCULATION_LOOP = 0x00;
    final static private int MSG_CALCULATE_TPUT = 0x01;
    final static private int MSG_STOP_TPUT_CALCULATION_LOOP = 0x02;

    final static private String KEY_FILE = "file";

    @Getter private String mCurrentWorkingDirectory = "/";

    private FTPClient mFTPClient;
    //private boolean mIsForcedAbort;
    private ILGFTPOperationListener mOperationListener;
    private InputStream mInputStream = null;

    private long mStartTimeForCalculationHandler;
    private long mElapsedTime;
    private long mDownloadedBytes;
    private float mAvgTPut;

    private String mDownloadFilePathAndName;
    private String mServerAddress;
    private int mPortNum;
    private String mUserID;
    private String mPassword;

    public LGFTPClient(ILGFTPOperationListener operationListener) {
        this.mOperationListener = operationListener;
        //this.mIsForcedAbort = false;

       /* this.mCopyStreamListener = new CopyStreamListener() {
            @Override
            public void bytesTransferred(CopyStreamEvent event) {
                Log.d(TAG + " copyStream", "bytesTransferred(CopyStreamEvent) " + event.toString());
                // TODO
            }

            @Override
            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - LGFTPClient.this.mStartTimeForCalculationHandler;
                *//*Log.d(TAG, "totalBytesTransferred = " + totalBytesTransferred + " bytes\n" +
                        "bytesTransferred = " + bytesTransferred + " bytes\n" +
                        "streamSize = " + streamSize + " bytes");*//*
                mDownloadedBytes = totalBytesTransferred;
                LGFTPClient.this.mAvgTPut = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
                Log.d(TAG + " copyStream", "totalBytesTransferred = " + totalBytesTransferred + " bytes");
                Log.d(TAG + " copyStream", "bytesTransferred = " + bytesTransferred + " bytes");
                Log.d(TAG + " copyStream", "AvgTput : " + mAvgTPut);
                // TODO
                //LGFTPClient.this.mOperationListener.onDownloadProgressPublished(LGFTPClient.this.mAvgTPut, mDownloadedBytes);
            }
        };*/
    }

    private boolean mShouldTerminateTestLoop = true;
    private ProtocolCommandListener mFTPProtocolListener = new ProtocolCommandListener() {
        @Override
        public void protocolCommandSent(ProtocolCommandEvent event) {
            Log.d(TAG, "protocolCommandSent : " + event.getMessage());
        }

        @Override
        public void protocolReplyReceived(ProtocolCommandEvent event) {
            Log.d(TAG, "protocolReplyReceived : " + event.getMessage());
            if (event.getReplyCode() == FTPReply.TRANSFER_ABORTED) {
                Log.d(TAG, "mark to terminate test loop");
                mShouldTerminateTestLoop = true;
            } else if (event.getReplyCode() == FTPReply.SERVICE_NOT_AVAILABLE) {
                Log.d(TAG, "perform connectToServer due to 421 response");
                //LGFTPClient.this.connectToServer(mServerAddress, mPortNum, mUserID, mPassword);
                LGFTPClient.this.disconnectFromServer();
            }
        }
    };
    public void connectToServer(final String serverAddress, final int portNum, final String userID, final String password) {
        Log.d(TAG, "connectToServer() " + serverAddress);
        this.mServerAddress = serverAddress;
        this.mPortNum = portNum;
        this.mUserID = userID;
        this.mPassword = password;

        boolean sResult = false;

        this.mFTPClient = new FTPClient();
        this.mFTPClient.setControlEncoding("UTF-8");
        this.mFTPClient.setConnectTimeout(10 * 1000);
        this.mFTPClient.setDefaultTimeout(20 * 1000);
        this.mFTPClient.addProtocolCommandListener(mFTPProtocolListener);

        ArrayList<LGFTPFile> fileList = null;

        try {
            this.mFTPClient.connect(serverAddress, portNum);
            if (this.mFTPClient instanceof FTPSClient) {
                Log.d(TAG, "FTP Cafe mimic, instance of FTPSClient");
                ((FTPSClient)this.mFTPClient).execPBSZ(0L);

                ((FTPSClient)this.mFTPClient).execPROT("C");
            }
            int reply = mFTPClient.getReplyCode();
            Log.d(TAG, "server connect reply : " + reply);
            if (!FTPReply.isPositiveCompletion(reply)) {
                mFTPClient.disconnect();
                Log.d(TAG, "connection failed, FTPReply code : " + reply);
            } else {
                Log.d(TAG, "successfully connected");
                sResult = true;
                this.mFTPClient.setFileType(BINARY_FILE_TYPE);
                if (loginToServer(userID, password)) {
                    Log.d(TAG, "Logged in successfully");
                    fileList = getFileList();
                    mConnectionKeepAliveHandler.sendEmptyMessage(MSG_START_KEEP_ALIVE_CONNECTION);
                }
                //sendCommandAndGetReply("FEAT");
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

    private void sendCommandAndGetReply(String cmd) {
        try {
            if (FTPReply.isPositiveCompletion(this.mFTPClient.sendCommand(cmd))) {
                String sReplyString = this.mFTPClient.getReplyString();
                Log.d(TAG, cmd + " replyString : " + sReplyString);
                String[] sReplyStringArray = this.mFTPClient.getReplyStrings();
                Log.d(TAG, "ArrayPrint START");
                for (String str : sReplyStringArray) {
                    Log.d(TAG, cmd + " replyString : " + str);
                }
                Log.d(TAG, "ArrayPrint END");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void disconnectFromServer() {
        mConnectionKeepAliveHandler.sendEmptyMessage(MSG_STOP_KEEP_ALIVE_CONNECTION);
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
        if (this.mFTPClient != null && this.mFTPClient.isAvailable()) {
            return true;
        }
        return false;
    }

    public boolean isConnected() {
        if (this.mFTPClient == null) {
            return false;
        } else {
            return this.mFTPClient.isConnected();
        }
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

    @SuppressLint("HandlerLeak")
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
                    LGFTPClient.this.mStartTimeForCalculationHandler = 0;
                    LGFTPClient.this.mElapsedTime = 0;
                    LGFTPClient.this.mAvgTPut = 0.0f;
                    break;
                default:
                    Log.d(TAG, "invalid msg delivered");
                    break;
            }
        }
    };

    final static private int MSG_START_KEEP_ALIVE_CONNECTION = 0x00;
    final static private int MSG_STOP_KEEP_ALIVE_CONNECTION = 0x01;
    final static private int MSG_SEND_NOOP = 0x02;

    final static private int KEEP_ALIVE_INTERVAL = 110 * 1000;

    @SuppressLint("HandlerLeak")
    private Handler mConnectionKeepAliveHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_KEEP_ALIVE_CONNECTION:
                    sendEmptyMessage(MSG_SEND_NOOP);
                    break;

                case MSG_SEND_NOOP:
                    if (mFTPClient != null && mFTPClient.isConnected()) {
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    mFTPClient.sendNoOp();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.d(TAG, "IOException while sending NoOp ");
                                }
                            }
                        }.start();

                        sendEmptyMessageDelayed(MSG_SEND_NOOP, KEEP_ALIVE_INTERVAL);
                    }
                    break;

                case MSG_STOP_KEEP_ALIVE_CONNECTION:
                    this.removeMessages(MSG_START_KEEP_ALIVE_CONNECTION);
                    this.removeMessages(MSG_SEND_NOOP);
                    break;

                default :
                    break;
            }
        }
    };

    private static final int MSG_DOWNLOAD_PROGRESS_START_CHECK = 0x00;
    private static final int MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES = 0x01;
    private static final int MSG_DOWNLOAD_PROGRESS_TERMINATE_CHECK = 0x02;

    private static final int FILE_MONITOR_INTERVAL = 1 * 1000;


    @SuppressLint("HandlerLeak")
    @Accessors(prefix = "m")
    protected Handler mDownloadProgressCheckHandler = new Handler() {
        private long mIntervalStart;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DOWNLOAD_PROGRESS_START_CHECK:
                    Log.d(TAG, "MSG_DOWNLOAD_PROGRESS_START_CHECK");
                    this.mIntervalStart = System.currentTimeMillis();
                    this.sendEmptyMessage(MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES);
                    break;

                case MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES: {
                    Log.d(TAG, "MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES block STARTS");

                    long intervalEnd = System.currentTimeMillis();
                    File f = new File(mDownloadFilePathAndName);
                    long sFileSizeGap = f.length() - mDownloadedBytes;
                    long sTimeGap = intervalEnd - mIntervalStart;
                    float sAvgForBetweenPolling = ((float) (sFileSizeGap) * 8 / 1024.0f / 1024) / ((float) sTimeGap / 1000);
                    Log.d(TAG, "AvgSpeed for " + sTimeGap + " ms = " + sAvgForBetweenPolling + " Mbps");

                    // update control variables here.
                    LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - mStartTimeForCalculationHandler;
                    LGFTPClient.this.mDownloadedBytes = f.length();
                    //LGFTPClient.this.mAvgTPut = ((float) mDownloadedBytes * 8 / 1024.0f / 1024) / ((float) mElapsedTime / 1000);

                    f = null;
                    this.mIntervalStart = intervalEnd;
                    this.sendEmptyMessageDelayed(MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES, FILE_MONITOR_INTERVAL);
                    Log.d(TAG, "MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES block ENDS");
                    break;
                }
                case MSG_DOWNLOAD_PROGRESS_TERMINATE_CHECK:
                    Log.d(TAG, "MSG_DOWNLOAD_PROGRESS_TERMINATE_CHECK");
                    LGFTPClient.this.mDownloadedBytes = 0;
                    LGFTPClient.this.mElapsedTime = 0;
                    LGFTPClient.this.mAvgTPut = 0;

                    if (this.hasMessages(MSG_DOWNLOAD_PROGRESS_START_CHECK)) {
                        this.removeMessages(MSG_DOWNLOAD_PROGRESS_START_CHECK);
                    }

                    if (this.hasMessages(MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES)) {
                        this.removeMessages(MSG_DOWNLOAD_PROGRESS_UPDATE_CONTROL_VARIABLES);
                    }
                    break;
            }
        }
    };

    public boolean retrieveFile(ArrayList<LGFTPFile> remoteFileList, boolean shouldWrite, int methodType) throws Exception {
        this.mConnectionKeepAliveHandler.sendEmptyMessage(MSG_STOP_KEEP_ALIVE_CONNECTION);

        for (LGFTPFile remoteFile: remoteFileList) {
            boolean sRet = this.retrieveFile(remoteFile, shouldWrite);
            Log.d(TAG, "single file download result = " + sRet);
            if (!sRet) {
                Log.d(TAG, "start keep alive loop.");
                this.mConnectionKeepAliveHandler.sendEmptyMessage(MSG_START_KEEP_ALIVE_CONNECTION);
                Log.d(TAG, "cancelling the rest of the files to download");
                return false;
            }
            Thread.sleep(3000);
        }

        Log.d(TAG, "all downloads completed.");

        Log.d(TAG, "start keep alive loop.");
        this.mConnectionKeepAliveHandler.sendEmptyMessage(MSG_START_KEEP_ALIVE_CONNECTION);
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
    public boolean retrieveFile(LGFTPFile remoteFile, boolean shouldWrite) {
        mShouldTerminateTestLoop = true;
        boolean ret;

        Log.d(TAG, "setUseEPSVwithIPv4()");
        this.mFTPClient.setUseEPSVwithIPv4(true);

        //Log.d(TAG, "enterLocalPassiveMode()");
        //this.mFTPClient.enterLocalPassiveMode();

        Log.d(TAG, "setFileType(BINARY_FILE_TYPE)");
        try {
            this.mFTPClient.setFileType(BINARY_FILE_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LGFTPClient.this.mDownloadedBytes = 0;
        LGFTPClient.this.mElapsedTime = 0;
        LGFTPClient.this.mAvgTPut = 0;

        if (shouldWrite) {
            ret = retrieveFileWithFileIO(remoteFile, 0);
        } else {
            ret = retrieveFileWithoutFileIO(remoteFile);
        }

        LGFTPClient.this.mDownloadedBytes = 0;
        LGFTPClient.this.mElapsedTime = 0;
        LGFTPClient.this.mAvgTPut = 0;

        return ret;
    }


    final static int METHOD_TYPE_CONVENTIONAL = 0x00;
    final static int METHOD_TYPE_APACHE = 0x01;
    final static int METHOD_TYPE_FILE_CHANNEL = 0x02;

    private boolean retrieveFileWithFileIO(LGFTPFile remoteFile, int methodType) {
        String sRemoteFileName = remoteFile.getName();
        Log.d(TAG, "sRemoteFileName " + sRemoteFileName);

        String sDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File sTargetDir = new File(sDirPath + "/TPutMonitor");
        File sTargetFile = null;
        if (!sTargetDir.exists()) {
            Log.d(TAG, sTargetDir.getName() + " does not exist, hence make dir");
            sTargetDir.mkdir();
        } else {
             sTargetFile = new File(sTargetDir + "/" + remoteFile.getName());
            if (sTargetFile.exists()) {
                Log.d(TAG, "");
                sTargetFile.delete();
            }
        }

        if (!sTargetDir.canWrite()) {
            Log.e(TAG, "ERROR : Cannot write logs to dir");
            return false;
        }

        sTargetFile = new File(sTargetDir + "/" + sRemoteFileName);
        LGFTPClient.this.mDownloadFilePathAndName = sTargetDir + "/" + sTargetFile.getName();

        switch (methodType) {
            case METHOD_TYPE_CONVENTIONAL:
                return retrieveFileUsingConventionalMethod(remoteFile, sTargetFile);
            case METHOD_TYPE_APACHE:
                return retrieveFileUsingApacheAPI(remoteFile, sTargetFile);
            case METHOD_TYPE_FILE_CHANNEL:
                return retrieveFileUsingFileChannel(remoteFile, sTargetFile);
            default :
                return false;
        }
    }

    private boolean retrieveFileUsingConventionalMethod(LGFTPFile remoteFile, File targetFile) {
        boolean ret = false;
        float sFinalAvgTput = 0.0f;
        FileOutputStream sFileOutputStream = null;
        try {
            sFileOutputStream =  new FileOutputStream(targetFile.getAbsolutePath(), false);
            Log.d(TAG, "retrievingFileStream...");
            mInputStream = this.mFTPClient.retrieveFileStream(remoteFile.getName());
            Log.d(TAG, "file stream retrieved... : " + mInputStream.available());

            Message msg = LGFTPClient.this.mTPutCalculationLoopHandler.obtainMessage(MSG_START_TPUT_CALCULATION_LOOP);
            Bundle b  = new Bundle();
            b.putSerializable(KEY_FILE, targetFile);
            msg.setData(b);

            // 1. initialize control variables.
            LGFTPClient.this.mStartTimeForCalculationHandler = System.currentTimeMillis();
            LGFTPClient.this.mDownloadedBytes = 0;
            LGFTPClient.this.mElapsedTime = 0;

            // 2. start t-put calculation msg loop
            LGFTPClient.this.mTPutCalculationLoopHandler.sendMessage(msg);

            // 3. inform the fragment that file DL has been started.
            LGFTPClient.this.mOperationListener.onDownloadStarted(remoteFile);

            //byte[] sBytesArray = new byte[20971520]; // 20 MBytes
            byte[] sBytesArray = new byte[24576]; // 20 MBytes
            int sBytesRead = -1;
            while ((sBytesRead = mInputStream.read(sBytesArray)) != -1) {
                LGFTPClient.this.mDownloadedBytes += sBytesRead;
                LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - LGFTPClient.this.mStartTimeForCalculationHandler;
                sFileOutputStream.write(sBytesArray, 0, sBytesRead);
            }

            Log.d(TAG, "conventional duration : " + (mElapsedTime/1000.0f));
            sFinalAvgTput = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
            Log.d(TAG, "test loop terminate : " + mShouldTerminateTestLoop);
            if (mShouldTerminateTestLoop) {
                ret = false;
            } else {
                ret = this.mFTPClient.completePendingCommand();
            }

            if (ret) {
                Log.d(TAG, "Download successful");
            } else {
                Log.d(TAG, "Download failed");
            }
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
            try {
                this.mFTPClient.abort();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            LGFTPClient.this.mTPutCalculationLoopHandler.sendEmptyMessage(MSG_STOP_TPUT_CALCULATION_LOOP);
            // 4. invoke callback
            LGFTPClient.this.mOperationListener.onDownloadFinished(ret, targetFile, sFinalAvgTput);

            // 5. initialize variables.
            LGFTPClient.this.mDownloadedBytes = 0;
            LGFTPClient.this.mElapsedTime = 0;
            LGFTPClient.this.mAvgTPut = 0;

            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (sFileOutputStream != null) {
                try {
                    sFileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }

    private boolean retrieveFileUsingApacheAPI(LGFTPFile remoteFile, File targetFile) {
        FileOutputStream sFileOutputStream = null;
        float sFinalAvgTput = 0.0f;
        boolean ret = false;

        try {
            sFileOutputStream =  new FileOutputStream(targetFile.getAbsolutePath(), false);
            mInputStream = this.mFTPClient.retrieveFileStream(remoteFile.getName());

            Message msg = LGFTPClient.this.mTPutCalculationLoopHandler.obtainMessage(MSG_START_TPUT_CALCULATION_LOOP);
            Bundle b  = new Bundle();
            b.putSerializable(KEY_FILE, targetFile);
            msg.setData(b);

            // 1. initialize control variables.
            this.mStartTimeForCalculationHandler = System.currentTimeMillis();
            this.mDownloadedBytes = 0;
            this.mElapsedTime = 0;

            // 2. start t-put calculation msg loop
            this.mTPutCalculationLoopHandler.sendMessage(msg);

            // 3. inform the fragment that file DL has been started.
            this.mOperationListener.onDownloadStarted(remoteFile);

            this.mFTPClient.setCopyStreamListener(new CopyStreamListener() {
                @Override
                public void bytesTransferred(CopyStreamEvent event) {

                }

                @Override
                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                    mDownloadedBytes = totalBytesTransferred;
                    mElapsedTime = System.currentTimeMillis() - mStartTimeForCalculationHandler;
                    mAvgTPut = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
                    LGFTPClient.this.mOperationListener.onDownloadProgressPublished(LGFTPClient.this.mAvgTPut, mDownloadedBytes);
                    Log.d(TAG, "avg tput = " + mAvgTPut + " Mbps");
                }
            });
            this.mFTPClient.retrieveFile(remoteFile.getName(), sFileOutputStream);

            Log.d(TAG, "apache duration : " + (mElapsedTime/1000.0f));
            sFinalAvgTput = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
            this.mFTPClient.setCopyStreamListener(null);

            Log.d(TAG, "test loop terminate : " + mShouldTerminateTestLoop);
            if (mShouldTerminateTestLoop) {
                ret = false;
            } else {
                ret = this.mFTPClient.completePendingCommand();
            }

            if (ret) {
                Log.d(TAG, "Download successful");
            } else {
                Log.d(TAG, "Download failed");
            }
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
            LGFTPClient.this.mTPutCalculationLoopHandler.sendEmptyMessage(MSG_STOP_TPUT_CALCULATION_LOOP);
            // 4. invoke callback
            LGFTPClient.this.mOperationListener.onDownloadFinished(ret, targetFile, sFinalAvgTput);

            // 5. initialize variables.
            LGFTPClient.this.mDownloadedBytes = 0;
            LGFTPClient.this.mElapsedTime = 0;
            LGFTPClient.this.mAvgTPut = 0;

            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (sFileOutputStream != null) {
                try {
                    sFileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }

    private boolean retrieveFileUsingFileChannel(LGFTPFile remoteFile, File targetFile) {
        boolean ret = false;
        float sFinalAvgTput = 0.0f;
        try {
            mInputStream = this.mFTPClient.retrieveFileStream(remoteFile.getName());

            // 2. start t-put calculation msg loop
            Message msg = LGFTPClient.this.mTPutCalculationLoopHandler.obtainMessage(MSG_START_TPUT_CALCULATION_LOOP);
            Bundle b  = new Bundle();
            b.putSerializable(KEY_FILE, targetFile);
            msg.setData(b);
            LGFTPClient.this.mTPutCalculationLoopHandler.sendMessage(msg);

            // 3. inform the fragment that file DL has been started.
            LGFTPClient.this.mOperationListener.onDownloadStarted(remoteFile);

            // 4. perform download and write
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(targetFile, "rw")) {
                // move the cursor to the end of the file
                randomAccessFile.seek(randomAccessFile.length());
                // obtain the a file channel from the RandomAccessFile
                try (
                        FileChannel fileChannel = randomAccessFile.getChannel();
                        ReadableByteChannel inputChannel = Channels.newChannel(mInputStream)
                ) {
                    mDownloadProgressCheckHandler.sendEmptyMessage(MSG_DOWNLOAD_PROGRESS_START_CHECK);
                    Log.d(TAG, "transfer started");
                    LGFTPClient.this.mStartTimeForCalculationHandler = System.currentTimeMillis();
                    LGFTPClient.this.mDownloadedBytes = fileChannel.transferFrom(inputChannel, 0, remoteFile.getSize());
                    LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - mStartTimeForCalculationHandler;
                    Log.d(TAG, "File channel duration : " + (mElapsedTime/1000.0f));
                    sFinalAvgTput = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
                    Log.d(TAG, "transfer completed.");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "e " + e.getMessage());
                } finally {
                    mDownloadProgressCheckHandler.sendEmptyMessage(MSG_DOWNLOAD_PROGRESS_TERMINATE_CHECK);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "e2 " + e.getMessage());
            }

            // 5. retrieve result.
            Log.d(TAG, "test loop terminate : " + mShouldTerminateTestLoop);
            if (mShouldTerminateTestLoop) {
                ret = false;
            } else {
                ret = this.mFTPClient.completePendingCommand();
            }

            if (ret) {
                Log.d(TAG, "Download successful");
            } else {
                Log.d(TAG, "Download failed");
            }
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
            // 6. invoke callback
            LGFTPClient.this.mOperationListener.onDownloadFinished(ret, targetFile, sFinalAvgTput);

            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            LGFTPClient.this.mTPutCalculationLoopHandler.sendEmptyMessage(MSG_STOP_TPUT_CALCULATION_LOOP);
        }

        return ret;
    }

    /*@TargetApi(Build.VERSION_CODES.O)
    private boolean retrieveFileWithFileIOViaAsynchronousFileChannel(LGFTPFile targetFile, int dummy) {
        Log.d(TAG, "retrieveFileWithFileIOViaAsynchronousFileChannel " + targetFile);

        String sRemoteFileName = targetFile.getName();
        Log.d(TAG, "sRemoteFileName " + sRemoteFileName);

        String sDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        File sTargetDir = new File(sDirPath + "/TPutMonitor");
        if (!sTargetDir.exists()) {
            Log.d(TAG, sTargetDir.getName() + " does not exist, hence make dir");
            sTargetDir.mkdir();
        } else {
            File sTargetFile = new File(sTargetDir + "/" + targetFile.getName());
            if (sTargetFile.exists()) {
                Log.d(TAG, "");
                sTargetFile.delete();
            }
        }

        if (!sTargetDir.canWrite()) {
            Log.e(TAG, "ERROR : Cannot write logs to dir");
            //throw new Exception("File cannot be written");
        }

        File sDownloadFile = new File(sTargetDir + "/" + sRemoteFileName);
        LGFTPClient.this.mDownloadFilePathAndName = sTargetDir + "/" + sRemoteFileName;

        try {
            mInputStream = this.mFTPClient.retrieveFileStream(sRemoteFileName);
            ReadableByteChannel inputChannel = Channels.newChannel(mInputStream);
            AsynchronousFileChannel outputChannel = AsynchronousFileChannel.open(
                    sDownloadFile.toPath(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            outputChannel.read()

            ByteBuffer buffer = ByteBuffer.allocate(100 * 1024 * 1024);
            int position = 0;
            int recievedBytes = 0;
            Future<Integer> lastWrite = null;

            while ((recievedBytes = inputChannel.read(buffer)) >= 0
                    || buffer.position() != 0) {
                Log.d(TAG, "Recieved bytes: " + recievedBytes);
                Log.d(TAG, "Buffer position: " + buffer.position());
                buffer.flip();
                lastWrite = outputChannel.write(buffer, position);
                // do extra work while asynchronous channel is writing bytes to disk,
                // in perfect case more extra work can be done, not just simple calculations
                position += recievedBytes;
                // extra work is done, we should wait, because we use only one buffer which can be still busy
                if (lastWrite != null)  lastWrite.get();
                buffer.compact();
            }

            outputChannel.close();
            inputChannel.close();
            mInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return true;
    }
    */

    private boolean retrieveFileWithoutFileIO(LGFTPFile targetFile) {
        boolean ret = false;

        Log.d(TAG, "retrieveFileWithoutFileIO " + targetFile);

        String sRemoteFileName = targetFile.getName();
        Log.d(TAG, "sRemoteFileName " + sRemoteFileName);

        mInputStream = null;
        float sFinalAvgTput = 0.0f;
        try {
            Log.d(TAG, "************************************************************");
            Log.d(TAG, "download started bufferSize = " + this.mFTPClient.getBufferSize() + " bytes");

            mInputStream = this.mFTPClient.retrieveFileStream(sRemoteFileName);

            Message msg = LGFTPClient.this.mTPutCalculationLoopHandler.obtainMessage(MSG_START_TPUT_CALCULATION_LOOP);
            Bundle b  = new Bundle();
            b.putSerializable(KEY_FILE, targetFile);
            msg.setData(b);

            // 1. initialize control variables.
            LGFTPClient.this.mStartTimeForCalculationHandler = System.currentTimeMillis();
            LGFTPClient.this.mDownloadedBytes = 0;
            LGFTPClient.this.mElapsedTime = 0;

            // 2. start t-put calculation msg loop
            LGFTPClient.this.mTPutCalculationLoopHandler.sendMessage(msg);

            // 3. inform the fragment that file DL has been started.
            LGFTPClient.this.mOperationListener.onDownloadStarted(targetFile);

            //byte[] sBytesArray = new byte[20971520]; // 20 MBytes
            byte[] sBytesArray = new byte[24576]; // 20 MBytes
            int sBytesRead = -1;
            while ((sBytesRead = mInputStream.read(sBytesArray)) != -1) {
                LGFTPClient.this.mDownloadedBytes += sBytesRead;
                LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - LGFTPClient.this.mStartTimeForCalculationHandler;
            }

            sFinalAvgTput = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
            Log.d(TAG, "test loop terminate : " + mShouldTerminateTestLoop);
            if (mShouldTerminateTestLoop) {
                ret = false;
            } else {
                ret = this.mFTPClient.completePendingCommand();
            }

            if (ret) {
                Log.d(TAG, "Download successful");
            } else {
                Log.d(TAG, "Download failed");
            }
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
            LGFTPClient.this.mTPutCalculationLoopHandler.sendEmptyMessage(MSG_STOP_TPUT_CALCULATION_LOOP);
            // 4. invoke callback
            File sDownloadFile = new File(sRemoteFileName);
            LGFTPClient.this.mOperationListener.onDownloadFinished(ret, sDownloadFile, sFinalAvgTput);

            // 5. initialize variables.
            LGFTPClient.this.mDownloadedBytes = 0;
            LGFTPClient.this.mElapsedTime = 0;
            LGFTPClient.this.mAvgTPut = 0;

            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }



    public String printWorkingDirectory() throws IOException {
        return this.mFTPClient.printWorkingDirectory();
    }

    public boolean stopDownloadAndCancelTheRest() {

        Log.d(TAG, "force closing inputStream.");
        /*try {
            mInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "force closing inputStream.");
                try {

                    Log.d(TAG, "closing result = " + mFTPClient.abort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return true;
    }
}
