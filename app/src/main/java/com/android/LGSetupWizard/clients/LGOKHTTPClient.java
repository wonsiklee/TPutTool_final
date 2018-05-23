package com.android.LGSetupWizard.clients;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public void publishCurrentTPut() {
        this.mDownloadRunnable.publishCurrentTPut();
    }

    public void setOnStateChangedListener(LGHTTPDownloadStateChangeListener listener) {
        this.mStateListener = listener;
    }

    public class DownloadRunnable implements Runnable {

        private long mStartTime;
        private long mCurrentTime;
        private long mSavedTime;
        private long mTotalSize;
        private long mSavedSize;
        private long mFullSize;
        private File directory;
        private File fileToBeDownloaded;

        public DownloadRunnable() {
        }

        @Override
        public void run() {
            requestFileDownload(mFileURL);
            Log.d(TAG, "run() EXIT");
        }

        public void publishCurrentTPut() {
            if (mStateListener != null) {
                mStateListener.onTPutPublished(getCurrentTPut(), getProgress());
            }
        }

        public float getCurrentTPut() {
            mCurrentTime = System.currentTimeMillis();
            long sDuration = mCurrentTime - mSavedTime;
            mSavedTime = mCurrentTime;
            long sSize = mTotalSize - mSavedSize;
            mSavedSize = mTotalSize;
            if (sDuration == 0) return 0;
            float avgTput = (sSize * 8.0f / 1000 / 1000) / (sDuration / 1000.0f);
            Log.d(TAG, "avgTput : " + avgTput + " Mbps");
            return avgTput;
        }

        public int getProgress() {
            if (mFullSize == 0) return 0;
            int sProgress = (int) ((mTotalSize * 100 / mFullSize));
            Log.d(TAG, "sProgress : " + sProgress + " %");
            return sProgress;
        }

        private void requestFileDownload(String fileUrl) {
            directory = Environment.getExternalStorageDirectory();
            fileToBeDownloaded = new File(directory.getAbsolutePath() + "/" + getFileNameFrom(fileUrl));
            try {
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

                HttpURLConnection localHttpURLConnection = (HttpURLConnection) new URL(fileUrl).openConnection();
                mFullSize = getHeaderFieldLong(localHttpURLConnection, "Content-Length", -1L);
                BufferedInputStream localBufferedInputStream = new BufferedInputStream(localHttpURLConnection.getInputStream(), BUFFER_SIZE);

                byte[] data = new byte[BUFFER_SIZE];

                int count;
                mTotalSize = 0;
                mSavedSize = 0;

                mStartTime = System.currentTimeMillis();
                mSavedTime = System.currentTimeMillis();
                try {
                    if (mStateListener != null) {
                        mStateListener.onDownloadStarted();
                    }

                    while ((count = localBufferedInputStream.read(data)) != -1 && mIsOkToGo) {
                        mTotalSize += count;
                        if (mEnableFileIO && os != null) {
                            os.write(data, 0, count);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException occurred : " + e.getMessage());
                } finally {
                    if (mStateListener != null) {
                        mCurrentTime = System.currentTimeMillis();
                        mStateListener.onDownloadFinished(mTotalSize, mCurrentTime - mStartTime);
                    }
                    Log.d(TAG, "executeReceiveLoop() EXIT");
                    mTotalSize = 0;
                    mSavedSize = 0;

                    if (mEnableFileIO && os != null) {
                        os.flush();
                        os.close();
                    }
                    localBufferedInputStream.close();
                    localHttpURLConnection.disconnect();
                }
            } catch (IOException localException) {
                Log.d(TAG, "Exception", localException);
                return;
            }
        }

        private String getFileNameFrom(String url) {
            int lastIndexOfSlash = url.lastIndexOf('/') + 1;
            return url.substring(lastIndexOfSlash, url.length());
        }

        private long getHeaderFieldLong(HttpURLConnection paramHttpURLConnection, String paramString, long paramLong)
        {
            if (paramHttpURLConnection == null) {
                return paramLong;
            }
            try {
                long l = Long.parseLong(paramHttpURLConnection.getHeaderField(paramString));
                return l;
            } catch (NumberFormatException localNumberFormatException) {}
            return paramLong;
        }
    }
}
