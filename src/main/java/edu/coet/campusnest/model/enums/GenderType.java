package edu.coet.campusnest.model.enums;

public enum GenderType {
    BOYS("Boys"),
    GIRLS("Girls");

    private final String label;

    GenderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static GenderType fromString(String val) {
        if (val == null) return null;
        for (GenderType g : values()) {
            if (g.name().equalsIgnoreCase(val.trim()) || g.label.equalsIgnoreCase(val.trim())) {
                return g;
            }
        }
        return null;
    }
}
