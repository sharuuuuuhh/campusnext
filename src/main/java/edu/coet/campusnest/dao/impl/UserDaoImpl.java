package edu.coet.campusnest.dao.impl;

import edu.coet.campusnest.dao.UserDao;
import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.AdminUser;
import edu.coet.campusnest.model.OwnerUser;
import edu.coet.campusnest.model.StudentUser;
import edu.coet.campusnest.model.User;
import edu.coet.campusnest.model.enums.AccountStatus;
import edu.coet.campusnest.model.enums.OwnerStatus;
import edu.coet.campusnest.model.enums.UserRole;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of the UserDao interface.
 * Demonstrates:
 * - Implementation of interface (Polymorphism)
 * - Polymorphic entity instantiation (User -> StudentUser / OwnerUser / AdminUser)
 */
public class UserDaoImpl extends AbstractJdbcDao implements UserDao {

    @Override
    public Optional<User> findById(Long id) throws DatabaseOperationException {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone_no, u.password_hash, u.role, u.account_status, u.created_at, " +
                     "o.owner_id, o.address, o.owner_status, o.id_proof, o.proof_original_name, o.proof_media_type, o.proof_reviewed_at " +
                     "FROM users u LEFT JOIN pg_owners o ON o.user_id = u.user_id WHERE u.user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleSqlException("Failed to find user by ID " + id, e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) throws DatabaseOperationException {
        if (email == null) return Optional.empty();
        String sql = "SELECT u.user_id, u.name, u.email, u.phone_no, u.password_hash, u.role, u.account_status, u.created_at, " +
                     "o.owner_id, o.address, o.owner_status, o.id_proof, o.proof_original_name, o.proof_media_type, o.proof_reviewed_at " +
                     "FROM users u LEFT JOIN pg_owners o ON o.user_id = u.user_id WHERE u.email = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleSqlException("Failed to find user by email", e);
        }
    }

    @Override
    public boolean emailExists(String email) throws DatabaseOperationException {
        if (email == null) return false;
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw handleSqlException("Failed to check if email exists", e);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseOperationException {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone_no, u.password_hash, u.role, u.account_status, u.created_at, " +
                     "o.owner_id, o.address, o.owner_status, o.id_proof, o.proof_original_name, o.proof_media_type, o.proof_reviewed_at " +
                     "FROM users u LEFT JOIN pg_owners o ON o.user_id = u.user_id ORDER BY u.created_at DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
            return list;
        } catch (SQLException e) {
            throw handleSqlException("Failed to retrieve all users", e);
        }
    }

    @Override
    public User save(User user) throws DatabaseOperationException {
        String sql = "INSERT INTO users (name, email, phone_no, password_hash, role, account_status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().toLowerCase());
            ps.setString(3, user.getPhoneNo());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getAccountStatus() != null ? user.getAccountStatus().name() : "ACTIVE");

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw handleSqlException("Failed to save user", e);
        }
    }

    @Override
    public boolean update(User user) throws DatabaseOperationException {
        String sql = "UPDATE users SET name = ?, phone_no = ?, account_status = ? WHERE user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getPhoneNo());
            ps.setString(3, user.getAccountStatus().name());
            ps.setLong(4, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update user", e);
        }
    }

    @Override
    public boolean updatePassword(long userId, String newPasswordHash) throws DatabaseOperationException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update password", e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseOperationException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to delete user", e);
        }
    }

    // Factory method demonstrating polymorphic instantiation
    private User mapRowToUser(ResultSet rs) throws SQLException {
        String roleStr = rs.getString("role");
        UserRole role = UserRole.fromString(roleStr);

        User user;
        if (role == UserRole.STUDENT) {
            user = new StudentUser();
        } else if (role == UserRole.OWNER) {
            OwnerUser owner = new OwnerUser();
            owner.setOwnerId(rs.getLong("owner_id"));
            owner.setAddress(rs.getString("address"));
            owner.setOwnerStatus(OwnerStatus.fromString(rs.getString("owner_status")));
            owner.setIdProofPath(rs.getString("id_proof"));
            owner.setProofOriginalName(rs.getString("proof_original_name"));
            owner.setProofMediaType(rs.getString("proof_media_type"));
            owner.setProofReviewedAt(getInstant(rs, "proof_reviewed_at"));
            user = owner;
        } else {
            user = new AdminUser();
        }

        user.setId(rs.getLong("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNo(rs.getString("phone_no"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setAccountStatus(AccountStatus.fromString(rs.getString("account_status")));
        user.setCreatedAt(getInstant(rs, "created_at"));
        return user;
    }
}
