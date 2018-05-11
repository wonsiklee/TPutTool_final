package com.android.LGSetupWizard.data;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
@AllArgsConstructor(suppressConstructorProperties = true)
public class DeviceStatsInfo {

    @Setter @Getter private long mTimeStamp;

    /* Network Stats */
    @Setter @Getter private long mTxBytes;
    @Setter @Getter private long mRxBytes;

    /* CPU Info */
    @Setter @Getter private ArrayList<Integer> mCpuCurFrequencyList;
    @Setter @Getter private ArrayList<Integer> mCpuMaxFrequencyList;
    @Setter @Getter private float mCpuTemperature;

    /* CPU Usage */
    @Setter @Getter private float mCpuUsage;

    /* Package Name */
    @Setter @Getter private String mPackageName;

    /* Network Type */
    @Setter @Getter private String mNetworkType;

    /* Call Count */
    @Setter @Getter private int mCallCnt;

    /* Direction */
    @Setter @Getter private IDeviceStatsInfoStorageManager.TEST_TYPE mDirection;

    public DeviceStatsInfo() {
        this.mCpuCurFrequencyList = new ArrayList<>();
        this.mCpuMaxFrequencyList = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CallCount : ").append(this.mCallCnt).append("\n");
        sb.append("Time : ").append(this.mTimeStamp).append("\n");
        sb.append("Tx : ").append(this.mTxBytes);
        sb.append(", Rx : ").append(this.mRxBytes).append("\n");
        if (this.mCpuCurFrequencyList != null) {
            for (int i = 0; i < this.mCpuCurFrequencyList.size(); ++i) {
                sb.append("CPU [").append(i).append("] (Current/Max)=(").append(this.mCpuCurFrequencyList.get(i)).append("/").append(this.mCpuMaxFrequencyList.get(i)).append(")").append(" MHz\n");
            }
        }

        sb.append("Temperature : ").append(this.mCpuTemperature).append("\n");
        sb.append("Usage : ").append(this.mCpuUsage).append(" %\n");
        sb.append("Direction : ").append(this.mDirection).append("\n");
        sb.append("NetworkType : ").append(this.mNetworkType).append("\n");
        sb.append("PackageName : ").append(this.mPackageName).append("\n");

        return sb.toString();
    }

    @Override
    public DeviceStatsInfo clone() throws CloneNotSupportedException {
        DeviceStatsInfo cloned =  new DeviceStatsInfo();
        cloned.mTimeStamp = this.mTimeStamp;
        cloned.mCallCnt = this.mCallCnt;
        cloned.mTxBytes = this.mTxBytes;
        cloned.mRxBytes = this.mRxBytes;
        cloned.mCpuCurFrequencyList = (ArrayList<Integer>) this.mCpuCurFrequencyList.clone();
        cloned.mCpuMaxFrequencyList = (ArrayList<Integer>) this.mCpuMaxFrequencyList.clone();
        cloned.mCpuTemperature = this.mCpuTemperature;
        cloned.mCpuUsage = this.mCpuUsage;
        cloned.mDirection = this.mDirection;
        cloned.mNetworkType = this.mNetworkType;
        cloned.mPackageName = this.mPackageName;
        return cloned;
    }
}
