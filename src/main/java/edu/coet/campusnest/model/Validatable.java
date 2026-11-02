package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;

/**
 * Interface contract for domain entities that can self-validate.
 */
public interface Validatable {
    void validate() throws ValidationException;
}
