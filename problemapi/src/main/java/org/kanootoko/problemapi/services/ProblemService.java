package org.kanootoko.problemapi.services;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.utils.ProblemFilter;

public interface ProblemService {
    public List<Problem> getProblemsByFilter(ProblemFilter pf);
    public Map<String, Integer> getGroupsSize(String labelName);
    public Map<String, Integer> getGroupsSize(String labelName, String date);
    public Problem getProblemByID(int problemID);
    public Double[] getEvaluationByMunicipality(String municipalityName);
    public Double[] getEvaluationByDistrict(String districtName);
    public Double[] getEvaluationByMunicipality(String municipalityName, String date);
    public Double[] getEvaluationByDistrict(String districtName, String date);
    public Double[] getEvaluationByMunicipality(String municipalityName, String date, boolean update);
    public Double[] getEvaluationByDistrict(String districtName, String date, boolean update);
    public Map<String, Double[]> getEvaluationOfMunicipalities();
    public Map<String, Double[]> getEvaluationOfDistricts();
    public Map<String, Double[]> getEvaluationOfMunicipalities(String date);
    public Map<String, Double[]> getEvaluationOfDistricts(String date);
    public void evaluateMunicipalities();
    public void evaluateDistricts();
    public List<Entry<String, Integer>> getProblemsMonthsCount(boolean update);
}