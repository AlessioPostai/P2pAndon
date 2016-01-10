package com.own.alessio.p2pandon;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Andon Created by alessio on 19.11.15.
 */

/**
 * DeviceListFragment show peer lists.
 * Implements the PeerListListener
 */
public class DeviceListFragment extends ListFragment  {


    /**
     * Fragment content view.
     */
    View mContentView = null;

    /**
     * Refresh progress dialog.
     */
    ProgressDialog progressDialog = null;

    /**
     * Instance of DeviceListClass.
     */
    private DeviceListClass deviceListClass = null;

    /**
     * Manage fragment state as the activity is created.
     * @param savedInstanceState fragment state.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,
                             Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_list_fragment, null);

        return mContentView;
    }

    /**
     * Return the device status description.
     * @param deviceStatus int device status.
     * @return string device status.
     */
    private static String getDeviceStatus(int deviceStatus) {
        return DeviceListClass.getDeviceStatus(deviceStatus);
    }

    /**
     * As the item is clicked show the detail.
     * @param l list view included in the list-fragment.
     * @param v view.
     * @param position position within the list.
     * @param id item id.
     */
    @Override
    public void onListItemClick(ListView l,View v,int position,long id) {

        /**
         * Retrieve the the device from the list.
         */
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);

        /**
         * Show device details.
         */
        ((DeviceActionListener) getActivity()).showDetails(device);
    }

    /**
     * A method to clear the peers list.
     */
    public void clearPeers() {
        try {
            ((DeviceListClass.WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show progress dialog for discovery peers.
     */
    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
    }

    /**
     * Implements deviceListInterface.
     */
    //@Override
    public void onPeersFound() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        try {
            ((DeviceListClass.WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    /**
     * Update UI for this device.
     * @param device WifiP2pDevice object
     */
    //@Override
    public void updateThisDevice(WifiP2pDevice device) {
        if(device != null) {
            TextView view = (TextView) mContentView.findViewById(R.id.my_name);
            view.setText(device.deviceName);
            view = (TextView) mContentView.findViewById(R.id.my_status);
            view.setText(getDeviceStatus(device.status));
        }

    }

    /**
     * Setter methods.
     */
    public void setDeviceListClass(DeviceListClass mDeviceListClass) {
        deviceListClass = mDeviceListClass;

        deviceListClass.setDeviceListFragment(this);

        this.setListAdapter(deviceListClass.getWiFiPeerListAdapter(getActivity(), R.layout.row_devices));
    }

    public void unsetDeviceListClass() {
        deviceListClass.unSetDeviceListFragment();
        deviceListClass = null;
        this.getListView().invalidate();
    }


    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.Methods are implemented in the main activity.
     */
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void connect(WifiP2pConfig config);

        void disconnect();
    }

}
