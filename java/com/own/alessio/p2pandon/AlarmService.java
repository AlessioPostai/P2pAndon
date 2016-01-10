package com.own.alessio.p2pandon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * All main basic tasks are held and managed by the background service.
 */

public class AlarmService extends Service
    implements WifiP2pManager.ChannelListener {

    /**
     * Constants.
     */
    private final String TAG = this.getClass().getName();
    private final String TAG_POWER = this.getClass().getName() + "_power";
    private final String TAG_WIFI = this.getClass().getName() + "_wifi";
    private final String ACTION_EXTRA = "actionExtra";
    private final String WIFI_STATE_EXTRA = "wifiStateExtra";
    private final String DEVICE_EXTRA = "deviceExtra";
    private final String NET_INFO_EXTRA = "networkInfoExtra";
    private final int NOTIFICATION_ID = 2;

    /**
     * Global variables.
     */

    /**
     * Intent filter:
     * which intents should be detected ?
     */
    private final IntentFilter intentFilter = new IntentFilter();

    /**
     * A channel to the framework.
     */
    private  WifiP2pManager.Channel mChannel;

    /**
     * A manager to manage the connections.
     */
    private WifiP2pManager mManager;

    /**
     * Connections monitoring broadcast receiver, this is the one who receive the intents above.
     */
    private MyReceiver broadCastReceiver = null;

    /**
     * Wifi enable status.
     */
    private boolean isWifiP2pEnabled;
    private WifiManager.WifiLock wifiLock = null;   // A wake lock to keep the wifi on.
    private PowerManager.WakeLock wakeLock = null;  // A wake lock for the CPU.

    /**
     * Device list class.
     */
    private DeviceListClass deviceListClass = null;

    /**
     * Connection info class.
     */
    private ConnectionDetailClass connectionDetailClass = null;

    /**
     * Has been the channel already retried ?
     */
    private boolean retryChannel = false;

    /**
     * Binder.
     */
    private MyLocalBinder myBinder = null;


    /**
     * Notify service is on.
     */
    private NotificationManager notificationManager = null;

    public AlarmService() {
    }

    @Override
    public void onCreate() {

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        /**
         * Initialize device list class.
         */
        deviceListClass = new DeviceListClass(getBaseContext());

        /**
         * Initialize connection info class.
         */
        connectionDetailClass = new ConnectionDetailClass(this);

        /**
         * Retrieve the peer to peer service.
         */
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        /**
         * Initialize the service and retrieve channel.
         */
        mChannel= mManager.initialize(this,getMainLooper(),null);


        /**
         * Initialize myBinder.
         */
        myBinder = new MyLocalBinder();

        Toast.makeText(getBaseContext(),getString(R.string.btn_alarm_on),Toast.LENGTH_LONG).show();

        /**
         * Hold wakelock.
         */
        holdWakeLock(this);

        /**
         * Hold wifi lock.
         */
        holdWifiLock(this);

        //setIsWifiP2pEnabled(isWifiOn());


        broadCastReceiver = new MyReceiver();
        registerReceiver(broadCastReceiver,intentFilter);

        notifyService();

        //resetData();
        //restartDiscoverPeers();
    }


    @Override
    public IBinder onBind(Intent intent) {

        return myBinder;

        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startID) {

        try {

            String action = intent.getStringExtra(ACTION_EXTRA);

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "WIFI_P2P_STATE_CHENGED_ACTION received");
                int state = intent.getIntExtra(WIFI_STATE_EXTRA, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    setIsWifiP2pEnabled(true);
                } else {
                    setIsWifiP2pEnabled(false);
                    resetData();
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION received");
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                if (mManager != null) {
                    mManager.requestPeers(mChannel, deviceListClass);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION received");
                if (mManager == null) {
                    return START_STICKY;
                }

                NetworkInfo networkInfo = intent.getParcelableExtra(NET_INFO_EXTRA);

                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request connection
                    // info to find group owner IP;
                    mManager.requestConnectionInfo(mChannel, connectionDetailClass);
                    notifyConnected();
                } else {
                    // It's a disconnect
                    resetData();
                    restartDiscoverPeers();
                    notifyService();
                }

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION received");
                WifiP2pDevice mDevice = (WifiP2pDevice) intent.getParcelableExtra(DEVICE_EXTRA);
                deviceListClass.updThisDevice(mDevice);
                Log.i("AndonReceiver", "I called updThisDevice on device " +
                        (mDevice == null ? "null" : "not null"));

            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        /**
         * Tell user the alarm service is on.
         */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        /**
         * Release wifi lock.
         */
        releaseWifiLock();

        /**
         * Release wake lock.
         */
        releaseWakeLock();

        connectionDetailClass.stopListening();

        unregisterReceiver(broadCastReceiver);

        stopNotifyingService();

        /**
         * Tell the user the alarm service was stopped.
         */
        Toast.makeText(getBaseContext(),getString(R.string.btn_alarm_off),Toast.LENGTH_LONG).show();
    }

    public class MyLocalBinder extends Binder {
        AlarmService getService() {
            return AlarmService.this;
        }
    }

    /**
     * Helper methods.
     */

    /**
     * Show notification.
     */
    private void notifyService() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.listening)
                .setContentTitle(getString(R.string.notification_service_title))
                .setContentText(getString(R.string.notification_service_text))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);
        Notification notification = builder.build();
        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID,notification);

    }

    private void stopNotifyingService() {
        if(notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void notifyConnected() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.connected)
                .setContentTitle(getString(R.string.notification_connected_title))
                .setContentText(getString(R.string.notification_connected_text))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);
        Notification notification = builder.build();
        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID,notification);
    }


    /**
     * Modify the wifi enable status.
     * @param mIsWifiP2pEnabled wifi enable status.
     */
    public void setIsWifiP2pEnabled(boolean mIsWifiP2pEnabled) {
        this.isWifiP2pEnabled = mIsWifiP2pEnabled;
    }

    /**
     * Reset data in classes.
     */
    public void resetData() {
        deviceListClass.clearPeers();
        connectionDetailClass.resetViews();
    }

    /**
     * Restart discovery peers.
     */
    public boolean restartDiscoverPeers() {
        if (!isWifiP2pEnabled) {
            Toast.makeText(getBaseContext(), R.string.p2p_off_warning,
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getBaseContext(), "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(getBaseContext(), "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
        return true;
    }

    /**
     * Hold wifi lock.
     */
    private void holdWifiLock(Context mContext) {
        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if(wifiLock == null) wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG_WIFI);
        wifiLock.setReferenceCounted(false);
        if(!wifiLock.isHeld()) {
            wifiLock.acquire();
            Log.i(TAG,"wifi lock held.");
        }
    }

    /**
     * Release wifi lock.
     */
    private void releaseWifiLock() {
        if((wifiLock != null) && wifiLock.isHeld()) {
            wifiLock.release();
            Log.i(TAG, "wifi lock released.");
        }
    }

    /**
     * Hold wakelock.
     */
    private void holdWakeLock(Context mContext) {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        if(wakeLock == null) wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG_POWER);

        wakeLock.setReferenceCounted(false);

        if(!wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.i(TAG, "wake lock held.");
        }
    }

    /**
     * Release wakelock.
     */
    private void releaseWakeLock() {
        if((wakeLock != null) && wakeLock.isHeld()) {
            wakeLock.release();
            Log.i(TAG,"wake lock released.");
        }
    }

    /**
     * Getter methods.
     */
    public DeviceListClass getDeviceListClass() {
        return this.deviceListClass;
    }

    public WifiP2pManager getmManager() {
        return mManager;
    }

    public WifiP2pManager.Channel getmChannel() {
        return mChannel;
    }

    /**
     * Return connection info class.
     */
    public ConnectionDetailClass getConnectionDetailClass() {
        return this.connectionDetailClass;
    }

    /**
     * Implements the channel listener interface.
     */
    @Override
    public void onChannelDisconnected() {
        if(mManager != null && !retryChannel) {
            Toast.makeText(getBaseContext(), "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            mManager.initialize(getBaseContext(),getMainLooper(),this);
        } else  {
            Toast.makeText(getBaseContext(),
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }
}