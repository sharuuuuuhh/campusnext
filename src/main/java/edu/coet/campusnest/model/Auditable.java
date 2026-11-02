package edu.coet.campusnest.model;

import java.time.Instant;

/**
 * Interface contract for timestamp tracking.
 */
public interface Auditable {
    Instant getCreatedAt();
    void setCreatedAt(Instant createdAt);
    Instant getUpdatedAt();
    void setUpdatedAt(Instant updatedAt);
}
