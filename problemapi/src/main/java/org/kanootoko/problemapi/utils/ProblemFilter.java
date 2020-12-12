package org.kanootoko.problemapi.utils;

import java.sql.PreparedStatement;
import java.time.LocalDate;

import org.kanootoko.problemapi.models.Coordinates;

public class ProblemFilter {
    public static final int LIMIT_DEFAULT = 100000;

    private String status, category, subcategory, municipality, district;
    private Coordinates firstCoord, secondCoord;
    private LocalDate minCreationDate, maxCreationDate;
    private String geoJSON;
    int limit;

    public ProblemFilter() {
        status = null;
        category = null;
        subcategory = null;
        municipality = null;
        district = null;
        firstCoord = null;
        secondCoord = null;
        minCreationDate = null;
        maxCreationDate = null;
        geoJSON = null;
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

    public ProblemFilter setDistrict(String district) {
        this.district = district;
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

    public ProblemFilter setGeoJSON(String geoJSON) {
        this.geoJSON = geoJSON;
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

    public String getDistrict() {
        return district;
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

    public String getGeoJSON() {
        return geoJSON;
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
        return status == null && category == null && subcategory == null && geoJSON == null && (firstCoord == null || secondCoord == null)
                && minCreationDate == null && maxCreationDate == null && municipality == null && district == null;
    }

    public boolean isDefault() {
        return isNulled() && limit == LIMIT_DEFAULT;
    }

    public String buildWhereString() {
        if (isNulled()) {
            if (limit > 0)
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
        count = addArgument(count, sb, "district", "=", district);
        if (geoJSON != null || (firstCoord != null && secondCoord != null)) {
            if (count == 0) {
                sb.append(" ");
            } else {
                sb.append(" and ");
            }
            count++;
            if (geoJSON != null) {
                sb.append(String.format("ST_WITHIN(coordinates, ST_SetSRID(ST_GeomFromGeoJSON('%s'::text), 4326))", geoJSON));
            } else {
                sb.append(String.format(
                        "ST_WITHIN(coordinates, ST_POLYGON('LINESTRING(%s %s, %s %s, %s %s, %s %s, %s %s)'::text, 4326))",
                        firstCoord.getLongitude(), firstCoord.getLatitude(), firstCoord.getLongitude(),
                        secondCoord.getLatitude(), secondCoord.getLongitude(), secondCoord.getLatitude(),
                        secondCoord.getLongitude(), firstCoord.getLatitude(), firstCoord.getLongitude(),
                        firstCoord.getLatitude()));
            }
        }
        count = addArgument(count, sb, "CreationDate", ">=", minCreationDate);
        count = addArgument(count, sb, "CreationDate", "<=", maxCreationDate);
        sb.append(" ");
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
            if (district != null) {
                statement.setString(k, district);
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