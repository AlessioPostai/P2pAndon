package com.own.alessio.p2pandon;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;


/**
 * TODO: idea !!!
 * TODO: a) memorize the status, 0) service off, 1) service on, 2) disconnected
 * TODO: 3) connected
 * TODO: b) memorize with who am I connected
 * TODO: c) periodically I'll check and automatically try to restore the connection.
 * TODO: d) make all the above with a content provider.
 */

/**
 * Main activity, implements ChannelListener.
 * The main activity has following functions:
 *    - starts/stops background service, that independently handle communication;
 *    - set-up connection fragments with class;
 *    - handle commands to fragments sent from the user interface.
 */

public class MainActivity extends AppCompatActivity
    implements DeviceListFragment.DeviceActionListener {

    /**
     * Constants.
     */
    private static final int RESULT_SETTINGS = 1;                   // Result code for user setting activity.
    private static final String MY_NAME = "myName";                 // Key to my name in user setting.
    private static final String MY_DEFAULT_NAME = "Operator";       // Default name.
    private static final String MY_NUMBER = "myNumber";             // Key to my number.
    private static final String MY_DEFAULT_NUMBER = "1";            // Default number in the group as String.

    /**
     * Global variables.
     */
    private String myIdentification = MY_DEFAULT_NAME;              // My name, it goes into the request for help.
    private int myNumber = Integer.parseInt(MY_DEFAULT_NUMBER);     // My number in the group.

    /**
     * Communication section.
     */
    private WifiP2pManager mManager;            // Manager: includes methods to handle connections.
    private  WifiP2pManager.Channel mChannel;   // Channel to the framework.

    /**
     * AlarmService.
     */
    private AlarmService alarmService = null;   // The background service.
    private boolean isBound = false;            // Boolean variable that tells if the service is connected.

    /**
     * Override the main activity creation.
     * @param savedInstanceState main activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Other activity life cycle overridings.
     */

    /**
     * On resume, bind to the background service if active..
     */
    @Override
    public void onResume() {
        super.onResume();

        /**
         * Retrieve default shared preferences.
         */
        myIdentification = getMyName();
        myNumber = getMyNumber();

        /**
         * Retrieve the alarm service status.
         */
        if(isMyServiceRunning(AlarmService.class)) {
            doBindService();
        }
   }

    /**
     * OnPause do nothing.
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * On stop, unbind background service but leave it alive.
     */
    @Override
    public void onStop() {
        super.onStop();
        if(isMyServiceRunning(AlarmService.class) && isBound) {
            doUnbindService();
        }
    }

    /**
     * Managing options menu.
     * @param menu my options menu.
     * @return always true.
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /**
     * Do selected action.
     * @param item menu item.
     * @return always true.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_user_setting:
                Intent i = new Intent(this, UserSettingActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                return true;
            case R.id.atn_direct_enable:
                /**
                 * Launch the wifi setting menu.
                 */
                if (mManager != null && mChannel != null && isMyServiceRunning(AlarmService.class)) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                }
                return true;

            case R.id.atn_direct_discover:
                /**
                 * Restart discover peers.
                 */
                restartDiscoverPeers();
                return true;

            case R.id.atn_service:
                /**
                 * If the service is not running start it and bind to it,
                 * otherwise unbind if necessary, then stop the service.
                 */
                if(!isMyServiceRunning(AlarmService.class)) {
                    startAlarmService();
                    doBindService();
                    //startMyReceiver();
                } else {

                    if(isBound) {
                        doUnbindService();
                    }
                    stopAlarmService();;
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Retriving user setting.
     * @param requestCode the request code.
     * @param resultCode the result code.
     * @param data the data intent.
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                myIdentification = getMyName();
                myNumber = getMyNumber();
                break;
        }
    }

    /**
     * Retrieve data from user settings.
     */

    /**
     * Retrieve my name.
     */
    private String getMyName() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs.getString(MY_NAME,MY_DEFAULT_NAME);
    }

    /**
     * Retrieve my number in the group.
     */
    private int getMyNumber() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int resultNumber = Integer.parseInt(MY_DEFAULT_NUMBER);
        try {
            resultNumber = Integer.parseInt(sharedPrefs.getString(MY_NUMBER, MY_DEFAULT_NUMBER));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return resultNumber;
    }

    /**
     * Implements device action listener.
     */

    /**
     * Make ConnectionDetailFragment to show the connected device details.
     * @param device connected device.
     */
    @Override
    public void showDetails(WifiP2pDevice device) {
        if(isMyServiceRunning(AlarmService.class) && isBound && (alarmService != null)) {
            alarmService.getConnectionDetailClass().showDetails(device);
        }
    }

    /**
     * Connect to a specified device.
     * @param config configuration.
     */
    @Override
    public void connect(WifiP2pConfig config) {
        if(isMyServiceRunning(AlarmService.class) && (mManager != null) && (mChannel != null) &&
                isBound && (alarmService != null)) {
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Disconnect a connected device.
     */
    @Override
    public void disconnect() {
        if(isMyServiceRunning(AlarmService.class) && (mManager != null) && (mChannel != null) &&
                isBound && (alarmService != null)) {
            alarmService.getConnectionDetailClass().resetViews();

            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                }

                @Override
                public void onSuccess() {

                }

            });
            restartDiscoverPeers();
        }
    }

    /**
     * WifiP2p connection handling.
     */

    /**
     * Discover peers.
     */
    public void restartDiscoverPeers() {
        if(isMyServiceRunning(AlarmService.class) && isBound && (alarmService != null)) {
            alarmService.restartDiscoverPeers();
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            fragment.onInitiateDiscovery();
        }
    }

    /**
     * Helper methods.
     */

    /**
     * Service section.
     */

    /**
     * Start alarm service.
     */
    public void startAlarmService() {
        Intent mIntent = new Intent(getBaseContext(),AlarmService.class);
        startService(mIntent);
    }

    /**
     * Stop alarm service.
     */
    public void stopAlarmService() {
        stopService(new Intent(getBaseContext(), AlarmService.class));
    }

    /**
     * Bind alarm service.
     */
    private void doBindService() {
        if(!isBound) {
            Intent bindIntent = new Intent(this,AlarmService.class);
            isBound = bindService(bindIntent,myConnection,0);
        }
    }

    /**
     * Unbind alarm service.
     */
    private void doUnbindService() {
        unbindService(myConnection);
        isBound = false;
    }

    /**
     * Service connection interface.
     */
    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            alarmService = ((AlarmService.MyLocalBinder) service).getService();
            isBound = true;
            /**
             * Retrieve WifiP2pManager and Channel.
             */
            mManager = alarmService.getmManager();
            mChannel = alarmService.getmChannel();
            /**
             * Pass DeviceListClass instance to the DeviceListFragment
             * pass ConnectionDetailClass to the ConnectionDetailFragment.
             */
            ((DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list))
                    .setDeviceListClass(alarmService.getDeviceListClass());
            ((ConnectionDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail))
                    .setConnectionDetailClass(alarmService.getConnectionDetailClass());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ((DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list))
                    .unsetDeviceListClass();
            ((ConnectionDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail))
                    .unsetConnectionDetailClass();
            alarmService = null;
            isBound = false;
       }
    };

    /**
     * Check whether service is running.
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager =  (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Getter methods.
     */
    public String getYourName() {
        return myIdentification;
    }

    public int getNumberInGroup() {
        return myNumber;
    }

}