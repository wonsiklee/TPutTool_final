package com.lge.tputmaster.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.lge.tputmaster.fragments.ConfigurationFragment;
import com.lge.tputmaster.fragments.LGFTPFragment;
import com.lge.tputmaster.fragments.LGNIAFragment;

import java.util.ArrayList;

public class FragmentPagerAdapter extends FragmentStatePagerAdapter {

    ArrayList<Fragment> mFragmentArrayList;

    public FragmentPagerAdapter(FragmentManager fm) {
        super(fm);

        this.mFragmentArrayList = new ArrayList<>();
        this.mFragmentArrayList.add(new ConfigurationFragment());
        this.mFragmentArrayList.add(new LGNIAFragment());
        this.mFragmentArrayList.add(new LGFTPFragment());
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
