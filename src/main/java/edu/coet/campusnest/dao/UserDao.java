package edu.coet.campusnest.dao;

import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.User;
import java.util.Optional;

/**
 * Data Access Object interface for User entities.
 * Demonstrates Interface Inheritance (extends GenericDao).
 */
public interface UserDao extends GenericDao<User, Long> {
    Optional<User> findByEmail(String email) throws DatabaseOperationException;
    boolean emailExists(String email) throws DatabaseOperationException;
    boolean updatePassword(long userId, String newPasswordHash) throws DatabaseOperationException;
}
