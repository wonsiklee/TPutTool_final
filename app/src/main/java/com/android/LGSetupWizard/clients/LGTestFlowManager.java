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
    public static final int RESPONSE_MSG_PREPARATION_COMPLETED = 0x01;

    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST = 0x03;
    private static final int CONTROL_MSG_NOTIFY_CLIENT_TO_ABORT_TEST = 0x04;
    public static final int RESPONSE_MSG_TEST_STARTED = 0x05;

    private static final int CONTROL_MSG_READ_PROGRESS = 0x06;

    private static final int CONTROL_MSG_TRIGGER_TEST_FLOW = 0x20;

    public static final int REPORT_MSG_TEST_FINISHED = 0x10;

    private void clearMessages() {
        this.removeMessages(CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE);
        this.removeMessages(RESPONSE_MSG_PREPARATION_COMPLETED);
        this.removeMessages(CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST);
        this.removeMessages(RESPONSE_MSG_TEST_STARTED);
        this.removeMessages(CONTROL_MSG_NOTIFY_CLIENT_TO_ABORT_TEST);
        this.removeMessages(REPORT_MSG_TEST_FINISHED);
        this.removeMessages(CONTROL_MSG_READ_PROGRESS);
    }
    @Override
    public void handleMessage(Message msg) {
        Log.d(TAG, "msg : " + msg.toString());
        switch (msg.what) {
            case CONTROL_MSG_TRIGGER_TEST_FLOW: // trigger test.
                if (this.mLGTestFlowControllerQueue.isEmpty()) {
                    Log.d(TAG, "No client to test.");
                    this.clearMessages();
                    break;
                } else {
                    this.mCurrentClient = this.mLGTestFlowControllerQueue.poll();
                }

                this.sendEmptyMessage(CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE);
                break;

            case CONTROL_MSG_NOTIFY_CLIENT_TO_PREPARE:
                this.mCurrentClient.prepareToLaunch();
                break;
            case RESPONSE_MSG_PREPARATION_COMPLETED: {
                // response received.
                this.sendEmptyMessage(CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST);
                break;
            }
            case CONTROL_MSG_NOTIFY_CLIENT_TO_START_TEST:
                // let the client launch.
                this.mCurrentClient.launch();
                break;
            case RESPONSE_MSG_TEST_STARTED:
                // when client acked back that the test is started, start to read progress every interval seconds.
                this.sendEmptyMessageDelayed(CONTROL_MSG_READ_PROGRESS, PROGRESS_PUBLISH_INTERVAL);
                break;

            case CONTROL_MSG_READ_PROGRESS:
                // TODO : need to implement what information should be acquired.
                this.mCurrentClient.getProgress();

                this.sendEmptyMessageDelayed(CONTROL_MSG_READ_PROGRESS, PROGRESS_PUBLISH_INTERVAL);
                break;

            case CONTROL_MSG_NOTIFY_CLIENT_TO_ABORT_TEST:
                // in case user cancel the test. call the abort method.
                // the client will answer back here with REPORT_MSG_TEST_FINISHED. it will
                this.mCurrentClient.abort();
                // set the abort flag to true to avoid trigger the next client test flow.
                this.mShouldAbort = true;
                break;

            // either test finished or aborted.
            // client should send this message
            case REPORT_MSG_TEST_FINISHED: {
                boolean sResult = msg.arg1 != 0;
                // clear msgs.
                this.clearMessages();

                // if the result was successful
                if (sResult) {
                    Log.d(TAG, "Test result ok");
                } else { // if not
                    Log.d(TAG, "Test result false");
                }

                // was it aborting case?
                if (!mShouldAbort) {
                    // if not, proceed to the next client.
                    // if there's no client left to test. it will be handled in the CONTROL_MSG_TRIGGER_TEST_FLOW
                    this.sendEmptyMessageDelayed(CONTROL_MSG_TRIGGER_TEST_FLOW, 3000);
                } else {
                    // if it wasn't a abort case, just finish the test handle loop.
                    // TODO : UI control msg should be implemented.
                }

                // reset the abort case flag.
                mShouldAbort = false;
                break;
            }
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

        // 1. enqueue clients flow controllers to the queue
        for (ILGTestFlowController con: this.mLGTestFlowControllerList) {
            this.mLGTestFlowControllerQueue.offer(con);
        }

        // 2. send message to trigger the test.
        this.sendEmptyMessage(CONTROL_MSG_TRIGGER_TEST_FLOW);
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