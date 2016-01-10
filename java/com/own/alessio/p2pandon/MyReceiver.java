package com.own.alessio.p2pandon;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    /**
     * Constants.
     */
    private final String TAG = this.getClass().getName();
    private final String ACTION_EXTRA = "actionExtra";
    private final String WIFI_STATE_EXTRA = "wifiStateExtra";
    private final String DEVICE_EXTRA = "deviceExtra";
    private final String NET_INFO_EXTRA = "networkInfoExtra";


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG,"Receiver has received this action:" + action);

        if(isMyServiceRunning(AlarmService.class,context)) {

            Intent serviceIntent = new Intent(context,AlarmService.class);
            serviceIntent.putExtra(ACTION_EXTRA,action);
            serviceIntent.putExtra(WIFI_STATE_EXTRA,intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1));
            serviceIntent.putExtra(DEVICE_EXTRA,intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
            serviceIntent.putExtra(NET_INFO_EXTRA,intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO));
            context.startService(serviceIntent);
        }
    }


    /**
     * Check whether service is running.
     */
    private boolean isMyServiceRunning(Class<?> serviceClass, Context mContext) {
        ActivityManager manager =  (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

