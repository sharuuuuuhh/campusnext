package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.enums.AccountStatus;
import edu.coet.campusnest.model.enums.UserRole;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract class representing a generic user in CampusNest.
 * Demonstrates:
 * - OOP Inheritance (Base class for StudentUser, OwnerUser, AdminUser)
 * - OOP Abstraction (Abstract methods getDashboardRole(), hasAdminPrivileges(), toSessionMap())
 * - OOP Encapsulation (Private fields with validated getters and setters)
 * - Polymorphism (Dynamic method dispatch for role behavior)
 */
public abstract class User extends BaseEntity {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private String name;
    private String email;
    private String phoneNo;
    private String passwordHash;
    private UserRole role;
    private AccountStatus accountStatus;

    public User() {
        super();
        this.accountStatus = AccountStatus.ACTIVE;
    }

    public User(Long id, String name, String email, String phoneNo, String passwordHash, UserRole role) {
        super(id);
        this.name = name;
        this.email = email;
        this.phoneNo = phoneNo;
        this.passwordHash = passwordHash;
        this.role = role;
        this.accountStatus = AccountStatus.ACTIVE;
    }

    // Abstract methods to be overridden by subclasses (Polymorphism)
    public abstract String getDashboardRole();
    public abstract boolean hasAdminPrivileges();
    public abstract Map<String, Object> toSessionMap();

    // Concrete domain method
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    @Override
    public void validate() throws ValidationException {
        if (name == null || name.trim().isBlank()) {
            throw new ValidationException("User full name is required.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("A valid email address is required.");
        }
        if (phoneNo != null && !phoneNo.trim().isBlank()) {
            String cleanPhone = phoneNo.trim().replaceAll("[\\s-]", "");
            if (cleanPhone.length() < 7 || cleanPhone.length() > 16) {
                throw new ValidationException("Contact number must be between 7 and 16 digits.");
            }
        }
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
        this.email = (email != null) ? email.trim().toLowerCase() : null;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    protected void setRole(UserRole role) {
        this.role = role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    protected Map<String, Object> baseSessionMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", getId());
        map.put("name", getName());
        map.put("email", getEmail());
        map.put("role", getRole() != null ? getRole().name() : null);
        map.put("status", getAccountStatus() != null ? getAccountStatus().name() : "ACTIVE");
        return map;
    }
}
