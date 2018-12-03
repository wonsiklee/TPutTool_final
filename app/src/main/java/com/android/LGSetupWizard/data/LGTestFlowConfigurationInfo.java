package com.android.LGSetupWizard.data;

import android.support.v4.app.Fragment;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(prefix = "m")
public class LGTestFlowConfigurationInfo {
    @Getter @Setter Fragment mFragmentInstance;
    @Getter @Setter boolean mIsGoodToGo = false;

    public LGTestFlowConfigurationInfo(Fragment fragmentInstance) {
        this.mFragmentInstance = fragmentInstance;
    }
}
