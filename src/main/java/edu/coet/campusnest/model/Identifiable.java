package edu.coet.campusnest.model;

/**
 * Interface contract for entities having a unique identifier.
 * Demonstrates Java Interface Abstraction for KTU S3 OOP.
 *
 * @param <T> Identifier type
 */
public interface Identifiable<T> {
    T getId();
    void setId(T id);
}
