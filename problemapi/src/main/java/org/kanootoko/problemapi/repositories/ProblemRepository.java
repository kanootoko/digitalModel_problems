package org.kanootoko.problemapi.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.utils.ProblemFilter;

public interface ProblemRepository {
    public List<Problem> findProblemsByCreationDate(LocalDate minDate, LocalDate maxDate);
    public List<Problem> findProblemsByFilter(ProblemFilter pf);
    public Map<String, Integer> getGroupsSize(String labelName);
    public Problem findProblemByID(int problemID);
    public Double[] getEvaluationByMunicipality(String municipalityName);
    public Double[] getEvaluationByRegion(String regionName);
    public void setEvaluationToMunicipaity(String municipalityName, double s, double i, double c, double total, int objects);
    public void setEvaluationToRegion(String regionName, double s, double i, double c, double total, int objects);
}