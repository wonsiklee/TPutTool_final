// IDeviceMonitoringService.aidl
package com.android.LGSetupWizard;

// Declare any non-default types here with import statements
import com.android.LGSetupWizard.IDeviceMonitoringServiceCallback;

interface IDeviceMonitoringService {
    boolean registerCallback(IDeviceMonitoringServiceCallback callback);
    void unregisterCallback(IDeviceMonitoringServiceCallback callback);

    void fireUpByUidMonitoringLoop(String packageName, long interval, String cpuFreqPath, String cpuThermalPath, long instantaneousTputCalculationTime, float tputThresholdValue, int direction, int thermalType, float cutOffTPut);
    void finishByUidMonitoringLoop();

    void fireUpByTotalTrafficMonitoringLoop(long interval, String cpuFreqPath, String cpuThermalPath, long instantaneousTputCalculationTime, float tputThresholdValue, int direction, int thermalType, float cutOffTPut);
    void finishByTotalTrafficMonitoringLoop();

    void fireUpForcedRecordingLoop(String packageName, long interval, String cpuFreqPath, String cpuThermalPath, int direction, int thermalType, float cutOffTPut);
    void finishForcedRecordingLoop();

    boolean isInMonitoringState();
    boolean isInForcibleRecordingState();
}