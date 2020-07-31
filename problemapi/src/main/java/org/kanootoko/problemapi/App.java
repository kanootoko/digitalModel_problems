package org.kanootoko.problemapi;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
        int api_port = 80, db_port = 5432;
        String db_addr = "localhost", db_name = "problems", db_user = "postgres", db_pass = "postgres";

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
                db_port = Integer.parseInt(env.get("PROBLEMS_DB_PORT"));
            } catch (Exception ex) {
                System.err.println("Port number specified in environment variable 'PROBLEMS_DB_PORT' ("
                    + env.get("PROBLEMS_DB_PORT") + ") is not an integer, ignoring");
            }
        }
        if (env.containsKey("PROBLEMS_DB_ADDR")) {
            db_addr = env.get("PROBLEMS_DB_ADDR");
        }
        if (env.containsKey("PROBLEMS_DB_NAME")) {
            db_name = env.get("PROBLEMS_DB_NAME");
        }
        if (env.containsKey("PROBLEMS_DB_USER")) {
            db_user = env.get("PROBLEMS_DB_USER");
        }
        if (env.containsKey("PROBLEMS_DB_PASS")) {
            db_pass = env.get("PROBLEMS_DB_PASS");
        }

        Options options = new Options();
        options.addOption(
                new Option("p", "port", true, String.format("port to run the server [default: %d]", api_port)));
        options.addOption(new Option("H", "db_addr", true,
                String.format("address of the postgres with problems [default: %s]", db_addr)));
        options.addOption(new Option("P", "db_port", true,
                String.format("port of the postgres with problems [default: %s]", db_port)));
        options.addOption(new Option("N", "db_name", true,
                String.format("name of the postgres database with problems [default: %s]", db_name)));
        options.addOption(
                new Option("U", "db_user", true, String.format("user name for database [default: %s]", db_user)));
        options.addOption(
                new Option("W", "db_pass", true, String.format("user password for database [default: %s]", db_pass)));

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            if (cmd.hasOption("port")) {
                api_port = Integer.parseInt(cmd.getOptionValue("port"));
            }
            if (cmd.hasOption("db_addr")) {
                db_addr = cmd.getOptionValue("db_addr");
            }
            if (cmd.hasOption("db_port")) {
                db_port = Integer.parseInt(cmd.getOptionValue("db_port"));
            }
            if (cmd.hasOption("db_name")) {
                db_name = cmd.getOptionValue("db_name");
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
        ConnectionManager.setDB_addr(db_addr);
        ConnectionManager.setDB_name(db_name);
        ConnectionManager.setDB_port(db_port);
        ConnectionManager.setDB_user(db_user);
        ConnectionManager.setDB_pass(db_pass);

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
        Spark.after((request, response) -> {
            response.header("Content-Encoding", "gzip");
        });

        Spark.get("/", "text/html", (req, res) -> "<html><body><h1>Problems API version 2020-07-30</h1>"
                + "<p>Set Accept header to include json or hal+json, and you will get api description in HAL format from this page</p>"
                + "<a href=\"/api/problems/search\">Search problems API</a><br/><a href=/api/groups>Get groups API</a></body></html>");
        Spark.get("/api", (req, res) -> {
            JSONObject result = new JSONObject();
            result.put("version", "2020-07-30");
            JSONObject links = new JSONObject();
            links.put("self", "/api");
            links.put("problems-search", new JSONObject());
            ((JSONObject) links.get("problems-search"))
                .put("href", "/api/problems/search{?minDate,maxDate,firstCoord,secondCoord,category,subcategory,status,municipality,region,limit}");
            ((JSONObject) links.get("problems-search")).put("templated", true);
            links.put("categories", new JSONObject());
            ((JSONObject) links.get("categories")).put("href", "/api/groups/category");
            links.put("subcategories", new JSONObject());
            ((JSONObject) links.get("subcategories")).put("href", "/api/groups/subcategory");
            links.put("statuses", new JSONObject());
            ((JSONObject) links.get("statuses")).put("href", "/api/groups/status");
            links.put("munitipalities", new JSONObject());
            ((JSONObject) links.get("munitipalities")).put("href", "/api/groups/municipality");
            links.put("regions", new JSONObject());
            ((JSONObject) links.get("regions")).put("href", "/api/groups/region");
            result.put("_links", links);
            res.type("application/hal+json");
            return result.toJSONString();
        });

        Spark.get("/", "application/json", (req, res) -> {
            res.redirect("/api", 303);
            return "OK";
        });

        Spark.get("/api/groups",
                (req, res) -> "<html><body><a href=groups/status>statuses</a>, <a href=groups/category</a>categories</a>," +
                    " <a href=groups/subcategory>subcategories</a>, <a href=groups/municipality>municipalities</a>," +
                    " <a href=groups/region>regions</a></body></html>");

        Spark.get("/api/problems/search", (req, res) -> {
            String minDateStr, maxDateStr;
            String firstCoordStr, secondCoordStr;
            ProblemFilter pf = new ProblemFilter();
            if (!req.body().isEmpty()) {
                JSONObject requestBody = (JSONObject) new JSONParser().parse(req.body());
                System.out.println("GET /api/problems: RequestBody: " + requestBody);
                minDateStr = (String) requestBody.get("minDate");
                maxDateStr = (String) requestBody.get("maxDate");
                firstCoordStr = (String) requestBody.get("firstCoord");
                secondCoordStr = (String) requestBody.get("secondCoord");
                pf.setCategory((String) requestBody.get("category"));
                pf.setSubcategory((String) requestBody.get("subcategory"));
                pf.setMunicipality((String) requestBody.get("municipality"));
                pf.setRegion((String) requestBody.get("region"));
                pf.setStatus((String) requestBody.get("status"));
                if (requestBody.containsKey("limit")) {
                    pf.setLimit(Integer.parseInt((String) requestBody.get("limit")));
                }
            } else if (!req.queryParams().isEmpty()) {
                System.out.println("GET /api/problems: request query: " + req.queryString());
                minDateStr = req.queryParams("minDate");
                maxDateStr = req.queryParams("maxDate");
                firstCoordStr = (String) req.queryParams("firstCoord");
                secondCoordStr = (String) req.queryParams("secondCoord");
                pf.setCategory((String) req.queryParams("category"));
                pf.setSubcategory((String) req.queryParams("subcategory"));
                pf.setMunicipality((String) req.queryParams("municipality"));
                pf.setRegion((String) req.queryParams("region"));
                pf.setStatus((String) req.queryParams("status"));
                if (req.queryParams("limit") != null) {
                    pf.setLimit(Integer.parseInt(req.queryParams("limit")));
                }
            } else {
                System.out.println("GET /api/problems: No query or body params");
                res.type("text/html");
                return "<html>" + "<body>" + "<h1>No parameters are given</h1>"
                        + "<p>Use this endpoint with <b>minDate</b> and/or <b>maxDate</b>, "
                        + "<b>firstPoint</b> and <b>secondPoint</b>, <b>status</b>, <b>category</b>,"
                        + "<b>subcategory</b>, <b>municipality</b>, <b>region</b>, <b>limit</b> parameters</p>"
                        + "<p>For points use format \"latitude,longitude\", for dates - \"YYYY-MM-DD\"</p>"
                        + "<p>The result will be a list of problems, coordinates in format of array [latitude, longitude]</p>"
                        + "</body>" + "</html>";
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
                    Coordinates first = new Coordinates(Double.parseDouble(coords_arr[0]),
                            Double.parseDouble(coords_arr[1]));
                    coords_arr = secondCoordStr.split(",");
                    Coordinates second = new Coordinates(Double.parseDouble(coords_arr[0]),
                            Double.parseDouble(coords_arr[1]));
                    pf.setFirstCoord(first);
                    pf.setSecondCoord(second);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ProblemService ps = ServiceFactory.getPorblemService();
            List<Problem> problems = ps.getProblemsByFilter(pf);
            JSONObject result = new JSONObject();
            result.put("_links", new JSONObject());
            ((JSONObject) result.get("_links")).put("self", new JSONObject());
            ((JSONObject) ((JSONObject) result.get("_links")).get("self")).put("href", req.uri() + "?" + req.queryString());
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
            ((JSONObject) result.get("_embedded")).put("problems", problemsArray);
            res.type("application/hal+json");
            return result.toJSONString();
        });
        Spark.get("/api/problems/:problemID", (req, res) -> {
            int problemID = Integer.parseInt(req.params(":problemID"));
            ProblemService ps = ServiceFactory.getPorblemService();
            Problem problem = ps.getProblemByID(problemID);
            JSONObject result = new JSONObject();
            result.put("_links", problem.getLinks());
            result.put("_embedded", problem.toJSON());
            res.type("application/hal+json");
            return result.toJSONString();
        });
        Spark.get("/api/groups/:labelName", (req, res) -> {
            ProblemService ps = ServiceFactory.getPorblemService();
            Map<String, Integer> groups = ps.getGroupsSize(req.params(":labelName"));
            JSONObject result = new JSONObject();
            result.put("_links", new JSONObject());
            ((JSONObject) result.get("_links")).put("self", new JSONObject());
            ((JSONObject) ((JSONObject) result.get("_links")).get("self")).put("href", req.uri());
            result.put("size", groups.size());
            JSONArray groupsArray = new JSONArray();
            for (Map.Entry<String, Integer> el : groups.entrySet()) {
                JSONObject group = new JSONObject();
                group.put("name", el.getKey());
                group.put("size", el.getValue());
                groupsArray.add(group);
            }
            result.put("_embedded", new JSONObject());
            ((JSONObject) result.get("_embedded")).put("groups", groupsArray);
            res.type("application/hal+json");
            return result.toJSONString();
        });
    }
}