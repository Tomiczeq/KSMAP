package com.example.recommender;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {

    private static final String TAG = "MainActivity";

    private RequestQueue requestQueue;
    public static JSONArray placesArray = new JSONArray();
    public static int requested_hour = 0;

    MyRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.requestQueue = getRequestQueue();

        findViewById(R.id.search_restaurants).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String[] currentCoords = getCurrentCoords();
                    String[] futureCoords = Util.getFutureCoords(18);

                    placesArray = new JSONArray();

                    if (currentCoords != null) {
                        getRestaurants(currentCoords);
                    }
                    if (futureCoords != null) {
                        getRestaurants(futureCoords);
                    }
                } catch (IOException | URISyntaxException e) {
                    Log.e(TAG, "places api error");
                    e.printStackTrace();
                }
            }
        });
    }

    public String[] getCurrentCoords() {
        String[] coords = new String[2];
        coords[0] = "50.0711510";
        coords[1] = "14.4006789";
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

                    boolean isOpened = Util.isOpened(opening_hours, requested_hour);
                    if (!isOpened) {
                        return;
                    }

                    JSONObject place = new JSONObject();
                    place.put("name", name);
                    place.put("address", address);
                    place.put("rating", rating);
                    place.put("website", website);
                    place.put("id", placeId);
                    placesArray.put(place);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sortPlacesArray();

                // set up the RecyclerView
                RecyclerView recyclerView = findViewById(R.id.search_results);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                adapter = new MyRecyclerViewAdapter(MainActivity.this, placesArray);
                adapter.setClickListener(MainActivity.this);
                recyclerView.setAdapter(adapter);
            }
        }, error -> {
            Log.e(TAG, "places api request error");
        });
        // Access the RequestQueue through your singleton class.
        this.requestQueue.add(jsonObjectRequest);
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

        // Start Service for recording GPS
        Intent intent = new Intent(this, GPSLogger.class); // Build the intent for the service
        startForegroundService(intent);

        // Start Service for processing GPS
        intent = new Intent(this, GPSProcesser.class); // Build the intent for the service
        startForegroundService(intent);
    }

    @Override
    public void onItemClick(View view, int position) throws JSONException {
        Toast.makeText(this, "You clicked " + adapter.getItem(position).toString() + " on row number " + position, Toast.LENGTH_SHORT).show();
    }
}