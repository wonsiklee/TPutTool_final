package com.android.LGSetupWizard.service;

/**
 * Created by wonsik.lee on 2017-03-16.
 */

public interface IDeviceMonitoringStateChangedListener {
    void onDeviceMonitoringLoopStarted();
    void onDeviceMonitoringLoopStopped();
    void onDeviceRecordingStarted();
    void onDeviceRecordingStopped(float cutoffTPut);
}
