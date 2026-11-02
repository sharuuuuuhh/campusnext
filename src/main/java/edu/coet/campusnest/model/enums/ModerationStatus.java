package edu.coet.campusnest.model.enums;

public enum ModerationStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static ModerationStatus fromString(String status) {
        if (status == null) return PENDING;
        for (ModerationStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) return s;
        }
        return PENDING;
    }
}
