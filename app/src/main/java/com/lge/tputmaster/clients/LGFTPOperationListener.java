package com.lge.tputmaster.clients;

import android.support.annotation.Nullable;
import org.apache.commons.net.ftp.FTPFile;

import java.util.ArrayList;

/**
 * Created by wonsik.lee on 2018-01-01.
 */

public interface LGFTPOperationListener {
    // server connection
    void onConnectToServerFinished(@Nullable ArrayList<FTPFile> fileNames);
    void onDisconnectToServerFinished();

    // download
    void onDownloadProgressPublished(float progress);
    void onDownloadStarted();
    void onDownloadFinished();

    // file exploring
    void onGetFileListFinished(ArrayList<FTPFile> fileList);
    void onChangeDirectoryFinished();
}
