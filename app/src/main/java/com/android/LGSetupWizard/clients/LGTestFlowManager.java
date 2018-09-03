package com.android.LGSetupWizard.clients;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class LGTestFlowManager extends Handler {
    private static String TAG = LGTestFlowManager.class.getSimpleName();

    private static LGTestFlowManager mInstance;

    private Context mContext;
    private HashMap<ClientType, ILGTestFlowController> mTestTriggerMap;

    private boolean mShouldKeepGoing;

    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE = 0x01;
    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST = 0x02;
    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_IMMEDIATE_ABORT = 0x03;
    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_PUBLISH_PROGRESS = 0x04;

    public static final int RESPONSE_MSG_PREPARATION_FINISHED = 0x05;
    public static final int RESPONSE_MSG_TEST_STARTED = 0x06;
    public static final int RESPONSE_MSG_ABORT_FINISHED = 0x07;
    public static final int RESPONSE_MSG_PUBLISH_PROGRESS = 0x08;

    public enum ClientType {
        TEST_FTP,
        TEST_IPERF,
        TEST_HTTP
    }

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

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE:
                break;
            case CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST:
                break;
            case CONTROL_MSG_NOTIFY_CLIENT_TO_IMMEDIATE_ABORT:
                break;
            case CONTROL_MSG_NOTIFY_CLIENT_TO_PUBLISH_PROGRESS:
                break;

            case RESPONSE_MSG_PREPARATION_FINISHED:
                break;
            case RESPONSE_MSG_TEST_STARTED:
                break;
            case RESPONSE_MSG_ABORT_FINISHED:
                break;
            case RESPONSE_MSG_PUBLISH_PROGRESS:
                break;
        }
    }

    private LGTestFlowManager(Context context) {
        this.mContext = context;
        this.mTestTriggerMap = new HashMap<>();
        this.mShouldKeepGoing = true;
    }

    public static LGTestFlowManager getInstance(Context context) {
        if (mInstance != null) {
            mInstance = new LGTestFlowManager(context);
        }
        return mInstance;
    }

    public void launch() {
        for (ClientType clientType : ClientType.values()) {
            ILGTestFlowController sTestController = this.mTestTriggerMap.get(clientType);
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
    public void registerTestController(ClientType clientType, ILGTestFlowController testFlowController) {
        this.mTestTriggerMap.put(clientType, testFlowController);
    }

    /**
     * Call this when a client wants to publish its current test progress,
     * which means clients call interval will be progress publish interval to the main UI.
     */
    public void publishProgress(ClientType clientType, int currentCount, int remainingCount, float progress, float currentTput, float avgTput) {

    }
}