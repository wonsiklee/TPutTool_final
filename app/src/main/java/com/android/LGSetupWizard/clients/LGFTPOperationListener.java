package com.android.LGSetupWizard.clients;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.android.LGSetupWizard.data.LGFTPFile;
import com.android.LGSetupWizard.data.MediaScanning;
import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.ui.fragments.LGFTPFragment;

import java.io.File;
import java.util.ArrayList;

public class LGFTPOperationListener implements ILGFTPOperationListener {
    private static String TAG = LGFTPOperationListener.class.getSimpleName();

    @Override
    public void onConnectToServerFinished(boolean result, ArrayList<LGFTPFile> fileList) {
        Log.d(TAG, "onConnectToServerFinished(boolean, ArrayList<FTPFile>) : " + result);
    }

    @Override
    public void onChangeWorkingDirectoryFinished(ArrayList<LGFTPFile> fileList) {
        Log.d(TAG, "onChangeWorkingDirectoryFinished()");
    }

    @Override
    public void onDisconnectToServerFinished() {
        Log.d(TAG, "onDisconnectToServerFinished()");
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
    public void onDownloadFinished(boolean result, File file, float avgTPut) {
        Log.d(TAG, "onDownloadFinished() " + result + ", " + file.toString());
    }
}
