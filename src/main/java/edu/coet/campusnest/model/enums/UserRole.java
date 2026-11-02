package edu.coet.campusnest.model.enums;

public enum UserRole {
    STUDENT,
    OWNER,
    ADMIN;

    public static UserRole fromString(String role) {
        if (role == null) return null;
        for (UserRole r : values()) {
            if (r.name().equalsIgnoreCase(role.trim())) {
                return r;
            }
        }
        return null;
    }
}
