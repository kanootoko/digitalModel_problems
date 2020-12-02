package org.kanootoko.problemapi.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.utils.ProblemFilter;

public interface ProblemService {
    public List<Problem> getProblemsByCreationDate(LocalDate minDate, LocalDate maxDate);
    public List<Problem> getProblemsByFilter(ProblemFilter pf);
    public Map<String, Integer> getGroupsSize(String labelName);
    public Problem getProblemByID(int problemID);
    public Double[] getEvaluationByMunicipality(String municipalityName);
    public Double[] getEvaluationByDistrict(String districtName);
    public Map<String, Double[]> getEvaluationOfMunicipalities();
    public Map<String, Double[]> getEvaluationOfDistricts();
    public void evaluateMunicipalities();
    public void evaluateDistricts();
}