package com.android.LGSetupWizard;

public interface ITestController {


    // This will be invoked prior to launch().
    // So, all the preperation codes should be implemented.
    public void prepareToLaunch();


    // this will be invoked after prepareToLaunch(),
    // actual test codes should be implemented in this.
    public boolean launch();
}
