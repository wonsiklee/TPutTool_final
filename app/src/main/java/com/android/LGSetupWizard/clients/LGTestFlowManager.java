package com.android.LGSetupWizard.clients;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class LGTestFlowManager extends Handler {

    private static String TAG = LGTestFlowManager.class.getSimpleName();

    private static LGTestFlowManager mInstance;

    private ArrayList<ILGTestFlowController> mLGTestFlowControllerList;

    // test in progress variables
    private Queue<ILGTestFlowController> mLGTestFlowControllerQueue;
    private ILGTestFlowController  mCurrentClient;

    @Setter private boolean mShouldAbort;

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


    private static final int PROGRESS_PUBLISH_INTERVAL = 1000;

    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE = 0x00;
    public static final int RESPONSE_MSG_PREPARATION_FINISHED = 0x01;

    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST = 0x03;
    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_ABORT_TEST = 0x04;
    public static final int RESPONSE_MSG_TEST_STARTED = 0x05;

    private static final int CONTROL_MSG_REQUEST_CLIENT_TO_PUBLISH_PROGRESS = 0x06;

    public static final int REPORT_MSG_TEST_TERMINATED = 0x10;

    @Override
    public void handleMessage(Message msg) {
        Log.d(TAG, "msg : " + msg.toString());
        switch (msg.what) {
            case CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE:
                this.mCurrentClient.prepareToLaunch();
                break;
            case RESPONSE_MSG_PREPARATION_FINISHED:
                this.sendEmptyMessage(CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST);
                break;

            case CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST:
                this.mCurrentClient.launch();
                break;
            case CONTROL_MSG_NOTIFY_CLIENT_TO_ABORT_TEST:
                this.mCurrentClient.abort();
                this.mShouldAbort = true;
                break;
            case RESPONSE_MSG_TEST_STARTED:
                this.sendEmptyMessageDelayed(CONTROL_MSG_REQUEST_CLIENT_TO_PUBLISH_PROGRESS, PROGRESS_PUBLISH_INTERVAL);
                break;

            case CONTROL_MSG_REQUEST_CLIENT_TO_PUBLISH_PROGRESS:
                this.mCurrentClient.requestToPublishProgress();
                this.sendEmptyMessageDelayed(CONTROL_MSG_REQUEST_CLIENT_TO_PUBLISH_PROGRESS, PROGRESS_PUBLISH_INTERVAL);
                break;

            case REPORT_MSG_TEST_TERMINATED:
                boolean sResult = msg.arg1 == 0 ? false : true;
                if (sResult && !mShouldAbort) {
                    if (this.mLGTestFlowControllerQueue.size() != 0) { // if there are clients left to run tests.
                        this.mCurrentClient = this.mLGTestFlowControllerQueue.poll();
                        this.sendEmptyMessageDelayed(CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE, 3000);
                    } else {
                        // all client test terminated.
                    }
                } else {
                    // just finish.
                }

                mShouldAbort = false;
                break;
        }
    }

    private LGTestFlowManager() {
        this.mLGTestFlowControllerList = new ArrayList<>();
        this.mLGTestFlowControllerQueue = new LinkedList<>();
        this.mCurrentClient = null;
    }

    public static LGTestFlowManager getInstance() {
        if (mInstance == null) {
            mInstance = new LGTestFlowManager();
        }
        return mInstance;
    }

    public void startTestFlow() {
        this.mShouldAbort = false;
        for (ILGTestFlowController con: this.mLGTestFlowControllerList) {
            this.mLGTestFlowControllerQueue.offer(con);
        }

        this.mCurrentClient = this.mLGTestFlowControllerQueue.poll();
        this.sendEmptyMessage(CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE);
    }

    public void stopTestFlow() {
        this.mShouldAbort = true;

    }

    /**
     * register test flow controller.
     * Client's own implementation will be executed.
     */
    public void registerTestController(ILGTestFlowController testFlowController) {
        this.mLGTestFlowControllerList.add(testFlowController);
    }

    public void unregisterTestController(ILGTestFlowController testFlowController) {
        this.mLGTestFlowControllerList.remove(testFlowController);
    }

    /**
     * Call this when a client wants to publish its current test progress,
     * which means clients call interval will be progress publish interval to the main UI.
     */
    public void publishProgress(int currentCount, int remainingCount, float progress, float currentTput, float avgTput) {

    }
}