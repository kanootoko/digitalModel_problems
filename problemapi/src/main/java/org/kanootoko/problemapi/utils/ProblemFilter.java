package org.kanootoko.problemapi.utils;

import java.sql.PreparedStatement;
import java.time.LocalDate;

import org.kanootoko.problemapi.models.Coordinates;

public class ProblemFilter {
    public static final int LIMIT_DEFAULT = 100000;

    private String status, category, subcategory, municipality, region;
    private Coordinates firstCoord, secondCoord;
    private LocalDate minCreationDate, maxCreationDate;
    int limit;

    public ProblemFilter() {
        status = null;
        category = null;
        subcategory = null;
        municipality = null;
        region = null;
        firstCoord = null;
        secondCoord = null;
        minCreationDate = null;
        maxCreationDate = null;
        limit = LIMIT_DEFAULT;
    }

    public ProblemFilter setStatus(String status) {
        this.status = status;
        return this;
    }

    public ProblemFilter setCategory(String category) {
        this.category = category;
        return this;
    }

    public ProblemFilter setSubcategory(String subcategory) {
        this.subcategory = subcategory;
        return this;
    }

    public ProblemFilter setMunicipality(String municipality) {
        this.municipality = municipality;
        return this;
    }

    public ProblemFilter setRegion(String region) {
        this.region = region;
        return this;
    }

    public ProblemFilter setCoords(Coordinates firstCoord, Coordinates secondCoord) {
        this.firstCoord = firstCoord;
        this.secondCoord = secondCoord;
        return this;
    }

    public ProblemFilter setCoords(String firstCoord, String secondCoord) {
        this.firstCoord = new Coordinates(firstCoord);
        this.secondCoord = new Coordinates(secondCoord);
        return this;
    }

    public ProblemFilter setMinCreationDate(LocalDate minCreationDate) {
        this.minCreationDate = minCreationDate;
        return this;
    }

    public ProblemFilter setMaxCreationDate(LocalDate maxCreationDate) {
        this.maxCreationDate = maxCreationDate;
        return this;
    }

    public ProblemFilter setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String getMunicipality() {
        return municipality;
    }

    public String getRegion() {
        return region;
    }

    public Coordinates getFirstCoord() {
        return firstCoord;
    }

    public Coordinates getSecondCoord() {
        return secondCoord;
    }

    public LocalDate getMinCreationDate() {
        return minCreationDate;
    }

    public LocalDate getMaxCreationDate() {
        return maxCreationDate;
    }

    public int getLimit() {
        return limit;
    }

    private int addArgument(int count, StringBuilder sb, String paramName, String operator, Object paramValue) {
        if (paramValue == null) {
            return count;
        }
        if (count == 0) {
            sb.append(" ");
        } else {
            sb.append(" and ");
        }
        sb.append(paramName);
        sb.append(" ");
        sb.append(operator);
        sb.append(" ?");
        return count + 1;
    }

    public boolean isNulled() {
        return status == null && category == null && subcategory == null && firstCoord == null && secondCoord == null
                && minCreationDate == null && maxCreationDate == null && municipality == null && region == null;
    }

    public boolean isDefault() {
        return isNulled() && limit == LIMIT_DEFAULT;
    }

    public String buildWhereString() {
        if (isNulled()) {
            if (limit != 0)
                return " limit " + limit;
            else
                return "";
        }
        StringBuilder sb = new StringBuilder(" where");
        int count = 0;
        count = addArgument(count, sb, "status", "=", status);
        count = addArgument(count, sb, "category", "=", category);
        count = addArgument(count, sb, "subcategory", "=", subcategory);
        count = addArgument(count, sb, "municipality", "=", municipality);
        count = addArgument(count, sb, "region", "=", region);
        if (firstCoord != null && secondCoord != null) {
            if (count == 0) {
                sb.append(" ");
            } else {
                sb.append(" and ");
            }
            count++;
            sb.append(String.format(
                    "ST_WITHIN(coordinates, ST_POLYGON(text('LINESTRING(%s %s, %s %s, %s %s, %s %s, %s %s)'), 4326))",
                    firstCoord.getLongitude(), firstCoord.getLatitude(), firstCoord.getLongitude(),
                    secondCoord.getLatitude(), secondCoord.getLongitude(), secondCoord.getLatitude(),
                    secondCoord.getLongitude(), firstCoord.getLatitude(), firstCoord.getLongitude(),
                    firstCoord.getLatitude()));
        }
        count = addArgument(count, sb, "CreationDate", ">=", minCreationDate);
        count = addArgument(count, sb, "CreationDate", "<=", maxCreationDate);
        if (count == 0) {
            sb.append(" ");
        } else {
            sb.append(" and ");
        }
        sb.append("municipality not like '%%(искл.)'");
        if (limit != 0) {
            sb.append(" limit ");
            sb.append(limit);
        }
        return sb.toString();
    }

    public void addParameters(PreparedStatement statement) {
        int k = 1;
        try {
            if (status != null) {
                statement.setString(k, status);
                k++;
            }
            if (category != null) {
                statement.setString(k, category);
                k++;
            }
            if (subcategory != null) {
                statement.setString(k, subcategory);
                k++;
            }
            if (municipality != null) {
                statement.setString(k, municipality);
                k++;
            }
            if (region != null) {
                statement.setString(k, region);
                k++;
            }
            if (minCreationDate != null) {
                statement.setDate(k, java.sql.Date.valueOf(minCreationDate));
                k++;
            }
            if (maxCreationDate != null) {
                statement.setDate(k, java.sql.Date.valueOf(maxCreationDate));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}