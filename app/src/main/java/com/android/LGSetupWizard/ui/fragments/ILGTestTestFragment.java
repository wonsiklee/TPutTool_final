package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.data.LGTestFlowConfigurationInfo;

public interface ILGTestTestFragment {

    LGTestFlowConfigurationInfo getTestConfigurationInfo();
    void setOnStateChangeListener(ILGTestFlowStateListener stateChangeListener);

    void runTest();
    void stopTest();
}