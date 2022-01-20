package com.example.recommender;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
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
        points.sort((l1, l2) -> {
            Long L1 = new Long(l1.get(0).longValue());
            Long L2 = new Long(l2.get(0).longValue());
            return L1.compareTo(L2);
        });
        return points;
    }

    static List<Integer> getNeighbors(List<Number> point, List<List<Number>> staypoints, int distance) {

        List<Integer> neighbors = new ArrayList<Integer>();
        for (int i = 0; i < staypoints.size(); i++) {
            if (staypointDistance(point, staypoints.get(i)) < distance) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }

    static List<Integer> DBSCAN(int distance, int minPts, List<List<Number>> staypoints) {

        List<Integer> labels = new ArrayList<Integer>();
        for (int i = 0; i < staypoints.size(); i++) {
            labels.add(-2);
        }


        int clusterId = -1;
        for (int i = 0; i < staypoints.size(); i++) {

            if (labels.get(i) != -2) {
                continue;
            }

            List<Number> currentPoint = staypoints.get(i);
            List<Integer> neighbors = getNeighbors(currentPoint, staypoints, distance);

            if (neighbors.size() < minPts) {
                labels.set(i, -1);
                continue;
            }

            clusterId += 1;
            labels.set(i, clusterId);

            int j = 0;
            while (j < neighbors.size()) {
                int index = neighbors.get(j);

                if (labels.get(index) == -1) {
                    labels.set(index, clusterId);
                }

                if (labels.get(index) != -2) {
                    j++;
                    continue;
                }

                labels.set(index, clusterId);

                List<Integer> anotherNeighbors = getNeighbors(staypoints.get(index), staypoints, distance);
                if (anotherNeighbors.size() >= minPts) {
                    for (int k = 0; k < anotherNeighbors.size(); k++) {
                        int anotherIndex = anotherNeighbors.get(k);
                        neighbors.add(anotherIndex);
                    }
                }

                j++;
            }
        }
        return labels;
    }

    static List<Double> getMeanCoords(List<List<Number>> points) {
        Double totalLats = 0d;
        Double totalLons = 0d;

        for (List<Number> point : points) {
            totalLats += (double) point.get(1);
            totalLons += (double) point.get(2);
        }

        List<Double> meanCoords = new ArrayList<Double>();
        meanCoords.add(totalLats / points.size());
        meanCoords.add(totalLons / points.size());
        return meanCoords;
    }

    static List<List<Number>> getStaypoints(List<List<Number>> points, long distT, long timeT) {
        int i = 0;

        List<List<Number>> staypoints = new ArrayList<List<Number>>();
        while (i < points.size()) {
            int j = i + 1;

            while (j < points.size()) {
                List<Number> point1 = points.get(i);
                List<Number> point2 = points.get(j);
                Double dist = pointDistance(point1, point2);

                if (dist > distT) {
                    long timeDelta = (long) point2.get(0) - (long) point1.get(0);
                    if (timeDelta > timeT) {
                        List<Double> meanCoords = getMeanCoords(points.subList(i, j));
                        List<Number> staypoint = new ArrayList<Number>();
                        staypoint.add(meanCoords.get(0));
                        staypoint.add(meanCoords.get(1));
                        staypoint.add(points.get(i).get(0));
                        staypoint.add(points.get(j - 1).get(0));

                        staypoints.add(staypoint);
                    }
                    break;
                }
                j += 1;
            }
            i = j;
        }
        Log.e(TAG, "num of staypoints: " + staypoints.size());
        return staypoints;
    }

    static double staypointDistance(List<Number> point1, List<Number> point2) {
        double lat1 = (double) point1.get(0);
        double lon1 = (double) point1.get(1);
        double lat2 = (double) point2.get(0);
        double lon2 = (double) point2.get(1);
        double distance =  haversine(lat1, lon1, lat2, lon2);
        Log.e(TAG, "DIST: " + distance);
        return distance;
    }

    static double pointDistance(List<Number> point1, List<Number> point2) {
        double lat1 = (double) point1.get(1);
        double lon1 = (double) point1.get(2);
        double lat2 = (double) point2.get(1);
        double lon2 = (double) point2.get(2);

        return haversine(lat1, lon1, lat2, lon2);
    }

    static Double toRad(Double value) {
        return value * Math.PI / 180;
    }

    static Double haversine(Double lat1, Double lon1, Double lat2, Double lon2) {
        // TODO Auto-generated method stub
        final int R = 6371000; // Radious of the earth
        Double latDistance = toRad(lat2-lat1);
        Double lonDistance = toRad(lon2-lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        Double distance = R * c;
        return distance;
    }

    public static double flatDistance(double lat1, double lng1,
                                  double lat2, double lng2){
        double a = (lat1-lat2)*distPerLat(lat1);
        double b = (lng1-lng2)*distPerLng(lat1);
        return Math.sqrt(a*a+b*b);
    }

    private static double distPerLng(double lat){
        return 0.0003121092*Math.pow(lat, 4)
                +0.0101182384*Math.pow(lat, 3)
                -17.2385140059*lat*lat
                +5.5485277537*lat+111301.967182595;
    }

    private static double distPerLat(double lat){
        return -0.000000487305676*Math.pow(lat, 4)
                -0.0033668574*Math.pow(lat, 3)
                +0.4601181791*lat*lat
                -1.4558127346*lat+110579.25662316;
    }

}
