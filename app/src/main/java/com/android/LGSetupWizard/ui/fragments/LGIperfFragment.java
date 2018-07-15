package com.android.LGSetupWizard.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.ui.popup.TestResultPopupWindow;

import java.util.List;

import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

@Accessors(prefix = "m")
public class LGIperfFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = LGIperfFragment.class.getSimpleName();

    private View mView;
    private Context mContext;

    private Button btn_iperf_start;
    private ImageButton imgBtn_iperf_result;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Log.i(TAG,"onAttach - regi - filter ");
        IntentFilter resultIntentFilter = new IntentFilter();
        resultIntentFilter.addAction("com.android.LGSetupWizard.LGIperf.save_result");
        context.registerReceiver(mResultReceiver, resultIntentFilter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        mContext = getContext();

        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_iperf, container, false);

            btn_iperf_start = (Button)mView.findViewById(R.id.btn_iperf_start);
            btn_iperf_start.setOnClickListener(this);

            imgBtn_iperf_result = (ImageButton)mView.findViewById(R.id.imgBtn_iperf_result);
            imgBtn_iperf_result.setOnClickListener(this);
        }

        return mView;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        Log.i(TAG,"onDetach");
        mContext.unregisterReceiver(mResultReceiver);
    }

    private final BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if( action.equals("com.android.LGSetupWizard.LGIperf.save_result")){
                float avg = intent.getFloatExtra("avg",0);
                Log.d(TAG, "get result = "+avg );
                saveIperfTput(avg);
            }
        }
    };

    @Override
    public void onClick(View v) {
        if( v == btn_iperf_start) {
            if (hasIperfApp()) {
                startIperfApp();
            } else {
                Toast.makeText(mContext, "Please install LG Iperf!!", Toast.LENGTH_SHORT).show();
            }
        }
        else if ( v == imgBtn_iperf_result ){
            new TestResultPopupWindow(getContext()).show(getView(),TestResultDBManager.TestCategory.iPerf);
        }
    }

    private void saveIperfTput(float avg){
        TestResultDBManager.getInstance(getContext()).insert(TestResultDBManager.TestCategory.iPerf, avg,null);
    }

    private static final String IPERF_APP= "com.lge.kobinfactory.lgiperf";
    private boolean hasIperfApp() {
        boolean isExist = false;

        PackageManager pkgMgr = getContext().getPackageManager();
        List<ResolveInfo> mApps;
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = pkgMgr.queryIntentActivities(mainIntent, 0);

        try {
            for (int i = 0; i < mApps.size(); i++) {
                if (mApps.get(i).activityInfo.packageName.startsWith(IPERF_APP)) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            isExist = false;
        }
        return isExist;
    }

    private void startIperfApp() {
        Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(IPERF_APP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }
}