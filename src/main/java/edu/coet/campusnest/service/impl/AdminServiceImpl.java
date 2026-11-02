package edu.coet.campusnest.service.impl;

import edu.coet.campusnest.dao.FeedbackDao;
import edu.coet.campusnest.dao.InquiryDao;
import edu.coet.campusnest.dao.ListingDao;
import edu.coet.campusnest.dao.OwnerDao;
import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ResourceNotFoundException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.OwnerUser;
import edu.coet.campusnest.model.enums.OwnerStatus;
import edu.coet.campusnest.service.AdminService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of AdminService.
 * Demonstrates:
 * - Aggregating multiple DAOs in a single cohesive service
 * - Access control and business state transitions
 */
public class AdminServiceImpl implements AdminService {

    private final OwnerDao ownerDao;
    private final ListingDao listingDao;
    private final InquiryDao inquiryDao;
    private final FeedbackDao feedbackDao;

    public AdminServiceImpl(OwnerDao ownerDao, ListingDao listingDao, InquiryDao inquiryDao, FeedbackDao feedbackDao) {
        this.ownerDao = ownerDao;
        this.listingDao = listingDao;
        this.inquiryDao = inquiryDao;
        this.feedbackDao = feedbackDao;
    }

    @Override
    public Map<String, Object> getDashboardStats() throws CampusNestException {
        long pendingOwners = ownerDao.countPendingOwners();
        long visibleListings = listingDao.countVisibleApprovedListings();
        long newInquiries = inquiryDao.countNewInquiries();
        long newFeedback = feedbackDao.countNewFeedback();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pendingOwners", pendingOwners);
        stats.put("visibleListings", visibleListings);
        stats.put("inquiries", newInquiries);
        stats.put("feedback", newFeedback);
        return stats;
    }

    @Override
    public List<OwnerUser> getOwnerQueue() throws CampusNestException {
        return ownerDao.findAllOrdered();
    }

    @Override
    public void updateOwnerStatus(long ownerId, OwnerStatus status) throws ValidationException, ResourceNotFoundException, CampusNestException {
        if (status == null || status == OwnerStatus.PENDING) {
            throw new ValidationException("Owner status must be either APPROVED or REJECTED.");
        }
        boolean updated = ownerDao.updateStatus(ownerId, status);
        if (!updated) {
            throw new ResourceNotFoundException("Owner profile not found with ID " + ownerId);
        }
    }

    @Override
    public Path getOwnerProofPath(long ownerId, String storageDir) throws ResourceNotFoundException, CampusNestException {
        Optional<OwnerUser> ownerOpt = ownerDao.findById(ownerId);
        if (ownerOpt.isEmpty() || ownerOpt.get().getIdProofPath() == null) {
            throw new ResourceNotFoundException("Owner proof document not found for ID " + ownerId);
        }
        String filename = ownerOpt.get().getIdProofPath();
        String base = storageDir != null ? storageDir :
                Path.of(System.getProperty("user.dir"), "campusnest-private", "owner-proofs").toString();
        Path dir = Path.of(base).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();

        if (!file.getParent().equals(dir) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Owner proof document file is missing from disk.");
        }
        return file;
    }

    @Override
    public String getOwnerProofMediaType(long ownerId) throws ResourceNotFoundException, CampusNestException {
        Optional<OwnerUser> ownerOpt = ownerDao.findById(ownerId);
        if (ownerOpt.isEmpty()) {
            throw new ResourceNotFoundException("Owner not found with ID " + ownerId);
        }
        String type = ownerOpt.get().getProofMediaType();
        return (type != null && !type.isBlank()) ? type : "application/octet-stream";
    }
}
