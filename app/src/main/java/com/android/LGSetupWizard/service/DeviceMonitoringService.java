package com.android.LGSetupWizard.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.android.LGSetupWizard.IDeviceMonitoringService;
import com.android.LGSetupWizard.IDeviceMonitoringServiceCallback;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.data.DeviceStatsInfo;
import com.android.LGSetupWizard.data.DeviceStatsInfoStorageManager;

import java.util.ArrayList;
import java.util.LinkedList;

import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class DeviceMonitoringService extends Service {

    private static String TAG = DeviceMonitoringService.class.getSimpleName();

    public static final int SHARED_PREFERENCES_DL_DIRECTION = 0;
    public static final int SHARED_PREFERENCES_UL_DIRECTION = 1;

    public static final int THERMAL_VTS = 1;

    private static final int EVENT_FIRE_UP_MONITORING_LOOP = 0x10;
    private static final int EVENT_TERMINATE_MONITORING_LOOP = 0x11;

    private static final int EVENT_ENTER_IDLE_MONITORING_STATE = 0x12;
    private static final int EVENT_EXIT_IDLE_MONITORING_STATE = 0x13;

    private static final int EVENT_ENTER_RECORDING_STATE = 0x14;
    private static final int EVENT_EXIT_RECORDING_STATE = 0x15;

    private static final int EVENT_READ_DEVICE_STATS = 0x18;
    private static final int EVENT_RECORD_CURRENT_STATS = 0x19;

    private static final int EVENT_FIRE_UP_FORCED_RECORDING_LOOP = 0x20;
    private static final int EVENT_RECORD_STATS_INFO_FORCIBLY = 0x21;
    private static final int EVENT_STOP_FORCED_RECORDING_LOOP = 0x22;

    private ArrayList<DeviceMonitoringStateChangedListener> mDeviceLoggingStateListenerList;

    enum MonitoringType { BY_UID, BY_ALL_TRAFFIC }

    private Handler mServiceLogicHandler = new Handler() {

        private MonitoringType mMonitoringType;

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case EVENT_FIRE_UP_MONITORING_LOOP:
                    this.mMonitoringType = (MonitoringType) msg.obj;
                    Log.d(TAG, "EVENT_FIRE_UP_MONITORING_LOOP arg=" + this.mMonitoringType);
                    for (DeviceMonitoringStateChangedListener l : mDeviceLoggingStateListenerList) {
                        l.onDeviceMonitoringLoopStarted();
                    }

                    int N = mCallbacks.beginBroadcast();

                    for (int i = 0; i < N; ++i) {
                        try {
                            mCallbacks.getBroadcastItem(i).onMonitoringStarted();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    mCallbacks.finishBroadcast();

                    Message sMsg = this.obtainMessage(EVENT_ENTER_IDLE_MONITORING_STATE);
                    sendMessage(sMsg);
                    break;

                case EVENT_TERMINATE_MONITORING_LOOP:
                    Log.d(TAG, "EVENT_TERMINATE_MONITORING_LOOP");
                    if (this.hasMessages(EVENT_RECORD_CURRENT_STATS)) {
                        Log.d(TAG, "it was in logging state, hence calling onDeviceRecordingStopped() mCallbacks");
                        removeMessages(EVENT_RECORD_CURRENT_STATS);
                        removeMessages(EVENT_READ_DEVICE_STATS);
                        sendEmptyMessage(EVENT_EXIT_RECORDING_STATE);
                        sendEmptyMessage(EVENT_EXIT_IDLE_MONITORING_STATE);
                        break;
                    } else {
                        Log.d(TAG, "dddddddddddddddddddddddddddddddd");
                        for (DeviceMonitoringStateChangedListener l : mDeviceLoggingStateListenerList) {
                            l.onDeviceMonitoringLoopStopped();
                        }

                        N = mCallbacks.beginBroadcast();

                        for (int i = 0; i < N; ++i) {
                            try {
                                mCallbacks.getBroadcastItem(i).onMonitoringStopped();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        mCallbacks.finishBroadcast();

                        // remove all messages
                        removeMessages(EVENT_ENTER_IDLE_MONITORING_STATE);
                        removeMessages(EVENT_READ_DEVICE_STATS);
                        removeMessages(EVENT_RECORD_CURRENT_STATS);
                        removeMessages(EVENT_EXIT_RECORDING_STATE);

                        removeMessages(EVENT_FIRE_UP_FORCED_RECORDING_LOOP);
                        removeMessages(EVENT_RECORD_STATS_INFO_FORCIBLY);
                    }
                    break;

                case EVENT_ENTER_IDLE_MONITORING_STATE:
                    Log.d(TAG, "EVENT_ENTER_IDLE_MONITORING_STATE");
                    sendEmptyMessage(EVENT_READ_DEVICE_STATS);

                    Log.d(TAG, "mInstantaneousTputCalculationTimeLength : " + mInstantaneousTputCalculationTimeLength);
                    Log.d(TAG, "Tput threshold value : " + mTputThresholdValue);
                    break;

                case EVENT_EXIT_IDLE_MONITORING_STATE:
                    Log.d(TAG, "EVENT_EXIT_IDLE_MONITORING_STATE ");
                    removeMessages(EVENT_FIRE_UP_MONITORING_LOOP);
                    removeMessages(EVENT_ENTER_IDLE_MONITORING_STATE);
                    removeMessages(EVENT_READ_DEVICE_STATS);
                    removeMessages(EVENT_ENTER_RECORDING_STATE);
                    removeMessages(EVENT_RECORD_CURRENT_STATS);

                    sendEmptyMessage(EVENT_TERMINATE_MONITORING_LOOP);
                    break;

                case EVENT_READ_DEVICE_STATS: {
                    Log.d(TAG, "EVENT_READ_DEVICE_STATS");
                    DeviceStatsInfoStorageManager dsis = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext());


                    DeviceStatsInfo sDeviceStatsInfo = ((this.mMonitoringType == MonitoringType.BY_UID) ?
                            dsis.readCurrentDeviceStatsInfoByUid(mTargetUid, mCPUTemperatureFilePath, mCPUClockFilePath, mTargetPackageName, mDirection, mThermalType) :
                            dsis.readCurrentDeviceStatsInfoByTotalBytes(mCPUTemperatureFilePath, mCPUClockFilePath, mDirection, mThermalType));
                    dsis.addToTPutCalculationBuffer(sDeviceStatsInfo);
                    Log.d(TAG, sDeviceStatsInfo.toString());
                    // if the avg t-put exceeds threshold, it's time to start record.
                    //Log.d(TAG, "Instantaneous t-put for " + dsis.getTimeLengthOfInstantaneousTputBuffer() + " ms : " + dsis.getInstantaneousTput(mDirection) + " Mbps");

                    if (dsis.getInstantaneousTput(mDirection) > mTputThresholdValue) {
                        Message eventMessage = this.obtainMessage(EVENT_ENTER_RECORDING_STATE);
                        eventMessage.obj = sDeviceStatsInfo;
                        sendMessage(eventMessage);
                    } else {
                        sendEmptyMessageDelayed(EVENT_READ_DEVICE_STATS, mLoggingInterval);
                    }
                    break;
                }

                case EVENT_ENTER_RECORDING_STATE:
                    Log.d(TAG, "EVENT_ENTER_RECORDING_STATE");

                    for (DeviceMonitoringStateChangedListener l : mDeviceLoggingStateListenerList) {
                        l.onDeviceRecordingStarted();
                    }

                    N = mCallbacks.beginBroadcast();

                    for (int i = 0; i < N; ++i) {
                        try {
                            mCallbacks.getBroadcastItem(i).onRecordingStarted();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    mCallbacks.finishBroadcast();

                    DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).migrateFromTPutCalculationBufferToRecordBuffer();
                    DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).addToStorage((DeviceStatsInfo) msg.obj);
                    sendEmptyMessageDelayed(EVENT_RECORD_CURRENT_STATS, mLoggingInterval);

                    break;

                case EVENT_RECORD_CURRENT_STATS: {
                    DeviceStatsInfo sDeviceStatsInfo = null;
                    if (this.mMonitoringType == MonitoringType.BY_UID) {
                        sDeviceStatsInfo = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).readCurrentDeviceStatsInfoByUid(mTargetUid, mCPUTemperatureFilePath, mCPUClockFilePath, mTargetPackageName, mDirection, mThermalType);
                    } else {
                        sDeviceStatsInfo = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).readCurrentDeviceStatsInfoByTotalBytes(mCPUTemperatureFilePath, mCPUClockFilePath, mDirection, mThermalType);
                    }

                    DeviceStatsInfoStorageManager dsis = DeviceStatsInfoStorageManager.getInstance(getApplicationContext());
                    long len = dsis.getTimeLengthOfInstantaneousTputBuffer();
                    Log.d(TAG, "EVENT_RECORD_CURRENT_STATS : length= " + len + " ms, " + dsis.getInstantaneousTput(mDirection) + " Mbps");

                    DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).addToTPutCalculationBuffer(sDeviceStatsInfo);
                    DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).addToStorage(sDeviceStatsInfo);

                    if (DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).getInstantaneousTput(mDirection) < mTputThresholdValue) {
                        sendEmptyMessage(EVENT_EXIT_RECORDING_STATE);
                    } else {
                        sendEmptyMessageDelayed(EVENT_RECORD_CURRENT_STATS, mLoggingInterval);
                    }
                    break;
                }

                case EVENT_EXIT_RECORDING_STATE:
                    Log.d(TAG, "EVENT_EXIT_RECORDING_STATE");

                    N = mCallbacks.beginBroadcast();

                    for (int i = 0; i < N; ++i) {
                        try {
                            DeviceStatsInfoStorageManager manager = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext());

                            Log.d(TAG, "*********************** Test Result Start ******************************");
                            LinkedList<DeviceStatsInfo> list = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).getDeviceStatsRecordList();
                            int index = 0;
                            for (DeviceStatsInfo info : list) {
                                Log.d(TAG, "i = " + index++ + ", timeStamp = " + info.getTimeStamp() + ", RxBytes = " + info.getRxBytes() + " bytes");
                            }
                            Log.d(TAG, " ");
                            float tput = manager.getOverallAvgTput(mDirection);
                            long duration = manager.getCurrentTestDurationTime(mDirection);
                            long tx = manager.getCurrentTestTotalTxBytes();
                            long rx = manager.getCurrentTestTotalRxBytes();
                            Log.d(TAG, " ");

                            Log.d(TAG, "rx : " + rx + ", duration : " + duration + " ms");
                            Log.d(TAG, "tput : " + tput + " Mbps");
                            Log.d(TAG, "*********************** Test Result End *************************************");

                            mCallbacks.getBroadcastItem(i).onRecordingStopped(tput, duration, tx, rx, manager.getCurrentTestCallCount());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    mCallbacks.finishBroadcast();

                    for (DeviceMonitoringStateChangedListener l : mDeviceLoggingStateListenerList) {
                        l.onDeviceRecordingStopped(mCutOffTPut);
                    }

                    sendEmptyMessageDelayed(EVENT_ENTER_IDLE_MONITORING_STATE, mLoggingInterval);
                    break;

                /* below this line in the handler, forced recording related messages */
                case EVENT_FIRE_UP_FORCED_RECORDING_LOOP:
                    this.mMonitoringType = (MonitoringType) msg.obj;
                    Log.d(TAG, "EVENT_FIRE_UP_FORCED_RECORDING_LOOP : " + this.mMonitoringType);
                    for (DeviceMonitoringStateChangedListener l : mDeviceLoggingStateListenerList) {
                        l.onDeviceRecordingStarted();
                    }

                    N = mCallbacks.beginBroadcast();

                    for (int i = 0; i < N; ++i) {
                        try {
                            mCallbacks.getBroadcastItem(i).onRecordingStarted();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    mCallbacks.finishBroadcast();

                    sendEmptyMessage(EVENT_RECORD_STATS_INFO_FORCIBLY);
                    break;

                case EVENT_RECORD_STATS_INFO_FORCIBLY: {
                    Log.d(TAG, "EVENT_RECORD_STATS_INFO_FORCIBLY : " + this.mMonitoringType.toString());
                    DeviceStatsInfo sDeviceStatsInfo = null;
                    DeviceStatsInfoStorageManager dsis = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext());
                    if (this.mMonitoringType == MonitoringType.BY_UID) {
                        Log.d(TAG, "mTargetUid : " + mTargetUid);
                        Log.d(TAG, "mTargetPackageName : " + mTargetPackageName);
                        sDeviceStatsInfo = dsis.readCurrentDeviceStatsInfoByUid(mTargetUid, mCPUTemperatureFilePath, mCPUClockFilePath, mTargetPackageName, mDirection, mThermalType);
                    } else {
                        sDeviceStatsInfo = dsis.readCurrentDeviceStatsInfoByTotalBytes(mCPUTemperatureFilePath, mCPUClockFilePath, mDirection, mThermalType);
                    }

                    dsis.addToTPutCalculationBuffer(sDeviceStatsInfo);
                    dsis.addToStorage(sDeviceStatsInfo);
                    Log.d(TAG, "time length : " + dsis.getTimeLengthOfInstantaneousTputBuffer() + " ms");
                    Log.d(TAG, "InstTput : " + dsis.getInstantaneousTput(mDirection) + " Mbps");

                    sendEmptyMessageDelayed(EVENT_RECORD_STATS_INFO_FORCIBLY, mLoggingInterval);
                    break;
                }

                case EVENT_STOP_FORCED_RECORDING_LOOP:
                    Log.d(TAG, "EVENT_STOP_FORCED_RECORDING_LOOP");
                    N = mCallbacks.beginBroadcast();

                    for (int i = 0; i < N; ++i) {
                        try {
                            DeviceStatsInfoStorageManager manager = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext());

                            Log.d(TAG, "*********************** Test Result Start ******************************");
                            LinkedList<DeviceStatsInfo> list = DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).getDeviceStatsRecordList();
                            int index = 0;
                            for (DeviceStatsInfo info : list) {
                                Log.d(TAG, "i = " + index++ + ", timeStamp = " + info.getTimeStamp() + ", RxBytes = " + info.getRxBytes() + " bytes");
                            }
                            Log.d(TAG, " ");
                            float tput = manager.getOverallAvgTput(mDirection);
                            long duration = manager.getCurrentTestDurationTime(mDirection);
                            long tx = manager.getCurrentTestTotalTxBytes();
                            long rx = manager.getCurrentTestTotalRxBytes();
                            Log.d(TAG, " ");
                            Log.d(TAG, " ");
                            Log.d(TAG, "rx : " + rx + ", duration : " + duration + "");
                            Log.d(TAG, "tput : " + tput + " Mbps");
                            Log.d(TAG, "*********************** Test Result End *************************************");

                            mCallbacks.getBroadcastItem(i).onRecordingStopped(tput, duration, tx, rx, manager.getCurrentTestCallCount());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    mCallbacks.finishBroadcast();

                    for (DeviceMonitoringStateChangedListener l : mDeviceLoggingStateListenerList) {
                        l.onDeviceRecordingStopped(mCutOffTPut);
                    }

                    this.removeMessages(EVENT_FIRE_UP_FORCED_RECORDING_LOOP);
                    this.removeMessages(EVENT_RECORD_STATS_INFO_FORCIBLY);
                    break;
                default:
                    break;
            }
        }
    };

    @Setter
    private long mLoggingInterval;
    @Setter
    private String mTargetPackageName;
    @Setter
    private int mTargetUid;
    @Setter
    private String mCPUClockFilePath;
    @Setter
    private String mCPUTemperatureFilePath;
    @Setter
    private long mInstantaneousTputCalculationTimeLength;
    @Setter
    private float mTputThresholdValue;
    @Setter
    private DeviceStatsInfoStorageManager.TEST_TYPE mDirection;
    @Setter
    private DeviceStatsInfoStorageManager.THERMAL_TYPE mThermalType;
    @Setter
    private float mCutOffTPut;

    private boolean mIsInMonitoring = false;
    private boolean mIsForciblyRecording = false;

    // constructor
    public DeviceMonitoringService() {
        Log.d(TAG, "DeviceMonitoringService()");
        this.mLoggingInterval = 1000;
    }

    public void setOnLoggingStateChangedListener(DeviceMonitoringStateChangedListener dlsc) {
        if (this.mDeviceLoggingStateListenerList == null) {
            this.mDeviceLoggingStateListenerList = new ArrayList<>();
        }
        this.mDeviceLoggingStateListenerList.add(dlsc);
        Log.d(TAG, "setOnLoggingStateChangedListener()");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        this.setOnLoggingStateChangedListener(DeviceStatsInfoStorageManager.getInstance(this.getApplicationContext()));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        this.mServiceLogicHandler = null;
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");
        if (this.mServiceLogicHandler.hasMessages(EVENT_FIRE_UP_FORCED_RECORDING_LOOP) ||
                this.mServiceLogicHandler.hasMessages(EVENT_RECORD_STATS_INFO_FORCIBLY)) {
            this.mServiceLogicHandler.sendEmptyMessage(EVENT_STOP_FORCED_RECORDING_LOOP);
        } else if (this.mServiceLogicHandler.hasMessages(EVENT_FIRE_UP_MONITORING_LOOP) ||
                this.mServiceLogicHandler.hasMessages(EVENT_ENTER_IDLE_MONITORING_STATE) ||
                this.mServiceLogicHandler.hasMessages(EVENT_ENTER_RECORDING_STATE) ||
                this.mServiceLogicHandler.hasMessages(EVENT_READ_DEVICE_STATS) ||
                this.mServiceLogicHandler.hasMessages(EVENT_RECORD_CURRENT_STATS)) {
            this.mServiceLogicHandler.sendEmptyMessage(EVENT_TERMINATE_MONITORING_LOOP);
        }
        return super.onUnbind(intent);
    }

    // monitoring controller
    private void startMonitoringDeviceStats(MonitoringType monitoringType) {
        Log.d(TAG, "monitoringType=" + monitoringType);
        Message sMsg = this.mServiceLogicHandler.obtainMessage();
        sMsg.what = EVENT_FIRE_UP_MONITORING_LOOP;
        sMsg.obj = monitoringType;
        this.mServiceLogicHandler.sendMessage(sMsg);
    }

    private void startRecordingDeviceStatsImmediately(MonitoringType monitoringType) {
        Message sMsg = this.mServiceLogicHandler.obtainMessage();
        sMsg.what = EVENT_FIRE_UP_FORCED_RECORDING_LOOP;
        sMsg.obj = monitoringType;
        this.mServiceLogicHandler.sendMessage(sMsg);
    }

    RemoteCallbackList<IDeviceMonitoringServiceCallback> mCallbacks = new RemoteCallbackList<>();

    private IDeviceMonitoringService.Stub mBinder = new IDeviceMonitoringService.Stub() {

        @Override
        public boolean registerCallback(IDeviceMonitoringServiceCallback callback) throws RemoteException {
            Log.d(TAG, "registering callback " + callback);
            boolean flag = false;
            Log.d(TAG, callback.toString());
            if (callback != null) {
                flag = mCallbacks.register(callback);
                Log.d(TAG, "registered callback count : " + mCallbacks.getRegisteredCallbackCount() + "");
            }
            return flag;
        }

        @Override
        public void unregisterCallback(IDeviceMonitoringServiceCallback callback) throws RemoteException {
            if (callback != null) mCallbacks.unregister(callback);
        }

        @Override
        public void fireUpByUidMonitoringLoop(String packageName, long interval, String cpuFreqPath, String cpuThermalPath, long instantaneousTputCalculationTime, float tputThresholdValue, int direction, int thermalType, float cutOffTPut) {
            Log.d(TAG, "PackageName : " + packageName);
            Log.d(TAG, "SamplingInterval : " + interval);
            Log.d(TAG, "CPU Freq path : " + cpuFreqPath);
            Log.d(TAG, "CPU Thermal path : " + cpuThermalPath);
            Log.d(TAG, "instantaneous tput time length : " + instantaneousTputCalculationTime);
            Log.d(TAG, "Direction : " + direction);
            Log.d(TAG, "thermalType : " + thermalType);

            setTargetPackageName(packageName);
            setTargetUid(DeviceMonitoringService.getUidByPackageName(getApplicationContext(), DeviceMonitoringService.this.mTargetPackageName));
            setCPUClockFilePath(cpuFreqPath);
            setCPUTemperatureFilePath(cpuThermalPath);
            setLoggingInterval(interval);
            setInstantaneousTputCalculationTimeLength(instantaneousTputCalculationTime);
            setTputThresholdValue(tputThresholdValue);
            DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).setInstantaneousTputCalculationTimeLength(mInstantaneousTputCalculationTimeLength);
            setDirection((direction == SHARED_PREFERENCES_DL_DIRECTION) ? DeviceStatsInfoStorageManager.TEST_TYPE.DL_TEST : DeviceStatsInfoStorageManager.TEST_TYPE.UL_TEST);
            setThermalType((thermalType == THERMAL_VTS) ? DeviceStatsInfoStorageManager.THERMAL_TYPE.THERMAL_VTS : DeviceStatsInfoStorageManager.THERMAL_TYPE.THERMAL_XO);
            setCutOffTPut(cutOffTPut);

            mIsInMonitoring = true;
            startMonitoringDeviceStats(MonitoringType.BY_UID);
        }

        @Override
        public void fireUpByTotalTrafficMonitoringLoop(long interval, String cpuFreqPath, String cpuThermalPath, long instantaneousTputCalculationTime, float tputThresholdValue, int direction, int thermalType, float cutOffTPut) {
            Log.d(TAG, "fireUpByTotalTrafficMonitoringLoop()");
            Log.d(TAG, "PackageName : " + "N/A");
            Log.d(TAG, "SamplingInterval : " + interval);
            Log.d(TAG, "CPU Freq path : " + cpuFreqPath);
            Log.d(TAG, "CPU Thermal path : " + cpuThermalPath);
            Log.d(TAG, "instantaneous tput time length : " + instantaneousTputCalculationTime);
            Log.d(TAG, "Direction : " + direction);
            Log.d(TAG, "thermalType : " + thermalType);

            setTargetPackageName("N/A");
            setTargetUid(DeviceMonitoringService.getUidByPackageName(getApplicationContext(), DeviceMonitoringService.this.mTargetPackageName));
            setCPUClockFilePath(cpuFreqPath);
            setCPUTemperatureFilePath(cpuThermalPath);
            setLoggingInterval(interval);
            setInstantaneousTputCalculationTimeLength(instantaneousTputCalculationTime);
            setTputThresholdValue(tputThresholdValue);
            DeviceStatsInfoStorageManager.getInstance(DeviceMonitoringService.this.getApplicationContext()).setInstantaneousTputCalculationTimeLength(mInstantaneousTputCalculationTimeLength);
            setDirection((direction == SHARED_PREFERENCES_DL_DIRECTION) ? DeviceStatsInfoStorageManager.TEST_TYPE.DL_TEST : DeviceStatsInfoStorageManager.TEST_TYPE.UL_TEST);
            setThermalType((thermalType == THERMAL_VTS) ? DeviceStatsInfoStorageManager.THERMAL_TYPE.THERMAL_VTS : DeviceStatsInfoStorageManager.THERMAL_TYPE.THERMAL_XO);
            setCutOffTPut(cutOffTPut);

            mIsInMonitoring = true;
            startMonitoringDeviceStats(MonitoringType.BY_ALL_TRAFFIC);
        }


        @Override
        public void fireUpForcedRecordingLoop(String packageName, long interval, String cpuFreqPath, String cpuThermalPath, int direction, int thermalType, float cutOffTPut) {
            Log.d(TAG, "PackageName : " + packageName);
            Log.d(TAG, "SamplingInterval : " + interval);
            Log.d(TAG, "CPU Freq path : " + cpuFreqPath);
            Log.d(TAG, "CPU Thermal path : " + cpuThermalPath);
            Log.d(TAG, "Direction : " + direction);
            Log.d(TAG, "thermalType : " + thermalType);

            setTargetPackageName(packageName);
            setTargetUid(DeviceMonitoringService.getUidByPackageName(getApplicationContext(), DeviceMonitoringService.this.mTargetPackageName));
            setCPUClockFilePath(cpuFreqPath);
            setCPUTemperatureFilePath(cpuThermalPath);
            setLoggingInterval(interval);
            setDirection((direction == SHARED_PREFERENCES_DL_DIRECTION) ? DeviceStatsInfoStorageManager.TEST_TYPE.DL_TEST : DeviceStatsInfoStorageManager.TEST_TYPE.UL_TEST);
            setThermalType((thermalType == THERMAL_VTS) ? DeviceStatsInfoStorageManager.THERMAL_TYPE.THERMAL_VTS : DeviceStatsInfoStorageManager.THERMAL_TYPE.THERMAL_XO);
            setCutOffTPut(cutOffTPut);

            mIsForciblyRecording = true;
            startRecordingDeviceStatsImmediately((getString(R.string.str_package_name_all_traffic).equals(packageName)) ? MonitoringType.BY_ALL_TRAFFIC : MonitoringType.BY_UID);
        }

        @Override
        public void finishByTotalTrafficMonitoringLoop() {
            Log.d(TAG, "finishByTotalTrafficMonitoringLoop()");
            finishByUidMonitoringLoop();
        }

        @Override
        public void finishByUidMonitoringLoop() {
            Log.d(TAG, "finishMonitoringLoop()");
            mIsInMonitoring = false;
            mServiceLogicHandler.sendEmptyMessage(EVENT_TERMINATE_MONITORING_LOOP);
        }

        @Override
        public boolean isInMonitoringState() throws RemoteException {
            return mIsInMonitoring;
        }

        @Override
        public boolean isInForcibleRecordingState() throws RemoteException {
            return mIsForciblyRecording;
        }

        @Override
        public void finishForcedRecordingLoop() {
            mIsForciblyRecording = false;
            mServiceLogicHandler.removeMessages(EVENT_RECORD_STATS_INFO_FORCIBLY);
            mServiceLogicHandler.removeMessages(EVENT_FIRE_UP_FORCED_RECORDING_LOOP);
            mServiceLogicHandler.sendEmptyMessage(EVENT_STOP_FORCED_RECORDING_LOOP);
        }
    };

    // static method
    private static int getUidByPackageName(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA).uid;
        } catch (NameNotFoundException e) {
            return -1;
        }
    }
}
