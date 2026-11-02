package edu.coet.campusnest.model.enums;

public enum RoomType {
    SHARED("Shared"),
    SINGLE("Single");

    private final String label;

    RoomType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RoomType fromString(String val) {
        if (val == null) return null;
        for (RoomType r : values()) {
            if (r.name().equalsIgnoreCase(val.trim()) || r.label.equalsIgnoreCase(val.trim())) {
                return r;
            }
        }
        return null;
    }
}
