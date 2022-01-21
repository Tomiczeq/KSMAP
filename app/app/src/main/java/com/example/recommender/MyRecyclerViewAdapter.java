package com.example.recommender;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private JSONArray placesData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    MyRecyclerViewAdapter(Context context, JSONArray data) {
        this.mInflater = LayoutInflater.from(context);
        this.placesData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.result_layout, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            JSONObject placeData = placesData.getJSONObject(position);
            holder.placeName.setText(placeData.getString("name"));
            holder.placeRatting.setText(placeData.getString("rating"));
            holder.placeWebsite.setText(placeData.getString("website"));
            holder.placeAddress.setText(placeData.getString("address"));
//            holder.placeOpen.setText(placeData.getString("opened"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return placesData.length();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView placeName;
        TextView placeAddress;
        TextView placeRatting;
        TextView placeWebsite;
        TextView placeOpen;

        ViewHolder(View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.placeName);
            placeAddress = itemView.findViewById(R.id.placeAddress);
            placeRatting = itemView.findViewById(R.id.placeRatting);
            placeWebsite = itemView.findViewById(R.id.placeWebsite);
//            placeOpen = itemView.findViewById(R.id.placeOpen);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                try {
                    mClickListener.onItemClick(view, getAdapterPosition());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // convenience method for getting data at click position
    JSONObject getItem(int id) throws JSONException {
        return placesData.getJSONObject(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position) throws JSONException;
    }
}
