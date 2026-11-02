package edu.coet.campusnest.service;

import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ResourceNotFoundException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.OwnerUser;
import edu.coet.campusnest.model.enums.OwnerStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Admin moderation and system dashboard.
 */
public interface AdminService {
    Map<String, Object> getDashboardStats() throws CampusNestException;
    List<OwnerUser> getOwnerQueue() throws CampusNestException;
    void updateOwnerStatus(long ownerId, OwnerStatus status) throws ValidationException, ResourceNotFoundException, CampusNestException;
    Path getOwnerProofPath(long ownerId, String storageDir) throws ResourceNotFoundException, CampusNestException;
    String getOwnerProofMediaType(long ownerId) throws ResourceNotFoundException, CampusNestException;
}
