package com.android.LGSetupWizard.clients;

/**
 * Created by yunsik.lee on 2018-05-03.
 */

public interface LGHTTPClient {
    void startHTTPDownload(String fileAddr, boolean enableFileIO);
    void stopDownload();
    void publishAvgTPut();
    void setOnStateChangedListener(LGHTTPDownloadStateChangeListener listener);
}