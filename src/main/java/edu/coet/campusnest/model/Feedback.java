package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.enums.FeedbackStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Domain model representing student/user feedback or bug reports.
 * Demonstrates:
 * - OOP Inheritance (extends BaseEntity)
 * - OOP Encapsulation with validation rules
 */
public class Feedback extends BaseEntity {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Long userId;
    private String name;
    private String email;
    private String message;
    private FeedbackStatus status = FeedbackStatus.NEW;

    public Feedback() {
        super();
    }

    public Feedback(Long id, Long userId, String name, String email, String message) {
        super(id);
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.message = message;
    }

    @Override
    public void validate() throws ValidationException {
        if (name == null || name.trim().isBlank()) {
            throw new ValidationException("Feedback submitter name is required.");
        }
        if (email != null && !email.trim().isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Please provide a valid contact email address.");
        }
        if (message == null || message.trim().isBlank()) {
            throw new ValidationException("Feedback message cannot be blank.");
        }
        if (message.length() > 2000) {
            throw new ValidationException("Feedback message cannot exceed 2000 characters.");
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", getId());
        m.put("name", name);
        m.put("email", email != null ? email : "");
        m.put("message", message);
        m.put("status", status != null ? status.name() : "NEW");
        m.put("date", getCreatedAt() != null ? getCreatedAt().toString() : null);
        return m;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public void setStatus(FeedbackStatus status) {
        this.status = status;
    }
}
