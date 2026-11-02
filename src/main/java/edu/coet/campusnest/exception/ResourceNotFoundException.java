package edu.coet.campusnest.exception;

/**
 * Thrown when an entity (PG listing, user, feedback, proof) cannot be found.
 */
public class ResourceNotFoundException extends CampusNestException {
    public ResourceNotFoundException(String message) {
        super(message, 404);
    }
}
