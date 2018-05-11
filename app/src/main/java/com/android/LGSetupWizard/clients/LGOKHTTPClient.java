package com.android.LGSetupWizard.clients;

import android.os.Environment;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.experimental.Accessors;

/**
 * Created by yunsik.lee on 2018-05-03.
 */

@Accessors(prefix = "m")
public class LGOKHTTPClient implements LGHTTPClient {
    private static String TAG = LGOKHTTPClient.class.getSimpleName();

    private LGHTTPDownloadStateChangeListener mStateListener;
    private DownloadRunnable mDownloadRunnable;
    private boolean mIsOkToGo = true;
    private String mFileURL;
    private boolean mEnableFileIO;

    public LGOKHTTPClient() {
    }

    @Override
    public void startHTTPDownload(String fileAddr, boolean enableFileIO) {
        this.mFileURL = fileAddr;
        this.mEnableFileIO = enableFileIO;
        this.mIsOkToGo = true;
        this.mDownloadRunnable = new DownloadRunnable();
        Thread t = new Thread(this.mDownloadRunnable);
        t.start();
    }

    public void stopDownload() {
        this.mIsOkToGo = false;
    }

    public void publishAvgTPut() {
        this.mDownloadRunnable.publishAvgTPut();
    }

    public void setOnStateChangedListener(LGHTTPDownloadStateChangeListener listener) {
        this.mStateListener = listener;
    }

    public class DownloadRunnable implements Runnable {
        private static final long DL_MAXIMUM_DURATION_LIMIT = 110000;
        private static final int BUFFER_SIZE = 2 * 1024 * 1024;

        private long mStartTime;
        private long mDuration;
        private long mCurrentTime;
        private long mTotalSize;

        public DownloadRunnable() {
        }

        @Override
        public void run() {
            requestFileDownload(mFileURL);
            Log.d(TAG, "run() EXIT");
        }

        public void publishAvgTPut() {
            if (mStateListener != null) {
                mStateListener.onTPutPublished(getAvgTPut());
            }
        }

        public float getAvgTPut() {
            mCurrentTime = System.currentTimeMillis();
            mDuration = mCurrentTime - mStartTime;
            float avgTput = (mTotalSize * 8.0f / 1000 / 1000) / (mDuration / 1000.0f);
            Log.d(TAG, "avgTput : " + avgTput + " Mbps");
            return avgTput;
        }

        private void requestFileDownload(String fileUrl) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(fileUrl)
                    .build();
            CallbackToDownloadFile cbToDownloadFile = new CallbackToDownloadFile(
                    Environment.getExternalStorageDirectory(),
                    getFileNameFrom(fileUrl)
            );
            client.newCall(request).enqueue(cbToDownloadFile);
        }

        private String getFileNameFrom(String url) {
            int lastIndexOfSlash = url.lastIndexOf('/') + 1;
            return url.substring(lastIndexOfSlash, url.length());
        }

        private class CallbackToDownloadFile implements Callback {

            private File directory;
            private File fileToBeDownloaded;

            public CallbackToDownloadFile(File directory, String fileName) {
                this.directory = directory;
                this.fileToBeDownloaded = new File(this.directory.getAbsolutePath() + "/" + fileName);
            }

            @Override
            public void onFailure(Request request, IOException e) {
                Log.d(TAG, "onFailure, request=" + request + " e=" + e);
                if (mStateListener != null) {
                    mStateListener.onError("onFailure, request=" + request + " e=" + e);
                }
            }

            @Override
            public void onResponse(Response response) throws IOException {
                OutputStream os = null;
                if (mEnableFileIO) {
                    if (!this.directory.exists()) {
                        this.directory.mkdirs();
                    }

                    if (this.fileToBeDownloaded.exists()) {
                        this.fileToBeDownloaded.delete();
                    }

                    try {
                        this.fileToBeDownloaded.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "Cannot create the download file. Check the write permission.");
                        if (mStateListener != null) {
                            mStateListener.onError("Cannot create the download file. Check the write permission.");
                        }
                        return;
                    }
                    os = new FileOutputStream(this.fileToBeDownloaded);
                }

                InputStream is = response.body().byteStream();

                final int BUFFER_SIZE = 2048;
                byte[] data = new byte[BUFFER_SIZE];

                int count;
                mTotalSize = 0;

                mStartTime = System.currentTimeMillis();
                try {
                    if (mStateListener != null) {
                        mStateListener.onDownloadStarted();
                    }

                    while ((count = is.read(data)) != -1 && mIsOkToGo) {
                        mTotalSize += count;
                        if (mEnableFileIO && os != null) {
                            os.write(data, 0, count);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException occurred : " + e.getMessage());
                } finally {
                    if (mStateListener != null) {
                        mStateListener.onDownloadFinished(mTotalSize, mDuration);
                    }
                    Log.d(TAG, "executeReceiveLoop() EXIT");
                    mTotalSize = 0;

                    if (mEnableFileIO && os != null) {
                        os.flush();
                        os.close();
                    }
                    is.close();
                }
            }
        }
    }
}
