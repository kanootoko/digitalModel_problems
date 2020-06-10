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
}