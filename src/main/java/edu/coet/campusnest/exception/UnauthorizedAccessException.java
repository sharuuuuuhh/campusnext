package edu.coet.campusnest.exception;

/**
 * Thrown when an authenticated user lacks permissions for an operation.
 */
public class UnauthorizedAccessException extends CampusNestException {
    public UnauthorizedAccessException(String message) {
        super(message, 403);
    }
}
