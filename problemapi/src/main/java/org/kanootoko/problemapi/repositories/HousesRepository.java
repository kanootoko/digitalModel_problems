package org.kanootoko.problemapi.repositories;

public interface HousesRepository {
    Integer countServicesInside(String serviceType, String location, String locationType);
}