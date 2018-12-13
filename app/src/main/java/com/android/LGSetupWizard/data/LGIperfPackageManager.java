package com.android.LGSetupWizard.data;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.LGSetupWizard.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by hyukbin.ko on 2018-07-25.
 */


public class LGIperfPackageManager {
    private static final String TAG = LGIperfPackageManager.class.getSimpleName();

    /**
     * need to increase when LGIperfApp's Version is changed. please Sync to LGIperfApp's VersionCode.
     */
    private int mLGIperfAppVersionCode = 5;

    private static final String LGIPERF_APP_PACKAGE_NAME = "com.lge.kobinfactory.lgiperf";
    private static final String LGIPERF_APP_FILE_NAME = "LGIperf.apk";

    public final int INSTALL_REPLACE_EXISTING = 2;

    private static final int OB_INSTALL_SUCCEEDED = 1;
    private static final int OB_INSTALL_FAILED_ALREADY_EXISTS = -1;
    private static final int OB_INSTALL_FAILED_INVALID_APK = -2;
    private static final int OB_INSTALL_FAILED_INVALID_URI = -3;
    private static final int OB_INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;
    private static final int OB_INSTALL_FAILED_DUPLICATE_PACKAGE = -5;
    private static final int OB_INSTALL_FAILED_NO_SHARED_USER = -6;
    private static final int OB_INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;
    private static final int OB_INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;
    private static final int OB_INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;
    private static final int OB_INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;
    private static final int OB_INSTALL_FAILED_DEXOPT = -11;
    private static final int OB_INSTALL_FAILED_OLDER_SDK = -12;
    private static final int OB_INSTALL_FAILED_CONFLICTING_PROVIDER = -13;
    private static final int OB_INSTALL_FAILED_NEWER_SDK = -14;
    private static final int OB_INSTALL_FAILED_TEST_ONLY = -15;
    private static final int OB_INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;
    private static final int OB_INSTALL_FAILED_MISSING_FEATURE = -17;
    private static final int OB_INSTALL_FAILED_CONTAINER_ERROR = -18;
    private static final int OB_INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;
    private static final int OB_INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;
    private static final int OB_INSTALL_PARSE_FAILED_NOT_APK = -100;
    private static final int OB_INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;
    private static final int OB_INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;
    private static final int OB_INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;
    private static final int OB_INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;
    private static final int OB_INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;
    private static final int OB_INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;
    private static final int OB_INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;
    private static final int OB_INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;
    private static final int OB_INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;
    private static final int OB_INSTALL_FAILED_INTERNAL_ERROR = -110;

    public static final int INSTALL_SUCCEEDED = 1;
    public static final int INSTALL_FAILED = 0;

    private Context mContext;
    private PackageInstallObserver observer;
    private PackageManager pm;
    private Method method;

