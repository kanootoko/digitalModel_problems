package org.kanootoko.problemapi.repositories;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.utils.ProblemFilter;

public interface ProblemRepository {
    public List<Problem> findProblemsByFilter(ProblemFilter pf);
    public Map<String, Integer> getGroupsSize(String labelName);
    public Map<String, Integer> getGroupsSize(String labelName, String date);
    public Problem findProblemByID(int problemID);
    public Double[] getEvaluationByMunicipality(String municipalityName, String date);
    public Double[] getEvaluationByDistrict(String districtName, String date);
    public void setEvaluationToMunicipaity(String municipalityName, String date, Double s, Double i, Double c, Double total, int objects);
    public void setEvaluationToDistrict(String districtName, String date, Double s, Double i, Double c, Double total, int objects);
    public List<Entry<String, Integer>> getProblemsMonthsCount();
    public Map<String, Map<String, Map<Double, Integer>>> getProblemsClassifiedCount(
            Map<String, Map<String, Map<Double, List<String>>>> problemsClassification, String service, String date, String location, String locationType);
}