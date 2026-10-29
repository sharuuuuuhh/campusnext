package edu.coet.campusnest;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebListener
public class CampusNestListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent event) {
        bootstrapAdmin(event);
        bootstrapLocalOwner(event);
    }

    private void bootstrapAdmin(ServletContextEvent event) {
        String email = System.getenv("CAMPUSNEST_ADMIN_EMAIL");
        String password = System.getenv("CAMPUSNEST_ADMIN_PASSWORD");
        boolean localDemo = Boolean.parseBoolean(System.getenv("CAMPUSNEST_LOCAL_DEMO"));
        int minimumLength = localDemo ? 8 : 12;
        if (email == null || email.isBlank() || password == null || password.length() < minimumLength) {
            event.getServletContext().log("Admin bootstrap skipped: configure CAMPUSNEST_ADMIN_EMAIL and a password meeting the configured minimum length.");
            return;
        }
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO users(name,email,password_hash,role,account_status) VALUES(?,?,?,'ADMIN','ACTIVE')")) {
            ps.setString(1, "CampusNest Admin"); ps.setString(2, email.trim().toLowerCase()); ps.setString(3, Passwords.hash(password));
            int inserted = ps.executeUpdate();
            if (inserted == 0) {
                try (PreparedStatement existing = c.prepareStatement("SELECT role FROM users WHERE email=?")) {
                    existing.setString(1, email.trim().toLowerCase(java.util.Locale.ROOT));
                    try (ResultSet rs = existing.executeQuery()) {
                        if (!rs.next() || !"ADMIN".equals(rs.getString("role"))) {
                            throw new IllegalStateException("The configured admin email is already used by a non-admin account. Choose a different CAMPUSNEST_ADMIN_EMAIL.");
                        }
                    }
                }
                if (localDemo) {
                    try (PreparedStatement update = c.prepareStatement("UPDATE users SET password_hash=? WHERE email=? AND role='ADMIN'")) {
                        update.setString(1, Passwords.hash(password));
                        update.setString(2, email.trim().toLowerCase(java.util.Locale.ROOT));
                        update.executeUpdate();
                    }
                }
            }
        } catch (Exception e) { event.getServletContext().log("Admin account bootstrap failed. Check the database connection and configured admin email.", e); }
    }

    private void bootstrapLocalOwner(ServletContextEvent event) {
        if (!Boolean.parseBoolean(System.getenv("CAMPUSNEST_LOCAL_DEMO"))) return;
        String email = System.getenv("CAMPUSNEST_LOCAL_OWNER_EMAIL");
        String password = System.getenv("CAMPUSNEST_LOCAL_OWNER_PASSWORD");
        // Local demo credentials intentionally use the same short password as the
        // local admin account. This bootstrap is disabled outside local demo mode.
        if (email == null || email.isBlank() || password == null || password.length() < 8) return;
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement insertUser = c.prepareStatement(
                    "INSERT IGNORE INTO users(name,email,password_hash,role,account_status) VALUES(?,?,?,'OWNER','ACTIVE')")) {
                insertUser.setString(1, "Local Demo Owner");
                insertUser.setString(2, normalizedEmail);
                insertUser.setString(3, Passwords.hash(password));
                insertUser.executeUpdate();
            }
            long userId;
            try (PreparedStatement findUser = c.prepareStatement("SELECT user_id,role FROM users WHERE email=?")) {
                findUser.setString(1, normalizedEmail);
                try (ResultSet rs = findUser.executeQuery()) {
                    if (!rs.next() || !"OWNER".equals(rs.getString("role"))) {
                        throw new IllegalStateException("The local owner email is already used by a non-owner account.");
                    }
                    userId = rs.getLong("user_id");
                }
            }
            try (PreparedStatement updateUser = c.prepareStatement("UPDATE users SET password_hash=? WHERE user_id=?")) {
                updateUser.setString(1, Passwords.hash(password));
                updateUser.setLong(2, userId);
                updateUser.executeUpdate();
            }
            try (PreparedStatement insertOwner = c.prepareStatement(
                    "INSERT IGNORE INTO pg_owners(user_id,owner_name,contact_no,address,owner_status,is_demo) VALUES(?,?,?,?, 'APPROVED',TRUE)")) {
                insertOwner.setLong(1, userId);
                insertOwner.setString(2, "Local Demo Owner");
                insertOwner.setString(3, "0000000000");
                insertOwner.setString(4, "Local development account");
                insertOwner.executeUpdate();
            }
            try (PreparedStatement findOwner = c.prepareStatement("SELECT owner_status FROM pg_owners WHERE user_id=?")) {
                findOwner.setLong(1, userId);
                try (ResultSet rs = findOwner.executeQuery()) {
                    if (!rs.next() || !"APPROVED".equals(rs.getString("owner_status"))) {
                        throw new IllegalStateException("The local owner profile is not approved.");
                    }
                }
            }
            c.commit();
        } catch (Exception e) {
            event.getServletContext().log("Local demo owner bootstrap failed. Check the configured local owner email.", e);
        }
    }
}
