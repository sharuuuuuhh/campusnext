package edu.coet.campusnest;

import edu.coet.campusnest.dao.UserDao;
import edu.coet.campusnest.dao.impl.UserDaoImpl;
import edu.coet.campusnest.model.AdminUser;
import edu.coet.campusnest.model.enums.AccountStatus;
import edu.coet.campusnest.thread.BackgroundSyncDaemon;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application Lifecycle Listener.
 * Demonstrates:
 * - Managing multithreaded daemon lifecycle in Java Web Applications
 * - Context initialization and destruction
 * - Bootstrapping administrative accounts
 */
@WebListener
public class CampusNestListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(CampusNestListener.class.getName());

    private BackgroundSyncDaemon syncDaemon;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("CampusNest application context initialized.");

        // Start background multithreaded worker
        syncDaemon = new BackgroundSyncDaemon();
        syncDaemon.start();
        event.getServletContext().setAttribute("syncDaemon", syncDaemon);

        // Bootstrap Admin user if configured in system properties or environment
        String email = System.getProperty("CAMPUSNEST_ADMIN_EMAIL", System.getenv("CAMPUSNEST_ADMIN_EMAIL"));
        String password = System.getProperty("CAMPUSNEST_ADMIN_PASSWORD", System.getenv("CAMPUSNEST_ADMIN_PASSWORD"));
        if (email == null || email.isBlank() || password == null || password.length() < 12) {
            event.getServletContext().log("Admin bootstrap skipped: configure CAMPUSNEST_ADMIN_EMAIL and a 12+ character CAMPUSNEST_ADMIN_PASSWORD.");
            return;
        }

        try {
            UserDao userDao = new UserDaoImpl();
            String cleanEmail = email.trim().toLowerCase();
            if (!userDao.emailExists(cleanEmail)) {
                AdminUser admin = new AdminUser();
                admin.setName("CampusNest Admin");
                admin.setEmail(cleanEmail);
                admin.setPasswordHash(Passwords.hash(password));
                admin.setAccountStatus(AccountStatus.ACTIVE);
                userDao.save(admin);
                event.getServletContext().log("CampusNest Admin account bootstrapped successfully: " + cleanEmail);
            } else {
                userDao.findByEmail(cleanEmail).ifPresent(u -> {
                    try {
                        userDao.updatePassword(u.getId(), Passwords.hash(password));
                        event.getServletContext().log("CampusNest Admin account password synchronized: " + cleanEmail);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Failed to synchronize admin password", ex);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Admin bootstrap could not complete", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        LOGGER.info("CampusNest application context destroying; shutting down background threads.");
        if (syncDaemon != null) {
            syncDaemon.stop();
        }
    }
}
