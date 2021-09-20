package org.kanootoko.problemapi.utils;

import org.kanootoko.problemapi.services.ProblemService;
import org.kanootoko.problemapi.services.defaults.ProblemServiceDefault;

public class ServiceFactory {
    private final ProblemService problemService;

    public ServiceFactory(RepositoryFactory rf) {
        problemService = new ProblemServiceDefault(rf);
    }

    public ProblemService getPorblemService() {
        return problemService;
    }
}