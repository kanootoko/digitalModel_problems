package org.kanootoko.problemapi.services.defaults;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.services.ProblemService;
import org.kanootoko.problemapi.utils.ProblemFilter;
import org.kanootoko.problemapi.utils.RepositoryFactory;

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
    
}