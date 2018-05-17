package com.android.LGSetupWizard.clients;

/**
 * Created by yunsik.lee on 2018-05-03.
 */

public interface LGHTTPClient {
    int BUFFER_SIZE = 2 * 1024 * 1024;
    void startHTTPDownload(String fileAddr, boolean enableFileIO);
    void stopDownload();
    void publishCurrentTPut();
    void setOnStateChangedListener(LGHTTPDownloadStateChangeListener listener);
}