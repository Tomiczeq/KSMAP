package com.example.recommender;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class GPSLogger extends Service {

    private static final String TAG = "GPSLogger";
    // Flag for GPS status
    boolean isGPSEnabled = false;
    // Declaring a Location Manager
    protected LocationManager locationManager;

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) throws IOException {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);

            if (mLastLocation != null) {
                Log.i(TAG, "received new location");
                String timestamp = Long.toString(mLastLocation.getTime());
                String latitude = Double.toString(mLastLocation.getLatitude());
                String longitude = Double.toString(mLastLocation.getLongitude());

                Context context = getBaseContext();
                if (context != null) {
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                    Date date = new Date();
                    String day = formatter.format(date);
                    Log.i(TAG, "day: " + day);

                    String filename = day + ".txt";
                    File file = new File(context.getFilesDir(), filename);
                    FileWriter fr = new FileWriter(file, true);
                    fr.write(timestamp + "," + latitude + "," + longitude);
                    fr.close();
                } else {
                    Log.e(TAG, "context is null");
                }
            } else {
                Log.e(TAG, "no location");
            }

        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    private class LoggerThread extends Thread {

        @Override
        public void run() {
            Looper.prepare();
            Context context = getApplicationContext();
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.i(TAG, "GPSEnabled: " + Boolean.toString(isGPSEnabled));
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            while (true) {
                GPSLogger.LocationListener locationListener = null;
                try {
                    locationListener = new GPSLogger.LocationListener(LocationManager.GPS_PROVIDER);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                locationManager.requestSingleUpdate(criteria, locationListener, null);
                try {
                    Log.i(TAG, "sleeping");
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            Looper.loop();
        }
    }


    public GPSLogger() throws IOException {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "started");

        LoggerThread logger = new LoggerThread();
        logger.start();
        // Getting GPS status
//        Context context = getApplicationContext();
//        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
//        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        Log.i(TAG, "GPSEnabled: " + Boolean.toString(isGPSEnabled));

//        Criteria criteria = new Criteria();
//        criteria.setAccuracy(Criteria.ACCURACY_FINE);
//        while (true) {
//            LocationListener locationListener = null;
//            try {
//                locationListener = new LocationListener(LocationManager.GPS_PROVIDER);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            locationManager.requestSingleUpdate(criteria, locationListener, null);
//            try {
//                Log.i(TAG, "sleeping");
//                Thread.sleep(15000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                break;
//            }
//        }
        // by returning this we make sure the service is restarted if the system kills the service
        Log.i(TAG, "returning start_sticky");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}