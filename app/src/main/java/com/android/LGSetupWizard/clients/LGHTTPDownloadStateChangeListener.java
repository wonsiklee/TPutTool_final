package com.android.LGSetupWizard.clients;

/**
 * Created by yunsik.lee on 2018-05-03.
 */

public interface LGHTTPDownloadStateChangeListener {
    void onDownloadStarted();
    void onDownloadFinished(long totalSize, long totalDuration);
    void onTPutPublished(float tput);
}