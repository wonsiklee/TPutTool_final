package com.android.LGSetupWizard.data;

import com.android.LGSetupWizard.service.DeviceMonitoringStateChangedListener;
import com.android.LGSetupWizard.statsreader.CPUStatsReader;
import com.android.LGSetupWizard.statsreader.NetworkStatsReader;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.v4.util.CircularArray;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by wonsik.lee on 2017-03-14.
 */

@Accessors(prefix = "m")
public class DeviceStatsInfoStorageManager implements DeviceMonitoringStateChangedListener {
    private static final String TAG = DeviceStatsInfoStorageManager.class.getSimpleName();

    private static boolean DBG = true;

    @Setter private static long mInstantaneousTputCalculationTimeLength;

    private static final String LINE_FEED = "\n";
    private static final String CARRIAGE_RETURN = "\r";
    private static final String SEPERATOR = ",";
    private static final String FILE_EXTENSION = ".csv";
    private static final String SW_VERION_PROPERTY = "ro.lge.swversion_short";

    private static SimpleDateFormat mDateTimeFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static String[] mColumns = null;

    private Context mContext;

    private static DeviceStatsInfoStorageManager mInstance;

    // actual data will be stored in the following buffer
    @Getter private LinkedList<DeviceStatsInfo> mDeviceStatsRecordList;
    private CircularArray<DeviceStatsInfo> mInstantaneousTPutCircularArray;

    private long mPivotRxBytes = Long.MIN_VALUE;
    private long mPivotTxBytes = Long.MIN_VALUE;

    private int mCpuCnt = -1; //initializing

    private String mFileName = "";
    private int mCallCount;

    private ExecutorService mExecutorService = null;

    public enum TEST_TYPE {
        DL_TEST, UL_TEST
    }

    public enum THERMAL_TYPE {
        THERMAL_XO, THERMAL_VTS
    }

    private DeviceStatsInfoStorageManager(Context context) {
        this.mDeviceStatsRecordList = new LinkedList<>();
        this.mInstantaneousTPutCircularArray = new CircularArray<>();
        this.mContext = context;
        this.mCallCount = 1;
    }

    public static DeviceStatsInfoStorageManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DeviceStatsInfoStorageManager(context);
        }
        return mInstance;
    }

    private float getTputBetweenTwoPoints(DeviceStatsInfo first, DeviceStatsInfo second) {
        // returns tput value between first and second points
        Log.d(TAG, "getTputBetweenTwoPoints(first, second) START");

        long sUnitTime = second.getTimeStamp() - first.getTimeStamp();
        Log.d(TAG, "sUnitTime : " + sUnitTime + " ms");
        long sSize = 0;
        if (first.getDirection() == TEST_TYPE.DL_TEST) {
            Log.d(TAG, "TestType is DL");
            sSize = second.getRxBytes();
        } else {
            Log.d(TAG, "TestType is UL");
            sSize = second.getTxBytes();
        }

        float sTput = (sSize / 1000.0f / 1000 * 8) / (sUnitTime / 1000.0f);
        Log.d(TAG, "sTput : " + sTput + " Mbps");
        Log.d(TAG, "getTputBetweenTwoPoints(first, second) END");
        return sTput;
    }

    private void calibrateRecordList(float cutOffTput) {
        // drop cutoff-below values from the recording list.
        Log.d(TAG, "calibrateRecordList() ENTER");
        Log.d(TAG, "this.mDeviceStatsRecordList.size() : " + this.mDeviceStatsRecordList.size());
        LinkedList<DeviceStatsInfo> refList = this.mDeviceStatsRecordList;
        this.mDeviceStatsRecordList = new LinkedList<>();

        Log.d(TAG, "cutOffTput : " + cutOffTput + " Mbps");
        for (int i = 1; i != refList.size(); i++) {
            Log.d(TAG, "index : " + i);
            float t = getTputBetweenTwoPoints(refList.get(i - 1), refList.get(i));
            Log.d(TAG, "returned tput : " + t + " Mbps");
            if (t > cutOffTput) {
                Log.d(TAG, "adding refList.get(" + i + ") to mDeviceStatsRecordList" );
                Log.d(TAG, "rxBytes : " + refList.get(i).getRxBytes() + " bytes");
                this.mDeviceStatsRecordList.add(refList.get(i));
                Log.d(TAG, "increased size : " + this.mDeviceStatsRecordList.size());
            }
        }

        Log.d(TAG, "final size : " + this.mDeviceStatsRecordList.size());
    }

    /* file exporting related methods STARTS */
    private int exportToFile(final String fileName, float cutOffTput) {
        Log.d(TAG, "exportToFile(), fileName: " + fileName);
        this.calibrateRecordList(cutOffTput);
        Log.d(TAG, "target size : " + this.mDeviceStatsRecordList.size());
        final LinkedList<DeviceStatsInfo> sTargetList = this.mDeviceStatsRecordList;
        this.mInstantaneousTPutCircularArray = new CircularArray<>();
        this.mDeviceStatsRecordList = new LinkedList<>();

        if (sTargetList.size() <= 0) {
            Log.d(TAG, "No record to export");
            return -1;
        } else {
            Log.d(TAG, "Exporting size : " + sTargetList.size());
        }
        //search how many cpu cores exist...
        mCpuCnt = sTargetList.getFirst().getCpuCurFrequencyList().size();

        //exception handling if there is no data...
        try {
            if (sTargetList == null || sTargetList.getFirst() == null) {}
        } catch (NoSuchElementException e) {
            Log.d(TAG, "sTargetList is null or First element is not exist.");
            return -1;
        }  catch (Exception e) {
            return -1;
        }

        //1. create thread pool
        mExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        makeColumns();

        //2. make task to work
        Runnable run = new Runnable() {
            @Override
            public void run() {
                handleFileWriting(sTargetList, fileName);
            }
        };

        //3. submit task to thread pool
        Future sFuture = mExecutorService.submit(run);

        //4. receive result of task
        try {
            sFuture.get(); //if return value were null, it will be good.
            Log.d(TAG, "File writing is completed.");
        } catch (InterruptedException | ExecutionException e) {
            Log.d(TAG, "InterruptedException or ExecutionException, e.getMessage: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, "Exception. e.getMessage: " + e.getMessage());
            e.printStackTrace();
        }

        //5. free thread pool
        mExecutorService.shutdown();

        return 0;
    }

    private void handleFileWriting(LinkedList<DeviceStatsInfo> targetList, String fileName) {
        String sDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        Iterator<DeviceStatsInfo> sIterator = targetList.iterator();

        byte[] sBuffer = null;
        FileOutputStream sFos = null;
        BufferedOutputStream sBos = null;

        boolean sIsMadeDir = false;
        StringBuilder sb = new StringBuilder();
        int cnt = 0; //initializing

        //1. make directory
        File sDir = new File(sDirPath+"/TputTracingApp_Logs");
        if (!sDir.exists()) {
            sIsMadeDir = sDir.mkdir();
        }
        Log.d(TAG, "sIsMadeDir: " + sIsMadeDir + ", Directory path for log files: " + sDirPath + "/TputTracingApp_Logs");

        if (!sDir.canWrite()) {
            Log.d(TAG, "Cannot write logs to dir");
        }

        //2. make file to write raw data
        File sFile = new File(sDir, fileName + FILE_EXTENSION);
        boolean isExistFile = sFile.exists();

        //3. prepare OutputStream and BufferedOutputStream to write logs to file
        try {
            if (isExistFile)
                sFos = new FileOutputStream(sFile, true); //add logs to already exist file
            else
                sFos = new FileOutputStream(sFile); //add logs to new file

            //to speed up file I/O
            sBos = new BufferedOutputStream(sFos);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "FileNotFoundException, e.getMessage(): " + e.getMessage());
            e.printStackTrace();
            return;
        } catch (Exception e) {
            Log.d(TAG, "Exception, e.getMessage(): " + e.getMessage());
            e.printStackTrace();
            return;
        }

        //For media scanning.
        //After file writing is completed, we should notify file's information to android system to see result file through file browser.
        MediaScanning sMediaScanning = new MediaScanning(mContext, sFile);

        //4. write raw data using BufferedOutputStream to file created before
        try {
            if (!isExistFile) {
                //first, write columns to file.
                sBos.write(makeColumnNameAsByte());
                sBos.flush();
            }

            //second, write each row's data to file.
            while (sIterator.hasNext()) {
                ++cnt;
                DeviceStatsInfo sDeviceStatsInfo = sIterator.next();
                sBuffer = getEachRowDataAsByte(sDeviceStatsInfo, cnt);
                sBos.write(sBuffer, 0, sBuffer.length);
                sBos.flush();
            }
            this.flushStoredData();
        } catch (IOException e) {
            Log.d(TAG, "IOException, e.getMessage(): " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.d(TAG, "Exception, e.getMessage(): " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sBos != null) {
                try {
                    sBos.flush(); sFos.close();
                } catch (IOException e) {e.printStackTrace();}
            }
        }
    }

    private void makeColumns() {
        StringBuilder sSb = new StringBuilder();
        sSb.append("No").append(SEPERATOR)
                .append("CallCnt").append(SEPERATOR)
                .append("PackageName").append(SEPERATOR)
                .append("Network").append(SEPERATOR)
                .append("Direction").append(SEPERATOR)
                .append("Time").append(SEPERATOR)
                .append("ReceivedBytes").append(SEPERATOR)
                .append("SentBytes").append(SEPERATOR)
                .append("Temperature").append(SEPERATOR)
                .append("CPU_Usage(%)").append(SEPERATOR);

        for (int i = 0; i < mCpuCnt; i++) {
            sSb.append("CPU_CUR_Freq" + i).append(SEPERATOR).append("CPU_MAX_Freq" + i).append(SEPERATOR);
        }
        mColumns = sSb.toString().split(SEPERATOR);
    }

    private byte[] makeColumnNameAsByte() {
        StringBuilder sColumns = new StringBuilder();

        for (int i=0; i<mColumns.length; i++) {
            sColumns.append(mColumns[i]).append(SEPERATOR);
        }
        sColumns.append(CARRIAGE_RETURN).append(LINE_FEED);
        return sColumns.toString().getBytes();
    }

    private byte[] getEachRowDataAsByte(DeviceStatsInfo deviceStatsInfo, int cnt) {
        StringBuilder sSb = new StringBuilder();
        String sDirection;

        if (deviceStatsInfo == null)
            return new byte[0];

        sDirection = (deviceStatsInfo.getDirection() == TEST_TYPE.DL_TEST ) ? "DL" : "UL";

        sSb.append(String.valueOf(cnt)).append(SEPERATOR)
                .append(deviceStatsInfo.getCallCnt()).append(SEPERATOR)
                .append(deviceStatsInfo.getPackageName()).append(SEPERATOR)
                .append(deviceStatsInfo.getNetworkType()).append(SEPERATOR)
                .append(sDirection).append(SEPERATOR)
                //.append(getDate(deviceStatsInfo.getTimeStamp())).append(SEPERATOR)
                .append(deviceStatsInfo.getTimeStamp()).append(SEPERATOR)
                .append(String.valueOf(deviceStatsInfo.getRxBytes())).append(SEPERATOR)
                .append(String.valueOf(deviceStatsInfo.getTxBytes())).append(SEPERATOR)
                .append(deviceStatsInfo.getCpuTemperature()).append(SEPERATOR)
                .append(deviceStatsInfo.getCpuUsage()).append(SEPERATOR);

        for (int i=0; i<mCpuCnt; i++) {
            sSb.append(deviceStatsInfo.getCpuCurFrequencyList().get(i)).append(SEPERATOR).append(deviceStatsInfo.getCpuMaxFrequencyList().get(i)).append(SEPERATOR);
        }
        sSb.append(CARRIAGE_RETURN).append(LINE_FEED);

        return sSb.toString().getBytes();
    }

    private static String getDate(long milliSeconds) {
        Calendar sCalendar = Calendar.getInstance();
        sCalendar.setTimeInMillis(milliSeconds);
        return mDateTimeFormatter.format(sCalendar.getTime());
    }
    /* file exporting related methods ENDS */

    public void addToStorage(DeviceStatsInfo deviceStatsInfo) {
        try {
            DeviceStatsInfo sDeviceStatsInfo = deviceStatsInfo.clone();
            if (this.mDeviceStatsRecordList.size() == 0) { // if it's the first element.
                this.mPivotTxBytes = sDeviceStatsInfo.getTxBytes();
                this.mPivotRxBytes = sDeviceStatsInfo.getRxBytes();
                sDeviceStatsInfo.setTxBytes(0);
                sDeviceStatsInfo.setRxBytes(0);
            } else {
                long sTempRx = sDeviceStatsInfo.getRxBytes();
                long sTempTx = sDeviceStatsInfo.getTxBytes();
                sDeviceStatsInfo.setTxBytes(sTempTx - this.mPivotTxBytes);
                sDeviceStatsInfo.setRxBytes(sTempRx - this.mPivotRxBytes);
                this.mPivotTxBytes = sTempTx;
                this.mPivotRxBytes = sTempRx;
            }
            this.mDeviceStatsRecordList.add(sDeviceStatsInfo);
        } catch(Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    public void addToTPutCalculationBuffer(DeviceStatsInfo deviceStatsInfo) {
        if ((this.mInstantaneousTPutCircularArray.size() > 0) &&
            ((this.mInstantaneousTPutCircularArray.getLast().getTimeStamp() - this.mInstantaneousTPutCircularArray.getFirst().getTimeStamp()) >= (mInstantaneousTputCalculationTimeLength - 300))) {
                this.mInstantaneousTPutCircularArray.popFirst();
        }
        this.mInstantaneousTPutCircularArray.addLast(deviceStatsInfo);
        Log.d(TAG, "TPut Calculation buffer : " + this.mInstantaneousTPutCircularArray.size());
    }

    public void migrateFromTPutCalculationBufferToRecordBuffer() {
        Log.d(TAG, "migrateFromTPutCalculationBufferToRecordBuffer()");

        Log.d(TAG, "calculationBufferSIze : " + this.mInstantaneousTPutCircularArray.size());
        Log.d(TAG, "recordBufferSize : " + this.mDeviceStatsRecordList.size());

        for (int i = 0; i != this.mInstantaneousTPutCircularArray.size() - 1; ++i) {
            try{
                this.addToStorage(this.mInstantaneousTPutCircularArray.get(i).clone());

                Log.d(TAG, i + " : " + this.mInstantaneousTPutCircularArray.get(i).getTimeStamp() + " , " + this.mInstantaneousTPutCircularArray.get(i).getRxBytes() + ")");
            } catch(Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    public long getTimeLengthOfInstantaneousTputBuffer() {
        if (this.mInstantaneousTPutCircularArray.size() <= 0) {
            return 0L;
        }
        return this.mInstantaneousTPutCircularArray.getLast().getTimeStamp() - this.mInstantaneousTPutCircularArray.getFirst().getTimeStamp();
    }

    public float getInstantaneousTput(TEST_TYPE type) {
        float sTput = 0.0f;

        long sBytes = 0;
        if (this.mInstantaneousTPutCircularArray.size() > 0) {
            if (this.mInstantaneousTPutCircularArray.getFirst().hashCode() != this.mInstantaneousTPutCircularArray.getLast().hashCode()) {
                long sDuration = this.mInstantaneousTPutCircularArray.getLast().getTimeStamp() - this.mInstantaneousTPutCircularArray.getFirst().getTimeStamp();

                if (type == TEST_TYPE.DL_TEST) {
                    sBytes = this.mInstantaneousTPutCircularArray.getLast().getRxBytes() - this.mInstantaneousTPutCircularArray.getFirst().getRxBytes();
                } else {
                    sBytes = this.mInstantaneousTPutCircularArray.getLast().getTxBytes() - this.mInstantaneousTPutCircularArray.getFirst().getTxBytes();
                }

                sTput = (sBytes / 1024 / 1024 * 8)/(sDuration / 1000.0f);
            }
        }
        return sTput;
    }

    public float getOverallAvgTput(TEST_TYPE type) {
        long targetBytes = (type == TEST_TYPE.DL_TEST) ? this.getCurrentTestTotalRxBytes(): this.getCurrentTestTotalTxBytes();

        Log.d(TAG, "divided by 1024 : " + (targetBytes * 8.0f /1024/1024) / (this.getCurrentTestDurationTime(type) / 1000));
        Log.d(TAG, "divided by 1000 : " + (targetBytes * 8.0f /1000/1000) / (this.getCurrentTestDurationTime(type) / 1000));

        return (targetBytes * 8.0f /1000/1000) / (this.getCurrentTestDurationTime(type) / 1000.0f);
    }

    public long getCurrentTestDurationTime(TEST_TYPE type) {
        int sStartIndex = 0;
        int sEndIndex = this.mDeviceStatsRecordList.size() - 1;

        for (int i = 0; i != this.mDeviceStatsRecordList.size(); ++i) {
            if (((type == TEST_TYPE.DL_TEST) ? this.mDeviceStatsRecordList.get(i).getRxBytes() : this.mDeviceStatsRecordList.get(i).getTxBytes()) != 0) {
                sStartIndex = ((i -1) < 0) ? 0 : i -1;
                break;
            }
        }

        if (sStartIndex < 0) {
            sStartIndex = 0;
        }

        for (int i = this.mDeviceStatsRecordList.size() - 1; i >= 0; --i) {
            if (((type == TEST_TYPE.DL_TEST) ? this.mDeviceStatsRecordList.get(i).getRxBytes() : this.mDeviceStatsRecordList.get(i).getTxBytes()) != 0) {
                sEndIndex = i;
                break;
            }
        }

        if (sEndIndex > this.mDeviceStatsRecordList.size()) {
            sEndIndex = this.mDeviceStatsRecordList.size() - 1;
        }

        Log.d(TAG, "******************* Time Duration ********************");
        Log.d(TAG, "StartIndex : " + sStartIndex + " ==> "  + this.mDeviceStatsRecordList.get(sStartIndex).getRxBytes());
        Log.d(TAG, "EndIndex : " + sEndIndex + " == >" + this.mDeviceStatsRecordList.get(sEndIndex).getRxBytes());
        Log.d(TAG, "duration : " + (this.mDeviceStatsRecordList.get(sEndIndex).getTimeStamp() - this.mDeviceStatsRecordList.get(sStartIndex).getTimeStamp()));
        Log.d(TAG, "******************************************************");

        return this.mDeviceStatsRecordList.get(sEndIndex).getTimeStamp() - this.mDeviceStatsRecordList.get(sStartIndex).getTimeStamp();
    }

    public long getCurrentTestTotalTxBytes() {
        long sum = 0;
        for (int i = 0; i != this.mDeviceStatsRecordList.size(); ++i) {
            sum += this.mDeviceStatsRecordList.get(i).getTxBytes();
        }
        return sum;
    }

    public long getCurrentTestTotalRxBytes() {
        long sum = 0;

        Log.d(TAG, "list size : " + this.mDeviceStatsRecordList.size() + "");
        Log.d(TAG, "start index 0, end index : " + (this.mDeviceStatsRecordList.size() -1));
        for (int i = 0; i != this.mDeviceStatsRecordList.size(); ++i) {
            sum += this.mDeviceStatsRecordList.get(i).getRxBytes();
            Log.d(TAG, " mDeviceStatsRecordingList.get(" + i + ") -> " + this.mDeviceStatsRecordList.get(i).getRxBytes() + " bytes");
        }
        Log.d(TAG, "total  : " + sum + " bytes");
        return sum;
    }

    public int getCurrentTestCallCount() {
        return this.mCallCount;
    }


    public DeviceStatsInfo readCurrentDeviceStatsInfoByUid(int targetUid, String cpuTemperatureFilePath, String cpuClockFilePath, String packageName, TEST_TYPE direction, THERMAL_TYPE thermalType) {
        DeviceStatsInfo sDeviceStatsInfo = new DeviceStatsInfo();

        sDeviceStatsInfo.setPackageName(packageName);
        sDeviceStatsInfo.setCallCnt(this.mCallCount);
        sDeviceStatsInfo.setDirection(direction);
        sDeviceStatsInfo.setTimeStamp(System.currentTimeMillis());
        sDeviceStatsInfo.setTxBytes(NetworkStatsReader.getTxBytesByUid(targetUid));
        sDeviceStatsInfo.setRxBytes(NetworkStatsReader.getRxBytesByUid(targetUid));
        sDeviceStatsInfo.setNetworkType(NetworkStatsReader.getNetworkTypeName(NetworkStatsReader.getNetworkType(this.mContext)));
        sDeviceStatsInfo.setCpuTemperature(CPUStatsReader.getThermalInfo(cpuTemperatureFilePath, thermalType));
        sDeviceStatsInfo.setCpuCurFrequencyList(CPUStatsReader.getInstance().getCpuCurFreq(cpuClockFilePath));
        sDeviceStatsInfo.setCpuMaxFrequencyList(CPUStatsReader.getInstance().getCpuMaxFreq(cpuClockFilePath));
        sDeviceStatsInfo.setCpuUsage(CPUStatsReader.getInstance().getCpuUsage());

        return sDeviceStatsInfo;
    }

    public DeviceStatsInfo readCurrentDeviceStatsInfoByTotalBytes(String cpuTemperatureFilePath, String cpuClockFilePath, TEST_TYPE direction, THERMAL_TYPE thermalType) {
        DeviceStatsInfo sDeviceStatsInfo = new DeviceStatsInfo();

        sDeviceStatsInfo.setPackageName("N/A");
        sDeviceStatsInfo.setCallCnt(this.mCallCount);
        sDeviceStatsInfo.setDirection(direction);
        sDeviceStatsInfo.setTimeStamp(System.currentTimeMillis());
        sDeviceStatsInfo.setTxBytes(NetworkStatsReader.getTotalMobileTxBytes());
        sDeviceStatsInfo.setRxBytes(NetworkStatsReader.getTotalMobileRxBytes());
        sDeviceStatsInfo.setNetworkType(NetworkStatsReader.getNetworkTypeName(NetworkStatsReader.getNetworkType(this.mContext)));
        sDeviceStatsInfo.setCpuTemperature(CPUStatsReader.getThermalInfo(cpuTemperatureFilePath, thermalType));
        sDeviceStatsInfo.setCpuCurFrequencyList(CPUStatsReader.getInstance().getCpuCurFreq(cpuClockFilePath));
        sDeviceStatsInfo.setCpuMaxFrequencyList(CPUStatsReader.getInstance().getCpuMaxFreq(cpuClockFilePath));
        sDeviceStatsInfo.setCpuUsage(CPUStatsReader.getInstance().getCpuUsage());

        return sDeviceStatsInfo;
    }

    private static String generateFileName() {
        return getDeviceName() + "_" + getSystemProperty(SW_VERION_PROPERTY, "unknown") + "_" + getDate(System.currentTimeMillis()) + "_" + (TimeZone.getDefault().getID().replace("/", "_"));
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    private void flushStoredData() {
        this.mDeviceStatsRecordList.clear();
    }

    public static String getSystemProperty(String key, String defaultValue) {
        String returnValue = "";
        try {
            @SuppressWarnings("rawtypes")
            Class SystemProperties = Class.forName("android.os.SystemProperties");

            Method getSystemProperty = SystemProperties.getMethod("get",
                    new Class[] {String.class, String.class});
            returnValue = (String)getSystemProperty.invoke(SystemProperties,
                    new Object[] {key, defaultValue});

        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (Exception e) {
            returnValue = defaultValue;
        }
        return returnValue;
    }

    @Override
    public void onDeviceMonitoringLoopStarted() {
        this.mFileName = generateFileName();
        Log.d(TAG, this.mFileName);
        Log.d(TAG, "onDeviceMonitoringLoopStarted()");
    }

    @Override
    public void onDeviceMonitoringLoopStopped() {
        Log.d(TAG, "onDeviceMonitoringLoopStopped()");
    }

    @Override
    public void onDeviceRecordingStarted() {
        Log.d(TAG, "onDeviceRecordingStarted()");
    }

    @Override
    public void onDeviceRecordingStopped(float cutOffTput) {
        Log.d(TAG, "onDeviceRecordingStopped() !!!!!!");

        if (TextUtils.isEmpty(this.mFileName)) {
            Log.d(TAG, "currentFileName is null, hence making one.");
            this.mFileName = generateFileName();
        }
        this.exportToFile(this.mFileName, cutOffTput);
    }
}
