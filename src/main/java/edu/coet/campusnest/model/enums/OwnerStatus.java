package edu.coet.campusnest.model.enums;

public enum OwnerStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static OwnerStatus fromString(String status) {
        if (status == null) return PENDING;
        for (OwnerStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) return s;
        }
        return PENDING;
    }
}
