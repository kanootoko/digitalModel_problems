package org.kanootoko.problemapi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kanootoko.problemapi.models.HALResponse;
import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.services.ProblemService;
import org.kanootoko.problemapi.utils.ConnectionManager;
import org.kanootoko.problemapi.utils.ProblemFilter;
import org.kanootoko.problemapi.utils.RepositoryFactory;
import org.kanootoko.problemapi.utils.ServiceFactory;
import org.kanootoko.problemapi.utils.Utils;

import spark.Request;
import spark.Response;
import spark.Spark;

public class App {

    private static Set<String> municipalities, districts;

    private static ServiceFactory serviceFactory;

    @SuppressWarnings("unchecked")
    private static String apiDefinition(Request req, Response res) {
        JSONObject result = new JSONObject();
        result.put("version", "2021-09-20");
        JSONObject links = new JSONObject();

        links.put("self", new JSONObject());
        ((JSONObject) links.get("self")).put("href", req.uri());

        links.put("problems-search", new JSONObject());
        ((JSONObject) links.get("problems-search"))
            .put("href", "/api/problems/search/{?minDate,maxDate,location,category,subcategory,status,municipality,district,limit}");
        ((JSONObject) links.get("problems-search")).put("templated", true);
        
        links.put("first-problem", new JSONObject());
        ((JSONObject) links.get("first-problem")).put("href", "/api/problems/1/");

        links.put("evaluation-polygon", new JSONObject());
        ((JSONObject) links.get("evaluation-polygon"))
            .put("href", "/api/evaluation/polygon/{?minDate,maxDate,location,limit}");
        ((JSONObject) links.get("evaluation-polygon")).put("templated", true);

        links.put("evaluation-objects", new JSONObject());
        ((JSONObject) links.get("evaluation-objects"))
            .put("href", "/api/evaluation/objects/{?minDate,maxDate,location,type,limit}");
        ((JSONObject) links.get("evaluation-objects")).put("templated", true);

        links.put("evaluation-districts", new JSONObject());
        ((JSONObject) links.get("evaluation-districts")).put("href", "/api/evaluation/districts/{?date,update}");
        ((JSONObject) links.get("evaluation-districts")).put("templated", true);

        links.put("evaluation-municipalities", new JSONObject());
        ((JSONObject) links.get("evaluation-municipalities")).put("href", "/api/evaluation/municipalities/{?date,update}");
        ((JSONObject) links.get("evaluation-municipalities")).put("templated", true);

        links.put("categories", new JSONObject());
        ((JSONObject) links.get("categories")).put("href", "/api/groups/category/{?date}");
        ((JSONObject) links.get("categories")).put("templated", true);

        links.put("subcategories", new JSONObject());
        ((JSONObject) links.get("subcategories")).put("href", "/api/groups/subcategory/{?date}");
        ((JSONObject) links.get("subcategories")).put("templated", true);

        links.put("statuses", new JSONObject());
        ((JSONObject) links.get("statuses")).put("href", "/api/groups/status/{?date}");
        ((JSONObject) links.get("statuses")).put("templated", true);

        links.put("municipalities", new JSONObject());
        ((JSONObject) links.get("municipalities")).put("href", "/api/groups/municipality/{?date}");
        ((JSONObject) links.get("municipalities")).put("templated", true);

        links.put("districts", new JSONObject());
        ((JSONObject) links.get("districts")).put("href", "/api/groups/district/{?date}");
        ((JSONObject) links.get("districts")).put("templated", true);

        links.put("evaluation-months-counts", new JSONObject());
        ((JSONObject) links.get("evaluation-months-counts")).put("href", "/api/evaluation/months/");
        
        links.put("platform-services-list", new JSONObject());
        ((JSONObject) links.get("platform-services-list")).put("href", "/api/platform-services/list/");

        links.put("platform-services-stats", new JSONObject());
        ((JSONObject) links.get("platform-services-stats")).put("href", "/api/platform-services/stats/{?service,date}");
        ((JSONObject) links.get("platform-services-stats")).put("templated", true);
        
        links.put("platform-services-evaluation", new JSONObject());
        ((JSONObject) links.get("platform-services-evaluation")).put("href", "/api/platform-services/evaluation/{?service,date}");
        ((JSONObject) links.get("platform-services-evaluation")).put("templated", true);


        result.put("_links", links);
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static String searchProblems(Request req, Response res) {
        String minDateStr = null, maxDateStr = null;
        String location = null;
        ProblemFilter pf = new ProblemFilter();
        if (!req.body().isEmpty()) {
            JSONObject requestBody;
            try {
                requestBody = (JSONObject) new JSONParser().parse(req.body());
            } catch (ParseException e) {
                res.status(400);
                return "{\"error\": \"Body cannot be parsed as json\"}";
            }
            System.out.println("GET /api/problems/search: RequestBody: " + requestBody);
            minDateStr = (String) requestBody.get("minDate");
            maxDateStr = (String) requestBody.get("maxDate");
            pf.setCategory((String) requestBody.get("category"));
            pf.setSubcategory((String) requestBody.get("subcategory"));
            pf.setStatus((String) requestBody.get("status"));
            location = (String) requestBody.get("location");
            if (requestBody.containsKey("limit")) {
                pf.setLimit(Integer.parseInt((String) requestBody.get("limit")));
            }
        }
        if (!req.queryParams().isEmpty()) {
            System.out.println("GET /api/problems/search: request query: " + req.queryString());
            if (req.queryParams().contains("minDate")) {
                minDateStr = req.queryParams("minDate");
            }
            if (req.queryParams().contains("maxDate")) {
                maxDateStr = req.queryParams("maxDate");
            }
            if (req.queryParams().contains("category")) {
                pf.setCategory((String) req.queryParams("category"));
            }
            if (req.queryParams().contains("subcategory")) {
                pf.setSubcategory((String) req.queryParams("subcategory"));
            }
            if (req.queryParams().contains("status")) {
                pf.setStatus((String) req.queryParams("status"));
            }
            if (req.queryParams().contains("limit")) {
                pf.setLimit(Integer.parseInt(req.queryParams("limit")));
            }
        } 
        if (req.body().isEmpty() && req.queryParams().isEmpty()) {
            System.out.println("GET /api/problems/search: No query or body params");
            res.type("text/html");
            return "<html><h1>Problems search: no parameters are given</h1>"
                    + "<p><p>Use this endpoint with <b>limit</b> (optional, default=" + ProblemFilter.LIMIT_DEFAULT
                    + "), <b>minDate</b> (optional), <b>maxDate</b> (optional), <b>status</b> (optional),"
                    + " <b>category</b> (optional), <b>subcategory</b> (optional) "
                    + " and <b>location</b> (optional) parameters </p>"
                    + "<p>Possible formats for <b>location</b> parameter:"
                    + "<ul><li>\"latitude1,longitude1;latitude2,longitude2\" for a pair of coordinates to define a square on a map</li>"
                    + "<li>\"municipality_name\" for a municipality (see the list at /api/groups/municipality)</li>"
                    + "<li>\"district_name\" for a district (see the list at /api/groups/district)</li>"
                    + "<li>(geojson) for a polygon on a map</li></ul></p>"
                    + "<p>For points use format \"latitude,longitude\", for dates - \"YYYY-MM-DD\"</p>"
                    + "<p>The result will be a list of problems, coordinates in format of array [latitude, longitude]</p>"
                    + "</body></html>";
        }
        try {
            if (minDateStr != null) {
                pf.setMinCreationDate(LocalDate.parse(minDateStr));
            }
        } catch (Exception e) {
            res.status(400);
            res.type("application/json");
            return "{\"error\": \"minDate is not a valid date (use format YYYY-MM-DD)\"}";
        }
        try {
            if (maxDateStr != null) {
                pf.setMaxCreationDate(LocalDate.parse(maxDateStr));
            }
        } catch (Exception e) {
            res.status(400);
            res.type("application/json");
            return "{\"error\": \"maxDate is not a valid date (use format YYYY-MM-DD)\"}";
        }
        if (location != null) {
            if (location.matches("^\\d+(\\.\\d+)?,\\d+(\\.\\d+)?;\\d+(\\.\\d+)?,\\d+(\\.\\d+)?$")) {
                String[] coordsPair = location.split(";");
                pf.setCoords(coordsPair[0], coordsPair[1]);
            } else if (location.startsWith("{")) {
                pf.setGeoJSON(location);
            } else if (districts.contains(location)) {
                pf.setDistrict(location);
            } else if (municipalities.contains(location)) {
                pf.setMunicipality(location);
            } else {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"location (" + location.replace("\"", "\\\"") + ") is not a municipality or district\"}";
            }
        }

        ProblemService ps = serviceFactory.getPorblemService();
        List<Problem> problems = ps.getProblemsByFilter(pf);
        HALResponse result = new HALResponse(req.uri() + ((req.queryString() == null || req.queryString().isEmpty()) ? "" : ("?" + req.queryString())));
        result.put("size", problems.size());
        JSONArray problemsArray = new JSONArray();
        for (Problem p : problems) {
            String description = p.getDescription();
            if (p.getName().length() < description.length()) {
                description = p.getName();
            }
            if (p.getReason().length() < description.length()) {
                description = p.getReason();
            }
            JSONObject problem = new JSONObject();
            problem.put("description", description);
            problem.put("coordinates", p.getCoordinates().toJSONArray());
            problem.put("status", p.getStatus());
            problem.put("creationDate", p.getCreationDate().toString());
            problem.put("_links", p.getLinks());
            problemsArray.add(problem);
        }
        result.put("_embedded", new JSONObject());
        result.embedded().put("problems", problemsArray);
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static String getProblem(Request req, Response res) {
        int problemID = Integer.parseInt(req.params(":problemID"));
        ProblemService ps = serviceFactory.getPorblemService();
        Problem problem = ps.getProblemByID(problemID);
        JSONObject result = new JSONObject();
        result.put("_links", problem.getLinksExtended());
        result.put("_embedded", problem.toJSON());
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static String getGroups(Request req, Response res) {
        ProblemService ps = serviceFactory.getPorblemService();
        Map<String, Integer> groups = ps.getGroupsSize(req.params(":labelName"), req.queryParams("date"));
        HALResponse result = new HALResponse(req.uri());
        result.put("size", groups.size());
        JSONArray groupsArray = new JSONArray();
        for (Map.Entry<String, Integer> el : groups.entrySet()) {
            JSONObject group = new JSONObject();
            group.put("name", el.getKey());
            group.put("size", el.getValue());
            groupsArray.add(group);
        }
        result.embedded().put("groups", groupsArray);
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static String evaluationPolygon(Request req, Response res) {
        String minDateStr = null, maxDateStr = null;
        String location = null;
        Integer limit = null;
        if (!req.body().isEmpty()) {
            JSONObject requestBody;
            try {
                requestBody = (JSONObject) new JSONParser().parse(req.body());
            } catch (ParseException e) {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"Body cannot be parsed as json\"}";
            }
            System.out.println("GET /api/evaluate/polygon: RequestBody: " + requestBody);
            minDateStr = (String) requestBody.get("minDate");
            maxDateStr = (String) requestBody.get("maxDate");
            location = (String) requestBody.get("location");
            if (requestBody.containsKey("limit")) {
                try {
                    limit = Integer.parseInt((String) requestBody.get("limit"));
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return "{\"error\":\"limit parameter cannot be parsed as integer\"}";
                }
            }
        }
        if (!req.queryParams().isEmpty()) {
            System.out.println("GET /api/evaluate/polygon: request query: " + req.queryString());
            if (req.queryParams().contains("minDate")) {
                minDateStr = req.queryParams("minDate");
            }
            if (req.queryParams().contains("maxDate")) {
                maxDateStr = req.queryParams("maxDate");
            }
            if (req.queryParams().contains("location")) {
                location = (String) req.queryParams("location");
            }
            if (req.queryParams().contains("limit")) {
                try {
                    limit = Integer.parseInt((String) req.queryParams("limit"));
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return "{\"error\":\"limit parameter cannot be parsed as integer\"}";
                }
            }
        }
        if (req.body().isEmpty() && req.queryParams().isEmpty()) {
            System.out.println("GET /api/evaluate/polygon: No query or body params");
            res.type("text/html");
            return "<html><body><h1>Polygon evaluation: no parameters are given</h1>"
                    + "<p>Use this endpoint with <b>limit</b> (optional, default=" + ProblemFilter.LIMIT_DEFAULT
                    + "), <b>minDate</b> (optional), <b>maxDate</b> (optional) "
                    + "and <b>location</b> (optional as a featue, when minDate or maxDate is set) parameters</p>"
                    + "<p>Possible formats for <b>location</b> parameter:"
                    + "<ul><li>\"latitude1,longitude1;latitude2,longitude2\" for a pair of coordinates to define a square on a map</li>"
                    + "<li>\"municipality_name\" for a municipality (see the list at /api/groups/municipality)</li>"
                    + "<li>\"district_name\" for a district (see the list at /api/groups/district)</li>"
                    + "<li>(geojson) for a polygon on a map</li></ul></p>"
                    + "<p>The result will be a number of problems used for calculations and 3 evaluation"
                    + " values with total evaluation value</p></body></html>";
        }
        if (minDateStr == null && maxDateStr == null && location == null) {
            res.status(400);
            return "{\"error\": \"Request is missing all of the minDateStr, maxDateStr and location\"}";
        }

        HALResponse result = new HALResponse(req.uri() + ((req.queryString() == null || req.queryString().isEmpty())  ? "" : ("?" + req.queryString())));
        boolean isCoordsPair = location != null ? location.matches("^\\d+(\\.\\d+)?,\\d+(\\.\\d+)?;\\d+(\\.\\d+)?,\\d+(\\.\\d+)?$") : false;
        if (isCoordsPair || location != null && location.startsWith("{") || minDateStr != null || maxDateStr != null) {
            ProblemFilter pf = new ProblemFilter();
            if (limit != null) {
                pf.setLimit(limit);
            }
            if (isCoordsPair) {
                String[] coordsPair = location.split(";");
                pf.setCoords(coordsPair[0], coordsPair[1]);
            } else if (location.startsWith("{")) {
                pf.setGeoJSON(location);
            } else if (location != null) {
                if (districts.contains(location)) {
                    pf.setDistrict(location);
                } else if (municipalities.contains(location)) {
                    pf.setMunicipality(location);
                } else {
                    res.status(400);
                    res.type("application/json");
                    return "{\"error\": \"location (" + location.replace("\"", "\\\"") + ") is not a municipality or district\"}";
                }
            }
            try {
                if (minDateStr != null) {
                    pf.setMinCreationDate(LocalDate.parse(minDateStr));
                }
            } catch (Exception e) {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"minDate is not a valid date (use format YYYY-MM-DD)\"}";
            }
            try {
                if (maxDateStr != null) {
                    pf.setMaxCreationDate(LocalDate.parse(maxDateStr));
                }
            } catch (Exception e) {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"maxDate is not a valid date (use format YYYY-MM-DD)\"}";
            }
            List<Problem> problems = serviceFactory.getPorblemService().getProblemsByFilter(pf);
            result.embedded().put("problems_number", problems.size());
            JSONObject rank = new JSONObject();
            if (problems.size() == 0) {
                rank.put("S", null);
                rank.put("I", null);
                rank.put("C", null);
                rank.put("total", null);
            } else {
                Double[] sict = Utils.evaluatePolygon(problems);
                rank.put("S", sict[0]);
                rank.put("I", sict[1]);
                rank.put("C", sict[2]);
                rank.put("total", sict[3]);
            }
            result.embedded().put("rank", rank);
        } else {
            Double[] tmp;
            if (districts.contains(location)) {
                tmp = serviceFactory.getPorblemService().getEvaluationByDistrict(location);
            } else if (municipalities.contains(location)) {
                tmp = serviceFactory.getPorblemService().getEvaluationByMunicipality(location);
            } else {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"location (" + location.replace("\"", "\\\"") + ") is not a municipality or district\"}";
            }
            result.embedded().put("problems_number", tmp[4]);
            JSONObject rank = new JSONObject();
            rank.put("S", tmp[0]);
            rank.put("I", tmp[1]);
            rank.put("C", tmp[2]);
            rank.put("total", tmp[3]);
            result.embedded().put("rank", rank);
        }
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static String objectsEvaluation(Request req, Response res) {
        String minDateStr = null, maxDateStr = null;
        String type = null, location = null;
        Integer limit = null;
        if (!req.body().isEmpty()) {
            JSONObject requestBody;
            try {
                requestBody = (JSONObject) new JSONParser().parse(req.body());
            } catch (ParseException e) {
                res.status(400);
                return "{\"error\": \"Body cannot be parsed as json\"}";
            }
            System.out.println("GET /api/evaluate/objects: RequestBody: " + requestBody);
            minDateStr = (String) requestBody.get("minDate");
            maxDateStr = (String) requestBody.get("maxDate");
            type = (String) requestBody.get("type");
            location = (String) requestBody.get("location");
            if (requestBody.containsKey("limit")) {
                try {
                    limit = Integer.parseInt((String) requestBody.get("limit"));
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return "{\"error\":\"limit parameter cannot be parsed as integer\"}";
                }
            }
        }
        if (!req.queryParams().isEmpty()) {
            System.out.println("GET /api/evaluate/objects: request query: " + req.queryString());
            if (req.queryParams().contains("minDate")) {
                minDateStr = req.queryParams("minDate");
            }
            if (req.queryParams().contains("maxDate")) {
                maxDateStr = req.queryParams("maxDate");
            }
            if (req.queryParams().contains("type")) {
                type = (String) req.queryParams("type");
            }
            if (req.queryParams().contains("location")) {
                location = (String) req.queryParams("location");
            }
            if (req.queryParams().contains("limit")) {
                try {
                    limit = Integer.parseInt((String) req.queryParams("limit"));
                } catch (NumberFormatException e) {
                    res.status(400);
                    res.type("application/json");
                    return "{\"error\":\"limit parameter cannot be parsed as integer\"}";
                }
            }
        }
        if (req.body().isEmpty() && req.queryParams().isEmpty()) {
            System.out.println("GET /api/evaluate/objects: No query or body params");
            res.type("text/html");
            return "<html><body><h1>Objects evaluation: no parameters are given</h1>"
                    + "<p>Use this endpoint with <b>limit</b> (optional, default=" + ProblemFilter.LIMIT_DEFAULT
                    + "), <b>minDate</b> (optional), <b>maxDate</b> (optional) "
                    + "and <b>location</b>  parameters </p>"
                    + "<p>Possible formats for <b>location</b> parameter:"
                    + "<ul><li>\"latitude1,longitude1;latitude2,longitude2\" for a pair of coordinates to define a square on a map</li>"
                    + "<li>\"municipality_name\" for a municipality (see the list at /api/groups/municipality)</li>"
                    + "<li>\"district_name\" for a district (see the list at /api/groups/district)</li>"
                    + "<li>(geojson) for a polygon on a map</li></ul>"
                    + "Type must be one of the: "
                    + "<i>building</i>, <i>yard</i>, <i>maf</i>, <i>water</i>, <i>greenzone</i>, <i>uds</i>, "
                    + "<i>everything</i> (this is also default value when type is not set)</i></p>"
                    + "<p>The result will be a list of coordinates and their evaluation results"
                    + " (S, I, C and total value)</p></body></html>";
        }

        if (type == null) {
            type = "everything";
        }

        HALResponse result = new HALResponse(req.uri() + ((req.queryString() == null || req.queryString().isEmpty())  ? "" : ("?" + req.queryString())));

        ProblemFilter pf = new ProblemFilter();
        if (limit != null) {
            pf.setLimit(limit);
        }
        if (location != null) {
            if (location.matches("^\\d+(\\.\\d+)?,\\d+(\\.\\d+)?;\\d+(\\.\\d+)?,\\d+(\\.\\d+)?$")) {
                String[] coordsPair = location.split(";");
                pf.setCoords(coordsPair[0], coordsPair[1]);
            } else if (location.startsWith("{")) {
                System.out.println("Set location");
                pf.setGeoJSON(location);
            } else if (districts.contains(location)) {
                pf.setDistrict(location);
            } else if (municipalities.contains(location)) {
                pf.setMunicipality(location);
            } else {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"location (" + location.replace("\"", "\\\"") + ") is not a municipality or district\"}";
            }
        } else {
            res.status(400);
            res.type("application/json");
            return "{\"error\": \"You need to set location for object evaluation\"}";
        }
        if (minDateStr != null) {
            try {
                pf.setMinCreationDate(LocalDate.parse(minDateStr));
            } catch (Exception e) {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"minDate is not a valid date (use format YYYY-MM-DD)\"}";
            }
        }
        if (maxDateStr != null) {
            try {
                pf.setMaxCreationDate(LocalDate.parse(maxDateStr));
            } catch (Exception e) {
                res.status(400);
                res.type("application/json");
                return "{\"error\": \"maxDate is not a valid date (use format YYYY-MM-DD)\"}";
            }
        }
        
        List<Problem> problems = serviceFactory.getPorblemService().getProblemsByFilter(pf);
        
        JSONArray evaluations = new JSONArray();
        List<Double[]> evaluation = Utils.evaluateObjects(problems, type);
        result.embedded().put("total_problems_number", problems.size());
        result.embedded().put("type_problems_number", evaluation.size());
        for (Double[] sict: evaluation) {
            JSONObject rank = new JSONObject();
            {
                JSONArray coordinates = new JSONArray();
                coordinates.add(sict[0]);
                coordinates.add(sict[1]);
                rank.put("coordinates", coordinates);
            }
            rank.put("S", sict[2]);
            rank.put("I", sict[3]);
            rank.put("C", sict[4]);
            rank.put("total", sict[5]);
            evaluations.add(rank);
        }
        result.embedded().put("evaluations", evaluations);
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static String getDistrictsEvaluation(Request req, Response res) {
        HALResponse result = new HALResponse(req.uri() + ((req.queryString() == null || req.queryString().isEmpty()) ? "" : ("?" + req.queryString())));
        List<JSONObject> districts = new ArrayList<>();
        
        ProblemService problemService = serviceFactory.getPorblemService();
        Map<String, Double[]> evaluation;
        if (req.queryParams().contains("date")) {
            evaluation = problemService.getEvaluationOfDistricts(req.queryParams("date"), req.queryParams().contains("update"));
        } else {
            evaluation = problemService.getEvaluationOfDistricts(req.queryParams().contains("update"));
        }
        for (Entry<String, Double[]> districtAndEvaluation: evaluation.entrySet()) {
            Double[] tmp = districtAndEvaluation.getValue();
            JSONObject rank = new JSONObject();
            rank.put("name", districtAndEvaluation.getKey());
            rank.put("S", tmp[0]);
            rank.put("I", tmp[1]);
            rank.put("C", tmp[2]);
            rank.put("total", tmp[3]);
            rank.put("problems_number", (int) (double) tmp[4]);
            districts.add(rank);
        }
        result.embedded().put("districts", districts);

        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static String getMunicipalitiesEvaluation(Request req, Response res) {
        HALResponse result = new HALResponse(req.uri() + ((req.queryString() == null || req.queryString().isEmpty()) ? "" : ("?" + req.queryString())));
        List<JSONObject> municipalities = new ArrayList<>();
        
        ProblemService problemService = serviceFactory.getPorblemService();
        Map<String, Double[]> evaluation;
        if (req.queryParams().contains("date")) {
            evaluation = problemService.getEvaluationOfMunicipalities(req.queryParams("date"), req.queryParams().contains("update"));
        } else {
            evaluation = problemService.getEvaluationOfMunicipalities(req.queryParams().contains("update"));
        }
        for (Entry<String, Double[]> municipalityAndEvaluation: evaluation.entrySet()) {
            Double[] tmp = municipalityAndEvaluation.getValue();
            JSONObject rank = new JSONObject();
            rank.put("name", municipalityAndEvaluation.getKey());
            rank.put("S", tmp[0]);
            rank.put("I", tmp[1]);
            rank.put("C", tmp[2]);
            rank.put("total", tmp[3]);
            rank.put("problems_number", (int) (double) tmp[4]);
            municipalities.add(rank);
        }
        result.embedded().put("municipalities", municipalities);

        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String getProblemsMonthsCount(Request req, Response res) {
        HALResponse result = new HALResponse(req.uri());
        List<List> counts = serviceFactory.getPorblemService()
                .getProblemsMonthsCount(req.queryParams().contains("update") ? true : false)
                .stream().map(monthAndCount -> {return List.of(monthAndCount.getKey(), monthAndCount.getValue());}).collect(Collectors.toList());
        result.embedded().put("counts", counts);
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    public static String getPlatformServicesClassifiedList(Request req, Response res) {
        HALResponse result = new HALResponse(req.uri());
        JSONArray services = new JSONArray();
        services.addAll(serviceFactory.getPorblemService().getServicesClassificated());
        result.embedded().put("services", services);
        res.type("application/hal+json");
        return result.toJSONString();
    }
    
    @SuppressWarnings("unchecked")
    public static String getPlatformServiceStats(Request req, Response res) {
        HALResponse result = new HALResponse(req.uri());
        String location = req.queryParams("location");
        try {
            JSONObject requestBody = (JSONObject) new JSONParser().parse(req.body());
            if (requestBody.containsKey("location")) {
                location = (String) requestBody.get("location");
            }
        } catch (ParseException e) {
        }
        String locationType = null;
        if (location != null) {
            if (location.startsWith("{")) {
                locationType = "geojson";
            } else if (districts.contains(location)) {
                locationType = "district";
            } else if (municipalities.contains(location)) {
                locationType = "municipality";
            } else {
                location = location + " (not found)";
            }
        }
        Map<String, Map<String, Map<Double, Integer>>> counts = 
                serviceFactory.getPorblemService().getProblemsClassifiedCount(req.queryParams("service"), req.queryParams("date"), location, locationType);
        JSONArray services = new JSONArray();
        counts.keySet().stream().forEach(service -> {
            JSONObject serv = new JSONObject();
            serv.put("service", service);
            JSONArray subcategories = new JSONArray();
            counts.get(service).keySet().stream().forEach(subcategory -> {
                JSONObject subcat = new JSONObject();
                subcat.put("subcategory", subcategory);
                counts.get(service).get(subcategory).keySet().stream().forEach(criticality -> {
                    subcat.put(criticality, counts.get(service).get(subcategory).get(criticality));
                });
                subcategories.add(subcat);
            });
            serv.put("subcategories", subcategories);
            services.add(serv);
        });
        result.addEmbedded("services", services);
        JSONObject parameters = new JSONObject();
        parameters.put("location", location);
        parameters.put("service", req.queryParams("service"));
        parameters.put("date", req.queryParams("date"));
        result.addEmbedded("parameters", parameters);
        res.type("application/hal+json");
        return result.toJSONString();
    }
    
    @SuppressWarnings("unchecked")
    public static String getPlatformServiceEvaluation(Request req, Response res) {
        HALResponse result = new HALResponse(req.uri());
        String location = req.queryParams("location");
        try {
            JSONObject requestBody = (JSONObject) new JSONParser().parse(req.body());
            if (requestBody.containsKey("location")) {
                location = (String) requestBody.get("location");
            }
        } catch (ParseException e) {
        }
        String locationType = null;
        if (location != null) {
            if (location.startsWith("{")) {
                locationType = "geojson";
            } else if (districts.contains(location)) {
                locationType = "district";
            } else if (municipalities.contains(location)) {
                locationType = "municipality";
            } else {
                location = location + " (not found)";
            }
        }
        Map<String, Map<String, Object>> counts = 
                serviceFactory.getPorblemService().getProblemsClassifiedEvaluation(
                        req.queryParams("service"), req.queryParams("date"), location, locationType);
        JSONArray services = new JSONArray();
        counts.keySet().stream().forEach(service -> {
            JSONObject serv = new JSONObject();
            serv.put("service", service);
            serv.put("evaluation", counts.get(service).get("evaluation"));
            serv.put("problems_number", counts.get(service).get("problems_number"));
            serv.put("problems_number_raw", counts.get(service).get("problems_number_raw"));
            serv.put("number_of_services", counts.get(service).get("number_of_services"));
            services.add(serv);
        });
        result.addEmbedded("services", services);
        JSONObject parameters = new JSONObject();
        parameters.put("location", location);
        parameters.put("service", req.queryParams("service"));
        parameters.put("date", req.queryParams("date"));
        result.addEmbedded("parameters", parameters);
        res.type("application/hal+json");
        return result.toJSONString();
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        int api_port = 80, problems_db_port = 5432, houses_db_port = 5432;
        String problems_db_addr = "127.0.0.1", problems_db_name = "problems", problems_db_user = "postgres", problems_db_pass = "postgres";
        String houses_db_addr = "127.0.0.1", houses_db_name = "citydb", houses_db_user = "postgres", houses_db_pass = "postgres";
        boolean skip_evaluation = false;

        // Getting properties from launch.properties

        try (FileInputStream fis = new FileInputStream("launch.properties")) {
            Properties props = new Properties();
            props.load(fis);
            if (props.containsKey("problems_db_addr")) {
                problems_db_addr = props.getProperty("problems_db_addr");
            }
            if (props.containsKey("problems_db_name")) {
                problems_db_name = props.getProperty("problems_db_name");
            }
            if (props.containsKey("problems_db_user")) {
                problems_db_user = props.getProperty("problems_db_user");
            }
            if (props.containsKey("problems_db_pass")) {
                problems_db_pass = props.getProperty("problems_db_pass");
            }
            if (props.containsKey("houses_db_addr")) {
                houses_db_addr = props.getProperty("houses_db_addr");
            }
            if (props.containsKey("houses_db_name")) {
                houses_db_name = props.getProperty("houses_db_name");
            }
            if (props.containsKey("houses_db_user")) {
                houses_db_user = props.getProperty("houses_db_user");
            }
            if (props.containsKey("houses_db_pass")) {
                houses_db_pass = props.getProperty("houses_db_pass");
            }
            if (props.containsKey("api_port")) {
                try {
                    api_port = Integer.parseInt(props.getProperty("api_port"));
                } catch (NumberFormatException ex) {
                    System.err.println("api_port value (" + props.getProperty("api_port") + ") is not an integer, ignoring");
                }
            }
            if (props.containsKey("problems_db_port")) {
                try {
                    problems_db_port = Integer.parseInt(props.getProperty("problems_db_port"));
                } catch (NumberFormatException ex) {
                    System.err.println("db_port value (" + props.getProperty("problems_db_port") + ") is not an integer, ignoring");
                }
            }
            if (props.containsKey("houses_db_port")) {
                try {
                    houses_db_port = Integer.parseInt(props.getProperty("houses_db_port"));
                } catch (NumberFormatException ex) {
                    System.err.println("db_port value (" + props.getProperty("houses_db_port") + ") is not an integer, ignoring");
                }
            }
            if (props.containsKey("skip_evaluation") && !props.getProperty("skip_evaluation").equals("false") && !props.getProperty("skip_evaluation").equals("0")) {
                skip_evaluation = true;
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Getting properties from environment variables

        Map<String, String> env = System.getenv();
        if (env.containsKey("PROBLEMS_API_PORT")) {
            try {
                api_port = Integer.parseInt(env.get("PROBLEMS_API_PORT"));
            } catch (Exception ex) {
                System.err.println("Port number specified in environment variable 'PROBLEMS_API_PORT' ("
                    + env.get("PROBLEMS_API_PORT") + ") is not an integer, ignoring");
            }
        }
        if (env.containsKey("PROBLEMS_DB_PORT")) {
            try {
                problems_db_port = Integer.parseInt(env.get("PROBLEMS_DB_PORT"));
            } catch (Exception ex) {
                System.err.println("Port number specified in environment variable 'PROBLEMS_DB_PORT' ("
                    + env.get("PROBLEMS_DB_PORT") + ") is not an integer, ignoring");
            }
        }
        if (env.containsKey("PROBLEMS_DB_ADDR")) {
            problems_db_addr = env.get("PROBLEMS_DB_ADDR");
        }
        if (env.containsKey("PROBLEMS_DB_NAME")) {
            problems_db_name = env.get("PROBLEMS_DB_NAME");
        }
        if (env.containsKey("PROBLEMS_DB_USER")) {
            problems_db_user = env.get("PROBLEMS_DB_USER");
        }
        if (env.containsKey("PROBLEMS_DB_PASS")) {
            problems_db_pass = env.get("PROBLEMS_DB_PASS");
        }
        if (env.containsKey("HOUSES_DB_PORT")) {
            try {
                houses_db_port = Integer.parseInt(env.get("HOUSES_DB_PORT"));
            } catch (Exception ex) {
                System.err.println("Port number specified in environment variable 'HOUSES_DB_PORT' ("
                    + env.get("HOUSES_DB_PORT") + ") is not an integer, ignoring");
            }
        }
        if (env.containsKey("HOUSES_DB_ADDR")) {
            houses_db_addr = env.get("PROBLEMS_DB_ADDR");
        }
        if (env.containsKey("HOUSES_DB_NAME")) {
            houses_db_name = env.get("PROBLEMS_DB_NAME");
        }
        if (env.containsKey("HOUSES_DB_USER")) {
            houses_db_user = env.get("PROBLEMS_DB_USER");
        }
        if (env.containsKey("HOUSES_DB_PASS")) {
            houses_db_pass = env.get("PROBLEMS_DB_PASS");
        }
        if (env.containsKey("PROBLEMS_SKIP_EVALUATION") && !env.get("PROBLEMS_SKIP_EVALUATION").equals("0") &&
                !env.get("PROBLEMS_SKIP_EVALUATION").equalsIgnoreCase("false")) {
            skip_evaluation = true;
        }

        // Getting properties from command line arguments

        Options options = new Options();
        options.addOption(
                new Option("p", "port", true, String.format("port to run the server [default: %d]", api_port)));
        options.addOption(new Option("pH", "problems_db_addr", true,
                String.format("address of the postgres with problems [default: %s]", problems_db_addr)));
        options.addOption(new Option("pP", "problems_db_port", true,
                String.format("port of the postgres with problems [default: %s]", problems_db_port)));
        options.addOption(new Option("pN", "problems_db_name", true,
                String.format("name of the postgres database with problems [default: %s]", problems_db_name)));
        options.addOption(
                new Option("pU", "problems_db_user", true, String.format("user name for database [default: %s]", problems_db_user)));
        options.addOption(
                new Option("pW", "problems_db_pass", true, String.format("user password for database [default: %s]", problems_db_pass)));
        options.addOption(new Option("hH", "houses_db_addr", true,
                String.format("address of the postgres with houses [default: %s]", houses_db_addr)));
        options.addOption(new Option("hP", "houses_db_port", true,
                String.format("port of the postgres with houses [default: %s]", houses_db_port)));
        options.addOption(new Option("hN", "houses_db_name", true,
                String.format("name of the postgres database with houses [default: %s]", houses_db_name)));
        options.addOption(
                new Option("hU", "houses_db_user", true, String.format("user name for database [default: %s]", houses_db_user)));
        options.addOption(
                new Option("hW", "houses_db_pass", true, String.format("user password for database [default: %s]", houses_db_pass)));
        options.addOption(
                new Option("s", "skip_evaluation", false, "skip total municipality and district evaluation (if already present in database)"));

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            if (cmd.hasOption("port")) {
                api_port = Integer.parseInt(cmd.getOptionValue("port"));
            }
            if (cmd.hasOption("problems_db_addr")) {
                problems_db_addr = cmd.getOptionValue("problems_db_addr");
            }
            if (cmd.hasOption("problems_db_port")) {
                problems_db_port = Integer.parseInt(cmd.getOptionValue("problems_db_port"));
            }
            if (cmd.hasOption("problems_db_name")) {
                problems_db_name = cmd.getOptionValue("problems_db_name");
            }
            if (cmd.hasOption("problems_db_user")) {
                problems_db_user = cmd.getOptionValue("problems_db_user");
            }
            if (cmd.hasOption("problems_db_pass")) {
                problems_db_pass = cmd.getOptionValue("problems_db_pass");
            }
            if (cmd.hasOption("houses_db_addr")) {
                houses_db_addr = cmd.getOptionValue("houses_db_addr");
            }
            if (cmd.hasOption("houses_db_port")) {
                houses_db_port = Integer.parseInt(cmd.getOptionValue("houses_db_port"));
            }
            if (cmd.hasOption("houses_db_name")) {
                houses_db_name = cmd.getOptionValue("houses_db_name");
            }
            if (cmd.hasOption("houses_db_user")) {
                houses_db_user = cmd.getOptionValue("houses_db_user");
            }
            if (cmd.hasOption("houses_db_pass")) {
                houses_db_pass = cmd.getOptionValue("houses_db_pass");
            }
            if (cmd.hasOption("skip_evaluation")) {
                skip_evaluation = true;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("problems-api-server", options);
            System.exit(1);
        }

        // Setting properties

        ConnectionManager problemsDB = new ConnectionManager(problems_db_addr, problems_db_port, problems_db_name, problems_db_user, problems_db_pass);
        ConnectionManager housesDB = new ConnectionManager(houses_db_addr, houses_db_port, houses_db_name, houses_db_user, houses_db_pass);
        serviceFactory = new ServiceFactory(new RepositoryFactory(housesDB, problemsDB));
        

        if (!skip_evaluation) {
            ProblemService problemService = serviceFactory.getPorblemService();
            System.out.println("Evaluating municipalities:");
            problemService.evaluateMunicipalities();
            System.out.println("Evaluating districts:");
            problemService.evaluateDistricts();
            System.out.println("Evaluation finished");
        } else {
            System.out.println("Evaluation skipped");
        }

        {
            ProblemService problemService = serviceFactory.getPorblemService();
            municipalities = problemService.getGroupsSize("municipality").keySet();
            districts = problemService.getGroupsSize("district").keySet();
        }

        // Configuring and launching server

        Spark.port(api_port);

        Spark.options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        Spark.before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
        Spark.after((request, response) -> response.header("Content-Encoding", "gzip"));

        Spark.exception(Exception.class, (e, req, res) -> {
            res.type("application/json");
            res.status(500);
            JSONObject result = new JSONObject();
            result.put("path", req.url());
            StringBuilder query = new StringBuilder("?");
            req.queryParams().forEach(param -> {
                query.append(param);
                query.append("=");
                query.append(req.queryParams(param));
                query.append("&");
            });
            query.setLength(query.length() - 1);
            result.put("params", query.toString());
            JSONArray exception = new JSONArray();
            for (String s : ExceptionUtils.getStackFrames(e)) {
                exception.add(s);
            }
            System.out.println(String.join("\n", exception));
            result.put("trace", exception);
            result.put("body", req.body());
            res.body(result.toJSONString());
        });

        // INFO BLOCK

        Spark.get("/", "text/html", (req, res) -> "<html><body><h1>Problems API version 2021-09-20</h1>"
                + "<p>Set Accept header to include \"json\" or \"hal+json\", and you will get api description in HAL"
                + " format from this page (or open <a href=/api>/api</a> page)</p>"
                + "<ul><li><a href=/api/problems/search>Search problems API</a></li>"
                + "<li><a href=/api/groups>Get groups API</a></li>"
                + "<li><a href=/api/evaluation/polygon>Polygon evaluation API</a></li>"
                + "<ul><li><a href=/api/evaluation/municipalities>Polygon evaluation (municipalities)</a></li>"
                + "<li><a href=/api/evaluation/districts>Polygon evaluation (districts)</a></li></ul>"
                + "<li><a href=/api/evaluation/objects>Objects evaluation API</a></li></ul></body></html>");

        Spark.get("/api", (req, res) -> apiDefinition(req, res));
        Spark.get("/api/", (req, res) -> apiDefinition(req, res));

        Spark.get("/", "application/json", (req, res) -> {
            res.redirect("/api", 303);
            return "OK";
        });

        
        Spark.redirect.get("/api/groups", "/api/groups/");
        Spark.get("/api/groups/",
                (req, res) -> "<html><body><a href=groups/status>statuses</a>, <a href=groups/category</a>categories</a>," +
                    " <a href=groups/subcategory>subcategories</a>, <a href=groups/municipality>municipalities</a>," +
                    " <a href=groups/district>districts</a></body></html>");

        // PROBLEMS BLOCK

        Spark.get("/api/problems/search", (req, res) -> searchProblems(req, res) );
        Spark.get("/api/problems/search/",  (req, res) -> searchProblems(req, res));

        Spark.get("/api/problems/:problemID", (req, res) -> getProblem(req, res));
        Spark.get("/api/problems/:problemID/", (req, res) -> getProblem(req, res));

        // GROUPS BLOCK

        Spark.get("/api/groups/:labelName", (req, res) -> getGroups(req, res));
        Spark.get("/api/groups/:labelName/", (req, res) -> getGroups(req, res));

        // EVALUATION BLOCK

        Spark.get("/api/evaluation/polygon", (req, res) -> evaluationPolygon(req, res));
        Spark.get("/api/evaluation/polygon/", (req, res) -> evaluationPolygon(req, res));

        Spark.get("/api/evaluation/districts", (req, res) -> getDistrictsEvaluation(req, res));
        Spark.get("/api/evaluation/districts/", (req, res) -> getDistrictsEvaluation(req, res));

        Spark.get("/api/evaluation/municipalities", (req, res) -> getMunicipalitiesEvaluation(req, res));
        Spark.get("/api/evaluation/municipalities/", (req, res) -> getMunicipalitiesEvaluation(req, res));

        Spark.get("/api/evaluation/objects", (req, res) -> objectsEvaluation(req, res));
        Spark.get("/api/evaluation/objects/", (req, res) -> objectsEvaluation(req, res));

        // EVALUATION BY MONTH BLOCK

        Spark.get("/api/evaluation/months", (req, res) -> getProblemsMonthsCount(req, res));
        Spark.get("/api/evaluation/months/", (req, res) -> getProblemsMonthsCount(req, res));

        // PLATFORM OBJECTS EVALUATION BLOCK
        
        Spark.get("/api/platform-services/list", (req, res) -> getPlatformServicesClassifiedList(req, res));
        Spark.get("/api/platform-services/list/", (req, res) -> getPlatformServicesClassifiedList(req, res));
        
        Spark.get("/api/platform-services/stats", (req, res) -> getPlatformServiceStats(req, res));
        Spark.get("/api/platform-services/stats/", (req, res) -> getPlatformServiceStats(req, res));
        
        Spark.get("/api/platform-services/evaluation", (req, res) -> getPlatformServiceEvaluation(req, res));
        Spark.get("/api/platform-services/evaluation/", (req, res) -> getPlatformServiceEvaluation(req, res));
        
    }
}