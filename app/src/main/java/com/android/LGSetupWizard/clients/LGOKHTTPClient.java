package com.android.LGSetupWizard.clients;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

/**
 * Created by yunsik.lee on 2018-05-03.
 */

public class LGOKHTTPClient implements LGHTTPClient {
    private static String TAG = LGOKHTTPClient.class.getSimpleName();

    private LGHTTPDownloadStateChangeListener mStateListener;
    private DownloadRunnable mDownloadRunnable;
    private boolean mIsOkToGo = true;

    public LGOKHTTPClient() {

    }

    public void startHTTPDownload() {
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
        private static final String SIGNAL_DATA = "00000000000000010000138900000000000000000000040000000002";
        private static final long DL_MAXIMUM_DURATION_LIMIT = 110000;
        private static final int BUFFER_SIZE = 2 * 1024 * 1024;

        private String mServerIP;
        private int mPortNum;
        private Socket mSocket;
        private BufferedWriter mNetworkWriter;

        private long mStartTime;
        private long mDuration;
        private long mCurrentTime;
        private long mTotalSize;

        public DownloadRunnable() {
        }

        @Override
        public void run() {
            queryServerIP();
            //mServerIP = "113.217.230.29";
            mPortNum = 5001;
            openConnection();
            executeReceiveLoop();
            closeConnection();
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

        private void executeReceiveLoop() {
            Log.d(TAG, "executeReceiveLoop() ENTRY");

            Log.d(TAG, "mIsOkToGo : " + mIsOkToGo);

            byte[] buffer = new byte[BUFFER_SIZE];

            PrintWriter out = new PrintWriter(mNetworkWriter, true);
            out.println(SIGNAL_DATA);
            mStartTime = System.currentTimeMillis();
            try {
                int size = -1;
                if (mStateListener != null) {
                    mStateListener.onDownloadStarted();
                }
                do {
                    size = (this.mSocket.getInputStream().read(buffer));
                    mTotalSize += size;
                } while(size != -1 && mDuration <= DL_MAXIMUM_DURATION_LIMIT && mIsOkToGo);

            } catch (IOException e) {
                Log.e(TAG, "IOException occurred : " + e.getMessage());
            } finally {
                if (mStateListener != null) {
                    mStateListener.onDownloadFinished(mTotalSize, mDuration);
                }
                Log.d(TAG, "executeReceiveLoop() EXIT");
                this.mTotalSize = 0;
            }
        }

        private void openConnection() {
            //http://speed.nia.or.kr/mobile/ispIPCall.asp?route_id=8
            Log.d(TAG, "openConnection()");
            try {
                Log.d(TAG, mServerIP + ", " + mPortNum);
                this.mSocket = new Socket(mServerIP, mPortNum);
                Log.d(TAG, "mSocket.InetAddress : " + mSocket.getInetAddress().toString());
                this.mSocket.setReceiveBufferSize(BUFFER_SIZE);
                this.mNetworkWriter = new BufferedWriter(new OutputStreamWriter(this.mSocket.getOutputStream()));
            } catch (IOException e) {
                Log.d(TAG, "IOException : " + e.getMessage());
                System.out.println(e);
                e.printStackTrace();
            }
        }

        private void queryServerIP() {
            Log.d(TAG, "queryServerIP() ENTER");
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                Log.d(TAG, "1");
                url = new URL("http://speed.nia.or.kr/mobile/ispIPCall.asp?route_id=8");
                urlConnection = (HttpURLConnection) url.openConnection();
                Log.d(TAG, "2");
                InputStream is = urlConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                Log.d(TAG, "3");
                int data;
                String content = "";
                while ((data = isr.read()) != -1) {
                    content += (char) data;
                }
                Log.d(TAG, "received : " + content);
                String[] array = content.split("<br>");
                this.mServerIP = array[0].split("=")[1];
                this.mPortNum = Integer.valueOf(array[1].split("=")[1]);
            } catch (MalformedURLException e) {
                Log.d(TAG, "MalformedURLException : " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "IOException : " + e.getMessage());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                Log.d(TAG, "NumberFormatException : " + e.getMessage());
                e.printStackTrace();
            }finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            Log.d(TAG, "queryServerIP() EXIT");
        }

        private void closeConnection() {
            try {
                this.mNetworkWriter.close();
                this.mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
