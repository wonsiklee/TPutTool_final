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
    private LGFTPOperationListener mOperationListener;

    public LGFTPClient(LGFTPOperationListener operationListener) {
        this.mFTPClient = new FTPClient();
        this.mOperationListener = operationListener;
    }

    //
    public void connectToServer(final String serverAddress, final int portNum, final String userID, final String password) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                ArrayList<LGFTPFile> fileList = null;
                Log.d(TAG, "connectToServer() " + serverAddress);
                try {
                    mFTPClient.connect(serverAddress, portNum);
                    int reply = mFTPClient.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        mFTPClient.disconnect();
                        Log.d(TAG, "connection failed, FTPReply code : " + reply);
                    } else {
                        Log.d(TAG, "successfully connected");
                        if (loginToServer(userID, password)) {
                            Log.d(TAG, "Logged in successfully");
                            fileList = nonThreadicGetFileList();
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
        }.start();
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

    private ArrayList<LGFTPFile> nonThreadicGetFileList() {
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

    @Getter private String mCurrentWorkingDirectory = "/";

    public void changeWorkingDirectory(final String path) {

        try {
            mFTPClient.changeWorkingDirectory(path);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mOperationListener.onChangeDirectoryFinished(nonThreadicGetFileList());
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

    final static private int MSG_START_LOOP = 0x00;
    final static private int MSG_CACULATE_TPUT = 0x01;
    final static private int MSG_STOP_LOOP = 0x02;

    private Handler mTputCalculationLoopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_LOOP:
                    sendEmptyMessage(MSG_CACULATE_TPUT);
                    mOperationListener.onDownloadStarted(msg.getData().getLong("file_size"));
                    break;
                case MSG_CACULATE_TPUT:
                    Log.d(TAG, "mDownloadedBytes = " + mDownloadedBytes + ", mElapsedTime = " + ((float) mElapsedTime/ 1000) + " secs");
                    LGFTPClient.this.mAvgTPut = ((float)mDownloadedBytes * 8 / 1024 / 1024)/((float) mElapsedTime / 1000);
                    Log.d(TAG, "Avg TPut : " + LGFTPClient.this.mAvgTPut + " Mbps");
                    mOperationListener.onDownloadProgressPublished(LGFTPClient.this.mAvgTPut, mDownloadedBytes);
                    sendEmptyMessageDelayed(MSG_CACULATE_TPUT, 1000);
                    break;
                case MSG_STOP_LOOP:
                    if (this.hasMessages(MSG_START_LOOP)) {
                        this.removeMessages(MSG_START_LOOP);
                    }
                    if (this.hasMessages(MSG_CACULATE_TPUT)) {
                        this.removeMessages(MSG_CACULATE_TPUT);
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
    public boolean retrieveFileOutputStream(LGFTPFile targetFile) throws Exception {
        boolean ret = false;
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
        try {
            this.mFTPClient.setCopyStreamListener(new CopyStreamListener() {
                @Override
                public void bytesTransferred(CopyStreamEvent event) {
                    Log.d(TAG, "event : " + event);
                }

                @Override
                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                    //Log.d(TAG, "total bytes : " + totalBytesTransferred + ", bytesTransferred : " + bytesTransferred);
                    LGFTPClient.this.mDownloadedBytes = totalBytesTransferred;
                    LGFTPClient.this.mElapsedTime = System.currentTimeMillis() - LGFTPClient.this.mStartTime;
                }
            });
            Log.d(TAG, "************************************************************");
            Log.d(TAG, "download started bufferSize = " + this.mFTPClient.getBufferSize() + " bytes");
            sOutputStream = new BufferedOutputStream(new FileOutputStream(sDownloadFile));

            this.mStartTime = System.currentTimeMillis();
            Message msg = LGFTPClient.this.mTputCalculationLoopHandler.obtainMessage(MSG_START_LOOP);
            Bundle b  = new Bundle();
            b.putLong("file_size", targetFile.getSize());
            msg.setData(b);
            //LGFTPClient.this.mTputCalculationLoopHandler.sendEmptyMessage(MSG_START_LOOP);
            LGFTPClient.this.mTputCalculationLoopHandler.sendMessage(msg);
            ret = this.mFTPClient.retrieveFile(sRemoteFileName, sOutputStream);
            if (ret) {
                Log.d(TAG, "downloaded successfully");
            } else {
                Log.d(TAG, "Download failed");
            }
            Log.d(TAG, "************************************************************");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (FTPConnectionClosedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            LGFTPClient.this.mTputCalculationLoopHandler.sendEmptyMessage(MSG_STOP_LOOP);
            if (sOutputStream != null) {
                try {
                    sOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mOperationListener.onDownloadFinished(ret, sDownloadFile);
            }
        }
        return ret;
    }

    public void retrieveFileUsingFileStream(LGFTPFile targetFile) {
        String remoteFile2 = targetFile.getName();
        File downloadFile2 = new File("D:/Downloads/song.mp3");
        OutputStream outputStream2 = null;
        InputStream inputStream = null;
        try {
            outputStream2 = new BufferedOutputStream(new FileOutputStream(downloadFile2));
            inputStream = this.mFTPClient.retrieveFileStream(remoteFile2);
            byte[] bytesArray = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(bytesArray)) != -1) {
                outputStream2.write(bytesArray, 0, bytesRead);
            }

            boolean success = this.mFTPClient.completePendingCommand();
            if (success) {
                Log.d(TAG, "File #2 has been downloaded successfully.");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream2 != null) {
                    outputStream2.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String printWorkingDirectory() throws IOException {
        return this.mFTPClient.printWorkingDirectory();
    }
}
