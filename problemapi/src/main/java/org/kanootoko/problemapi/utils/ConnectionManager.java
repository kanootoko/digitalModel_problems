package org.kanootoko.problemapi.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {
    private static Connection conn = null;
    private static String db_string = "jdbc:postgresql://127.0.0.1:5432/problems", db_user = "postgres", db_pass = "postgres";

    public static Connection getConnection() {
        if (conn == null) {
            try {
                conn = DriverManager.getConnection(db_string, db_user, db_pass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return conn;
    }

    public static void setDB_string(String db_string) {
        ConnectionManager.db_string = db_string;
    }

    public static void setDB_user(String db_user) {
        ConnectionManager.db_user = db_user;
    }

    public static void setDB_pass(String db_pass) {
        ConnectionManager.db_pass = db_pass;
    }
}