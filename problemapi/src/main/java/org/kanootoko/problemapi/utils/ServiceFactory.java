package org.kanootoko.problemapi.utils;

import org.kanootoko.problemapi.services.ProblemService;
import org.kanootoko.problemapi.services.defaults.ProblemServiceDefault;

public class ServiceFactory {
    private final static ProblemService problemService = new ProblemServiceDefault();

    public static ProblemService getPorblemService() {
        return problemService;
    }
}