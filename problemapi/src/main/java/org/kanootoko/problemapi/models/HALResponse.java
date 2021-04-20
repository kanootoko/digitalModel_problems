package org.kanootoko.problemapi.models;

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
}
