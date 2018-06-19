package com.lge.kobinfactory.lgiperf.iperf;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.lge.kobinfactory.lgiperf.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import lombok.Setter;
import lombok.experimental.Accessors;


/**
 * Created by hyukbin.ko on 2018-05-03.
 */

@Accessors(prefix = "m")
public class LGIperfClient {
    private static final boolean DBG = false;
    static final private String TAG = LGIperfClient.class.getSimpleName();

    private LGIperfTask mIperfTask;
    private Context mContext;

    private static final int RETURN_SUCCESS = 0;
    private static final int RETURN_EXCEPTION = 1;
    private static final int RETURN_ARGUMENT = 2;

    private ArrayList<String> outputLog;

    @Setter public int mIperfVersion;
    private String currentOption;

    private LGIperfClient.OnStateChangeListener mListener;
    public void setOnStateChangeListener (LGIperfClient.OnStateChangeListener listener){
        mListener = listener;
    }

    public LGIperfClient(Context context){
        mContext = context;
        mIperfTask = new LGIperfTask();
    }
    public boolean loadIperfFile(){
        if (checkAndCreateIperfFile(LGIperfConstants.IPERF_NAME) && checkAndCreateIperfFile(LGIperfConstants.IPERF3_NAME)){
            Log.i(TAG,"loadIperfFile - success!");
            return true;
        }
        Log.i(TAG,"loadIperfFile - fail!");
        return false;
    }

    private boolean checkAndCreateIperfFile(String iperfName){
        if(!LGIperfConstants.IPERF3_NAME.equals(iperfName) && !LGIperfConstants.IPERF_NAME.equals(iperfName)){
            Log.e(TAG, "checkAndCrateIperfFile : invalid iperfName="+iperfName);
            return false;
        }

        File file = new File(mContext.getFilesDir().getPath()+"/"+iperfName);

        if( !(file.exists() && file.canExecute())){
            Log.i(TAG, "not exist or not execute");
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
                 Log.e(TAG,"loadIperfFile ("+iperfName+") Fail:"+e.toString());
                return false;
            }
        }
        if(DBG) Log.d(TAG,"loadIperfFile ("+iperfName+") success!");
        return true;
    }

    public void start(String option){
        Log.i(TAG,"start :"+option +"/version="+mIperfVersion);
        currentOption = option;

        mIperfTask = new LGIperfTask();
        mIperfTask.execute(new String[]{
                new StringBuilder(mContext.getFilesDir().getPath()+"/")
                        .append((mIperfVersion == LGIperfConstants.IPERF_VERSION2)? LGIperfConstants.IPERF_NAME : LGIperfConstants.IPERF3_NAME)
                        .append(" "+option).append((mIperfVersion == LGIperfConstants.IPERF_VERSION3)? " --forceflush": "")
                        .toString()
        });

    }
    public void stop(){
        if(mIperfTask != null)
            mIperfTask.stop();
        mIperfTask = null;
    }


    class LGIperfTask extends AsyncTask {
        Process iProcess;
        private Integer runIperf(String... arg){
            if(arg == null || arg[0] == null){
                return Integer.valueOf(RETURN_SUCCESS);
            }
            try {
                if(DBG) Log.d(TAG, "runIperf command-"+arg[0] );

                outputLog = new ArrayList<>();

                iProcess = new ProcessBuilder(new String[0]).command(arg[0].split(" ")).
                        redirectErrorStream(true).start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(iProcess.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null ){
                    outputLog.add(line);
                    publishProgress( new String[]{line+"\n"});
                }
                bufferedReader.close();

            } catch (IOException e) {
                Log.e(TAG, "runIperf-exception="+ e.toString() );
                return Integer.valueOf(RETURN_EXCEPTION);
            }
            return Integer.valueOf(RETURN_SUCCESS);
        }

        public final void stop() {
            cancel(true);
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
            if(DBG)Log.d(TAG,"onCancelled" );
            mListener.onStopped();
        }
        @Override
        protected final /* synthetic */ void onPostExecute(Object obj) {
            Integer num = (Integer) obj;
            if (num != null) {
                if(DBG) Log.d(TAG, "onPostExecute - "+ new StringBuilder("onPostExecute got ").append(num.toString()).toString());
                switch (num.intValue()){
                    case RETURN_SUCCESS:
                        Log.i(TAG,"onPostExecute!!");
                        float avg = getResultAvg();
                        Log.i(TAG,"onPostExecute!! avg = "+avg);
                        if(avg != -1) mListener.onResultSave(avg);
                        break;
                    case RETURN_EXCEPTION:
                        break;
                    case RETURN_ARGUMENT:
                        break;
                }
            }
            mListener.onStopped();
        }

        protected final void onProgressUpdate(Object[] objArr) {
            String[] strArr = (String[]) objArr;
            if (strArr != null && strArr[0] != null) {
                if(DBG) Log.d(TAG, "onProgressUpdate-"+strArr[0]+"<end>");
                mListener.onGettingMeesage(strArr[0]);
            }
        }
    }

    private float getResultAvg(){
        if(!currentOption.contains("-c ") || outputLog==null || outputLog.isEmpty() ){
            Log.i(TAG, "getResultAvg not -c or output null or empty");
            return -1;
        }
        if(mIperfVersion == LGIperfConstants.IPERF_VERSION2){
            String lastLog = outputLog.get(outputLog.size()-1);
            String [] logs = lastLog.split(" ");
            for(int i = 0 ; i< logs.length ; i ++){
                if(logs[i].contains("/sec")){
                    float avg = Float.parseFloat(logs[i-1]);
                    if( logs[i].contains("K")) {
                        avg = avg / 1000;
                    }
                    else if( logs[i].contains("M")) {
                        avg = avg ;
                    }
                    else if( logs[i].contains("G")){
                        avg = avg * 1000;
                    }
                    else{
                        avg = avg / (1000 * 1000);
                    }
                    return avg;
                }
            }
            return -1;
        }
        else{
            String resultLog = null;
            for(int i = outputLog.size()-1 ; i >= 0; i--){
                String log = outputLog.get(i);
                if( currentOption.contains("-P") ){
                    if(log.contains("SUM") && log.contains("receiver") ) {
                        resultLog = log;
                    }
                } else{
                    if(log.contains("receiver") ) {
                        resultLog = log;
                    }
                }
            }

            if(resultLog==null) return -1;

            String [] logs = resultLog.split(" ");
            for(int i = 0 ; i< logs.length ; i ++){
                if(logs[i].contains("/sec")){
                    float avg = Float.parseFloat(logs[i-1]);
                    if( logs[i].contains("K")) {
                        avg = avg / 1000;
                    }
                    else if( logs[i].contains("M")) {
                        avg = avg ;
                    }
                    else if( logs[i].contains("G")){
                        avg = avg * 1000;
                    }
                    else{
                        avg = avg / (1000 * 1000);
                    }
                    return avg;
                }
            }
        }


        return -1;
    }

    public interface OnStateChangeListener{
        void onGettingMeesage(String message);
        void onStarted();
        void onResultSave(float avg);
        void onStopped();
    }
}
