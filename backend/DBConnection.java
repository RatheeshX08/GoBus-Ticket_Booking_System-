package com.busbooking.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates MySQL connections using environment-configurable settings.
 */
public class DBConnection {
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private static final String URL = setting(
            "DB_URL",
            "jdbc:mysql://localhost:3306/bus_booking_db?useSSL=false&allowPublicKeyRetrieval=true"
    );
    private static final String USER = setting("DB_USER", "root");
    private static final String PASSWORD = setting("DB_PASSWORD", "");

    private DBConnection() {}

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER_CLASS);
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC Error] MySQL Driver not found: " + e.getMessage());
            throw new SQLException("MySQL Driver class missing", e);
        } catch (SQLException e) {
            System.err.println("[JDBC Error] Connection failed: " + e.getMessage());
            throw e;
        }
    }

    public static void closeConnection() {
        // Connections are owned and closed by the caller that opened them.
    }
}
