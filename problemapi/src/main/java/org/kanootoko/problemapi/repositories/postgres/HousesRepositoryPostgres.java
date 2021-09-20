package org.kanootoko.problemapi.repositories.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.kanootoko.problemapi.repositories.HousesRepository;
import org.kanootoko.problemapi.utils.ConnectionManager;

public class HousesRepositoryPostgres implements HousesRepository {

    ConnectionManager connectionManager;

    public HousesRepositoryPostgres(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Integer countServicesInside(String serviceType, String location, String locationType) {
        Connection conn = connectionManager.getConnection();
        String statementText = "SELECT count(*) from all_services WHERE service_type = ?";
        if (locationType != null) {
            statementText += " AND ";
            if (locationType.toLowerCase().equals("geojson")) {
                statementText += "ST_Within(center, ST_SetSRID(ST_GeomFromGeoJSON(?), 4326))";
            } else if (locationType.toLowerCase().equals("district")) {
                statementText += "district_name = ?";
            } else if (locationType.toLowerCase().equals("municipality")) {
                statementText += "municipal_name = ?";
            } else {
                locationType = null;
            }
        }
        try {
            PreparedStatement ps = conn.prepareStatement(statementText);
            ps.setString(1, serviceType);
            if (locationType != null) {
                ps.setString(2, location);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
