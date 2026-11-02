package edu.coet.campusnest.model;

import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.enums.UserRole;
import java.util.Map;

/**
 * Represents a Student user in CampusNest.
 * Demonstrates:
 * - OOP Inheritance (extends User)
 * - Method Overriding (Polymorphism: getDashboardRole, hasAdminPrivileges, toSessionMap, validate)
 */
public class StudentUser extends User {
    private String collegeName = "College of Engineering Thalassery";

    public StudentUser() {
        super();
        setRole(UserRole.STUDENT);
    }

    public StudentUser(Long id, String name, String email, String phoneNo, String passwordHash) {
        super(id, name, email, phoneNo, passwordHash, UserRole.STUDENT);
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    @Override
    public String getDashboardRole() {
        return "STUDENT";
    }

    @Override
    public boolean hasAdminPrivileges() {
        return false;
    }

    @Override
    public Map<String, Object> toSessionMap() {
        Map<String, Object> map = baseSessionMap();
        map.put("college", collegeName);
        return map;
    }

    @Override
    public void validate() throws ValidationException {
        super.validate();
    }
}
