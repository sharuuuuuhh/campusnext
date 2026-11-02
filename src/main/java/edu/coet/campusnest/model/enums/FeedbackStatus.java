package edu.coet.campusnest.model.enums;

public enum FeedbackStatus {
    NEW,
    REVIEWED,
    CLOSED;

    public static FeedbackStatus fromString(String val) {
        if (val == null) return NEW;
        for (FeedbackStatus s : values()) {
            if (s.name().equalsIgnoreCase(val.trim())) return s;
        }
        return NEW;
    }
}
