package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Abstract base class for all persistent domain entities in CampusNest.
 * Demonstrates:
 * 1. Abstraction (abstract base class)
 * 2. Encapsulation (protected/private fields with accessors)
 * 3. Interface implementation (Multiple interfaces: Identifiable, Auditable, Validatable, Serializable)
 * 4. Method overriding (equals, hashCode, toString)
 */
public abstract class BaseEntity implements Identifiable<Long>, Auditable, Validatable, Serializable {
    private static final long serialVersionUID = 1L;

    protected Long id;
    protected Instant createdAt;
    protected Instant updatedAt;

    public BaseEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public BaseEntity(Long id) {
        this();
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public void validate() throws ValidationException {
        // Base entities default to no-op validation; sub-classes override to enforce domain rules
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
