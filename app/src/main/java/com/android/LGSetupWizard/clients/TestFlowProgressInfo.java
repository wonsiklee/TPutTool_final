package com.android.LGSetupWizard.clients;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class TestFlowProgressInfo {
    @Getter @Setter private float mAvgTPut;
    @Getter @Setter private float mInstantaneousTPut;
    @Getter @Setter private float mProgressInPercentage;
    @Getter @Setter private int mTotalCount;
    @Getter @Setter private int mCurrentCount;
}