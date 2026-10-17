package edu.coet.campusnest;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebListener
public class CampusNestListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) {
        String email = System.getenv("CAMPUSNEST_ADMIN_EMAIL");
        String password = System.getenv("CAMPUSNEST_ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.length() < 12) {
            event.getServletContext().log("Admin bootstrap skipped: configure CAMPUSNEST_ADMIN_EMAIL and a 12+ character CAMPUSNEST_ADMIN_PASSWORD.");
            return;
        }
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO users(full_name,email,password_hash,role,status) VALUES(?,?,?,'ADMIN','ACTIVE')")) {
            ps.setString(1, "CampusNest Admin"); ps.setString(2, email.trim().toLowerCase()); ps.setString(3, Passwords.hash(password));
            ps.executeUpdate();
        } catch (Exception e) { event.getServletContext().log("Admin bootstrap could not connect to the database.", e); }
    }
}
