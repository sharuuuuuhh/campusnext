package edu.coet.campusnest.service;

import edu.coet.campusnest.exception.AuthenticationException;
import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.DuplicateResourceException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.OwnerUser;
import edu.coet.campusnest.model.StudentUser;
import edu.coet.campusnest.model.User;
import java.io.InputStream;

/**
 * Service interface for authentication and registration workflows.
 * Demonstrates Service Layer Abstraction.
 */
public interface AuthService {
    User authenticate(String email, String password) throws AuthenticationException, ValidationException, CampusNestException;
    StudentUser registerStudent(String name, String email, String phone, String password) throws ValidationException, DuplicateResourceException, CampusNestException;
    OwnerUser registerOwner(String name, String email, String phone, String address, String password,
                            String proofOriginalName, String proofMediaType, InputStream proofInputStream, long proofSize, String storageDir)
            throws ValidationException, DuplicateResourceException, CampusNestException;
}
