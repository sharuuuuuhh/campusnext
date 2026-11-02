package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.enums.GenderType;
import edu.coet.campusnest.model.enums.ModerationStatus;
import edu.coet.campusnest.model.enums.RoomType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing a Paying Guest (PG) accommodation listing.
 * Demonstrates:
 * - OOP Inheritance (extends BaseEntity)
 * - OOP Encapsulation (Private fields with accessors, invariant checks)
 * - Method Overriding (validate(), toString())
 * - Helper business methods (hasVacancy(), getOccupancyRate())
 */
public class PgListing extends BaseEntity {
    private Long ownerId;
    private String ownerName;
    private String ownerPhone;
    private String demoKey;
    private String pgName;
    private GenderType genderType = GenderType.BOYS;
    private BigDecimal monthlyRent = BigDecimal.ZERO;
    private RoomType roomType = RoomType.SHARED;
    private boolean foodFacility = false;
    private int totalBeds = 1;
    private int vacantBeds = 0;
    private double distFromCoet = 0.0;
    private String pgAddress;
    private List<String> amenities = new ArrayList<>();
    private String description;
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;
    private boolean visible = true;
    private boolean demo = false;

    public PgListing() {
        super();
    }

    public PgListing(Long id, Long ownerId, String pgName, GenderType genderType,
                     BigDecimal monthlyRent, RoomType roomType, boolean foodFacility,
                     int totalBeds, int vacantBeds, double distFromCoet, String pgAddress) {
        super(id);
        this.ownerId = ownerId;
        this.pgName = pgName;
        this.genderType = genderType;
        this.monthlyRent = monthlyRent;
        this.roomType = roomType;
        this.foodFacility = foodFacility;
        this.totalBeds = totalBeds;
        this.vacantBeds = vacantBeds;
        this.distFromCoet = distFromCoet;
        this.pgAddress = pgAddress;
    }

    public boolean hasVacancy() {
        return vacantBeds > 0;
    }

    public double getOccupancyPercentage() {
        if (totalBeds <= 0) return 0.0;
        int occupied = totalBeds - vacantBeds;
        return ((double) occupied / totalBeds) * 100.0;
    }

    @Override
    public void validate() throws ValidationException {
        if (pgName == null || pgName.trim().isBlank()) {
            throw new ValidationException("PG accommodation name is required.");
        }
        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ONE) < 0) {
            throw new ValidationException("Monthly rent must be greater than zero.");
        }
        if (totalBeds < 1) {
            throw new ValidationException("Total beds must be at least 1.");
        }
        if (vacantBeds < 0 || vacantBeds > totalBeds) {
            throw new ValidationException("Vacant beds must be between 0 and total beds (" + totalBeds + ").");
        }
        if (distFromCoet < 0.0 || distFromCoet > 25.0) {
            throw new ValidationException("Distance from COET Thalassery must be between 0 and 25 km.");
        }
        if (pgAddress == null || pgAddress.trim().isBlank()) {
            throw new ValidationException("PG physical address is required.");
        }
    }

    public Map<String, Object> toMap(boolean includeAdminFields) {
        Map<String, Object> m = new LinkedHashMap<>();
        long currentId = (id != null) ? id : 0L;
        m.put("id", currentId);
        m.put("name", pgName);
        m.put("owner", ownerName);
        m.put("gender", genderType != null ? genderType.getLabel() : "Boys");
        m.put("rent", monthlyRent != null ? monthlyRent.intValue() : 0);
        m.put("room", roomType != null ? roomType.getLabel() : "Shared");
        m.put("food", foodFacility);
        m.put("total", totalBeds);
        m.put("vacant", vacantBeds);
        m.put("distance", distFromCoet);
        m.put("address", pgAddress);
        m.put("amenities", amenities != null ? amenities : List.of());
        m.put("desc", description);
        m.put("phone", ownerPhone);
        m.put("scene", "scene-" + (char) ('a' + (currentId % 3)));
        m.put("demo", demo);
        if (includeAdminFields) {
            m.put("status", moderationStatus != null ? moderationStatus.name() : "PENDING");
            m.put("visible", visible);
        }
        return m;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public String getDemoKey() {
        return demoKey;
    }

    public void setDemoKey(String demoKey) {
        this.demoKey = demoKey;
    }

    public String getPgName() {
        return pgName;
    }

    public void setPgName(String pgName) {
        this.pgName = pgName;
    }

    public GenderType getGenderType() {
        return genderType;
    }

    public void setGenderType(GenderType genderType) {
        this.genderType = genderType;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public boolean isFoodFacility() {
        return foodFacility;
    }

    public void setFoodFacility(boolean foodFacility) {
        this.foodFacility = foodFacility;
    }

    public int getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(int totalBeds) {
        this.totalBeds = totalBeds;
    }

    public int getVacantBeds() {
        return vacantBeds;
    }

    public void setVacantBeds(int vacantBeds) {
        this.vacantBeds = vacantBeds;
    }

    public double getDistFromCoet() {
        return distFromCoet;
    }

    public void setDistFromCoet(double distFromCoet) {
        this.distFromCoet = distFromCoet;
    }

    public String getPgAddress() {
        return pgAddress;
    }

    public void setPgAddress(String pgAddress) {
        this.pgAddress = pgAddress;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean demo) {
        this.demo = demo;
    }
}
