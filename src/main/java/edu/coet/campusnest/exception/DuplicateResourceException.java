package edu.coet.campusnest.exception;

/**
 * Thrown when trying to register an existing unique entity (e.g., duplicate email).
 */
public class DuplicateResourceException extends CampusNestException {
    public DuplicateResourceException(String message) {
        super(message, 409);
    }
}
