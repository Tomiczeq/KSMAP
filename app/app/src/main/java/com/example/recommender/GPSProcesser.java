package com.example.recommender;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GPSProcesser extends Service {
    private static final String TAG = "GPSProcesser";
    private static final String CHANNEL_ID = "2222";
    private static final int NOTIFICATION_ID = 2222;
    private static final String TITLE = "GPSProcesser";
    protected NotificationManager notificationManager;

    private class ProcesserThread extends Thread {

        @Override
        public void run() {
            Looper.prepare();
            while (true) {
                Log.i(TAG, "Processing collected GPS logs");
                Context context = getBaseContext();
                File dir = context.getFilesDir();

                List<List<Number>> points = Util.getPoints(dir);


                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                Notification notification = Util.createServiceNotification(context, CHANNEL_ID, TITLE, null);
                notificationManager.notify(NOTIFICATION_ID, notification);

            }
            Looper.loop();
        }

    }

    public GPSProcesser() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Started");
        Context context = getApplicationContext();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Util.createNotificationChannel(this, CHANNEL_ID);
        Notification notification = Util.createServiceNotification(
                this, CHANNEL_ID, "GPSProcesser", "GPS logs processed yet");
        startForeground(NOTIFICATION_ID, notification);
        notificationManager.notify(NOTIFICATION_ID, notification);

        ProcesserThread processer = new GPSProcesser.ProcesserThread();
        processer.start();
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY;
    }
}