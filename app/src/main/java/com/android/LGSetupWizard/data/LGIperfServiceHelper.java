package com.android.LGSetupWizard.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Created by hyukbin.ko on 2018-08-08.
 */
@Accessors(prefix = "m")
public class LGIperfServiceHelper {
    private static final String TAG = LGIperfServiceHelper.class.getSimpleName();

    private Context mContext;
    private Messenger mServiceMessenger;
    @Getter private boolean mIsBound = false;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceMessenger = new Messenger(service);
            Log.d(TAG,"service connected");

            //ready Iperf and iperf3
            try {
                //TODO change LGIperfConstants
                int REQUEST_INSTALL_IPERF = 0x10;
                mServiceMessenger.send(Message.obtain(null,REQUEST_INSTALL_IPERF));
            } catch (RemoteException e) {
                Log.d(TAG, e.toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            Log.d(TAG,"service disconnected");
        }
    };

    public LGIperfServiceHelper(Context context){
        mContext = context;
    }

    public boolean doBindService(){
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.lge.kobinfactory.lgiperf",
                "com.lge.kobinfactory.lgiperf.iperf.LGIperfService");
        intent.setComponent(cn);
        mIsBound = mContext.bindService(intent,mConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG,"doBindService = "+ mIsBound);
        return mIsBound;
    }

    public void unBindService(){
        if(mIsBound){
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
        Log.d(TAG,"unBindService = "+ mIsBound);
    }

    private void runService(int service_code, String message){
        try {
            Bundle b = new Bundle();
            b.putString("key",message);

            Message m = Message.obtain(null,service_code);
            m.setData(b);

            mServiceMessenger.send(m);
        } catch (RemoteException e) {
            Log.d(TAG, "runService exception:"+e.toString());
        }
    }
    private void runService(int service_code){
        try {
            mServiceMessenger.send(Message.obtain(null,service_code));
        } catch (RemoteException e) {
            Log.d(TAG, "runService exception:"+e.toString());
        }
    }

    public void startCommand(String command){
        Log.d(TAG,"start command = "+command);
        runService(LGIperfConstants.Message.REQUEST_RUN_COMMAND, command);
    }

    public void stopCommand(){
        runService(LGIperfConstants.Message.REQUEST_STOP_COMMAND);
    }

    public void checkIperf(){
        runService(LGIperfConstants.Message.REQUEST_INSTALL_IPERF);
    }
}
