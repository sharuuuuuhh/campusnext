package edu.coet.campusnest.model;

import edu.coet.campusnest.model.enums.GenderType;
import edu.coet.campusnest.model.enums.RoomType;

/**
 * Filter criteria DTO for searching and querying PG listings.
 * Demonstrates:
 * - OOP Encapsulation (Private fields with accessors)
 * - Method Overloading / Fluent Builder pattern
 */
public class ListingFilterCriteria {
    private String query;
    private GenderType gender;
    private Integer maxRent;
    private RoomType roomType;
    private Boolean foodFacility;
    private Double maxDistance;
    private String sortBy; // "rent-asc", "distance", or default
    private boolean adminView;

    public ListingFilterCriteria() {}

    public ListingFilterCriteria(String query) {
        this.query = query;
    }

    public ListingFilterCriteria(String query, GenderType gender, Integer maxRent) {
        this.query = query;
        this.gender = gender;
        this.maxRent = maxRent;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public GenderType getGender() {
        return gender;
    }

    public void setGender(GenderType gender) {
        this.gender = gender;
    }

    public Integer getMaxRent() {
        return maxRent;
    }

    public void setMaxRent(Integer maxRent) {
        this.maxRent = maxRent;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Boolean getFoodFacility() {
        return foodFacility;
    }

    public void setFoodFacility(Boolean foodFacility) {
        this.foodFacility = foodFacility;
    }

    public Double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(Double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public boolean isAdminView() {
        return adminView;
    }

    public void setAdminView(boolean adminView) {
        this.adminView = adminView;
    }
}
