package com.android.LGSetupWizard.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.android.LGSetupWizard.ui.fragments.ConfigurationFragment;
import com.android.LGSetupWizard.ui.fragments.LGFTPFragment;
import com.android.LGSetupWizard.ui.fragments.LGHTTPFragment;
import com.android.LGSetupWizard.ui.fragments.LGIperfFragment;
import com.android.LGSetupWizard.ui.fragments.TestFlowConfigFragment;

import java.util.ArrayList;

public class FragmentPagerAdapter extends FragmentStatePagerAdapter {

    ArrayList<Fragment> mFragmentArrayList;

    public FragmentPagerAdapter(FragmentManager fm) {
        super(fm);

        this.mFragmentArrayList = new ArrayList<>();
        this.mFragmentArrayList.add(new ConfigurationFragment());
        this.mFragmentArrayList.add(new TestFlowConfigFragment());
        this.mFragmentArrayList.add(new LGFTPFragment());
        this.mFragmentArrayList.add(new LGIperfFragment());
        this.mFragmentArrayList.add(new LGHTTPFragment());
    }

    @Override
    public Fragment getItem(int position) {
        return this.mFragmentArrayList.get(position);
    }

    @Override
    public int getCount() {
        return this.mFragmentArrayList.size();
    }


}
