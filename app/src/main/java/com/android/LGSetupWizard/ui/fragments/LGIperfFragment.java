package com.android.LGSetupWizard.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
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
import com.android.LGSetupWizard.utils.LGIperfPackageManager;
import com.android.LGSetupWizard.utils.OnInstalledPackaged;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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

    private int mLGIperfAppVersionCode = 2;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Log.i(TAG,"onAttach - regi - filter ");
        IntentFilter resultIntentFilter = new IntentFilter();
        resultIntentFilter.addAction("com.android.LGSetupWizard.LGIperf.save_result");
        context.registerReceiver(mResultReceiver, resultIntentFilter);
        IntentFilter resultIntentFilter2 = new IntentFilter();
        resultIntentFilter.addAction("android.lgiperf.INSTALL");
        context.registerReceiver(mInstallReceiver, resultIntentFilter2);
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


            /*
            btn_iperf_install = (Button)mView.findViewById(R.id.btn_iperf_install);
            btn_iperf_install.setOnClickListener(this);
            */

            imgBtn_iperf_result = (ImageButton)mView.findViewById(R.id.imgBtn_iperf_result);
            imgBtn_iperf_result.setOnClickListener(this);

        }
        loadIperfApp();
        return mView;
    }

    private void loadIperfApp() {
        //load Apk file

        boolean needInstall = false;


        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo("com.lge.kobinfactory.lgiperf",0);
            if( packageInfo.versionCode < mLGIperfAppVersionCode ){
                Log.i(TAG, "LGiperf version code update! need update");
                needInstall = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "LGIperf not exist.. install !!");
            needInstall = true;
        }

        if(needInstall){
            File file = mContext.getExternalFilesDir(null);
            if (!file.exists()){
                file.mkdir();
            }
            file = new File(file,"LGIperf.apk");
            InputStream sOpenRawResource = mContext.getResources().openRawResource(R.raw.lgiperfapp);

            try {
                byte[] sBuffer = new byte[sOpenRawResource.available()];
                sOpenRawResource.read(sBuffer);
                sOpenRawResource.close();

                FileOutputStream sOpenFileOutput = new FileOutputStream(file);
                //mContext.openFileOutput("LGIperf.apk", Context.MODE_PRIVATE);

                sOpenFileOutput.write(sBuffer);
                sOpenFileOutput.close();
            }catch (Exception e){
                Log.e(TAG,"loadIperfApp - Fail:"+e.toString());
                //TODO need error logic
                return;
            }
            installIperfApp();
        }
    }



    private void installIperfApp() {

/*
        TODO : Mehod 1 : but some device is not working !!
        PackageManager packageManger = mContext.getPackageManager();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            PackageInstaller packageInstaller = packageManger.getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);

            params.setAppPackageName("com.lge.kobinfactory.lgiperf");
            Log.d(TAG, "packageInstaller = "+packageInstaller.toString());

            int sessionId = 0;
            PackageInstaller.Session session = null;
            try {
                sessionId = packageInstaller.createSession(params);
                session = packageInstaller.openSession(sessionId);
            } catch (IOException e) {
                Log.d(TAG, "install session is not created !!"+e.toString());
                return;
            }

            try {
                File file = new File(mContext.getExternalFilesDir(null),"LGIperf.apk");
                long sizeBytes = file.length();
                OutputStream out = session.openWrite("LGIperfApp", 0,sizeBytes);

                InputStream in = new FileInputStream(file);

                final byte[] buffer = new byte[65536];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                session.fsync(out);
                in.close();
                out.close();

                session.commit(PendingIntent.getBroadcast(getContext(), sessionId,
                        new Intent("android.lgiperf.INSTALL"), PendingIntent.FLAG_UPDATE_CURRENT).getIntentSender());


                Log.i(TAG, "send Install request");

                //return true;
            } catch (IOException e) {
                Log.i(TAG,"IO Exception:"+e.toString());
                session.close();
            }
        }*/

        try {
            LGIperfPackageManager pm = new LGIperfPackageManager(mContext);
            pm.setOnInstalledPackaged(new OnInstalledPackaged() {
                @Override
                public void packageInstalled(String packageName, int returnCode) {
                    if (returnCode == LGIperfPackageManager.INSTALL_SUCCEEDED) {
                        Log.d(TAG, "Install succeeded");
                    } else {
                        Log.d(TAG, "Install failed: " + returnCode);
                    }
                }
            });
            File file = new File(mContext.getExternalFilesDir(null),"LGIperf.apk");
            pm.installPackage(file);
        } catch (NoSuchMethodException e) {
            Log.d(TAG,"install failed ="+e.toString());
        } catch (IllegalAccessException e) {
            Log.d(TAG,"install failed ="+e.toString());
        } catch (InvocationTargetException e) {
            Log.d(TAG,"install failed ="+e.toString());
        }

    }


    @Override
    public void onDetach(){
        super.onDetach();
        Log.i(TAG,"onDetach");
        mContext.unregisterReceiver(mResultReceiver);
        mContext.unregisterReceiver(mInstallReceiver);
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
    private static final int PACKAGE_INSTALLER_STATUS_UNDEFINED = -1000;
    private final BroadcastReceiver mInstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if( action.equals("android.lgiperf.INSTALL")){
                int status  = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,PACKAGE_INSTALLER_STATUS_UNDEFINED);
                Log.i(TAG, "InstallReceiver . get Extra : " + status);
            }
        }
    };

    @Override
    public void onClick(View v) {
        if( v == btn_iperf_start) {
            if (hasIperfApp()) {
                startIperfApp();
            } else {
                installIperfApp();
                Toast.makeText(mContext, "try after 5sec or restart app!!", Toast.LENGTH_SHORT).show();
            }
        }
        else if ( v == imgBtn_iperf_result ){
            new TestResultPopupWindow(getContext()).show(getView(),TestResultDBManager.TestCategory.iPerf);
        }
        /*
        else if (v == btn_iperf_install){
            installIperfApp();
        }*/
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