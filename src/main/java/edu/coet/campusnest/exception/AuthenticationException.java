package edu.coet.campusnest.exception;

/**
 * Thrown when credentials fail or authentication is rejected.
 */
public class AuthenticationException extends CampusNestException {
    public AuthenticationException(String message) {
        super(message, 401);
    }
}
