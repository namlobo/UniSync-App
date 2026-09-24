package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = cfg("unisync.db.url", "UNISYNC_DB_URL", "jdbc:mysql://localhost:3306/unisync");
        String user = cfg("unisync.db.user", "UNISYNC_DB_USER", "root");
        String pass = cfg("unisync.db.password", "UNISYNC_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, pass);
    }

    private static String cfg(String sysPropKey, String envKey, String defaultValue) {
        String v = System.getProperty(sysPropKey);
        if (v == null || v.isBlank()) {
            v = System.getenv(envKey);
        }
        if (v == null || v.isBlank()) {
            v = defaultValue;
        }
        return v;
    }
}
