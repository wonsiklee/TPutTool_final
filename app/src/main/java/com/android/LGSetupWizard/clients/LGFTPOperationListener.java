package com.android.LGSetupWizard.clients;

import android.support.annotation.Nullable;

import com.android.LGSetupWizard.data.LGFTPFile;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by wonsik.lee on 2018-01-01.
 */

public interface LGFTPOperationListener {
    // server connection
    void onConnectToServerFinished(@Nullable ArrayList<LGFTPFile> fileNames);
    void onDisconnectToServerFinished();

    // download
    void onDownloadProgressPublished(float progress);
    void onDownloadStarted();
    void onDownloadFinished(boolean result, File file);

    // file exploring
    void onGetFileListFinished(ArrayList<LGFTPFile> fileList);
    void onChangeDirectoryFinished(ArrayList<LGFTPFile> fileList);
}
