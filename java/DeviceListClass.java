package com.own.alessio.p2pandon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;;

import java.util.ArrayList;
import java.util.List;


/**
 * P2pAndon Created by alessio on 20.12.15.
 */
public class DeviceListClass implements PeerListListener {

    /**
     * Instance of the context.
     */
    private Context context = null;
    private DeviceListFragment deviceListFragment = null;

    private WifiP2pDevice myDevice = null;

    /**
     * Peers devices list.
     */
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    /**
     * Constructor method:
     *    - it takes the string owner name as parameter.
     */
    public DeviceListClass(Context mContext) {
        this.context = mContext;
    }

    /**
    * Return the device status description.
    * @param deviceStatus int device status.
    * @return string device status.
    */
    protected static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    /**
     * Update UI for this device.
     * @param device WifiP2pDevice object
     */
    public void updThisDevice(WifiP2pDevice device) {
        this.myDevice = device;
            deviceListFragment.updateThisDevice(this.myDevice);
        Log.i("AndonDeviceListClass", "updThisDevice to " + device);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peersList) {
        peers.clear();
        peers.addAll(peersList.getDeviceList());
        //checkPeers(peers);
        if(deviceListFragment != null) {
            deviceListFragment.onPeersFound();
        }
        if (peers.size() == 0) {
            return;
        }
    }

    /**
     * Helper methods.
     */

    /**
     * A method to clear the peers list.
     */
    public void clearPeers() {
        peers.clear();
        if(deviceListFragment != null) {
            deviceListFragment.clearPeers();
        }
    }

    /**
     * Getter methods.
     */
    public void setDeviceListFragment(DeviceListFragment mDeviceListFragment) {
        deviceListFragment = mDeviceListFragment;
        if(myDevice != null) {
            deviceListFragment.updateThisDevice(this.myDevice);
            Log.i("AndonDeviceListClass", "updateThisDeviceCalled");
        }
    }

    public void  unSetDeviceListFragment() {
        deviceListFragment = null;
    }

    /**
     * @param context the context.
     * @param textViewResourceId text view Id.
     * @return WiFiPeerListAdapter.
     */
    public WiFiPeerListAdapter getWiFiPeerListAdapter(Context context, int textViewResourceId) {
        return new WiFiPeerListAdapter(context, textViewResourceId, peers);
    }

    /********************************************************************
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     * Dynamic class.
     *******************************************************************/
    protected class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        /**
         * Items list, it's a class field.
         */
        private List<WifiP2pDevice> items;
        private int textViewResId;

        /**
         * Constructor.
         * @param context context.
         * @param textViewResourceId resource list id.
         * @param objects objects included in the list.
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            /**
             * Assign objects as adapter's items.
             */
            items = objects;
            textViewResId = textViewResourceId;
        }

        /**
         * Return the view given the item's list.
         * @param position item's position.
         * @param convertView view.
         * @param parent parent's view.
         * @return returned view.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            /**
             * Retrieve the view showing a single device.
             */
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(textViewResId, null);
            }

            /**
             *Retrieve the device.
             */
            WifiP2pDevice device = items.get(position);

            /**
             * Assign the device data to the text field.
             */
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                }
            }
            return v;
        }
    }
}

