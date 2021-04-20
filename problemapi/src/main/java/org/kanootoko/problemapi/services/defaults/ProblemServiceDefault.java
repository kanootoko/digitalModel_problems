package org.kanootoko.problemapi.services.defaults;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.repositories.ProblemRepository;
import org.kanootoko.problemapi.services.ProblemService;
import org.kanootoko.problemapi.utils.ProblemFilter;
import org.kanootoko.problemapi.utils.RepositoryFactory;
import org.kanootoko.problemapi.utils.Utils;

public class ProblemServiceDefault implements ProblemService {

    List<Entry<String, Integer>> problemsByMonths = null;

    @Override
    public List<Problem> getProblemsByFilter(ProblemFilter pf) {
        return RepositoryFactory.getPorblemRepository().findProblemsByFilter(pf);
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName) {
        return RepositoryFactory.getPorblemRepository().getGroupsSize(labelName);
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName, String date) {
        return RepositoryFactory.getPorblemRepository().getGroupsSize(labelName, date);
    }

    @Override
    public Problem getProblemByID(int problemID) {
        return RepositoryFactory.getPorblemRepository().findProblemByID(problemID);
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName) {
        return RepositoryFactory.getPorblemRepository().getEvaluationByMunicipality(municipalityName, null);
    }

    @Override
    public Double[] getEvaluationByDistrict(String districtName) {
        return RepositoryFactory.getPorblemRepository().getEvaluationByDistrict(districtName, null);
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName, String date, boolean update) {
        ProblemRepository problemRepository = RepositoryFactory.getPorblemRepository();
        Double[] sictn = problemRepository.getEvaluationByMunicipality(municipalityName, date);
        if (sictn != null && sictn[4] == null) {
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
        ProblemRepository problemRepository = RepositoryFactory.getPorblemRepository();
        Double[] sictn = problemRepository.getEvaluationByDistrict(districtName, date);
        if (sictn != null && sictn[4] == null) {
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
        ProblemRepository problemRepository = RepositoryFactory.getPorblemRepository();
        for (Entry<String, Integer> dateAndCount: getProblemsMonthsCount(false)) {
            for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality", dateAndCount.getKey()).entrySet()) {
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
        for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality").entrySet()) {
            System.out.printf("\tEvaluating municipality '%s'\n", municipalityAndSize.getKey());
            Double[] sictn = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                    new ProblemFilter().setMunicipality(municipalityAndSize.getKey()).setLimit(0)));
            problemRepository.setEvaluationToMunicipaity(municipalityAndSize.getKey(), null, sictn[0], sictn[1], sictn[2], sictn[3],
                    (sictn[4] == null ? null : sictn[4].intValue()));
        }
    }

    @Override
    public void evaluateDistricts() {
        ProblemRepository problemRepository = RepositoryFactory.getPorblemRepository();
        for (Entry<String, Integer> dateAndCount: getProblemsMonthsCount(false)) {
            for (Entry<String, Integer> districtAndSize: getGroupsSize("district", dateAndCount.getKey()).entrySet()) {
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
        for (Entry<String, Integer> districtAndSize: getGroupsSize("district").entrySet()) {
            System.out.printf("\tEvaluating district '%s'\n", districtAndSize.getKey());
            Double[] sictn = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(
                    new ProblemFilter().setDistrict(districtAndSize.getKey()).setLimit(0)));
            problemRepository.setEvaluationToDistrict(districtAndSize.getKey(), null, sictn[0], sictn[1], sictn[2], sictn[3],
                    (sictn[4] == null ? null : sictn[4].intValue()));
        }
    }

    @Override
    public Map<String, Double[]> getEvaluationOfMunicipalities() {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality").entrySet()) {
            res.put(municipalityAndSize.getKey(), getEvaluationByMunicipality(municipalityAndSize.getKey()));
        }
        return res;
    }

    @Override
    public Map<String, Double[]> getEvaluationOfDistricts() {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> districtAndSize: getGroupsSize("district").entrySet()) {
            res.put(districtAndSize.getKey(), getEvaluationByDistrict(districtAndSize.getKey()));
        }
        return res;
    }

    @Override
    public Map<String, Double[]> getEvaluationOfMunicipalities(String date) {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality", date).entrySet()) {
            res.put(municipalityAndSize.getKey(), getEvaluationByMunicipality(municipalityAndSize.getKey(), date));
        }
        return res;
    }

    @Override
    public Map<String, Double[]> getEvaluationOfDistricts(String date) {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> districtAndSize: getGroupsSize("district", date).entrySet()) {
            res.put(districtAndSize.getKey(), getEvaluationByDistrict(districtAndSize.getKey(), date));
        }
        return res;
    }

    @Override
    public List<Entry<String, Integer>> getProblemsMonthsCount(boolean update) {
        if (problemsByMonths == null || update) {
            problemsByMonths = RepositoryFactory.getPorblemRepository().getProblemsMonthsCount();
        }
        return Collections.unmodifiableList(problemsByMonths);
    }
}
