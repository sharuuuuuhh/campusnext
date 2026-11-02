package edu.coet.campusnest.service.impl;

import edu.coet.campusnest.Passwords;
import edu.coet.campusnest.dao.OwnerDao;
import edu.coet.campusnest.dao.UserDao;
import edu.coet.campusnest.exception.AuthenticationException;
import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.DuplicateResourceException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.OwnerUser;
import edu.coet.campusnest.model.StudentUser;
import edu.coet.campusnest.model.User;
import edu.coet.campusnest.model.enums.AccountStatus;
import edu.coet.campusnest.model.enums.OwnerStatus;
import edu.coet.campusnest.service.AuthService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of AuthService.
 * Demonstrates:
 * - OOP Encapsulation (Private DAO dependencies injected via constructor)
 * - Polymorphism (implements AuthService)
 * - Custom Exception handling
 */
public class AuthServiceImpl implements AuthService {
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of(
            "application/pdf",
            "application/x-pdf",
            "image/jpeg",
            "image/jpg",
            "image/pjpeg",
            "image/png",
            "image/x-png",
            "image/webp"
    );

    private final UserDao userDao;
    private final OwnerDao ownerDao;

    public AuthServiceImpl(UserDao userDao, OwnerDao ownerDao) {
        this.userDao = userDao;
        this.ownerDao = ownerDao;
    }

    @Override
    public User authenticate(String email, String password) throws AuthenticationException, ValidationException, CampusNestException {
        if (email == null || email.trim().isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Email and password are required.");
        }
        Optional<User> userOpt = userDao.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Email or password is incorrect.");
        }
        User user = userOpt.get();
        if (!Passwords.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Email or password is incorrect.");
        }
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new AuthenticationException("This account has been disabled. Please contact admin.");
        }
        return user;
    }

    @Override
    public StudentUser registerStudent(String name, String email, String phone, String password)
            throws ValidationException, DuplicateResourceException, CampusNestException {
        if (name == null || name.trim().isBlank() || email == null || email.trim().isBlank()) {
            throw new ValidationException("Please provide your name and email.");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        if (userDao.emailExists(email)) {
            throw new DuplicateResourceException("An account already uses that email address.");
        }

        StudentUser student = new StudentUser();
        student.setName(name.trim());
        student.setEmail(email.trim().toLowerCase(Locale.ROOT));
        student.setPhoneNo(phone != null && !phone.isBlank() ? phone.trim() : null);
        student.setPasswordHash(Passwords.hash(password));
        student.setAccountStatus(AccountStatus.ACTIVE);

        student.validate();
        userDao.save(student);
        return student;
    }

    @Override
    public OwnerUser registerOwner(String name, String email, String phone, String address, String password,
                                  String proofOriginalName, String proofMediaType, InputStream proofInputStream,
                                  long proofSize, String storageDir)
            throws ValidationException, DuplicateResourceException, CampusNestException {
        if (name == null || name.trim().isBlank() || email == null || email.trim().isBlank() ||
            phone == null || phone.trim().isBlank() || address == null || address.trim().isBlank()) {
            throw new ValidationException("Please fill out all owner profile details.");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        if (proofInputStream == null || proofSize <= 0) {
            throw new ValidationException("A valid identity proof file (PDF, JPG, or PNG) is required.");
        }

        String originalName = (proofOriginalName != null) ? proofOriginalName.trim() : "";
        String ext = "";
        String lower = originalName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) ext = ".pdf";
        else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ext = ".jpg";
        else if (lower.endsWith(".png")) ext = ".png";
        else if (lower.endsWith(".webp")) ext = ".webp";

        String cleanType = (proofMediaType != null) ? proofMediaType.split(";")[0].trim().toLowerCase(Locale.ROOT) : "";
        if (cleanType.isBlank() || cleanType.equals("application/octet-stream")) {
            if (ext.equals(".pdf")) cleanType = "application/pdf";
            else if (ext.equals(".jpg")) cleanType = "image/jpeg";
            else if (ext.equals(".png")) cleanType = "image/png";
            else if (ext.equals(".webp")) cleanType = "image/webp";
        }

        if (ext.isBlank() || !ALLOWED_MEDIA_TYPES.contains(cleanType)) {
            throw new ValidationException("Identity proof must be a valid PDF, JPG, or PNG document.");
        }

        if (userDao.emailExists(email)) {
            throw new DuplicateResourceException("An account already uses that email address.");
        }

        // Save physical document securely
        Path baseDir = Path.of(storageDir != null ? storageDir :
                Path.of(System.getProperty("user.dir"), "campusnest-private", "owner-proofs").toString())
                .toAbsolutePath().normalize();
        String storedFilename = UUID.randomUUID().toString() + ext;
        Path targetPath = baseDir.resolve(storedFilename).normalize();

        try {
            Files.createDirectories(baseDir);
            Files.copy(proofInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new CampusNestException("Failed to store identity proof file.", e, 500);
        }

        OwnerUser owner = new OwnerUser();
        owner.setName(name.trim());
        owner.setEmail(email.trim().toLowerCase(Locale.ROOT));
        owner.setPhoneNo(phone.trim());
        owner.setAddress(address.trim());
        owner.setPasswordHash(Passwords.hash(password));
        owner.setAccountStatus(AccountStatus.ACTIVE);
        owner.setOwnerStatus(OwnerStatus.PENDING);
        owner.setIdProofPath(storedFilename);
        owner.setProofOriginalName(originalName.substring(Math.max(0, originalName.length() - 180)));
        owner.setProofMediaType(cleanType.equals("image/jpg") || cleanType.equals("image/pjpeg") ? "image/jpeg" : cleanType);

        try {
            owner.validate();
            // Save user account first
            userDao.save(owner);
            // Save owner record linked to user
            ownerDao.save(owner);
            return owner;
        } catch (Exception ex) {
            // Rollback stored file if database write fails
            try { Files.deleteIfExists(targetPath); } catch (Exception ignored) {}
            if (ex instanceof CampusNestException cne) throw cne;
            throw new CampusNestException("Owner registration failed: " + ex.getMessage(), ex, 500);
        }
    }
}
