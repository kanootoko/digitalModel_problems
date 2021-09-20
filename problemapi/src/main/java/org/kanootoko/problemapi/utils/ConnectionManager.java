package org.kanootoko.problemapi.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {
    private Connection conn = null;
    private String db_addr, db_name, db_user, db_pass;
    private int db_port = 5432;

    public ConnectionManager(String db_addr, Integer db_port, String db_name, String db_user, String db_pass) {
        this.db_addr = db_addr;
        this.db_port = db_port;
        this.db_name = db_name;
        this.db_user = db_user;
        this.db_pass = db_pass;
    }

    public Connection getConnection() {
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

    public void setDB_addr(String db_addr) {
        this.db_addr = db_addr;
    }

    public void setDB_name(String db_name) {
        this.db_name = db_name;
    }

    public void setDB_port(int db_port) {
        this.db_port = db_port;
    }

    public void setDB_user(String db_user) {
        this.db_user = db_user;
    }

    public void setDB_pass(String db_pass) {
        this.db_pass = db_pass;
    }
}