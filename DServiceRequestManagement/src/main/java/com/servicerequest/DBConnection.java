package com.servicerequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/service_request_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = getSetting("SERVICE_REQUEST_DB_URL", "service.request.db.url", DEFAULT_URL);
        String username = getSetting("SERVICE_REQUEST_DB_USER", "service.request.db.user", null);
        String password = getSetting("SERVICE_REQUEST_DB_PASSWORD", "service.request.db.password", null);

        if (username == null || username.isBlank()) {
            throw new SQLException("Set SERVICE_REQUEST_DB_USER to your MySQL username.");
        }
        if (password == null) {
            throw new SQLException("Set SERVICE_REQUEST_DB_PASSWORD to your MySQL password.");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException(
                    "MySQL Connector/J is missing from the application classpath.", exception);
        }

        return DriverManager.getConnection(url, username, password);
    }

    private static String getSetting(String environmentName, String propertyName, String defaultValue) {
        String value = System.getenv(environmentName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyName);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}