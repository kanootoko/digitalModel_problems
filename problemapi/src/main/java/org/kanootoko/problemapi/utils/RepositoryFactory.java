package org.kanootoko.problemapi.utils;

import org.kanootoko.problemapi.repositories.HousesRepository;
import org.kanootoko.problemapi.repositories.ProblemRepository;
import org.kanootoko.problemapi.repositories.postgres.HousesRepositoryPostgres;
import org.kanootoko.problemapi.repositories.postgres.ProblemRepositoryPostgres;

public class RepositoryFactory {

    ConnectionManager housesDB, problemsDB;
    private ProblemRepository problemRepository;
    private HousesRepository housesRepository;

    public RepositoryFactory(ConnectionManager housesDB, ConnectionManager problemsDB) {
        this.housesDB = housesDB;
        this.problemsDB = problemsDB;
        problemRepository = new ProblemRepositoryPostgres(problemsDB);
        housesRepository = new HousesRepositoryPostgres(housesDB);
    }

    public ProblemRepository getPorblemRepository() {
        return problemRepository;
    }

    public HousesRepository getHousesRepository() {
        return housesRepository;
    }
}