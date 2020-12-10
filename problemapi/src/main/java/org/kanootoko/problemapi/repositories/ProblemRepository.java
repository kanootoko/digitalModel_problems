package org.kanootoko.problemapi.repositories;

import java.util.List;
import java.util.Map;

import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.utils.ProblemFilter;

public interface ProblemRepository {
    public List<Problem> findProblemsByFilter(ProblemFilter pf);
    public Map<String, Integer> getGroupsSize(String labelName);
    public Problem findProblemByID(int problemID);
    public Double[] getEvaluationByMunicipality(String municipalityName);
    public Double[] getEvaluationByDistrict(String districtName);
    public void setEvaluationToMunicipaity(String municipalityName, double s, double i, double c, double total, int objects);
    public void setEvaluationToDistrict(String districtName, double s, double i, double c, double total, int objects);
}