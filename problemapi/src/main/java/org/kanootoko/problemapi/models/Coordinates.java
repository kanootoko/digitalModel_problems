package org.kanootoko.problemapi.models;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Coordinates {
    private double latitude;
    private double longitude;

    public Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Coordinates(String latAndLong) {
        String[] coords_arr = latAndLong.split(",");
        latitude = Double.parseDouble(coords_arr[0]);
        longitude = Double.parseDouble(coords_arr[1]);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("latitude", java.lang.Math.round(latitude * 10000) / 10000.0);
        res.put("longitude", java.lang.Math.round(longitude * 10000) / 10000.0);
        return res;
    }

    @SuppressWarnings("unchecked")
    public JSONArray toJSONArray() {
        JSONArray res = new JSONArray();
        res.add(java.lang.Math.round(latitude * 10000) / 10000.0);
        res.add(java.lang.Math.round(longitude * 10000) / 10000.0);
        return res;
    }

    @Override
    public String toString() {
        return String.format("Coordinates{latitude: %.4f, longitude: %.4f}", longitude, latitude);
    }
}