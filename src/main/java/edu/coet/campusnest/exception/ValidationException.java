package edu.coet.campusnest.exception;

/**
 * Thrown when input data fails domain validation rules.
 */
public class ValidationException extends CampusNestException {
    public ValidationException(String message) {
        super(message, 400);
    }
}
