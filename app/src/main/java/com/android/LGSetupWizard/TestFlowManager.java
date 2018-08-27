package com.android.LGSetupWizard;

import android.content.Context;

import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class TestFlowManager {
    private static TestFlowManager mInstance;
    private static Context mContext;

    private TestFlowManager(Context context) {

    }

    public static TestFlowManager getInstance(Context context) {
        mContext = context;
        if (mInstance != null) {
            mInstance = new TestFlowManager(context);
        }

        return mInstance;
    }
}
