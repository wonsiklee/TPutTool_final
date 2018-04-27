package com.android.LGSetupWizard.clients;

import android.util.Log;

import com.android.LGSetupWizard.data.LGFTPFile;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2018-01-01.
 */
@Accessors(prefix = "m")
public class LGFTPClient {
    static final private String TAG = LGFTPClient.class.getSimpleName();

    public FTPClient mFTPClient;
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
                        }
                    }
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
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    logoutFromServer();
                    mFTPClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mOperationListener.onDisconnectToServerFinished();
            }
        }.start();

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
            this.mFTPClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        //return new ArrayList<>(Arrays.asList(lgftpFiles));
        return retArray;
    }

    @Getter private String mCurrentWorkingDirectory = "/";

    public void changeWorkingDirectory(final String path) {
        new Thread() {
            @Override
            public void run() {
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
                super.run();
            }
        }.start();
    }
}
