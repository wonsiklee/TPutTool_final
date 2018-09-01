package com.android.LGSetupWizard;

import android.content.Context;

import java.util.HashMap;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class TestFlowManager {
    private static TestFlowManager mInstance;
    private static Context mContext;

    public enum ClientType {
        TEST_FTP,
        TEST_IPERF,
        TEST_HTTP
    };

    // FTP config values;
    @Getter private boolean mUsingFTPFileIO;
    @Getter private int mFTPBufferSize;
    @Getter private int mFTPRepeatCount;
    @Getter private int mFTPRepeatInterval;
    @Getter private boolean mUsingFTPPSV;
    @Getter private boolean mUsingPv4EPSV;

    // iPerf config values
    // TODO : please add necessary configuration values for auto test

    // HTTP config values
    // TODO : please add necessary configuration values for auto test


    private HashMap<ClientType, ITestController> mTestTriggerMap;

    private TestFlowManager(Context context) {
        this.mTestTriggerMap = new HashMap<>();
    }

    public static TestFlowManager getInstance(Context context) {
        mContext = context;
        if (mInstance != null) {
            mInstance = new TestFlowManager(context);
        }
        return mInstance;
    }

    public void registerTestTrigger(ClientType clientType, ITestController testTrigger) {
        this.mTestTriggerMap.put(clientType, testTrigger);
    }

    public void runTriggers() {
        for (ClientType clientType : ClientType.values()) {
            ITestController sTestController = this.mTestTriggerMap.get(clientType);
            if (sTestController != null) {
                sTestController.prepareToLaunch();
                sTestController.launch();
            }
        }
    }
}