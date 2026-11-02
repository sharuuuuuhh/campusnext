package edu.coet.campusnest.dao;

import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.ListingFilterCriteria;
import edu.coet.campusnest.model.PgListing;
import edu.coet.campusnest.model.enums.ModerationStatus;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for PG listing records.
 * Demonstrates:
 * - Interface Inheritance
 * - Method Overloading (findListings with criteria vs by ownerId)
 */
public interface ListingDao extends GenericDao<PgListing, Long> {
    // Method Overloading
    List<PgListing> findListings(ListingFilterCriteria criteria) throws DatabaseOperationException;
    List<PgListing> findListings(long ownerId) throws DatabaseOperationException;

    Optional<PgListing> findById(long id, boolean adminView) throws DatabaseOperationException;
    boolean updateVacancy(long pgId, int vacantBeds, long ownerId) throws DatabaseOperationException;
    boolean updateVisibility(long pgId, boolean visible) throws DatabaseOperationException;
    boolean updateModerationStatus(long pgId, ModerationStatus status) throws DatabaseOperationException;
    long countVisibleApprovedListings() throws DatabaseOperationException;
}
