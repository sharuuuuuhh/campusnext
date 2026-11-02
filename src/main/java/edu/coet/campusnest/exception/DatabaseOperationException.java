package edu.coet.campusnest.exception;

/**
 * Wraps low-level SQLExceptions into a domain exception.
 */
public class DatabaseOperationException extends CampusNestException {
    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause, 500);
    }

    public DatabaseOperationException(String message) {
        super(message, 500);
    }
}
