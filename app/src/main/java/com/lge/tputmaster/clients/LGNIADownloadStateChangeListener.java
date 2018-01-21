package com.lge.tputmaster.clients;

/**
 * Created by wonsik.lee on 2017-06-07.
 */

public interface LGNIADownloadStateChangeListener {
    void onDownloadStarted();
    void onDownloadFinished(long totalSize, long totalDuration);
    void onTPutPublished(float tput);
}