package org.kanootoko.problemapi.models;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class HALResponse extends JSONObject {

    private static final long serialVersionUID = 3002907072999910L;

    @SuppressWarnings("unchecked")
    public HALResponse(String uri) {
        super();
        put("_links", new JSONObject());
        ((JSONObject) get("_links")).put("self", new JSONObject());
        ((JSONObject) ((JSONObject) get("_links")).get("self")).put("href", uri);
        put("_embedded", new JSONObject());
    }

    public JSONObject embedded() {
        return (JSONObject) get("_embedded");
    }

    @SuppressWarnings("unchecked")
    public void addEmbedded(String name, JSONObject obj) {
        ((JSONObject) get("_embedded")).put(name, obj);
    }

    @SuppressWarnings("unchecked")
    public void addEmbedded(String name, JSONArray arr) {
        ((JSONObject) get("_embedded")).put(name, arr);
    }

    public JSONObject links() {
        return (JSONObject) get("_links");
    }
}
