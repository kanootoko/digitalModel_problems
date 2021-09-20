package org.kanootoko.problemapi.services.defaults;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.repositories.ProblemRepository;
import org.kanootoko.problemapi.services.ProblemService;
import org.kanootoko.problemapi.utils.ProblemFilter;
import org.kanootoko.problemapi.utils.RepositoryFactory;
import org.kanootoko.problemapi.utils.Utils;

public class ProblemServiceDefault implements ProblemService {

    RepositoryFactory rf;

    List<Entry<String, Integer>> problemsByMonths;
    Map<String, Map<String, Map<Double, List<String>>>> problemsClassification;
    Map<String, List<Double>> problemsEvaluationCoeffs;

    @SuppressWarnings("unchecked")
    public ProblemServiceDefault(RepositoryFactory rf, String classificationFilename, String evaluationCoeffsFilename) {
        this.rf = rf;
        problemsByMonths = null;
        problemsClassification = new HashMap<>();
        problemsEvaluationCoeffs = new HashMap<>();
        JSONParser parser = new JSONParser();
        try (FileReader json = new FileReader(classificationFilename)) {
            JSONObject problems = (JSONObject) parser.parse(json);
            for (Object service : problems.keySet()) {
                Map<String, Map<Double, List<String>>> serv = new HashMap<>();
                JSONObject serv_value = (JSONObject) problems.get(service);
                for (Object subcategory : serv_value.keySet()) {
                    Map<Double, List<String>> sub = new HashMap<>();
                    JSONObject sub_value = (JSONObject) serv_value.get(subcategory);
                    for (Object criticality : sub_value.keySet()) {
                        List<String> names = new ArrayList<>();
                        for (Object name : (JSONArray) sub_value.get(criticality)) {
                            names.add((String) name);
                        }
                        sub.put(Double.valueOf(criticality.toString()), names);
                    }
                    serv.put((String) subcategory, sub);
                }
                problemsClassification.put((String) service, serv);
            }
        } catch (NullPointerException | ParseException e) {
            System.err.println("Error while parsing file of problems classification \"" + classificationFilename + "\", it will be empty");
            e.printStackTrace();
            return;
        } catch (IOException ex) {
            System.err.println("Error while parsing file of problems classification \"" + classificationFilename + "\"(IO exception), it will be empty");
            return;
        }
        try (FileReader json = new FileReader(evaluationCoeffsFilename)) {
            JSONObject coeffs = (JSONObject) parser.parse(json);
            for (Object service : coeffs.keySet()) {
                List<Double> coefficients = ((JSONArray) coeffs.get(service)).stream().mapToDouble(s -> Double.parseDouble(s.toString())).boxed().collect(Collectors.toList());
                problemsEvaluationCoeffs.put((String) service, coefficients);
            }

        } catch (NullPointerException | ParseException e) {
            System.err.println("Error while parsing file of problems evaluation coefficients \"" + classificationFilename + "\", it will be empty");
            e.printStackTrace();
            return;
        } catch (IOException ex) {
            System.err.println("Error while parsing file of problems evaluation coefficients \"" + classificationFilename + "\" (IO exception), it will be empty");
            return;
        }
    }

    public ProblemServiceDefault(RepositoryFactory rf) {
        this(rf, "services_coeffs.json", "services_evaluation_coeffs.json");
    }

    @Override
    public List<Problem> getProblemsByFilter(ProblemFilter pf) {
        return rf.getPorblemRepository().findProblemsByFilter(pf);
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName) {
        return rf.getPorblemRepository().getGroupsSize(labelName);
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName, String date) {
        return rf.getPorblemRepository().getGroupsSize(labelName, date);
    }

    @Override
    public Problem getProblemByID(int problemID) {
        return rf.getPorblemRepository().findProblemByID(problemID);
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName) {
        return rf.getPorblemRepository().getEvaluationByMunicipality(municipalityName, null);
    }

