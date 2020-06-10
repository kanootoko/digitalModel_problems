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
                    "select ID, OuterID, Name, Region, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_X(Coordinates),"
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
                    "select ID, OuterID, Name, Region, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_X(Coordinates),"
                            + " ST_Y(Coordinates), Address, Municipality, Reason, Category, Subcategory from problems"
                            + pf.buildWhereString());
            // System.out.println(
            // "select ID, OuterID, Name, Region, Status, CreationDate, UpdateDate,
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
                .executeQuery(String.format("select %s, count(*) from problems group by %s", labelName, labelName))) {
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
                "select ID, OuterID, Name, Region, Status, CreationDate, UpdateDate, Description, UserName, UserID, ST_X(Coordinates),"
                        + " ST_Y(Coordinates), Address, Municipality, Reason, Category, Subcategory from problems where id = "
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

}