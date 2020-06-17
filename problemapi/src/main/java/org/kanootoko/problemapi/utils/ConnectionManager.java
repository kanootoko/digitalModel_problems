package org.kanootoko.problemapi.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {
    private static Connection conn = null;
    // private static String db_string = "jdbc:postgresql://127.0.0.1:5432/problems", db_user = "postgres", db_pass = "postgres";
    private static String db_addr = "localhost", db_name = "problems", db_user = "postgres", db_pass = "postgres";
    private static int db_port = 5432;

    public static Connection getConnection() {
        boolean newConnectionNeeded = false;
        if (conn == null) {
            newConnectionNeeded = true;
        } else {
            try {
                newConnectionNeeded = conn.isClosed();
            } catch (SQLException e) {
            }
        }
        if (newConnectionNeeded) {
            try {
                conn = DriverManager.getConnection("jdbc:postgresql://" + db_addr + ":" + db_port + "/" + db_name, db_user, db_pass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return conn;
    }

    public static void setDB_addr(String db_addr) {
        ConnectionManager.db_addr = db_addr;
    }

    public static void setDB_name(String db_name) {
        ConnectionManager.db_name = db_name;
    }

    public static void setDB_port(int db_port) {
        ConnectionManager.db_port = db_port;
    }

    public static void setDB_user(String db_user) {
        ConnectionManager.db_user = db_user;
    }

    public static void setDB_pass(String db_pass) {
        ConnectionManager.db_pass = db_pass;
    }
}