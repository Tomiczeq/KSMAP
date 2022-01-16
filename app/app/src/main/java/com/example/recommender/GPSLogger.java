package com.example.recommender;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class GPSLogger extends Service implements LocationListener {

    private static final String TAG = "GPSLogger";
    private static final String CHANNEL_ID = "1111";
    private static final String TITLE = "Logging GPS";
    private static final int NOTIFICATION_ID = 1111;

    // Flag for GPS status
    boolean isGPSEnabled = false;
    // Declaring a Location Manager
    protected LocationManager locationManager;
    protected NotificationManager notificationManager;

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "onLocationChanged: " + location);

        // save GPS log
        String timestamp = Long.toString(location.getTime());
        String latitude = Double.toString(location.getLatitude());
        String longitude = Double.toString(location.getLongitude());

        String gpsLog = timestamp + ',' + latitude + ',' + longitude;
        String filename = Util.getCurrentLogFile();
        File file = new File(this.getFilesDir(), filename);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            bw.append(gpsLog);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        // update notification
        Notification notification = Util.createServiceNotification(
                this, CHANNEL_ID, TITLE, null);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public GPSLogger() throws IOException {
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "Creating notification channel");
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("1111", "test", importance);
            channel.setDescription("test");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        } else {
            Log.i(TAG, "not creating any channel");
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "started");
        Context context = getApplicationContext();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

        createNotificationChannel();
        Notification notification = Util.createServiceNotification(
                this, CHANNEL_ID, "Logging GPS", "No update yet");
        startForeground(NOTIFICATION_ID, notification);
        notificationManager.notify(NOTIFICATION_ID, notification);

        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.i(TAG, "GPSEnabled: " + Boolean.toString(isGPSEnabled));

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                15000,
                0,
                this
        );
        Log.i(TAG, "returning start_sticky");
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}