package com.android.LGSetupWizard.clients;

public interface ILGTestFlowController {

    // This will be invoked prior to launch().
    // So, all the preperation codes should be implemented.
    void prepareToLaunch();

    // this will be invoked after prepareToLaunch(),
    // actual test codes should be implemented in this.
    void launch();

    void abort();

    void requestToPublishProgress();
}
