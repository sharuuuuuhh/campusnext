package edu.coet.campusnest.dao;

import edu.coet.campusnest.exception.DatabaseOperationException;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object (DAO) interface.
 * Demonstrates:
 * - OOP Generics
 * - Interface Abstraction
 * - Contract-driven design
 *
 * @param <T>  Domain entity type
 * @param <ID> Unique identifier type
 */
public interface GenericDao<T, ID> {
    Optional<T> findById(ID id) throws DatabaseOperationException;
    List<T> findAll() throws DatabaseOperationException;
    T save(T entity) throws DatabaseOperationException;
    boolean update(T entity) throws DatabaseOperationException;
    boolean deleteById(ID id) throws DatabaseOperationException;
}
