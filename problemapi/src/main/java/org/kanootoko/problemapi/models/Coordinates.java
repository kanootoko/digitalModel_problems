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

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("latitude", latitude);
        res.put("longitude", longitude);
        return res;
    }

    @SuppressWarnings("unchecked")
    public JSONArray toJSONArray() {
        JSONArray res = new JSONArray();
        res.add(latitude);
        res.add(longitude);
        return res;
    }

    @Override
    public String toString() {
        return String.format("Coordinates{latitude: %.2f, longitude: %.2f}", longitude, latitude);
    }
}