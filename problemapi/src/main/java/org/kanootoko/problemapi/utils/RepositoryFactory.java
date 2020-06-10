package org.kanootoko.problemapi.utils;

import org.kanootoko.problemapi.repositories.ProblemRepository;
import org.kanootoko.problemapi.repositories.postgres.ProblemRepositoryPostgres;

public class RepositoryFactory {
    private final static ProblemRepository problemRepository = new ProblemRepositoryPostgres();

    public static ProblemRepository getPorblemRepository() {
        return problemRepository;
    }
}