package com.android.LGSetupWizard.data;

import android.content.Context;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.android.LGSetupWizard.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by hyukbin.ko on 2018-07-25.
 */


public class LGIperfPackageManager {
        private static final String TAG = LGIperfPackageManager.class.getSimpleName();

        /**
         * need to increase when LGIperfApp's Version is changed. please Sync to LGIperfApp's VersionCode.
         * */
        private int mLGIperfAppVersionCode = 5;

        private static final String LGIPERF_APP_PACKAGE_NAME = "com.lge.kobinfactory.lgiperf";
        private static final String LGIPERF_APP_FILE_NAME = "LGIperf.apk";

        public final int INSTALL_REPLACE_EXISTING = 2;


        public static final int INSTALL_SUCCEEDED = 1;
        public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;
        public static final int INSTALL_FAILED_INVALID_APK = -2;
        public static final int INSTALL_FAILED_INVALID_URI = -3;
        public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;
        public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;
        public static final int INSTALL_FAILED_NO_SHARED_USER = -6;
        public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;
        public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;
        public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;
        public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;
        public static final int INSTALL_FAILED_DEXOPT = -11;
        public static final int INSTALL_FAILED_OLDER_SDK = -12;
        public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;
        public static final int INSTALL_FAILED_NEWER_SDK = -14;
        public static final int INSTALL_FAILED_TEST_ONLY = -15;
        public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;
        public static final int INSTALL_FAILED_MISSING_FEATURE = -17;
        public static final int INSTALL_FAILED_CONTAINER_ERROR = -18;
        public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;
        public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;
        public static final int INSTALL_PARSE_FAILED_NOT_APK = -100;
        public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;
        public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;
        public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;
        public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;
        public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;
        public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;
        public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;
        public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;
        public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;
        public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;

        private Context mContext;
        private PackageInstallObserver observer;
        private PackageManager pm;
        private Method method;

        private OnInstalledPackaged onInstalledPackaged;

        class PackageInstallObserver extends IPackageInstallObserver.Stub {
            public void packageInstalled(String packageName, int returnCode) throws RemoteException {
                if (onInstalledPackaged != null) {
                    onInstalledPackaged.packageInstalled(packageName, returnCode);
                }
            }
        }

        public LGIperfPackageManager(Context context) throws SecurityException, NoSuchMethodException {

            mContext = context;
            observer = new PackageInstallObserver();
            pm = context.getPackageManager();

            Class<?>[] types = new Class[] {Uri.class, IPackageInstallObserver.class, int.class, String.class};
            method = pm.getClass().getMethod("installPackage", types);
        }

        public void setOnInstalledPackaged(OnInstalledPackaged onInstalledPackaged) {
            this.onInstalledPackaged = onInstalledPackaged;
        }

    /**
     * return value
     *    true : success to request
     *    false : fail to request
     */
        public boolean requestInstallIperfPackage(){
            if (!copyIperfPackageFile()) return false;
            try {
                File file = new File(mContext.getExternalFilesDir(null),LGIPERF_APP_FILE_NAME);
                installPackage(file);
            } catch (IllegalAccessException e) {
                Log.e(TAG,"install failed ="+e.toString());
                return false;
            } catch (InvocationTargetException e) {
                Log.e(TAG,"install failed ="+e.toString());
                return false;
            }

            return true;
        }
        private void installPackage(String apkFile) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            installPackage(new File(apkFile));
        }

        private void installPackage(File apkFile) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (!apkFile.exists()) throw new IllegalArgumentException();
            Uri packageURI = Uri.fromFile(apkFile);
            installPackage(packageURI);
        }

        private void installPackage(Uri apkFile) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            method.invoke(pm, new Object[] {apkFile, observer, INSTALL_REPLACE_EXISTING, null});
        }


        public boolean needIstallIperfPackage(){
            try {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(LGIPERF_APP_PACKAGE_NAME,0);
                if( packageInfo.versionCode < mLGIperfAppVersionCode ){
                    Log.i(TAG, "LGiperf version code update! need update");
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.i(TAG, "LGIperf not exist.. install !!");
                return true;
            }
            return false;
        }

        private boolean copyIperfPackageFile(){
            File file = mContext.getExternalFilesDir(null);
            if (!file.exists()){
                file.mkdir();
            }
            file = new File(file,LGIPERF_APP_FILE_NAME);
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
                Log.e(TAG,"copyIperfPackageFile - Fail:"+e.toString());
                return false;
            }
            return true;
        }

        public interface OnInstalledPackaged {
            void packageInstalled(String packageName, int returnCode);
        }
    }

