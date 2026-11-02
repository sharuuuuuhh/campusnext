package edu.coet.campusnest.exception;

/**
 * Base custom exception for the CampusNest application.
 * Demonstrates Java Exception hierarchy and custom exception handling for KTU S3 OOPs.
 */
public class CampusNestException extends Exception {
    private final int statusCode;

    public CampusNestException(String message) {
        this(message, 500);
    }

    public CampusNestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CampusNestException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
