package edu.coet.campusnest.dao;

import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.OwnerUser;
import edu.coet.campusnest.model.enums.OwnerStatus;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for PG Owner profiles.
 */
public interface OwnerDao extends GenericDao<OwnerUser, Long> {
    Optional<OwnerUser> findByUserId(long userId) throws DatabaseOperationException;
    List<OwnerUser> findPendingQueue() throws DatabaseOperationException;
    List<OwnerUser> findAllOrdered() throws DatabaseOperationException;
    boolean updateStatus(long ownerId, OwnerStatus status) throws DatabaseOperationException;
    long countPendingOwners() throws DatabaseOperationException;
}
