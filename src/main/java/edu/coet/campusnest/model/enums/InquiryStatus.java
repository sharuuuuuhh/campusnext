package edu.coet.campusnest.model.enums;

public enum InquiryStatus {
    NEW,
    READ,
    CLOSED;

    public static InquiryStatus fromString(String val) {
        if (val == null) return NEW;
        for (InquiryStatus s : values()) {
            if (s.name().equalsIgnoreCase(val.trim())) return s;
        }
        return NEW;
    }
}
