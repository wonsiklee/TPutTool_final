package com.android.LGSetupWizard.clients;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.LGSetupWizard.R;
import com.android.LGSetupWizard.data.LGIperfCommand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import lombok.Setter;
import lombok.experimental.Accessors;


/**
 * Created by hyukbin.ko on 2018-05-03.
 */

//TODO need Permissive mode ! because of System app.
//TODO need fix encording output issue
@Accessors(prefix = "m")
public class LGIperfClient {
    static final private String TAG = LGIperfClient.class.getSimpleName();

    //private LGIperfRunnable mIperfRunnable;
    private LGIperfTask mIperfTask;
    private Context mContext;


    @Setter public int mIperfVersion;

    private LGIperfClient.OnStateChangeListener mListener;
    public void setOnStateChangeListener (LGIperfClient.OnStateChangeListener listener){
        mListener = listener;
    }

    public LGIperfClient(Context context){
        mContext = context;
        //mIperfRunnable = new LGIperfRunnable();
        mIperfTask = new LGIperfTask();
    }
    public boolean loadIperfFile(){
        if (checkAndCreateIperfFile("iperf") && checkAndCreateIperfFile("iperf3")){
            Log.d(TAG,"loadIperfFile - success!");
            return true;
        }
        Log.d(TAG,"loadIperfFile - fail!");
        return false;
    }

    private boolean checkAndCreateIperfFile(String iperfName){
        if(!LGIperfConstants.IPERF3_NAME.equals(iperfName) && !LGIperfConstants.IPERF_NAME.equals(iperfName)){
            Log.e(TAG, "checkAndCrateIperfFile : invalid iperfName="+iperfName);
            return false;
        }

        File file = new File(mContext.getFilesDir().getPath()+"/"+iperfName);

        if( !(file.exists() && file.canExecute())){
            InputStream sOpenRawResource = LGIperfConstants.IPERF_NAME.equals(iperfName)?
                    mContext.getResources().openRawResource(R.raw.iperf):mContext.getResources().openRawResource(R.raw.iperf3);

            try {
                byte[] sBuffer = new byte[sOpenRawResource.available()];
                sOpenRawResource.read(sBuffer);
                sOpenRawResource.close();
                FileOutputStream sOpenFileOutput = mContext.openFileOutput(iperfName, Context.MODE_PRIVATE);
                sOpenFileOutput.write(sBuffer);
                sOpenFileOutput.close();
                mContext.getFileStreamPath(iperfName).setExecutable(true);

            }catch (Exception e){
                Log.d(TAG,"loadIperfFile ("+iperfName+") Fail:"+e.toString());
                return false;
            }
        }
        Log.d(TAG,"loadIperfFile ("+iperfName+") success!");
        return true;
    }

    public void start(){

    }

    public void start(String option){
        mIperfTask = new LGIperfTask();
        StringBuilder sCmdBuilder = new StringBuilder(
                (mIperfVersion == LGIperfConstants.IPERF_VERSION2)?
                        mContext.getFilesDir().getPath()+"/"+LGIperfConstants.IPERF_NAME:
                 mContext.getFilesDir().getPath()+"/"+LGIperfConstants.IPERF3_NAME).append(" ");
        sCmdBuilder.append(option);
        //mIperfRunnable.run(sCmdBuilder.toString().split(" "));*/

        mIperfTask.execute(new String[]{sCmdBuilder.toString()});
    }
    public void stop(){
        //mIperfRunnable.stop();
        if(mIperfTask != null)
            mIperfTask.stop();

        mIperfTask = null;
    }


    class LGIperfTask extends AsyncTask {
        String ITAG = "LGIperfTask";
        Process iProcess;
        private Integer runIperf(String... arg){
            if(arg == null || arg[0] == null){
                return Integer.valueOf(99);
            }
            try {
                iProcess = new ProcessBuilder(new String[0]).command(arg[0].split(" ")).redirectErrorStream(true).start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(iProcess.getInputStream()));
                int sRead = 0;
                char[] sBuffer = new char[4096];
                /*
                while (bufferedReader.readLine() != null) {
                    publishProgress(new String[]{bufferedReader.readLine() + "\n"});
                }*/
                while ((sRead = bufferedReader.read(sBuffer)) > 0){
                        final String sResult = new StringBuffer().append(sBuffer, 0, sRead).toString();
                        publishProgress( new String[]{sResult});
                }
            } catch (IOException e) {
                Log.e(ITAG, "runIperf-exception="+ e.toString() );
                iProcess.destroy();
                iProcess = null;
            }

            return Integer.valueOf(0);
        }

        public final void stop() {
            cancel(false);
            if (iProcess != null) {
                iProcess.destroy();
                iProcess = null;
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            return runIperf((String[])objects);
        }

        @Override
        protected final /* synthetic */ void onCancelled(Object obj) {
            Log.d(ITAG,"onCancelled" );

            mListener.onStopped();
        }
        @Override
        protected final /* synthetic */ void onPostExecute(Object obj) {
            Integer num = (Integer) obj;
            if (num != null) {
                Log.d(ITAG, "onPostExecute - "+ new StringBuilder("onPostExecute got ").append(num.toString()).toString());
                if (num.intValue() == 88) {

                }
            }

            mListener.onStopped();
        }

        protected final /* synthetic */ void onProgressUpdate(Object[] objArr) {
            String[] strArr = (String[]) objArr;
            if (strArr != null && strArr[0] != null) {
                Log.d(ITAG, "onProgressUpdate-"+strArr[0]+"<end>");
                mListener.onGettingMeesage(strArr[0]);
            }
        }


    }

    /*
    class LGIperfRunnable implements Runnable  {
        String ITAG = "LGIperfRunnable";
        String iOption = null;
        Process iProcess;
        String iCmd[];

        public void stop(){
            iOption = null;
            iCmd = null;
            iProcess.destroy();

        }
        @Override
        public void run() {
            try {
                Log.d(ITAG, "=============== START IperfRunnable run =====================");
                iProcess = new ProcessBuilder().command(iCmd).redirectErrorStream(true).start();

                // Reads stdout.
                // NOTE: You can write to stdin of the command using
                //       process.getOutputStream().
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(iProcess.getInputStream()));


                int sRead =0;

                char[] buffer = new char[4096];

                while ( (sRead = reader.read(buffer)) > 0 ) {
                    final String sResult = new StringBuffer().append(buffer, 0, sRead).toString();
                    new Runnable(){

                        @Override
                        public void run() {
                            if(mListener != null ) mListener.onGettingMeesage(sResult);
                        }
                    }.run();

                    Log.d(ITAG,sResult);
                }
                // Waits for the command to finish.
                reader.close();
                Log.d(ITAG, "=============== STOP IperfRunnable run =====================");
            } catch (IOException e) {
                Log.d(ITAG, "run IOException =" + e.toString());
            } finally {
                iCmd = null;
            }
            LGIperfRunnable.this.stop();
            if(mListener != null ) mListener.onStopped();
        }

        public void run(String[] cmd){
            iCmd = cmd;
            LGIperfRunnable.this.run();
        }
    }*/

    public interface OnStateChangeListener{
        void onGettingMeesage(String message);
        void onStarted();
        void onStopped();
    }
}
