package com.android.LGSetupWizard.ui.fragments;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.LGSetupWizard.BuildConfig;
import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.database.TestResultDBManager;
import com.android.LGSetupWizard.ui.popup.TestResultPopupWindow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
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

    private Button btn_iperf_start, btn_iperf_install;
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


            btn_iperf_install = (Button)mView.findViewById(R.id.btn_iperf_install);
            btn_iperf_install.setOnClickListener(this);


            imgBtn_iperf_result = (ImageButton)mView.findViewById(R.id.imgBtn_iperf_result);
            imgBtn_iperf_result.setOnClickListener(this);

        }
        loadIperfApp();
        return mView;
    }

    private void loadIperfApp() {
        //load Apk file
        boolean needInstall = false;


        //check path
        File file = mContext.getExternalFilesDir(null);
        if (!file.exists()){
            file.mkdir();
        }

        file = new File(file,"LGIperf.apk");
        //mContext.getExternalFilesDir().getPath()+""
        if( !file.exists()){
            Log.i(TAG, "loadIperfApp - lgiperfappapk not exist or not execute");
            InputStream sOpenRawResource = mContext.getResources().openRawResource(R.raw.lgiperfapp);

            try {
                byte[] sBuffer = new byte[sOpenRawResource.available()];
                sOpenRawResource.read(sBuffer);
                sOpenRawResource.close();

                FileOutputStream sOpenFileOutput = new FileOutputStream(file);
                //mContext.openFileOutput("LGIperf.apk", Context.MODE_PRIVATE);

                sOpenFileOutput.write(sBuffer);
                sOpenFileOutput.close();
                //mContext.getFileStreamPath("LGIperf.apk").setExecutable(true);
                needInstall = true;
            }catch (Exception e){
                Log.e(TAG,"loadIperfApp - Fail:"+e.toString());
                return;
            }
        }
        else{
            needInstall = !hasIperfApp();
        }

        if(needInstall){
            //TODO need change slient install..
            //installIperfApp();
        }
    }



    private void installIperfApp() {
        //TODO need change Silent Install targetSDK lower than L-OS

        PackageManager packageManger = mContext.getPackageManager();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            PackageInstaller packageInstaller = packageManger.getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName("com.lge.kobinfactory.lgiperf");
            try {
                int sessionId = packageInstaller.createSession(params);
                PackageInstaller.Session session = packageInstaller.openSession(sessionId);
                OutputStream out = session.openWrite("LGIperf.apk", 0, -1);

                InputStream in = new FileInputStream((new File(mContext.getExternalFilesDir(null),"LGIperf.apk")));

                final int bufsize = 4096;
                byte[] bytes = new byte[bufsize];

                int len = 1; // any value > 0
                //int tot = 0;
                while (len > 0) {
                    len = in.read(bytes, 0, bufsize);
                    if (len < 1) break;
                    out.write(bytes, 0, len);
                    //tot += len;
                }
                in.close();

                session.fsync(out);
                out.close();
                System.out.println("installing...");
                session.commit(PendingIntent.getBroadcast(mContext, sessionId,
                        new Intent("android.intent.action.MAIN"), 0).getIntentSender());
                Log.i(TAG, "send Install request");

                //return true;
            } catch (IOException e) {
                e.printStackTrace();
                //return false;
            }
        }
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
            if( action.equals("com.android.LGSetupWizard.lgiperfapp.save_result")){
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
        else if (v == btn_iperf_install){
            installIperfApp();
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