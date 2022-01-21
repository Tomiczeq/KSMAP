package com.example.recommender;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {

    private static final String TAG = "MainActivity";

    private RequestQueue requestQueue;
    public static JSONArray placesArray = new JSONArray();
    public static int requested_hour = 0;
    public static Button setTime;
    public static int selectedHour;
    public static int selectedMinute;
    protected LocationManager locationManager;

    MyRecyclerViewAdapter adapter;

    /** Method to turn on GPS **/
    public void turnGPSOn(){
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MainActivity.this,
                                        LocationRequest.PRIORITY_HIGH_ACCURACY);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent intent;
        switch (requestCode) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        Log.i(TAG, "onActivityResult: GPS Enabled by user");
                        // Start Service for recording GPS
                        intent = new Intent(this, GPSLogger.class); // Build the intent for the service
                        startForegroundService(intent);
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Log.i(TAG, "onActivityResult: User rejected GPS request");
                        intent = new Intent(this, GPSLogger.class); // Build the intent for the service
                        stopService(intent);
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.requestQueue = getRequestQueue();

        findViewById(R.id.search_restaurants).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (!isGPSEnabled) {
                    turnGPSOn();
                }
                try {
                    String[] futureCoords = Util.getFutureCoords(MainActivity.this.getFilesDir(), "topWh.json", selectedHour);
                    placesArray = new JSONArray();
                    int currentHour = Util.getCurrentHour();
                    if ((currentHour == selectedHour) || (futureCoords == null)){
                        Log.e(TAG, "using current coords");
                        String[] currentCoords = getCurrentCoords();
                        if (currentCoords == null) {
                            Toast.makeText(MainActivity.this,
                                    "No GPS location found", Toast.LENGTH_LONG).show();
                            actualizeResults();
                            return;
                        }
                        getRestaurants(currentCoords);
                    } else {
                        Log.e(TAG, "using future coords");
                        getRestaurants(futureCoords);
                    }
                } catch (IOException | URISyntaxException | JSONException e) {
                    Log.e(TAG, "places api error");
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Date date = new Date();
        selectedHour = date.getHours();
        selectedMinute = date.getMinutes();
        setTime = (Button) findViewById(R.id.time_picker);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String time = sdf.format(date);
        setTime.setText(time);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            Date currentDate = new Date();
            Date date = new Date();
            date.setHours(hourOfDay);
            date.setMinutes(minute);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            String time;
            if (currentDate.after(date)) {
                time = sdf.format(currentDate);
                hourOfDay = currentDate.getHours();
                minute = currentDate.getMinutes();
            } else {
                time = sdf.format(date);
            }
            setTime.setText(time);
            selectedHour = hourOfDay;
            selectedMinute = minute;
        }
    }


    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public String[] getCurrentCoords() {
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (locationGPS == null) {
            return null;
        }
        String[] coords = new String[2];
        coords[0] = Double.toString(locationGPS.getLatitude());
        coords[1] = Double.toString(locationGPS.getLongitude());
        return coords;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return requestQueue;
    }

    public void getRestaurants(String[] coords) throws URISyntaxException, IOException {
        //String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=50.0711510%2C14.4006789&radius=100&type=restaurant&key=AIzaSyCAGYJAo2vthRKK3NEacAyGb2--WLgWmx8";
//        String baseUrl = "/maps.googleapis.com/maps/api/place/nearbysearch/json";
        Uri.Builder bu = new Uri.Builder();
        bu.scheme("https");
        bu.authority("maps.googleapis.com");
        bu.path("maps/api/place/nearbysearch/json");
        String location = coords[0] + "," + coords[1];
        bu.appendQueryParameter("location", location);
        bu.appendQueryParameter("radius", "100");
        bu.appendQueryParameter("type", "restaurant");
        bu.appendQueryParameter("key", "AIzaSyCAGYJAo2vthRKK3NEacAyGb2--WLgWmx8");
        String url = bu.build().toString();
        Log.e(TAG, url);

        requestRestaurants(url);
    }

    public void getDetails(String placeId) {
        Uri.Builder bu = new Uri.Builder();
        bu.scheme("https");
        bu.authority("maps.googleapis.com");
        bu.path("maps/api/place/details/json");
        bu.appendQueryParameter("place_id", placeId);
        bu.appendQueryParameter("fields", "rating,formatted_address,name,website,opening_hours");
        bu.appendQueryParameter("key", "AIzaSyCAGYJAo2vthRKK3NEacAyGb2--WLgWmx8");
        String url = bu.build().toString();
        Log.e(TAG, url);

        requestDetails(url, placeId);
    }

    // TODO
    private void sortPlacesArray() {
    }

    private void requestDetails(String url, String placeId) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject result = response.getJSONObject("result");

                    String name = result.getString("name");
                    String address = result.getString("formatted_address");
                    String website = "";
                    if (result.has("website")) {
                        website = result.getString("website");
                    }
                    String rating = "";
                    if (result.has("rating")) {
                        rating = result.getString("rating");
                    }

                    JSONObject opening_hours = null;
                    if (result.has("opening_hours")) {
                        opening_hours = result.getJSONObject("opening_hours");
                    }
                    if (opening_hours == null) {
                        return;
                    }

                    boolean isOpened = Util.isOpened(opening_hours, selectedHour, selectedMinute);
                    if (isOpened) {
                        JSONObject place = new JSONObject();
                        place.put("name", name);
                        place.put("opened", Boolean.toString(isOpened));
                        place.put("address", address);
                        place.put("rating", rating);
                        place.put("website", website);
                        place.put("id", placeId);
                        placesArray.put(place);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sortPlacesArray();

                Log.e(TAG, "placesArray: " + placesArray.toString());
                actualizeResults();
            }
        }, error -> {
            Log.e(TAG, "places api request error");
        });
        // Access the RequestQueue through your singleton class.
        this.requestQueue.add(jsonObjectRequest);
    }

    public void actualizeResults() {
        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.search_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        adapter = new MyRecyclerViewAdapter(MainActivity.this, placesArray);
        adapter.setClickListener(MainActivity.this);
        recyclerView.setAdapter(adapter);
    }

    private void requestRestaurants(String url) throws IOException {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.i(TAG, "Response: " + response.toString());
                    JSONArray results = null;
                    try {
                        results = response.getJSONArray("results");
                        ArrayList<String> placeIds = new ArrayList<String>();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject result = results.getJSONObject(i);
                            getDetails(result.getString("place_id"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, error -> {
                Log.e(TAG, "places api request error");
        });
        // Access the RequestQueue through your singleton class.
        this.requestQueue.add(jsonObjectRequest);
    }

//    @Override
//    public void onResponse(JSONObject response) {
//        Log.i(TAG, "Response: " + response.toString());
//        JSONArray results = null;
//        try {
//            results = response.getJSONArray("results");
//            for (int i = 0; i < results.length(); i++) {
//                JSONObject result = results.getJSONObject(i);
//
//                if (!result.has("rating")) {
//                    continue;
//                }
//
//                JSONObject place = new JSONObject();
//                String placeName = result.getString("name");
//                String placeAddress = result.getString("vicinity");
//                double rating = result.getDouble("rating");
//
//                place.put("name", placeName);
//                place.put("address", placeAddress);
//                place.put("rating", rating);
//                placesArray.put(place);
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        // set up the RecyclerView
//        RecyclerView recyclerView = findViewById(R.id.search_results);
//        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
//        adapter = new MyRecyclerViewAdapter(MainActivity.this, placesArray);
//        adapter.setClickListener(MainActivity.this);
//        recyclerView.setAdapter(adapter);
//    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onStart() {
        super.onStart();

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        // Start Service for processing GPS
        Intent intent = new Intent(this, GPSProcesser.class); // Build the intent for the service
        startForegroundService(intent);

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGPSEnabled) {
            turnGPSOn();
        } else {
            intent = new Intent(this, GPSLogger.class); // Build the intent for the service
            startForegroundService(intent);

        }
    }

    @Override
    public void onItemClick(View view, int position) throws JSONException {
        Toast.makeText(this, "You clicked " + adapter.getItem(position).toString() + " on row number " + position, Toast.LENGTH_SHORT).show();
    }
}