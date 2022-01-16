package com.example.recommender;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {

    private static final String TAG = "MainActivity";

    private RequestQueue requestQueue;

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
                    requestRestaurants();
                } catch (IOException e) {
                    Log.e(TAG, "places api error");
                    e.printStackTrace();
                }
            }
        });

//        JSONArray array = new JSONArray();
//        JSONObject item = new JSONObject();
//        try {
//            item.put("name", "Test");
//            item.put("vicinity", "PodLabutkou");
//            item.put("rating", 4.8);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        array.put(item);
//        JSONObject item1 = new JSONObject();
//        try {
//            item1.put("name", "Test2");
//            item1.put("vicinity", "PodLabutkou");
//            item1.put("rating", 4.8);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        array.put(item1);

//        // set up the RecyclerView
//        RecyclerView recyclerView = findViewById(R.id.search_results);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
////        adapter = new MyRecyclerViewAdapter(this, animalNames);
//        adapter = new MyRecyclerViewAdapter(this, array);
//        adapter.setClickListener(this);
//        recyclerView.setAdapter(adapter);
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return requestQueue;
    }

    private void requestRestaurants() throws IOException {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=50.0711510%2C14.4006789&radius=100&type=restaurant&key=AIzaSyCAGYJAo2vthRKK3NEacAyGb2--WLgWmx8";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "Response: " + response.toString());
                        JSONArray results = null;
                        JSONArray placesArray = new JSONArray();
                        try {
                            results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);

                                if (!result.has("rating")) {
                                    continue;
                                }

                                JSONObject place = new JSONObject();
                                String placeName = result.getString("name");
                                String placeAddress = result.getString("vicinity");
                                double rating = result.getDouble("rating");

                                place.put("name", placeName);
                                place.put("address", placeAddress);
                                place.put("rating", rating);
                                placesArray.put(place);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // set up the RecyclerView
                        RecyclerView recyclerView = findViewById(R.id.search_results);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                        adapter = new MyRecyclerViewAdapter(MainActivity.this, placesArray);
                        adapter.setClickListener(MainActivity.this);
                        recyclerView.setAdapter(adapter);
                    }
                }, error -> {
                    // TODO: Handle error
                    Log.i(TAG, "places api request error");
                });
        // Access the RequestQueue through your singleton class.
        this.requestQueue.add(jsonObjectRequest);
    }

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

        Context context = getApplicationContext();
        Intent intent = new Intent(context, GPSLogger.class); // Build the intent for the service
//        context.startForegroundService(intent);
//        context.startService(intent);
    }

    @Override
    public void onItemClick(View view, int position) throws JSONException {
        Toast.makeText(this, "You clicked " + adapter.getItem(position).toString() + " on row number " + position, Toast.LENGTH_SHORT).show();
    }
}