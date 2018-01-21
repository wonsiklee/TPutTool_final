package com.lge.tputmaster;

// Declare any non-default types here with import statements

interface IDeviceMonitoringServiceCallback {
        void onMonitoringStarted();
        void onMonitoringStopped();
        void onRecordingStarted();
        void onRecordingStopped(float overallTput, long duration, long totalTxBytes, long totalRxBytes, int callCount);
}
