package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.enums.OwnerStatus;
import edu.coet.campusnest.model.enums.UserRole;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a PG Property Owner in CampusNest.
 * Demonstrates:
 * - OOP Inheritance (extends User)
 * - Domain Specific Fields (ownerId, property address, ID proof metadata, verification status)
 * - Method Overriding (Polymorphism: getDashboardRole, hasAdminPrivileges, toSessionMap, validate)
 */
public class OwnerUser extends User {
    private Long ownerId;
    private String demoKey;
    private String address;
    private String idProofPath;
    private String proofOriginalName;
    private String proofMediaType;
    private Instant proofReviewedAt;
    private OwnerStatus ownerStatus = OwnerStatus.PENDING;
    private boolean isDemo = false;

    public OwnerUser() {
        super();
        setRole(UserRole.OWNER);
    }

    public OwnerUser(Long userId, String name, String email, String phoneNo, String passwordHash,
                     Long ownerId, String address) {
        super(userId, name, email, phoneNo, passwordHash, UserRole.OWNER);
        this.ownerId = ownerId;
        this.address = address;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getDemoKey() {
        return demoKey;
    }

    public void setDemoKey(String demoKey) {
        this.demoKey = demoKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdProofPath() {
        return idProofPath;
    }

    public void setIdProofPath(String idProofPath) {
        this.idProofPath = idProofPath;
    }

    public String getProofOriginalName() {
        return proofOriginalName;
    }

    public void setProofOriginalName(String proofOriginalName) {
        this.proofOriginalName = proofOriginalName;
    }

    public String getProofMediaType() {
        return proofMediaType;
    }

    public void setProofMediaType(String proofMediaType) {
        this.proofMediaType = proofMediaType;
    }

    public Instant getProofReviewedAt() {
        return proofReviewedAt;
    }

    public void setProofReviewedAt(Instant proofReviewedAt) {
        this.proofReviewedAt = proofReviewedAt;
    }

    public OwnerStatus getOwnerStatus() {
        return ownerStatus;
    }

    public void setOwnerStatus(OwnerStatus ownerStatus) {
        this.ownerStatus = ownerStatus;
    }

    public boolean isDemo() {
        return isDemo;
    }

    public void setDemo(boolean demo) {
        isDemo = demo;
    }

    public boolean isApproved() {
        return ownerStatus == OwnerStatus.APPROVED;
    }

    @Override
    public String getDashboardRole() {
        return "OWNER";
    }

    @Override
    public boolean hasAdminPrivileges() {
        return false;
    }

    @Override
    public Map<String, Object> toSessionMap() {
        Map<String, Object> map = baseSessionMap();
        map.put("ownerId", ownerId);
        map.put("status", ownerStatus != null ? ownerStatus.name() : "PENDING");
        return map;
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
        if (address == null || address.trim().isBlank()) {
            throw new ValidationException("Owner address is required for verification.");
        }
    }
}
