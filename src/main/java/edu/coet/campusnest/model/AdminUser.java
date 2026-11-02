package edu.coet.campusnest.model;

import edu.coet.campusnest.model.enums.UserRole;
import java.util.Map;

/**
 * Represents a System Administrator in CampusNest.
 * Demonstrates:
 * - OOP Inheritance (extends User)
 * - Method Overriding (Polymorphism: getDashboardRole, hasAdminPrivileges, toSessionMap)
 */
public class AdminUser extends User {

    public AdminUser() {
        super();
        setRole(UserRole.ADMIN);
    }

    public AdminUser(Long id, String name, String email, String phoneNo, String passwordHash) {
        super(id, name, email, phoneNo, passwordHash, UserRole.ADMIN);
    }

    @Override
    public String getDashboardRole() {
        return "ADMIN";
    }

    @Override
    public boolean hasAdminPrivileges() {
        return true;
    }

    @Override
    public Map<String, Object> toSessionMap() {
        return baseSessionMap();
    }
}
