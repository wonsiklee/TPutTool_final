package com.android.LGSetupWizard.data;

import android.util.Log;

import com.android.LGSetupWizard.Util.LGIperfUtils;
import com.android.LGSetupWizard.clients.LGIperfConstants;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-05-18.
 */

@Accessors(prefix="m")
public class LGIperfCommand {
    @Getter @Setter private int mVersion;
    @Getter @Setter private int mMode = -1;
    @Getter @Setter private String mHost;
    @Getter @Setter private int mPort;
    @Getter @Setter private boolean mReverMode = false;
    @Getter @Setter private boolean mUDPmode = false;
    @Getter @Setter private int mRate = -1;
    @Getter @Setter private int mRateUnit = -1;
    @Getter @Setter private int mDuration = -1;
    @Getter @Setter private int mInterval = -1;
    @Getter @Setter private int mStream =-1 ;
    @Getter @Setter private String mOtherOptions="";

    @Getter private boolean isValid = true;

    private static final String TAG = LGIperfCommand.class.getSimpleName();

    public LGIperfCommand(String command){

        String subString;
        if (command.startsWith(LGIperfConstants.IPERF3_NAME)) {
            mVersion = LGIperfConstants.IPERF_VERSION3;
            subString  = command.substring(LGIperfConstants.IPERF3_NAME.length()+1);
        }else if(command.startsWith("iperf")) {
            mVersion = LGIperfConstants.IPERF_VERSION2;
            subString  = command.substring(LGIperfConstants.IPERF_NAME.length()+1);
        }else {
            isValid = false;
            return;
        }

        String[] commandOptions = subString.split(" ");
        Log.i(TAG,"commandOptions = ");
        for(int i=0; i< commandOptions.length ; i++)
            Log.i(TAG, "       " +commandOptions[i]);

        if (commandOptions == null || commandOptions.length == 0 ) return;

        for(int i =0; i < commandOptions.length ; i++){
            switch(commandOptions[i]){
                case "-c" : mMode = LGIperfConstants.IPERF_MODE_CLIENT;
                    i++;
                    if (i >= commandOptions.length){
                        isValid = false;
                        return;
                    }
                    if(LGIperfUtils.isValidIpAddress(commandOptions[i]) || LGIperfUtils.isValidDNS(commandOptions[i]))
                        mHost = commandOptions[i];
                    break;

                case "-s" : mMode = LGIperfConstants.IPERF_MODE_SERVER;
                    break;

                case "-R" : mReverMode = true;
                    break;

                case "-u" : mUDPmode = true ;
                    break;

                case "-b" :
                    i++;
                    if (i >= commandOptions.length) {
                        isValid = false;
                        return;
                    }

                    if(!LGIperfUtils.isValidRate(commandOptions[i])){
                        isValid = false;
                        return;
                    }
                    if(commandOptions[i].contains("K") || commandOptions[i].contains("k")){
                        mRateUnit = LGIperfConstants.IPERF_RATEUNIT_KBPS;
                        commandOptions[i].replace("K","");
                        commandOptions[i].replace("k","");
                    }else if (commandOptions[i].contains("M") || commandOptions[i].contains("m") ){
                        mRateUnit = LGIperfConstants.IPERF_RATEUNIT_MBPS;
                        commandOptions[i].replace("M","");
                        commandOptions[i].replace("m","");
                    }else{
                        mRateUnit = LGIperfConstants.IPERF_RATEUNIT_BPS;
                    }
                    mRate = Integer.valueOf(commandOptions[i]);
                    break;

                case "-t":
                    i++;
                    if (i >= commandOptions.length) {
                        isValid = false;
                        Log.e(TAG, "-t need value!");
                        return;
                    }
                    Log.d(TAG, "-t value = "+commandOptions[i] +">> " + Integer.valueOf(commandOptions[i]) );
                    mDuration = Integer.valueOf(commandOptions[i]);
                    break;

                case "-i" :
                    i++;
                    if (i >= commandOptions.length) {
                        isValid = false;
                        Log.e(TAG, "-i need value!");
                        return;
                    }
                    Log.d(TAG, "-i value = "+commandOptions[i] +">> " + Integer.valueOf(commandOptions[i]) );
                    mInterval = Integer.valueOf(commandOptions[i]);
                    break;
                case "-p" :
                    i++;
                    if (i >= commandOptions.length) {
                        isValid = false;
                        Log.e(TAG, "-p need value!");
                        return;
                    }
                    Log.d(TAG, "-p value = "+commandOptions[i] +">> " + Integer.valueOf(commandOptions[i]) );
                    mPort = Integer.valueOf(commandOptions[i]);
                    break;

                case "-P" :
                    i++;
                    if (i >= commandOptions.length) {
                        isValid = false;
                        return;
                    }
                    Log.d(TAG, "-P value = "+commandOptions[i] +">> " + Integer.valueOf(commandOptions[i]) );
                    mStream = Integer.valueOf(commandOptions[i]);
                    break;
                default : //todo others.
                    if(!mOtherOptions.isEmpty()) mOtherOptions += " ";
                    mOtherOptions += commandOptions[i];
            }

        }
    }

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append( (mVersion==LGIperfConstants.IPERF_VERSION2)?LGIperfConstants.IPERF_NAME: LGIperfConstants.IPERF3_NAME )
                .append( (mMode == LGIperfConstants.IPERF_MODE_CLIENT)? " -c":"")
                .append( (mMode == LGIperfConstants.IPERF_MODE_CLIENT && mHost!=null)? " "+mHost:"")
                .append( (mMode == LGIperfConstants.IPERF_MODE_SERVER)? " -s":"")
                .append( (mReverMode)? " -r":"")
                .append( (mUDPmode)? " -u":"")
                .append( (mRate!=-1 && mRateUnit!=-1)? " -b "+mRate+ LGIperfConstants.toStringRateUnit(mRate):"")
                .append( (mDuration!=-1)? " -t "+mDuration:"")
                .append( (mInterval!=-1)? " -i "+mInterval:"")
                .append( (mPort!=-1)? " -p "+mPort:"")
                .append( (mStream!=-1)? " -P "+mStream:"")
                .append( (mOtherOptions!=null)? " "+ mOtherOptions:"");
        return stringBuilder.toString();
    }
}
