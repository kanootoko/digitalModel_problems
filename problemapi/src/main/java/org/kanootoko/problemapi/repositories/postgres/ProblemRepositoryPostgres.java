package org.kanootoko.problemapi.repositories.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kanootoko.problemapi.models.Coordinates;
import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.repositories.ProblemRepository;
import org.kanootoko.problemapi.utils.ConnectionManager;
import org.kanootoko.problemapi.utils.ProblemFilter;

public class ProblemRepositoryPostgres implements ProblemRepository {

    @Override
    public List<Problem> findProblemsByCreationDate(LocalDate minDate, LocalDate maxDate) {
        List<Problem> problems = new ArrayList<>();
        Connection conn = ConnectionManager.getConnection();
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(
                    "select ID, OuterID, Name, District, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_X(Coordinates),"
                            + " ST_Y(Coordinates), Address, Municipality, Reason, Category, Subcategory from problems as problem"
                            + " where problem.CreationDate >= (?) and problem.CreationDate <= (?) limit 100000");
            statement.setDate(1, java.sql.Date.valueOf(minDate));
            statement.setDate(2, java.sql.Date.valueOf(maxDate));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                problems.add(new Problem(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getString(8), rs.getString(9),
                        rs.getInt(10), new Coordinates(rs.getDouble(11), rs.getDouble(12)), rs.getString(13),
                        rs.getString(14), rs.getString(15), rs.getString(16), rs.getString(17)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return problems;
    }

    @Override
    public List<Problem> findProblemsByFilter(ProblemFilter pf) {
        List<Problem> problems = new ArrayList<>();
        Connection conn = ConnectionManager.getConnection();
        PreparedStatement statement;
        try {
            statement = conn.prepareStatement(
                    "select ID, OuterID, Name, District, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_Y(Coordinates),"
                            + " ST_X(Coordinates), Address, Municipality, Reason, Category, Subcategory from problems"
                            + pf.buildWhereString());
            // System.out.println(
            // "select ID, OuterID, Name, District, Status, CreationDate, UpdateDate,
            // Description, UserName, UserID, ST_X(Coordinates),"
            // + " ST_Y(Coordinates), Address, Municipality, Reason, Category, Subcategory
            // from problems"
            // + pf.buildWhereString());
            pf.addParameters(statement);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                problems.add(new Problem(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getString(8), rs.getString(9),
                        rs.getInt(10), new Coordinates(rs.getDouble(11), rs.getDouble(12)), rs.getString(13),
                        rs.getString(14), rs.getString(15), rs.getString(16), rs.getString(17)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return problems;
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName) {
        Map<String, Integer> res = new HashMap<>();
        Connection conn = ConnectionManager.getConnection();
        try (ResultSet rs = conn.createStatement()
                .executeQuery(String.format("select %s, count(*) from problems where municipality not like '%%(искл.)' group by %s", labelName, labelName))) {
            while (rs.next()) {
                res.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    @Override
    public Problem findProblemByID(int problemID) {
        Connection conn = ConnectionManager.getConnection();
        try (ResultSet rs = conn.createStatement().executeQuery(
                "select ID, OuterID, Name, District, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_Y(Coordinates),"
                        + " ST_X(Coordinates), Address, Municipality, Reason, Category, Subcategory from problems where id = "
                        + problemID)) {
            if (rs.next()) {
                return new Problem(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getString(8), rs.getString(9),
                        rs.getInt(10), new Coordinates(rs.getDouble(11), rs.getDouble(12)), rs.getString(13),
                        rs.getString(14), rs.getString(15), rs.getString(16), rs.getString(17));
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement("SELECT s, i, c, total_value, objects_number from evaluation_municipalities where municipality_name = ?");
            statement.setString(1, municipalityName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Double[] {rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), (double) (int) rs.getInt(5)};
                } else {
                    return new Double[] {null, null, null, null};
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Double[] getEvaluationByDistrict(String districtName) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement("SELECT s, i, c, total_value, objects_number from evaluation_districts where district_name = ?");
            statement.setString(1, districtName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Double[] {rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), (double) (int) rs.getInt(5)};
                } else {
                    return new Double[] {null, null, null, null};
                }
            } 
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setEvaluationToMunicipaity(String municipalityName, double s, double i, double c, double total, int objects) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO evaluation_municipalities (municipality_name, s, i, c, total_value, objects_number) values"
                            + " (?, ?, ?, ?, ?, ?) ON CONFLICT (municipality_name) DO UPDATE SET s = excluded.s,"
                            + " i = excluded.i, c = excluded.c, total_value = excluded.total_value, objects_number = excluded.objects_number");
            statement.setString(1, municipalityName);
            statement.setDouble(2, s);
            statement.setDouble(3, i);
            statement.setDouble(4, c);
            statement.setDouble(5, total);
            statement.setInt(6, objects);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setEvaluationToDistrict(String districtName, double s, double i, double c, double total, int objects) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO evaluation_districts (district_name, s, i, c, total_value, objects_number) values"
                            + " (?, ?, ?, ?, ?, ?) ON CONFLICT (district_name) DO UPDATE SET s = excluded.s,"
                            + " i = excluded.i, c = excluded.c, total_value = excluded.total_value, objects_number = excluded.objects_number");
            statement.setString(1, districtName);
            statement.setDouble(2, s);
            statement.setDouble(3, i);
            statement.setDouble(4, c);
            statement.setDouble(5, total);
            statement.setInt(6, objects);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}