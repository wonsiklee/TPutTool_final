package com.android.LGSetupWizard.clients;

import android.support.annotation.Nullable;

import com.android.LGSetupWizard.data.LGFTPFile;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by wonsik.lee on 2018-01-01.
 */

public interface ILGFTPOperationListener {
    // server connection
    void onConnectToServerFinished(@Nullable ArrayList<LGFTPFile> fileNames);
    void onDisconnectToServerFinished();

    // download
    void onDownloadProgressPublished(float tputValue, long downloadedBytes);
    void onDownloadStarted(LGFTPFile fileName);
    void onDownloadFinished(boolean wasSuccessful, File file, float avgTPut);

    // file exploring
    void onChangeWorkingDirectoryFinished(ArrayList<LGFTPFile> fileList);
}
