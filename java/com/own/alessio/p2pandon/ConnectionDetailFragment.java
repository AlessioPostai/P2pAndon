package com.own.alessio.p2pandon;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.own.alessio.p2pandon.DeviceListFragment.DeviceActionListener;

/**
 * Andon Created by alessio on 19.11.15.
 */
public class ConnectionDetailFragment extends Fragment {

    /**
     * Class variables.
     */

    private static String GROUP_OWNER = null; // The owner's group name, to be initialized as the activity will be attached.

    private View mContentView = null; // View containing the connection's detail fragment.

    private WifiP2pDevice device;  // Connected device.

    ProgressDialog progressDialog = null;   // Progress dialog.

    ConnectionDetailClass connectionDetailClass = null;

    WifiP2pInfo myInfo = null;

    /**
     * As the activity is created manage the fragment state.
     * @param savedInstanceState fragment state.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /**
         * Assign the name for the group owner.
         */
        GROUP_OWNER = getActivity().getString(R.string.group_owner_name);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState) {

        /**
         * Inflate the fragment layout.
         */
        mContentView = inflater.inflate(R.layout.connection_detail_fragment, null);

        /**
         * Connect button.
         */
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();

                /**
                 * Retrieve device configuration.
                 */
                config.deviceAddress = device.deviceAddress;

                /**
                 * Set this device as group owner or client.
                 */
                config.groupOwnerIntent = ((MainActivity) getActivity()).getNumberInGroup();


                /**
                 * Set-up the wps configuration.
                 */
                config.wps.setup = WpsInfo.PBC;

                /**
                 * Show progress for device connection.
                 */
                if((progressDialog != null) && progressDialog.isShowing()) {
                    progressDialog = ProgressDialog.show(getActivity(),"Press back to cancel",
                            "Connecting to" + device.deviceAddress,true,true);
                }

                /**
                 * Start connection.
                 */
                ((DeviceActionListener) getActivity()).connect(config);
            }
        });

        /**
         * Button to disconnect the device.
         */
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DeviceActionListener) getActivity()).disconnect();
            }
        });

        /**
         * Button to send a message.
         */
        mContentView.findViewById(R.id.btn_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(getActivity(),MessageTransferService.class);
                serviceIntent.setAction(MessageTransferService.ACTION_SEND_FILE);
                serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        myInfo.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(MessageTransferService.EXTRAS_GROUP_OWNER_PORT,8988);
                serviceIntent.putExtra(MessageTransferService.EXTRAS_MY_NAME,
                        ((MainActivity) getActivity()).getYourName());
                getActivity().startService(serviceIntent);

            }
        });


        return mContentView;
    }

    public void onConnectionInfo(WifiP2pInfo info) {

        myInfo = info;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

            this.getView().setVisibility(View.VISIBLE);


        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        if(!info.isGroupOwner) {
            mContentView.findViewById(R.id.btn_help).setVisibility(View.VISIBLE);
        }


        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_help).setVisibility(View.GONE);

        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);

        this.getView().setVisibility(View.GONE);
    }

    /**
     * Setter methods.
     */
    public void setConnectionDetailClass(ConnectionDetailClass mConnectionDetailClass) {
        connectionDetailClass = mConnectionDetailClass;
        connectionDetailClass.setConnectionDetailFragment(this);
    }

    public void unsetConnectionDetailClass() {
        connectionDetailClass.unsetConnectionDetailFragment();
        connectionDetailClass = null;
    }




}
