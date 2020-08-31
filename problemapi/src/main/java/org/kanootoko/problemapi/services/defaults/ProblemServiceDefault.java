package org.kanootoko.problemapi.services.defaults;

import java.time.LocalDate;
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

    @Override
    public List<Problem> getProblemsByCreationDate(LocalDate minDate, LocalDate maxDate) {
        return RepositoryFactory.getPorblemRepository().findProblemsByCreationDate(minDate, maxDate);
    }

    @Override
    public List<Problem> getProblemsByFilter(ProblemFilter pf) {
        return RepositoryFactory.getPorblemRepository().findProblemsByFilter(pf);
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName) {
        return RepositoryFactory.getPorblemRepository().getGroupsSize(labelName);
    }

    @Override
    public Problem getProblemByID(int problemID) {
        return RepositoryFactory.getPorblemRepository().findProblemByID(problemID);
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName) {
        return RepositoryFactory.getPorblemRepository().getEvaluationByMunicipality(municipalityName);
    }

    @Override
    public Double[] getEvaluationByRegion(String regionName) {
        return RepositoryFactory.getPorblemRepository().getEvaluationByRegion(regionName);
    }

    @Override
    public void evaluateMunicipalities() {
        ProblemRepository problemRepository = RepositoryFactory.getPorblemRepository();
        for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality").entrySet()) {
            Double[] sict = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(new ProblemFilter().setMunicipality(municipalityAndSize.getKey()).setLimit(0)));
            problemRepository.setEvaluationToMunicipaity(municipalityAndSize.getKey(), sict[0], sict[1], sict[2], sict[3], municipalityAndSize.getValue());
        }
    }

    @Override
    public void evaluateRegions() {
        ProblemRepository problemRepository = RepositoryFactory.getPorblemRepository();
        for (Entry<String, Integer> regionAndSize: getGroupsSize("region").entrySet()) {
            Double[] sict = Utils.evaluatePolygon(problemRepository.findProblemsByFilter(new ProblemFilter().setRegion(regionAndSize.getKey()).setLimit(0)));
            problemRepository.setEvaluationToRegion(regionAndSize.getKey(), sict[0], sict[1], sict[2], sict[3], regionAndSize.getValue());
        }
    }

    @Override
    public Map<String, Double[]> getEvaluationOfMunicipalities() {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> municipalityAndSize: getGroupsSize("municipality").entrySet()) {
            Double[] sict = getEvaluationByMunicipality(municipalityAndSize.getKey());
            res.put(municipalityAndSize.getKey(), new Double[] {sict[0], sict[1], sict[2], sict[3], (double) (int) municipalityAndSize.getValue()});
        }
        return res;
    }

    @Override
    public Map<String, Double[]> getEvaluationOfRegions() {
        Map<String, Double[]> res = new HashMap<>();
        for (Entry<String, Integer> regionAndSize: getGroupsSize("region").entrySet()) {
            Double[] sict = getEvaluationByRegion(regionAndSize.getKey());
            res.put(regionAndSize.getKey(), new Double[] {sict[0], sict[1], sict[2], sict[3], (double) (int) regionAndSize.getValue()});
        }
        return res;
    }
}
