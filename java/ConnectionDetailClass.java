package com.own.alessio.p2pandon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * P2pAndon Created by alessio on 26.12.15.
 */
public class ConnectionDetailClass implements ConnectionInfoListener
{

    /**
     * Constants.
     */
    private static final String STATUS_CONNECTED= "connected";
    private static final String STATUS_DOWN= "down";
    private static final int REQUEST_CODE = 1;
    private static final int NOTIFICATION_ID = 1;

    /**
     * Status.
     */
    private String connectionStatus = STATUS_DOWN;

    /**
     * Context variable.
     */
    private Context connectionDetailContext = null;

    ConnectionDetailFragment connectionDetailFragment = null;
    WifiP2pInfo myInfo = null;

    MessageServerAsyncTask messageTask = null;

    /**
     * Ringtone to be played on a call for help.
     */
    private Ringtone r = null;
    private NotificationManager myNotificationManager = null;


    /**
     * Constructor to pass the connectionDetailContext.
     * @param mContext
     */
    public ConnectionDetailClass(Context mContext) {
        this.connectionDetailContext = mContext;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        myInfo = info;
        connectionStatus = STATUS_CONNECTED;

        if(info.groupFormed && info.isGroupOwner) {
            listenMessage();
        }


        if(connectionDetailFragment != null) {
            if(connectionDetailFragment.isResumed()) {
                connectionDetailFragment.onConnectionInfo(myInfo);
            }
        }
    }

    /**
     * Setter methods.
     */
    public void setConnectionDetailFragment(ConnectionDetailFragment cdFragment) {
        connectionDetailFragment = cdFragment;
        if(connectionStatus.equals(STATUS_CONNECTED)) {
            if(connectionDetailFragment.isResumed()) {
                connectionDetailFragment.onConnectionInfo(myInfo);
            }
        }
    }

    public void unsetConnectionDetailFragment() {
        connectionDetailFragment = null;
    }

    /**
     * Helper methods.
     */
    public void resetViews() {
        connectionStatus = STATUS_DOWN;
        if(connectionDetailFragment != null) {
            if(connectionDetailFragment.isResumed()) {
                connectionDetailFragment.resetViews();
            }
        }
    }

    public void showDetails(WifiP2pDevice device) {
        if(connectionDetailFragment != null) {
            if(connectionDetailFragment.isResumed()) {
                connectionDetailFragment.showDetails(device);
            }
        }
    }

    public void listenMessage() {
        messageTask = new MessageServerAsyncTask(this);
        messageTask.execute();

    }

    /**
     * Setter methods.
     */
    public void stopListening() {
        if(messageTask != null) {
            messageTask.cancel(true);
        }
    }

    /**
     * Send notification.
     */
    protected void sendNotification(String whoRequestedForHelp) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(connectionDetailContext);
        builder.setSmallIcon(R.drawable.help_me)
                .setContentTitle(connectionDetailContext.getString(R.string.dialog_help_title))
                .setContentText(whoRequestedForHelp + " " + connectionDetailContext.getString(R.string.dialog_help_message))
                .setWhen(System.currentTimeMillis())
                .setTicker(connectionDetailContext.getText(R.string.dialog_help))
                .setOngoing(false)
                .setAutoCancel(true);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        Notification notification = builder.build();
        myNotificationManager =
                (NotificationManager) connectionDetailContext.getSystemService(Context.NOTIFICATION_SERVICE);
        myNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class MessageServerAsyncTask extends AsyncTask<Void, Void, String> {

        private ConnectionDetailClass connectionDetailClass;

        public MessageServerAsyncTask(ConnectionDetailClass connectionDetail) {
            this.connectionDetailClass = connectionDetail;
        }

        @Override
        protected String doInBackground(Void... params) {
            ServerSocket serverSocket = null;
            Socket client = null;
            DataInputStream inputStream = null;

            try {
                /**
                 * From where.
                 */
                serverSocket = new ServerSocket(8988);
                client = serverSocket.accept();

                /**
                 * From where input stream.
                 */
                inputStream = new DataInputStream(client.getInputStream());
                String str = inputStream.readUTF();
                serverSocket.close();
                return str;
            } catch (IOException e) {
                return null;
            } finally {
                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(client != null) {
                    try{
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                this.connectionDetailClass.sendNotification(result);
                this.connectionDetailClass.listenMessage();
            }
        }
    }
}