    private OnInstalledPackaged onInstalledPackaged;

    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) throws RemoteException {
            Log.d(TAG,"PackageInstallObserver install return value = "+returnCode);
            if (onInstalledPackaged != null) {
                int value  = (returnCode == OB_INSTALL_SUCCEEDED)? INSTALL_SUCCEEDED:INSTALL_FAILED;
                onInstalledPackaged.packageInstalled(value);
            }
        }
    }

    class InstallReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if( PACKAGE_INSTALL.equals(action)){
                Bundle extras = intent.getExtras();
                int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
                String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Log.d(TAG,"PackageInstaller Recevier got message = "+message);
                if (onInstalledPackaged != null) {
                    int value = (status == PackageInstaller.STATUS_SUCCESS)? INSTALL_SUCCEEDED:INSTALL_FAILED;
                    onInstalledPackaged.packageInstalled(value);
                }
                /*
                switch (status) {
                    case PackageInstaller.STATUS_PENDING_USER_ACTION:
                        // This test app isn't privileged, so the user has to confirm the install.
                        Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                        mContext.startActivity(confirmIntent);
                        break;

                    case PackageInstaller.STATUS_SUCCESS:
                        Log.d(TAG,"Install succeeded!");
                        break;

                    case PackageInstaller.STATUS_FAILURE:
                    case PackageInstaller.STATUS_FAILURE_ABORTED:
                    case PackageInstaller.STATUS_FAILURE_BLOCKED:
                    case PackageInstaller.STATUS_FAILURE_CONFLICT:
                    case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                    case PackageInstaller.STATUS_FAILURE_INVALID:
                    case PackageInstaller.STATUS_FAILURE_STORAGE:
                        Log.d(TAG,"Install failed! " + status + ", " + message);
                        break;
                    default:
                        Log.d(TAG, "Unrecognized status received from installer: " + status);
                }*/
            }
        }
    }
    InstallReceiver mReceiver;

    public LGIperfPackageManager(Context context) throws SecurityException, NoSuchMethodException {

        mContext = context;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            observer = new PackageInstallObserver();
            pm = context.getPackageManager();

            Class<?>[] types = new Class[]{Uri.class, IPackageInstallObserver.class, int.class, String.class};
            method = pm.getClass().getMethod("installPackage", types);
        }
        else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PACKAGE_INSTALL);
            mReceiver = new InstallReceiver();
            mContext.registerReceiver(mReceiver,intentFilter);
        }
    }

    public void detach(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
            Log.d(TAG, "check!");
        }
        else {
            if(mReceiver!=null){
                try {
                    mContext.unregisterReceiver(mReceiver);
                }catch (Exception e){
                    Log.d(TAG, "unregi fail");
                }
            }
        }
    }

    public void setOnInstalledPackaged(OnInstalledPackaged onInstalledPackaged) {
        this.onInstalledPackaged = onInstalledPackaged;
    }

    /**
     * return value
     * true : success to request
     * false : fail to request
     */
    public boolean requestInstallIperfPackage() {
        if (!copyIperfPackageFile()) return false;
        try {
            File file = new File(mContext.getExternalFilesDir(null), LGIPERF_APP_FILE_NAME);
            installPackage(file);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "install failed =" + e.toString());
            return false;
        } catch (InvocationTargetException e) {
            Log.e(TAG, "install failed =" + e.toString());
            return false;
        }

        return true;
    }

    private void installPackage(String apkFile) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        installPackage(new File(apkFile));
    }

    private void installPackage(File apkFile) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (!apkFile.exists()) throw new IllegalArgumentException();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Uri packageURI = Uri.fromFile(apkFile);
            installPackage(packageURI);
        } else {
            installPackageSession(apkFile);
        }
    }

    private void installPackage(Uri apkFile) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        method.invoke(pm, new Object[]{apkFile, observer, INSTALL_REPLACE_EXISTING, null});
    }


    public boolean needIstallIperfPackage() {
        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(LGIPERF_APP_PACKAGE_NAME, 0);
            if (packageInfo.versionCode < mLGIperfAppVersionCode) {
                Log.i(TAG, "LGiperf version code update! need update");
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "LGIperf not exist.. install !!");
            return true;
        }
        return false;
    }

    private boolean copyIperfPackageFile() {
        File file = mContext.getExternalFilesDir(null);
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(file, LGIPERF_APP_FILE_NAME);
        InputStream sOpenRawResource = mContext.getResources().openRawResource(R.raw.lgiperfapp);

        try {
            byte[] sBuffer = new byte[sOpenRawResource.available()];
            sOpenRawResource.read(sBuffer);
            sOpenRawResource.close();

            FileOutputStream sOpenFileOutput = new FileOutputStream(file);
            //mContext.openFileOutput("LGIperf.apk", Context.MODE_PRIVATE);

            sOpenFileOutput.write(sBuffer);
            sOpenFileOutput.close();
        } catch (Exception e) {
            Log.e(TAG, "copyIperfPackageFile - Fail:" + e.toString());
            return false;
        }
        return true;
    }

    public interface OnInstalledPackaged {
        void packageInstalled(int returnCode);
    }

    private static final int APK_BUFFER_SIZE = 16384;
    private static final String PACKAGE_INSTALL = "com.android.LGSetupWizard.PACKAGE_INSTALL";

    private void installPackageSession(File file) {

        PackageInstaller.Session session = null;
        OutputStream os = null;
        InputStream is = null;

        try {
            PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);

            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);

            os = session.openWrite("package", 0, -1);
            is = new FileInputStream(file);

            byte[] buffer = new byte[APK_BUFFER_SIZE];
            int n;
            while ((n = is.read(buffer)) >= 0) {
                os.write(buffer, 0, n);
            }

            is.close();
            os.close();

            Intent intent = new Intent(PACKAGE_INSTALL);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, sessionId, intent, 0);
            IntentSender statusReceiver = pendingIntent.getIntentSender();

            session.commit(statusReceiver);

        } catch (IOException e) {
            throw new RuntimeException("Couldn't install package", e);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.abandon();
            }
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

