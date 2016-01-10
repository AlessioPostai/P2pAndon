package com.own.alessio.p2pandon;

/**
 * Andon Created by alessio on 20.11.15.
 */

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class MessageTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 60000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_MY_NAME = "extrasMyName";
    private final int NOTIFICATION_ID = 3;

    private String myIdentification = null;

    public MessageTransferService(String name) {
        super(name);
    }

    public MessageTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * As an intent is received send the file requested.
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        /**
         * Retrieve identification.
         */
        myIdentification = intent.getStringExtra(EXTRAS_MY_NAME);

        /**
         * Application context.
         */
        Context context = getApplicationContext();

        /**
         * If the intent is a send file.
         */
        if (intent.getAction().equals(ACTION_SEND_FILE)) {

            /**
             * Retrieve the host name.
             */
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            /**
             * Create socket.
             */
            Socket socket = new Socket();
            /**
             * Retrieve port.
             */
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            DataOutputStream stream = null;
            try {
                /**
                 * Connect the socke, host and port are needed.
                 */
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                /**
                 * Now that socked is connected, retrieve output stream.
                 */
                stream = new DataOutputStream(socket.getOutputStream());
                stream.writeUTF(myIdentification);
                notifyMessageSent();

            } catch (IOException e) {;
                e.printStackTrace();
            } finally {
                if(stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                 if (socket != null) {

                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // Give up
                                e.printStackTrace();
                            }
                        }
                    }
            }
        }
    }

    /**
     * Helper methods.
     */
    private void notifyMessageSent() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.prayer)
                .setContentTitle(getString(R.string.notification_message_title))
                .setContentText(getString(R.string.notification_message_text))
                .setTicker(getString(R.string.notification_message_text))
                .setWhen(System.currentTimeMillis())
                .setOngoing(false);
        Notification notification = builder.build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID,notification);
    }
}