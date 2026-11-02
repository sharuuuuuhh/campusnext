package edu.coet.campusnest.model.enums;

public enum AccountStatus {
    ACTIVE,
    DISABLED;

    public static AccountStatus fromString(String status) {
        if (status == null) return ACTIVE;
        for (AccountStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) return s;
        }
        return ACTIVE;
    }
}
