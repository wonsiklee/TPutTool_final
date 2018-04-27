package com.android.LGSetupWizard.adapters;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.LGSetupWizard.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

//test start
//test end

/**
 * Created by heeyeon.nah on 2017-03-24.
 */

public class PackageNameSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

    private final static String TAG = PackageNameSpinnerAdapter.class.getSimpleName();

    private final Context mContext;
    private ArrayList<PackageInfo> mPackageNames;

    public PackageNameSpinnerAdapter(Context context) {
        this.mContext = context;
        this.loadPackageInfo();
    }

    public int getCount() {
        return mPackageNames.size() + 2;
    }

    public PackageInfo getItem(int i) {
        if (i < this.mPackageNames.size()) {
            return mPackageNames.get(i);
        } else {
            PackageInfo info = new PackageInfo();
            if (i == this.mPackageNames.size()) {
                info.packageName = this.mContext.getString(R.string.str_package_name_manual_input);
            } else {
                info.packageName = this.mContext.getString(R.string.str_package_name_all_traffic);
            }
            return info;
        }
    }

    public long getItemId(int i) {
        return (long)i;
    }

    /*@Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return  getView(position, convertView, parent);
    }*/

    @Override
    public View getView(int i, View convertView, ViewGroup viewgroup) {
        if (i >= this.mPackageNames.size()) {
            Log.d(TAG, "\ti = " + i + "");
            TextView txt = new TextView(this.mContext);
            txt.setTextSize(16);
            txt.setTextColor(Color.parseColor("#000000"));
            txt.setPadding(10, 20, 0, 10);
            if (i == this.mPackageNames.size()) {
                txt.setText(this.mContext.getString(R.string.str_package_name_manual_input));
            } else {
                txt.setText(this.mContext.getString(R.string.str_package_name_all_traffic));
            }
            return txt;
        } else {
            LayoutInflater layoutInflater = (LayoutInflater) this.mContext.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = layoutInflater.inflate(R.layout.package_info_custom_view, viewgroup, false);

            ImageView imgViewIcon = (ImageView) v.findViewById(R.id.imgView_app_icon);
            imgViewIcon.setImageDrawable(this.mContext.getPackageManager().getApplicationIcon(this.mPackageNames.get(i).applicationInfo));
            TextView txtViewAppName = (TextView) v.findViewById(R.id.txtView_app_name);
            txtViewAppName.setText(this.mContext.getPackageManager().getApplicationLabel(this.mPackageNames.get(i).applicationInfo));

            return v;
        }
    }

    private void loadPackageInfo() {
        Log.d(TAG, "loadPackageInfo() Entry.");

        this.mPackageNames = new ArrayList<>();
        final List<PackageInfo> sPackageInfos = this.mContext.getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
        PriorityQueue<PackageInfo> queue = new PriorityQueue<>(sPackageInfos.size(), new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo t0, PackageInfo t1) {
                if (t0.firstInstallTime > t1.firstInstallTime) {
                    return -1;
                } else if (t0.firstInstallTime == t1.firstInstallTime) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        for (PackageInfo pi: sPackageInfos) {
            if (pi.applicationInfo.uid < 10000) {
                if (pi.applicationInfo.packageName.contains("tputmaster")) {
                    queue.offer(pi);
                } else {
                    Log.d(TAG, "Skipping " + pi.applicationInfo.name+ ", uid : " + pi.applicationInfo.uid);
                }
                continue;
            }

            String sPackageName = pi.applicationInfo.packageName;
            if ((sPackageName.contains("com.lg")
                            || sPackageName.contains("lge.")
                            || sPackageName.contains("google.")
                            || sPackageName.contains("com.olleh")
                            || sPackageInfos.contains("facebook")
                            || (sPackageName.contains("kt.") && !sPackageName.contains("giga"))
                            || (sPackageName.contains("com.android") && !sPackageName.contains("chrome")))) {
                Log.d(TAG, "Skipping : " + sPackageName);
                continue;
            }


            String[] sRequestedPermissions = pi.requestedPermissions;
            if (sRequestedPermissions != null) {
                for (String requestedPermission : sRequestedPermissions) {
                    if (requestedPermission.contains("android.permission.INTERNET")) {
                        queue.offer(pi);
                        break;
                    }
                }
            }
        }

        while (queue.iterator().hasNext()) {
            mPackageNames.add(queue.poll());
        }

    }
}
