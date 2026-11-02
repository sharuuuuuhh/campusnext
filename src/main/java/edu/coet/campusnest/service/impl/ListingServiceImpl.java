package edu.coet.campusnest.service.impl;

import edu.coet.campusnest.dao.ListingDao;
import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ResourceNotFoundException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.ListingFilterCriteria;
import edu.coet.campusnest.model.PgListing;
import edu.coet.campusnest.model.enums.ModerationStatus;
import edu.coet.campusnest.service.ListingService;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ListingService.
 * Demonstrates:
 * - Business logic encapsulation
 * - Domain validation before database invocation
 * - Polymorphic method implementation
 */
public class ListingServiceImpl implements ListingService {

    private final ListingDao listingDao;

    public ListingServiceImpl(ListingDao listingDao) {
        this.listingDao = listingDao;
    }

    @Override
    public List<PgListing> searchListings(ListingFilterCriteria criteria) throws CampusNestException {
        return listingDao.findListings(criteria);
    }

    @Override
    public PgListing getListingById(long id, boolean adminView) throws ResourceNotFoundException, CampusNestException {
        Optional<PgListing> opt = listingDao.findById(id, adminView);
        if (opt.isEmpty()) {
            throw new ResourceNotFoundException("PG listing not found with ID " + id);
        }
        return opt.get();
    }

    @Override
    public List<PgListing> getOwnerListings(long ownerId) throws CampusNestException {
        return listingDao.findListings(ownerId);
    }

    @Override
    public PgListing createListing(long ownerId, PgListing listing) throws ValidationException, CampusNestException {
        listing.setOwnerId(ownerId);
        listing.validate();
        return listingDao.save(listing);
    }

    @Override
    public void updateListing(long ownerId, PgListing listing) throws ValidationException, ResourceNotFoundException, CampusNestException {
        listing.setOwnerId(ownerId);
        listing.validate();
        boolean updated = listingDao.update(listing);
        if (!updated) {
            throw new ResourceNotFoundException("PG listing not found or not owned by you.");
        }
    }

    @Override
    public void updateVacancy(long ownerId, long listingId, int vacantBeds) throws ValidationException, ResourceNotFoundException, CampusNestException {
        if (vacantBeds < 0) {
            throw new ValidationException("Vacant beds must be zero or a positive number.");
        }
        boolean ok = listingDao.updateVacancy(listingId, vacantBeds, ownerId);
        if (!ok) {
            throw new ValidationException("Could not update vacancy. Check the listing ID and total bed capacity.");
        }
    }

    @Override
    public void toggleVisibility(long listingId, boolean visible) throws ResourceNotFoundException, CampusNestException {
        boolean ok = listingDao.updateVisibility(listingId, visible);
        if (!ok) {
            throw new ResourceNotFoundException("Listing not found with ID " + listingId);
        }
    }

    @Override
    public void updateModerationStatus(long listingId, ModerationStatus status) throws ResourceNotFoundException, CampusNestException {
        if (status == null) {
            throw new ResourceNotFoundException("Invalid status provided.");
        }
        boolean ok = listingDao.updateModerationStatus(listingId, status);
        if (!ok) {
            throw new ResourceNotFoundException("Listing not found with ID " + listingId);
        }
    }

    @Override
    public void deleteListing(long listingId) throws ResourceNotFoundException, CampusNestException {
        boolean ok = listingDao.deleteById(listingId);
        if (!ok) {
            throw new ResourceNotFoundException("Listing not found with ID " + listingId);
        }
    }
}
