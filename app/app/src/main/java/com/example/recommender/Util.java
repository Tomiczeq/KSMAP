package com.example.recommender;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Util {
    private static final String TAG = "Util";

    static void createNotificationChannel(Context context, String channelId) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channelId,"test", importance);
            channel.setDescription("test");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        } else {
            Log.i(TAG, "not creating any channel");
        }
    }

    static Notification createServiceNotification(Context context,
                                                  String channelId,
                                                  String title,
                                                  String text) {
        NotificationManagerCompat notification = NotificationManagerCompat.from(context);
        boolean isEnabled = notification.areNotificationsEnabled();
        Log.i(TAG, "Notifications enabled: " + Boolean.toString(isEnabled));

        if (text == null) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            text = "Last update: " + formatter.format(date);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSilent(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    static String getCurrentLogFile() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date();
        String day = formatter.format(date);
        String filename = day + ".glog";
        return filename;
    }

    static List<List<Number>> getPoints(File dir) {
        File[] files = dir.listFiles();

        List<List<Number>> points = new ArrayList<List<Number>>();
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();

            if (filename.endsWith("glog")) {
                try (BufferedReader br = new BufferedReader(new FileReader(files[i]))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Log.i(TAG, "line: " + line);
                        String[] splitted = line.split(",");
                        long timestamp = Long.parseLong(splitted[0]);
                        Double latitude = Double.parseDouble(splitted[1]);
                        Double longitude = Double.parseDouble(splitted[2]);
                        List<Number> point = new ArrayList<Number>();
                        point.add(timestamp);
                        point.add(latitude);
                        point.add(longitude);
                        points.add(point);
                        // process the line.
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return points;
    }

}
