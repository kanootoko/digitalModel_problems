package org.kanootoko.problemapi.repositories.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kanootoko.problemapi.models.Coordinates;
import org.kanootoko.problemapi.models.entities.Problem;
import org.kanootoko.problemapi.repositories.ProblemRepository;
import org.kanootoko.problemapi.utils.ConnectionManager;
import org.kanootoko.problemapi.utils.ProblemFilter;

public class ProblemRepositoryPostgres implements ProblemRepository {

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
            pf.addParameters(statement);
            try {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    problems.add(new Problem(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getString(8), rs.getString(9),
                            rs.getInt(10), new Coordinates(rs.getDouble(11), rs.getDouble(12)), rs.getString(13),
                            rs.getString(14), rs.getString(15), rs.getString(16), rs.getString(17)));
                }
            } catch (Exception e) {
                System.out.println(
                    "SELECT ID, OuterID, Name, District, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_X(Coordinates),"
                    + " ST_Y(Coordinates), Address, Municipality, Reason, Category, Subcategory FROM problems"
                    + pf.buildWhereString());
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return problems;
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName, String date) {
        Map<String, Integer> res = new HashMap<>();
        Connection conn = ConnectionManager.getConnection();
        ResultSet rs = null;
        try {
            if (date == null) {
                rs = conn.createStatement().executeQuery(String.format("SELECT %s, count(*) FROM problems GROUP BY %s", labelName, labelName));
            } else {
                rs = conn.createStatement().executeQuery(String.format(
                        "SELECT %s, count(*) FROM problems WHERE creationdate >= '%s-01' AND"
                        + " creationdate < '%s-01'::timestamp + '1 month'::interval GROUP BY %s", labelName, date, date, labelName));
            }
            while (rs.next()) {
                res.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            try {
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
            } catch (SQLException _e) {
            }
            throw new RuntimeException(e);
        }
        return res;
    }

    @Override
    public Map<String, Integer> getGroupsSize(String labelName) {
        return getGroupsSize(labelName, null);
    }

    @Override
    public Problem findProblemByID(int problemID) {
        Connection conn = ConnectionManager.getConnection();
        try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT ID, OuterID, Name, District, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_Y(Coordinates),"
                        + " ST_X(Coordinates), Address, Municipality, Reason, Category, Subcategory FROM problems WHERE id = "
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
            throw new RuntimeException(e);
        }
    }

    @Override
    public Double[] getEvaluationByMunicipality(String municipalityName, String date) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement(
                    "SELECT s, i, c, total_value, objects_number FROM evaluation_municipalities WHERE municipality_name = ? AND date "
                    + (date == null ? "is ?" : "= ?"));
            statement.setString(1, municipalityName);
            statement.setString(2, date);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Double[] sictn = new Double[5];
                    for (int i = 0; i < 4; i++) {
                        sictn[i] = rs.getDouble(i + 1);
                        if (rs.wasNull()) {
                            sictn[i] = null;
                        }
                    }
                    sictn[4] = (double) rs.getInt(5);
                    return sictn;
                } else {
                    return new Double[] {null, null, null, null};
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Double[] getEvaluationByDistrict(String districtName, String date) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement(
                    "SELECT s, i, c, total_value, objects_number FROM evaluation_districts WHERE district_name = ? AND date "
                    + (date == null ? "is ?" : "= ?"));
            statement.setString(1, districtName);
            statement.setString(2, date);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Double[] sictn = new Double[5];
                    for (int i = 0; i < 4; i++) {
                        sictn[i] = rs.getDouble(i + 1);
                        if (rs.wasNull()) {
                            sictn[i] = null;
                        }
                    }
                    sictn[4] = (double) rs.getInt(5);
                    return sictn;
                } else {
                    return new Double[] {null, null, null, null, null};
                }
            } 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setEvaluationToMunicipaity(String municipalityName, String date, Double s, Double i, Double c, Double total, int objects) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO evaluation_municipalities (municipality_name, date, s, i, c, total_value, objects_number) values"
                            + " (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (municipality_name) DO UPDATE SET s = excluded.s,"
                            + " i = excluded.i, c = excluded.c, total_value = excluded.total_value, objects_number = excluded.objects_number");
            statement.setString(1, municipalityName);
            statement.setString(2, date);
            if (s != null) {
                statement.setDouble(3, s);
            } else {
                statement.setNull(3, Types.DOUBLE);
            }
            if (i != null) {
                statement.setDouble(4, i);
            } else {
                statement.setNull(4, Types.DOUBLE);
            }
            if (c != null) {
                statement.setDouble(5, c);
            } else {
                statement.setNull(5, Types.DOUBLE);
            }
            if (total != null) {
                statement.setDouble(6, total);
            } else {
                statement.setNull(6, Types.DOUBLE);
            }
            statement.setInt(7, objects);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setEvaluationToDistrict(String districtName, String date, Double s, Double i, Double c, Double total, int objects) {
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO evaluation_districts (district_name, date, s, i, c, total_value, objects_number) VALUES"
                            + " (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (district_name) DO UPDATE SET s = excluded.s,"
                            + " i = excluded.i, c = excluded.c, total_value = excluded.total_value, objects_number = excluded.objects_number");
            statement.setString(1, districtName);
            statement.setString(2, date);
            if (s != null) {
                statement.setDouble(3, s);
            } else {
                statement.setNull(3, Types.DOUBLE);
            }
            if (i != null) {
                statement.setDouble(4, i);
            } else {
                statement.setNull(4, Types.DOUBLE);
            }
            if (c != null) {
                statement.setDouble(5, c);
            } else {
                statement.setNull(5, Types.DOUBLE);
            }
            if (total != null) {
                statement.setDouble(6, total);
            } else {
                statement.setNull(6, Types.DOUBLE);
            }
            statement.setInt(7, objects);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Entry<String, Integer>> getProblemsMonthsCount() {
        Connection conn = ConnectionManager.getConnection();
        List<Entry<String, Integer>> result = new ArrayList<>();
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT to_char(creationdate, 'YYYY-MM'), count(*) FROM problems GROUP BY 1 ORDER BY 1")) {
            while (rs.next()) {
                result.add(new SimpleEntry<String, Integer>(rs.getString(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}