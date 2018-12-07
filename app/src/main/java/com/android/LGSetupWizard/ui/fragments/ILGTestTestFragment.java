package com.android.LGSetupWizard.ui.fragments;

import com.android.LGSetupWizard.data.LGTestFlowConfigurationInfo;

public interface ILGTestTestFragment {
    void setPredefinedConfiguration(LGTestFlowConfigurationInfo info);
    LGTestFlowConfigurationInfo getTestConfigurationInfo();
    void setOnStateChangeListener(ILGTestFlowStateListener stateChangeListener);

    void runTest(LGTestFlowConfigurationInfo info);
    void stopTest();
}