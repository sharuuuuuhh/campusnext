package edu.coet.campusnest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {}

    public static Connection connect() throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("MySQL Connector/J is missing from the web application.", e); }
        String url = System.getenv().getOrDefault("CAMPUSNEST_DB_URL",
                "jdbc:mysql://localhost:3306/campusnest?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
        String user = System.getenv().getOrDefault("CAMPUSNEST_DB_USER", "root");
        String password = System.getenv().getOrDefault("CAMPUSNEST_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
