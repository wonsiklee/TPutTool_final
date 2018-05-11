package com.android.LGSetupWizard.clients;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.data.LGFTPFile;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.Buffer;
import java.util.ArrayList;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-03.
 */

public class LGIperfClient {
    static final private String TAG = LGIperfClient.class.getSimpleName();

    private IperfRunnable mIperfRunnable;
    private Context mContext;

    private static final String IPERF_NAME = "iperf";
    private static final String IPERF3_NAME = "iperf3";

    public LGIperfClient(Context context){
        mContext = context;
        mIperfRunnable = new IperfRunnable();
    }
    public void loadIperfFile(){
        if(!hasFile(IPERF_NAME)){
            createIperfFile(IPERF_NAME);
        }
        //TODO iperf3
    }
    private boolean hasFile(String filename){
        FileInputStream fis ;
        try {
            fis = mContext.openFileInput(filename);
            fis.close();
        }catch (FileNotFoundException e){
            Log.d(TAG, " hasFile : not found "+filename);
            return false;
        }catch (IOException e){
            e.printStackTrace();
        }

        Log.d(TAG, " hasFile : found "+filename);
        return true;
    }

    private void createIperfFile(String filename){
        InputStream is = null ;
        byte[] buffer;
        switch (filename) {
            case IPERF_NAME :
                is = mContext.getResources().openRawResource(R.raw.iperf);
                break;
        }

        try {
            if (is == null || is.available() <= 0) {
                Log.d(TAG, "createIperfFile : can't got resource  -" + filename);
                return;
            }
            int size = is.available();
            buffer = new byte [size];
            is.read(buffer);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "createIperfFile : can't read resource  -" + filename);
            return;
        }


        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(buffer);
            fos.close();
            Runtime.getRuntime().exec("chmod 777 "+mContext.getFilesDir()+"/"+filename).waitFor();
        } catch (FileNotFoundException e) {
            Log.d(TAG,"createIperfFile : can't create file -"+filename);
            return;
        } catch (IOException e) {
            Log.d(TAG,"createIperfFile : can't close file -"+filename);
            return;
        } catch (InterruptedException e) {
            Log.d(TAG,"createIperfFile : can't chmod file -"+filename);
            return;
        }

        Log.d(TAG,"createIperfFile : success -"+filename);

    }

    public void start(){
        mIperfRunnable.run();
    }

    public void stop(){
        mIperfRunnable.abort();
    }

    class IperfRunnable implements Runnable  {
        boolean mFlag = false;
        Process process;

        public void abort(){
            /*TODO check destroy  */
            //process.destroy();
            //android.os.Process.killProcess(int pid);
            mFlag = false;
        }
        @Override
        public void run() {
            mFlag = true;
            try {
                Log.d(TAG, "=============== START IperfRunnable run =====================");
                // Executes the command.
                String[] cmd = {mContext.getFilesDir()+"/"+IPERF_NAME, "--h"};
                Log.d(TAG,"run  - " +cmd.toString());
                process = Runtime.getRuntime().exec(cmd);


                // Reads stdout.
                // NOTE: You can write to stdin of the command using
                //       process.getOutputStream().
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                BufferedReader reader_e = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));

                int read =0;
                int read_e =0;

                char[] buffer = new char[4096];
                char[] buffer2 = new char[4096];
                while (mFlag && /*TODO process check  &&*/ (read = reader.read(buffer)) > 0 || (read_e = reader_e.read(buffer2)) > 0 ) {
                    StringBuffer output = new StringBuffer();

                    if(read > 0)
                        output.append(buffer, 0, read);
                    else if(read_e > 0)
                        output.append(buffer2, 0, read_e);

                    Log.d(TAG,output.toString());
                }
                // Waits for the command to finish.
                process.waitFor();
                Log.d(TAG, "=============== STOP IperfRunnable run =====================");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
