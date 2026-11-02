package edu.coet.campusnest;

import edu.coet.campusnest.dao.UserDao;
import edu.coet.campusnest.dao.impl.UserDaoImpl;
import edu.coet.campusnest.model.AdminUser;
import edu.coet.campusnest.model.enums.AccountStatus;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded Pure Java Application Server for Local Development and KTU Evaluation.
 * Runs 100% in Java with zero external dependencies required:
 * - Automatically connects to local MySQL if available on port 3306.
 * - Otherwise falls back to an embedded in-process Java database (H2 in MySQL mode) and seeds initial PG records.
 * - Boots embedded Tomcat 10.1 (Jakarta Servlet 6.0) on port 8081.
 */
public class LocalServer {
    private static final Logger LOGGER = Logger.getLogger(LocalServer.class.getName());
    public static final int DEFAULT_PORT = 8081;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }

        LOGGER.info("Starting CampusNest pure Java server for localhost...");

        try {
            boolean forceLocal = "true".equalsIgnoreCase(System.getProperty("USE_LOCAL_DB", System.getenv("USE_LOCAL_DB")));
            if (!forceLocal) {
                loadEnvFile();
            } else {
                LOGGER.info("[Database] USE_LOCAL_DB=true: Using purely local database (ignoring Supabase / external DB).");
                System.clearProperty("CAMPUSNEST_DB_URL");
                System.clearProperty("CAMPUSNEST_DB_USER");
                System.clearProperty("CAMPUSNEST_DB_PASSWORD");
            }
            configureDatabase();
            configureAdmin();
            startTomcat(port);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start CampusNest LocalServer", e);
            System.exit(1);
        }
    }

    private static void loadEnvFile() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) return;
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                        val = val.substring(1, val.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, val);
                    }
                }
            }
            LOGGER.info("[Config] Successfully loaded environment variables from .env");
        } catch (Exception e) {
            LOGGER.warning("[Config] Could not parse .env file: " + e.getMessage());
        }
    }

    private static void configureDatabase() throws Exception {
        boolean forceLocal = "true".equalsIgnoreCase(System.getProperty("USE_LOCAL_DB", System.getenv("USE_LOCAL_DB")));
        if (!forceLocal) {
            String customUrl = System.getProperty("CAMPUSNEST_DB_URL", System.getenv("CAMPUSNEST_DB_URL"));
            if (customUrl != null && !customUrl.isBlank()) {
                LOGGER.info("[Database] Using configured database URL: " + customUrl.replaceAll(":[^:@/]+@", ":****@"));
                try (Connection c = Db.connect()) {
                    if (c.isValid(5)) {
                        LOGGER.info("[Database] Connected successfully to configured external database.");
                        return;
                    }
                } catch (Exception e) {
                    LOGGER.warning("[Database] Connection to configured database failed: " + e.getMessage());
                    throw e;
                }
            }
        }

        boolean mysqlAvailable = isPortOpen("127.0.0.1", 3306);
        if (mysqlAvailable) {
            LOGGER.info("[Database] Detected active MySQL server on 127.0.0.1:3306. Using MySQL database.");
            try (Connection c = Db.connect()) {
                if (c.isValid(2)) {
                    LOGGER.info("[Database] Connected successfully to MySQL.");
                    return;
                }
            } catch (Exception e) {
                LOGGER.info("[Database] MySQL on port 3306 refused connection: " + e.getMessage() + ". Falling back to embedded Java database.");
            }
        }

        // Setup Embedded Pure Java H2 Database in MySQL mode
        Path dbDir = Path.of("work", "h2-data").toAbsolutePath();
        Files.createDirectories(dbDir);
        String h2Url = "jdbc:h2:" + dbDir.resolve("campusnest").toString().replace('\\', '/') +
                ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";

        System.setProperty("CAMPUSNEST_DB_URL", h2Url);
        System.setProperty("CAMPUSNEST_DB_USER", "sa");
        System.setProperty("CAMPUSNEST_DB_PASSWORD", "");
        LOGGER.info("[Database] Embedded pure Java H2 database configured at: " + h2Url);

        initializeH2SchemaAndSeed();
    }

    private static void initializeH2SchemaAndSeed() {
        try (Connection c = Db.connect(); Statement s = c.createStatement()) {
            boolean schemaExists = false;
            try {
                s.executeQuery("SELECT 1 FROM users LIMIT 1");
                schemaExists = true;
            } catch (Exception ex) {
                schemaExists = false;
            }

            if (!schemaExists) {
                LOGGER.info("[Database] Initializing schema from database/schema.sql...");
                executeSqlScript(c, Path.of("database", "schema.sql"));
                LOGGER.info("[Database] Loading simulated listings from database/seed.sql...");
                executeSqlScript(c, Path.of("database", "seed.sql"));
                LOGGER.info("[Database] Database initialized and seeded successfully.");
            } else {
                LOGGER.info("[Database] Existing database tables found; skipping re-initialization.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[Database] Note on database initialization: " + e.getMessage(), e);
        }
    }

    private static void executeSqlScript(Connection conn, Path path) throws Exception {
        if (!Files.exists(path)) return;
        String content = Files.readString(path);
        // Strip line comments and block comments first so semicolons inside comments are eliminated
        content = content.replaceAll("(?m)--.*$", "")
                         .replaceAll("/\\*.*?\\*/", "")
                         .replaceAll("(?i)CREATE DATABASE[^;]+;", "")
                         .replaceAll("(?i)USE\\s+[^;]+;", "")
                         .replaceAll("(?i)CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci", "")
                         .replaceAll("(?i)COLLATE utf8mb4_0900_ai_ci", "")
                         .replaceAll("(?i)ON UPDATE CURRENT_TIMESTAMP", "")
                         .replaceAll("(?i)ENGINE\\s*=\\s*InnoDB", "")
                         .replaceAll("(?i),?\\s*INDEX\\s+[a-zA-Z0-9_]+\\s*\\([^)]+\\)", "");

        String[] statements = content.split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String clean = sql.trim();
                if (!clean.isBlank()) {
                    try {
                        stmt.execute(clean);
                    } catch (Exception ex) {
                        if (!ex.getMessage().contains("already exists")) {
                            LOGGER.log(Level.WARNING, "Failed to execute SQL: " + clean.substring(0, Math.min(60, clean.length())) + "... -> " + ex.getMessage());
                        }
                    }
                }
            }
        }
    }

    private static void configureAdmin() {
        if (System.getProperty("CAMPUSNEST_ADMIN_EMAIL") == null && System.getenv("CAMPUSNEST_ADMIN_EMAIL") == null) {
            System.setProperty("CAMPUSNEST_ADMIN_EMAIL", "admin@campusnest.local");
            System.setProperty("CAMPUSNEST_ADMIN_PASSWORD", "admin12345678");
        }
    }

    private static void startTomcat(int port) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Triggers default HTTP connector initialization

        Path baseDir = Path.of("target", "tomcat-embed").toAbsolutePath();
        Files.createDirectories(baseDir);
        tomcat.setBaseDir(baseDir.toString());

        File webappDir = new File("src/main/webapp").getAbsoluteFile();
        if (!webappDir.exists()) {
            webappDir = new File(".").getAbsoluteFile();
        }

        // Register web context programmatically
        Context ctx = tomcat.addContext("/campusnest", webappDir.getAbsolutePath());
        ctx.setParentClassLoader(LocalServer.class.getClassLoader());

        Tomcat.addServlet(ctx, "default", new org.apache.catalina.servlets.DefaultServlet());
        ctx.addServletMappingDecoded("/", "default");
        ctx.addWelcomeFile("index.html");

        Tomcat.addServlet(ctx, "apiServlet", new ApiServlet());
        ctx.addServletMappingDecoded("/api/*", "apiServlet");
        ctx.addApplicationListener(CampusNestListener.class.getName());

        // Also register root context for http://localhost:8081/
        Context rootCtx = tomcat.addContext("", webappDir.getAbsolutePath());
        rootCtx.setParentClassLoader(LocalServer.class.getClassLoader());

        Tomcat.addServlet(rootCtx, "defaultRoot", new org.apache.catalina.servlets.DefaultServlet());
        rootCtx.addServletMappingDecoded("/", "defaultRoot");
        rootCtx.addWelcomeFile("index.html");

        Tomcat.addServlet(rootCtx, "apiServletRoot", new ApiServlet());
        rootCtx.addServletMappingDecoded("/api/*", "apiServletRoot");
        rootCtx.addApplicationListener(CampusNestListener.class.getName());

        tomcat.start();

        System.out.println("==================================================================");
        System.out.println("  COET CampusNest - Pure Java Application is READY on localhost!");
        System.out.println("==================================================================");
        System.out.println("  -> Primary URL:   http://localhost:" + port + "/campusnest/");
        System.out.println("  -> Root URL:      http://localhost:" + port + "/");
        System.out.println("  -> Health Check:  http://localhost:" + port + "/campusnest/api/health");
        System.out.println("------------------------------------------------------------------");
        System.out.println("  Admin Login Credentials:");
        System.out.println("     Email:    admin@campusnest.local");
        System.out.println("     Password: admin12345678");
        System.out.println("==================================================================");
        System.out.println("  Press Ctrl+C to terminate the server.");

        tomcat.getServer().await();
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket s = new Socket(host, port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
