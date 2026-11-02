package edu.coet.campusnest.thread;

import edu.coet.campusnest.Db;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background worker thread demonstrating Multithreading for KTU S3 Java OOPs (CST 205 Module 3).
 * Demonstrates:
 * - Implementing the Runnable interface
 * - Managing thread lifecycle (start, sleep, interrupt, join)
 * - Thread synchronization (synchronized logSyncSummary)
 * - Safe resource cleanup on application shutdown
 */
public class BackgroundSyncDaemon implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(BackgroundSyncDaemon.class.getName());
    private static final long INTERVAL_MS = 60_000; // Check every 60 seconds

    private volatile boolean running = true;
    private Thread workerThread;
    private int syncCycles = 0;

    public synchronized void start() {
        if (workerThread == null || !workerThread.isAlive()) {
            running = true;
            workerThread = new Thread(this, "CampusNest-BackgroundSyncThread");
            workerThread.setDaemon(true);
            workerThread.start();
            LOGGER.info("[KTU OOPs Multithreading] BackgroundSyncDaemon worker thread started successfully.");
        }
    }

    public synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("[KTU OOPs Multithreading] BackgroundSyncDaemon worker thread stopped.");
        }
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                performHealthAndSyncCycle();
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                LOGGER.info("[KTU OOPs Multithreading] Daemon thread received interrupt signal; exiting.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during background sync cycle", e);
            }
        }
    }

    private void performHealthAndSyncCycle() {
        boolean dbAlive = false;
        try (Connection c = Db.connect()) {
            dbAlive = c.isValid(2);
        } catch (Exception ignored) {}

        syncCycles++;
        logSyncSummary(dbAlive, syncCycles);
    }

    /**
     * Synchronized method demonstrating Java thread synchronization and critical section protection.
     */
    private synchronized void logSyncSummary(boolean dbAlive, int cycle) {
        LOGGER.info(String.format(
                "[Sync Thread Heartbeat] Cycle #%d: Database Status = %s, Active Thread = %s",
                cycle, dbAlive ? "ONLINE" : "OFFLINE", Thread.currentThread().getName()
        ));
    }

    public int getSyncCycles() {
        return syncCycles;
    }

    public boolean isRunning() {
        return running && workerThread != null && workerThread.isAlive();
    }
}
