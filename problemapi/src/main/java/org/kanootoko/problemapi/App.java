package org.kanootoko.problemapi;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.kanootoko.problemapi.models.Coordinates;
import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.services.ProblemService;
import org.kanootoko.problemapi.utils.ConnectionManager;
import org.kanootoko.problemapi.utils.ProblemFilter;
import org.kanootoko.problemapi.utils.ServiceFactory;

import spark.Spark;

public class App {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        int port_number = 80;
        String db_string = "jdbc:postgresql://127.0.0.1:5432/problems", db_user = "postgres", db_pass = "postgres";

        Options options = new Options();
        options.addOption(new Option("p", "port", true, String.format("port to run the server [default: %d]", port_number)));
        options.addOption(new Option("d", "db_string", true, String.format("string for jdbc to connect to database [default: %s]", db_string)));
        options.addOption(new Option("u", "db_user", true, String.format("user name for database [default: %s]", db_user)));
        options.addOption(new Option("P", "db_pass", true, String.format("user password for database [default: %s]", db_pass)));

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            if (cmd.hasOption("port")) {
                port_number = Integer.parseInt(cmd.getOptionValue("port"));
            }
            if (cmd.hasOption("db_string")) {
                db_string = cmd.getOptionValue("db_string");
            }
            if (cmd.hasOption("db_user")) {
                db_user = cmd.getOptionValue("db_user");
            }
            if (cmd.hasOption("db_pass")) {
                db_pass = cmd.getOptionValue("db_pass");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("problems-api-server", options);
            System.exit(1);
        }
        ConnectionManager.setDB_string(db_string);
        ConnectionManager.setDB_user(db_user);
        ConnectionManager.setDB_pass(db_pass);

        Spark.port(port_number);

        Spark.get("/", (req, res) -> "<html><body><a href=\"/problems/search\">Search problems API</a><br/><a href=/groups>Get groups API</a></body></html>");
        Spark.get("/groups", (req, res) -> "<html><body>Most needed variants: /status , /category , /subcategory</body></html>");

        Spark.get("/problems/search", (req, res) -> {
            String minDateStr, maxDateStr;
            String firstCoordStr, secondCoordStr;
            ProblemFilter pf = new ProblemFilter();
            if (!req.body().isEmpty()) {
                JSONObject requestBody = (JSONObject) new JSONParser().parse(req.body());
                System.out.println("GET /problems: RequestBody: " + requestBody);
                minDateStr = (String) requestBody.get("minDate");
                maxDateStr = (String) requestBody.get("maxDate");
                firstCoordStr = (String) requestBody.get("firstCoord");
                secondCoordStr = (String) requestBody.get("secondCoord");
                pf.setCategory((String) requestBody.get("category"));
                pf.setSubcategory((String) requestBody.get("subcategory"));
                pf.setStatus((String) requestBody.get("status"));
                if (requestBody.containsKey("limit")) {
                    pf.setLimit(Integer.parseInt((String) requestBody.get("limit")));
                }
            } else if (!req.queryParams().isEmpty()) {
                System.out.println("GET /problems: request query: " + req.queryString());
                minDateStr = req.queryParams("minDate");
                maxDateStr = req.queryParams("maxDate");
                firstCoordStr = (String) req.queryParams("firstCoord");
                secondCoordStr = (String) req.queryParams("secondCoord");
                pf.setCategory((String) req.queryParams("category"));
                pf.setSubcategory((String) req.queryParams("subcategory"));
                pf.setStatus((String) req.queryParams("status"));
                if (req.queryParams("limit") != null) {
                    pf.setLimit(Integer.parseInt(req.queryParams("limit")));
                }
            } else {
                System.out.println("GET /problems: No query or body params");
                res.type("text/html");
                return "<html>" +
                    "<body>" +
                        "<h1>No parameters are given</h1>" +
                        "<p>Use this endpoint with <b>minDate</b> and/or <b>maxDate</b>, " +
                        "<b>firstPoint</b> and <b>secondPoint</b>, <b>status</b>, <b>category</b>," +
                        "<b>subcategory</b>, <b>limit</b> parameters</p>" +
                        "<p>For points use format \"longitude,latitude\", for dates - \"YYYY-MM-DD\"</p>" +
                        "<p>The result will be a list of problems, coordinates in format [latitude, longitude] (!)</p>" +
                    "</body>" +
                "</html>";
            }
            try {
                if (minDateStr != null) {
                    pf.setMinCreationDate(LocalDate.parse(minDateStr));
                }
                if (maxDateStr != null) {
                    pf.setMaxCreationDate(LocalDate.parse(maxDateStr));
                }
            } catch (Exception e) {
                pf.setMinCreationDate(null).setMaxCreationDate(null);
                e.printStackTrace();
            }
            if (firstCoordStr != null && secondCoordStr != null) {
                try {
                    String[] coords_arr = firstCoordStr.split(",");
                    Coordinates first = new Coordinates(Double.parseDouble(coords_arr[0]), Double.parseDouble(coords_arr[1]));
                    coords_arr = secondCoordStr.split(",");
                    Coordinates second = new Coordinates(Double.parseDouble(coords_arr[0]), Double.parseDouble(coords_arr[1]));
                    pf.setFirstCoord(first);
                    pf.setSecondCoord(second);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ProblemService ps = ServiceFactory.getPorblemService();
            List<Problem> problems = ps.getProblemsByFilter(pf);
            res.type("application/hal+json");
            JSONObject result = new JSONObject();
            result.put("problems_number", problems.size());
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
                problem.put("_links", p.getLinks());
                problemsArray.add(problem);
            }
            result.put("problems", problemsArray);
            return result.toJSONString();
        });
        Spark.get("/problem/:problemID", (req, res) -> {
            int problemID = Integer.parseInt(req.params(":problemID"));
            ProblemService ps = ServiceFactory.getPorblemService();
            JSONObject result = new JSONObject();
            result.put("problem", ps.getProblemByID(problemID).toJSON());
            res.type("application/hal+json");
            return result.toJSONString();
        });
        Spark.get("/groups/:labelName", (req, res) -> {
            ProblemService ps = ServiceFactory.getPorblemService();
            JSONObject result = new JSONObject();
            Map<String, Integer> groups = ps.getGroupsSize(req.params(":labelName"));
            result.put("label_name", req.params(":labelName"));
            result.put("groups_number", groups.size());
            JSONArray groupsArray = new JSONArray();
            for (Map.Entry<String, Integer> el: groups.entrySet()) {
                JSONObject group = new JSONObject();
                group.put("name", el.getKey());
                group.put("size", el.getValue());
                groupsArray.add(group);
            }
            result.put("groups", groupsArray);
            res.type("application/hal+json");
            return result.toJSONString();
        });
    }
}