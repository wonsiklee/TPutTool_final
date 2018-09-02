package com.android.LGSetupWizard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.HashMap;
import java.util.logging.LogRecord;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class TestFlowManager {
    private static String TAG = TestFlowManager.class.getSimpleName();

    private static TestFlowManager mInstance;
    private static Context mContext;
    private HashMap<ClientType, ITestFlowController> mTestTriggerMap;

    private boolean mShouldKeepGoing;

    public enum ClientType {
        TEST_FTP,
        TEST_IPERF,
        TEST_HTTP
    };

    // FTP config values;
    @Getter @Setter private boolean mUsingFTPFileIO;
    @Getter @Setter private int mFTPBufferSize;
    @Getter @Setter private int mFTPRepeatCount;
    @Getter @Setter private int mFTPRepeatInterval;
    @Getter @Setter private boolean mUsingFTPPSV;
    @Getter @Setter private boolean mUsingPv4EPSV;

    // iPerf config values
    // TODO : please add necessary configuration values for auto test

    // HTTP config values
    // TODO : please add necessary configuration values for auto test

    @SuppressLint("HandlerLeak")
    private Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private TestFlowManager(Context context) {
        this.mTestTriggerMap = new HashMap<>();
        this.mShouldKeepGoing = true;
    }

    public static TestFlowManager getInstance(Context context) {
        mContext = context;
        if (mInstance != null) {
            mInstance = new TestFlowManager(context);
        }
        return mInstance;
    }



    public void launch() {
        for (ClientType clientType : ClientType.values()) {
            ITestFlowController sTestController = this.mTestTriggerMap.get(clientType);
            if (sTestController != null && this.mShouldKeepGoing) {
                sTestController.prepareToLaunch();
                sTestController.launch();
            }
        }
    }

    /**
     * This will abort the entire test flow.
     */
    public void emergencyAbort() {
        this.mShouldKeepGoing = false;
    }

    /**
     * register test flow controller.
     * Client's own implementation will be executed.
     */
    public void registerTestController(ClientType clientType, ITestFlowController testFlowController) {
        this.mTestTriggerMap.put(clientType, testFlowController);
    }

    /**
     * Call this when a client wants to publish its current test progress,
     * which means clients call interval will be progress publish interval to the main UI.
     */
    public void publishProgress(ClientType clientType, int currentCount, int remainingCount, float progress, float currentTput, float avgTput) {

    }
}