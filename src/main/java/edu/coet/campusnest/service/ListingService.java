package edu.coet.campusnest.service;

import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ResourceNotFoundException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.ListingFilterCriteria;
import edu.coet.campusnest.model.PgListing;
import edu.coet.campusnest.model.enums.ModerationStatus;
import java.util.List;

/**
 * Service interface for PG Listing management and search.
 */
public interface ListingService {
    List<PgListing> searchListings(ListingFilterCriteria criteria) throws CampusNestException;
    PgListing getListingById(long id, boolean adminView) throws ResourceNotFoundException, CampusNestException;
    List<PgListing> getOwnerListings(long ownerId) throws CampusNestException;
    PgListing createListing(long ownerId, PgListing listing) throws ValidationException, CampusNestException;
    void updateListing(long ownerId, PgListing listing) throws ValidationException, ResourceNotFoundException, CampusNestException;
    void updateVacancy(long ownerId, long listingId, int vacantBeds) throws ValidationException, ResourceNotFoundException, CampusNestException;
    void toggleVisibility(long listingId, boolean visible) throws ResourceNotFoundException, CampusNestException;
    void updateModerationStatus(long listingId, ModerationStatus status) throws ResourceNotFoundException, CampusNestException;
    void deleteListing(long listingId) throws ResourceNotFoundException, CampusNestException;
}
