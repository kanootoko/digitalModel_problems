package org.kanootoko.problemapi.models;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Coordinates {
    private double longitude;
    private double latitude;
    public Coordinates(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("longitude", longitude);
        res.put("latitude", latitude);
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
        return String.format("Coordinates{longitude: %.2f, latitude: %.2f}", longitude, latitude);
    }
}