package com.android.LGSetupWizard.data;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.util.Log;

import java.io.File;

/**
 * Created by heeyeon.nah on 2017-05-25.
 */

public class MediaScanning implements MediaScannerConnectionClient {
    private MediaScannerConnection mConnection;
    private File mTargetFile;
    private String TAG = "TputTracintFile";
    private Context mContext;
    private String mFilePath;

    public MediaScanning (Context context, File targetFile) {
        this.mContext = context;
        this.mTargetFile = targetFile;
        mConnection = new MediaScannerConnection(mContext, this);
        mConnection.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        mFilePath = mTargetFile.getAbsolutePath();
        mConnection.scanFile(mTargetFile.getAbsolutePath(), null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        Log.d(TAG, "Writing of result file is completed. path: " + mFilePath);
        mConnection.disconnect();
    }
}
