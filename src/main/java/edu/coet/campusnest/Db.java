package edu.coet.campusnest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {}

    public static Connection connect() throws SQLException {
        String url = System.getProperty("CAMPUSNEST_DB_URL",
                System.getenv().getOrDefault("CAMPUSNEST_DB_URL",
                        "jdbc:mysql://localhost:3306/campusnest?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"));
        String user = System.getProperty("CAMPUSNEST_DB_USER",
                System.getenv().getOrDefault("CAMPUSNEST_DB_USER", ""));
        String password = System.getProperty("CAMPUSNEST_DB_PASSWORD",
                System.getenv().getOrDefault("CAMPUSNEST_DB_PASSWORD", ""));

        try {
            if (url.startsWith("jdbc:h2:")) {
                Class.forName("org.h2.Driver");
            } else if (url.startsWith("jdbc:postgresql:")) {
                Class.forName("org.postgresql.Driver");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database JDBC driver is missing: " + e.getMessage(), e);
        }

        // If credentials are embedded directly inside the connection URL
        if (user.isBlank() && (url.contains("user=") || url.contains(":****@"))) {
            return DriverManager.getConnection(url);
        }

        if (user.isBlank() && url.startsWith("jdbc:mysql:")) {
            user = "root";
        }

        return DriverManager.getConnection(url, user, password);
    }
}