    @Override
    public Double[] getEvaluationByDistrict(String districtName) {
        return rf.getPorblemRepository().getEvaluationByDistrict(districtName, null);
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName, String date, boolean update) {
        ProblemRepository problemRepository = rf.getPorblemRepository();
        Double[] sictn = null;
        if (!update) {
            sictn = problemRepository.getEvaluationByMunicipality(municipalityName, date);
        }
        if (sictn == null || sictn[4] == null) {
            sictn =  Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                    new ProblemFilter()
                            .setDistrict(municipalityName)
                            .setMinCreationDate(LocalDate.parse(date + "-01"))
                            .setMaxCreationDate(LocalDate.parse(date + "-01").plusMonths(1))
                            .setLimit(0)));
            problemRepository.setEvaluationToMunicipaity(municipalityName, date, sictn[0], sictn[1], sictn[2], sictn[3],
                    (sictn[4] == null ? null : sictn[4].intValue()));
        }
        return sictn;
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName, String date) {
        return getEvaluationByMunicipality(municipalityName, date, false);
    }

    @Override
    public Double[] getEvaluationByDistrict(String districtName, String date, boolean update) {
        ProblemRepository problemRepository = rf.getPorblemRepository();
        Double[] sictn = null;
        if (!update) {
            sictn = problemRepository.getEvaluationByDistrict(districtName, date);
        }
        if (sictn == null || sictn[4] == null) {
            sictn =  Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                    new ProblemFilter()
                            .setDistrict(districtName)
                            .setMinCreationDate(LocalDate.parse(date + "-01"))
                            .setMaxCreationDate(LocalDate.parse(date + "-01").plusMonths(1))
                            .setLimit(0)));
            problemRepository.setEvaluationToDistrict(districtName, date, sictn[0], sictn[1], sictn[2], sictn[3],
                    (sictn[4] == null ? null : sictn[4].intValue()));
        }
        return sictn;
    }

    @Override
    public Double[] getEvaluationByDistrict(String districtName, String date) {
        return getEvaluationByDistrict(districtName, date, false);
    }

    @Override
    public void evaluateMunicipalities() {
        boolean update = false;
        ProblemRepository problemRepository = rf.getPorblemRepository();
        for (Entry<String, Integer> dateAndCount: getProblemsMonthsCount(false)) {
            for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality", dateAndCount.getKey()).entrySet()) {
                boolean needToEvaluate = true;
                if (!update) {
                    Double[] sictn = problemRepository.getEvaluationByMunicipality(municipalityAndSize.getKey(), dateAndCount.getKey());
                    if (sictn[4] != null) {
                        needToEvaluate = false;
                        System.out.printf("\tSkipping municipality '%s' at date %s as it is already evaluated (%.3f, %.3f, %.3f, %.3f, %d)\n",
                                municipalityAndSize.getKey(), dateAndCount.getKey(), sictn[0], sictn[1], sictn[2], sictn[3], (int) (double) sictn[4]);
                    }
                }
                if (needToEvaluate) {
                    System.out.printf("\tEvaluating municipality '%s' at date %s\n", municipalityAndSize.getKey(), dateAndCount.getKey());
                    Double[] sictn = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                            new ProblemFilter()
                                    .setMunicipality(municipalityAndSize.getKey())
                                    .setMinCreationDate(LocalDate.parse(dateAndCount.getKey() + "-01"))
                                    .setMaxCreationDate(LocalDate.parse(dateAndCount.getKey() + "-01").plusMonths(1))
                                    .setLimit(0)));
                    problemRepository.setEvaluationToMunicipaity(municipalityAndSize.getKey(), dateAndCount.getKey(), sictn[0], sictn[1], sictn[2], sictn[3],
                            (sictn[4] == null ? null : sictn[4].intValue()));
                }
            }
        }
        for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality").entrySet()) {
            boolean needToEvaluate = true;
            if (!update) {
                Double[] sictn = problemRepository.getEvaluationByMunicipality(municipalityAndSize.getKey(), null);
                if (sictn[4] != null) {
                    needToEvaluate = false;
                    System.out.printf("\tSkipping municipality '%s' as it is already evaluated (%.3f, %.3f, %.3f, %.3f, %d)\n",
                            municipalityAndSize.getKey(), sictn[0], sictn[1], sictn[2], sictn[3], (int) (double) sictn[4]);
                }
            }
            if (needToEvaluate) {
                System.out.printf("\tEvaluating municipality '%s'\n", municipalityAndSize.getKey());
                Double[] sictn = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                        new ProblemFilter().setMunicipality(municipalityAndSize.getKey()).setLimit(0)));
                problemRepository.setEvaluationToMunicipaity(municipalityAndSize.getKey(), null, sictn[0], sictn[1], sictn[2], sictn[3],
                        (sictn[4] == null ? null : sictn[4].intValue()));
            }
        }
    }

    @Override
    public void evaluateDistricts() {
        boolean update = false;
        ProblemRepository problemRepository = rf.getPorblemRepository();
        for (Entry<String, Integer> dateAndCount: getProblemsMonthsCount(false)) {
            for (Entry<String, Integer> districtAndSize: getGroupsSize("district", dateAndCount.getKey()).entrySet()) {
                boolean needToEvaluate = true;
                if (!update) {
                    Double[] sictn = problemRepository.getEvaluationByDistrict(districtAndSize.getKey(), dateAndCount.getKey());
                    if (sictn[4] != null) {
                        needToEvaluate = false;
                        System.out.printf("\tSkipping district '%s' at date %s as it is already evaluated (%.3f, %.3f, %.3f, %.3f, %d)\n",
                                districtAndSize.getKey(), dateAndCount.getKey(), sictn[0], sictn[1], sictn[2], sictn[3], (int) (double) sictn[4]);
                    }
                }
                if (needToEvaluate) {
                    System.out.printf("\tEvaluating district '%s' at date %s\n", districtAndSize.getKey(), dateAndCount.getKey());
                    Double[] sictn = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                            new ProblemFilter()
                                    .setDistrict(districtAndSize.getKey())
                                    .setMinCreationDate(LocalDate.parse(dateAndCount.getKey() + "-01"))
                                    .setMaxCreationDate(LocalDate.parse(dateAndCount.getKey() + "-01").plusMonths(1))
                                    .setLimit(0)));
                    problemRepository.setEvaluationToDistrict(districtAndSize.getKey(), dateAndCount.getKey(), sictn[0], sictn[1], sictn[2], sictn[3],
                            (sictn[4] == null ? null : sictn[4].intValue()));
                }
            }
        }
        for (Entry<String, Integer> districtAndSize: getGroupsSize("district").entrySet()) {
            boolean needToEvaluate = true;
            if (!update) {
                Double[] sictn = problemRepository.getEvaluationByDistrict(districtAndSize.getKey(), null);
                if (sictn[4] != null) {
                    needToEvaluate = false;
                    System.out.printf("\tSkipping district '%s' as it is already evaluated (%.3f, %.3f, %.3f, %.3f, %d)\n",
                            districtAndSize.getKey(), sictn[0], sictn[1], sictn[2], sictn[3], (int) (double) sictn[4]);
                }
            }
            if (needToEvaluate) {
                System.out.printf("\tEvaluating district '%s'\n", districtAndSize.getKey());
                Double[] sictn = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                        new ProblemFilter().setDistrict(districtAndSize.getKey()).setLimit(0)));
                problemRepository.setEvaluationToDistrict(districtAndSize.getKey(), null, sictn[0], sictn[1], sictn[2], sictn[3],
                        (sictn[4] == null ? null : sictn[4].intValue()));
            }
        }
    }

    @Override
    public Map<String, Double[]> getEvaluationOfMunicipalities(String date, boolean update) {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality", date).entrySet()) {
            res.put(municipalityAndSize.getKey(), getEvaluationByMunicipality(municipalityAndSize.getKey(), date, update));
        }
        return res;
    }

    @Override
    public Map<String, Double[]> getEvaluationOfMunicipalities() {
        return getEvaluationOfMunicipalities(null, false);
    }

    @Override
    public Map<String, Double[]> getEvaluationOfMunicipalities(String date) {
        return getEvaluationOfMunicipalities(date, false);
    }

    @Override
    public Map<String, Double[]> getEvaluationOfMunicipalities(boolean update) {
        return getEvaluationOfMunicipalities(null, update);
    }

    @Override
    public Map<String, Double[]> getEvaluationOfDistricts(String date, boolean update) {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> districtAndSize: getGroupsSize("district", date).entrySet()) {
            res.put(districtAndSize.getKey(), getEvaluationByDistrict(districtAndSize.getKey(), date, update));
        }
        return res;
    }

    @Override
    public Map<String, Double[]> getEvaluationOfDistricts() {
        return getEvaluationOfDistricts(null, false);
    }

    @Override
    public Map<String, Double[]> getEvaluationOfDistricts(String date) {
        return getEvaluationOfDistricts(date, false);
    }

    @Override
    public Map<String, Double[]> getEvaluationOfDistricts(boolean update) {
        return getEvaluationOfDistricts(null, update);
    }

    @Override
    public List<Entry<String, Integer>> getProblemsMonthsCount(boolean update) {
        if (problemsByMonths == null || update) {
            problemsByMonths = rf.getPorblemRepository().getProblemsMonthsCount();
        }
        return Collections.unmodifiableList(problemsByMonths);
    }

    @Override
    public Set<String> getServicesClassificated() {
        return problemsClassification.keySet();
    }

    @Override
    public Map<String, Map<String, Map<Double, Integer>>> getProblemsClassifiedCount(String service, String date, String location, String locationType) {
        return rf.getPorblemRepository().getProblemsClassifiedCount(problemsClassification, service, date, location, locationType);
    }

    @Override
    public Map<String, Map<String, Object>> getProblemsClassifiedEvaluation(String service, String date, String location, String locationType) {
        Map<String, Map<String, Map<Double, Integer>>> counts =
                rf.getPorblemRepository().getProblemsClassifiedCount(problemsClassification, service, date, location, locationType);
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (String serviceName: counts.keySet()) {
            Integer numberOfServices = rf.getHousesRepository().countServicesInside(serviceName, location, locationType);
            Integer countRaw = 0;
            Double rating = 0.0;
            for (String subcategoryName: counts.get(serviceName).keySet()) {
                for (Double criticality: counts.get(serviceName).get(subcategoryName).keySet()) {
                    Integer numberOfProblems = counts.get(serviceName).get(subcategoryName).get(criticality);
                    countRaw += numberOfProblems;
                    rating += numberOfProblems * criticality;
                }
            }
            if (!numberOfServices.equals(0)) {
                rating /= numberOfServices;
            }
            rating = Math.round(rating * 100) / 100.0;
            Integer evaluation;
            if (rating.equals(0.0)) {
                evaluation = 5;
            } else {
                evaluation = 10;
                for (Double value : problemsEvaluationCoeffs.getOrDefault(serviceName, List.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0))) {
                    if (rating <= value || evaluation == 0) {
                        break;
                    }
                    evaluation -= 1;
                }
            }
            result.put(serviceName, Map.of(
                    "evaluation", evaluation, "problems_number_raw", countRaw, "problems_number", rating, "number_of_services", numberOfServices));
        }
        return result;
    }
}
