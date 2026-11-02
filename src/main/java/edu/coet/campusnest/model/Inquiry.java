package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.enums.InquiryStatus;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain model representing a student inquiry sent to a PG listing.
 * Demonstrates:
 * - OOP Inheritance (extends BaseEntity)
 * - Encapsulation with validation
 */
public class Inquiry extends BaseEntity {
    private Long pgId;
    private Long studentUserId;
    private String senderName;
    private String senderPhone;
    private String message;
    private InquiryStatus status = InquiryStatus.NEW;
    private String pgName; // For display

    public Inquiry() {
        super();
    }

    public Inquiry(Long id, Long pgId, Long studentUserId, String senderName, String senderPhone, String message) {
        super(id);
        this.pgId = pgId;
        this.studentUserId = studentUserId;
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.message = message;
    }

    @Override
    public void validate() throws ValidationException {
        if (pgId == null || pgId < 1) {
            throw new ValidationException("A valid PG listing ID is required.");
        }
        if (senderName == null || senderName.trim().isBlank()) {
            throw new ValidationException("Sender name is required.");
        }
        if (senderPhone == null || senderPhone.trim().isBlank()) {
            throw new ValidationException("Sender phone number is required.");
        }
        if (message == null || message.trim().isBlank()) {
            throw new ValidationException("Inquiry message cannot be blank.");
        }
        if (message.length() > 2000) {
            throw new ValidationException("Inquiry message cannot exceed 2000 characters.");
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", getId());
        m.put("listingId", pgId);
        m.put("listing", pgName != null ? pgName : "");
        m.put("name", senderName);
        m.put("phone", senderPhone);
        m.put("message", message);
        m.put("status", status != null ? status.name() : "NEW");
        m.put("date", getCreatedAt() != null ? getCreatedAt().toString() : null);
        return m;
    }

    public Long getPgId() {
        return pgId;
    }

    public void setPgId(Long pgId) {
        this.pgId = pgId;
    }

    public Long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public InquiryStatus getStatus() {
        return status;
    }

    public void setStatus(InquiryStatus status) {
        this.status = status;
    }

    public String getPgName() {
        return pgName;
    }

    public void setPgName(String pgName) {
        this.pgName = pgName;
    }
}
