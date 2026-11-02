package edu.coet.campusnest.dao.impl;

import edu.coet.campusnest.Db;
import edu.coet.campusnest.exception.DatabaseOperationException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Abstract base class for all JDBC-based DAO implementations.
 * Demonstrates:
 * - OOP Abstraction & Code Reuse
 * - Template method pattern / helper routines
 * - Safe resource handling using try-with-resources
 */
public abstract class AbstractJdbcDao {

    protected Connection getConnection() throws SQLException {
        return Db.connect();
    }

    protected Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }

    protected DatabaseOperationException handleSqlException(String message, SQLException e) {
        return new DatabaseOperationException(message + ": " + e.getMessage(), e);
    }
}
