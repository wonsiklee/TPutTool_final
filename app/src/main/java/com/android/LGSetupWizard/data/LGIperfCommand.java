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
    @Getter @Setter private int mMode = LGIperfConstants.IPERF_NOT_SET;
    @Getter @Setter private String mHost;
    @Getter @Setter private int mPort = LGIperfConstants.IPERF_NOT_SET;
    @Getter @Setter private boolean mUDPmode = false;
    @Getter @Setter private int mRate = LGIperfConstants.IPERF_NOT_SET;
    @Getter @Setter private int mRateUnit = LGIperfConstants.IPERF_NOT_SET;
    @Getter @Setter private int mDuration = LGIperfConstants.IPERF_NOT_SET;
    @Getter @Setter private int mInterval = LGIperfConstants.IPERF_NOT_SET;
    @Getter @Setter private int mStream = LGIperfConstants.IPERF_NOT_SET ;
    @Getter @Setter private String mOtherOptions="";

    @Getter private boolean isValid = true;

    private static final String TAG = LGIperfCommand.class.getSimpleName();

    public LGIperfCommand(String command){

        String subString;
        if (command.startsWith(LGIperfConstants.IPERF3_NAME)) {
            mVersion = LGIperfConstants.IPERF_VERSION3;
            if(command.equals(LGIperfConstants.IPERF3_NAME))return;
            subString  = command.substring(LGIperfConstants.IPERF3_NAME.length()+1);
        }else if(command.startsWith(LGIperfConstants.IPERF_NAME)) {
            mVersion = LGIperfConstants.IPERF_VERSION2;
            if(command.equals(LGIperfConstants.IPERF_NAME))return;
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
                    String temp = commandOptions[i];
                    if(temp.contains("K") || temp.contains("k")){
                        mRateUnit = LGIperfConstants.IPERF_RATEUNIT_KBPS;
                        temp = temp.replace("K","");
                        temp = temp.replace("k","");
                    }else if (temp.contains("M") || temp.contains("m") ){
                        mRateUnit = LGIperfConstants.IPERF_RATEUNIT_MBPS;
                        temp = temp.replace("M","");
                        temp = temp.replace("m","");
                    }else{
                        mRateUnit = LGIperfConstants.IPERF_RATEUNIT_BPS;
                    }

                    mRate = Integer.valueOf(temp);
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
                .append( (mMode == LGIperfConstants.IPERF_MODE_SERVER)? " -s":"")
                .append( (mMode == LGIperfConstants.IPERF_MODE_CLIENT && mHost!=null)? " "+mHost:"")
                .append( (mUDPmode)? " -u":"")
                .append( (mUDPmode && mRate!=LGIperfConstants.IPERF_NOT_SET && mRateUnit!=LGIperfConstants.IPERF_NOT_SET)?
                        " -b "+mRate+ LGIperfConstants.toStringRateUnit(mRateUnit):"")
                .append( (mPort!=LGIperfConstants.IPERF_NOT_SET)? " -p "+mPort:"")
                .append( (mInterval!=LGIperfConstants.IPERF_NOT_SET)? " -i "+mInterval:"")
                .append( (mDuration!=LGIperfConstants.IPERF_NOT_SET)? " -t "+mDuration:"")
                .append( (mStream!=LGIperfConstants.IPERF_NOT_SET)? " -P "+mStream:"")
                .append( (mOtherOptions!=null)? " "+ mOtherOptions:"");
        return stringBuilder.toString();
    }
}
