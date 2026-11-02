package edu.coet.campusnest.dao.impl;

import edu.coet.campusnest.dao.OwnerDao;
import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.OwnerUser;
import edu.coet.campusnest.model.enums.AccountStatus;
import edu.coet.campusnest.model.enums.OwnerStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of OwnerDao.
 */
public class OwnerDaoImpl extends AbstractJdbcDao implements OwnerDao {

    @Override
    public Optional<OwnerUser> findById(Long ownerId) throws DatabaseOperationException {
        String sql = "SELECT o.owner_id, o.user_id, o.demo_key, o.owner_name, o.contact_no, o.address, o.id_proof, " +
                     "o.proof_original_name, o.proof_media_type, o.proof_reviewed_at, o.owner_status, o.is_demo, o.created_at, " +
                     "u.email, u.password_hash, u.account_status " +
                     "FROM pg_owners o LEFT JOIN users u ON u.user_id = o.user_id WHERE o.owner_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToOwner(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleSqlException("Failed to find owner by ID " + ownerId, e);
        }
    }

    @Override
    public Optional<OwnerUser> findByUserId(long userId) throws DatabaseOperationException {
        String sql = "SELECT o.owner_id, o.user_id, o.demo_key, o.owner_name, o.contact_no, o.address, o.id_proof, " +
                     "o.proof_original_name, o.proof_media_type, o.proof_reviewed_at, o.owner_status, o.is_demo, o.created_at, " +
                     "u.email, u.password_hash, u.account_status " +
                     "FROM pg_owners o JOIN users u ON u.user_id = o.user_id WHERE o.user_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToOwner(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleSqlException("Failed to find owner by user ID " + userId, e);
        }
    }

    @Override
    public List<OwnerUser> findPendingQueue() throws DatabaseOperationException {
        String sql = "SELECT o.owner_id, o.user_id, o.demo_key, o.owner_name, o.contact_no, o.address, o.id_proof, " +
                     "o.proof_original_name, o.proof_media_type, o.proof_reviewed_at, o.owner_status, o.is_demo, o.created_at, " +
                     "u.email, u.password_hash, u.account_status " +
                     "FROM pg_owners o JOIN users u ON u.user_id = o.user_id " +
                     "WHERE o.owner_status = 'PENDING' ORDER BY o.created_at ASC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<OwnerUser> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToOwner(rs));
            }
            return list;
        } catch (SQLException e) {
            throw handleSqlException("Failed to retrieve pending owners", e);
        }
    }

    @Override
    public List<OwnerUser> findAllOrdered() throws DatabaseOperationException {
        String sql = "SELECT o.owner_id, o.user_id, o.demo_key, o.owner_name, o.contact_no, o.address, o.id_proof, " +
                     "o.proof_original_name, o.proof_media_type, o.proof_reviewed_at, o.owner_status, o.is_demo, o.created_at, " +
                     "u.email, u.password_hash, u.account_status " +
                     "FROM pg_owners o JOIN users u ON u.user_id = o.user_id " +
                     "ORDER BY FIELD(o.owner_status, 'PENDING', 'APPROVED', 'REJECTED'), o.created_at DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<OwnerUser> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToOwner(rs));
            }
            return list;
        } catch (SQLException e) {
            throw handleSqlException("Failed to retrieve ordered owner queue", e);
        }
    }

    @Override
    public boolean updateStatus(long ownerId, OwnerStatus status) throws DatabaseOperationException {
        String sql = "UPDATE pg_owners SET owner_status = ?, proof_reviewed_at = CURRENT_TIMESTAMP WHERE owner_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update owner status", e);
        }
    }

    @Override
    public long countPendingOwners() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM pg_owners WHERE owner_status = 'PENDING'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw handleSqlException("Failed to count pending owners", e);
        }
    }

    @Override
    public List<OwnerUser> findAll() throws DatabaseOperationException {
        return findAllOrdered();
    }

    @Override
    public OwnerUser save(OwnerUser owner) throws DatabaseOperationException {
        String sql = "INSERT INTO pg_owners (user_id, owner_name, contact_no, address, id_proof, " +
                     "proof_original_name, proof_media_type, owner_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, owner.getId()); // user_id
            ps.setString(2, owner.getName());
            ps.setString(3, owner.getPhoneNo());
            ps.setString(4, owner.getAddress());
            ps.setString(5, owner.getIdProofPath());
            ps.setString(6, owner.getProofOriginalName());
            ps.setString(7, owner.getProofMediaType());
            ps.setString(8, owner.getOwnerStatus() != null ? owner.getOwnerStatus().name() : "PENDING");

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    owner.setOwnerId(keys.getLong(1));
                }
            }
            return owner;
        } catch (SQLException e) {
            throw handleSqlException("Failed to save owner profile", e);
        }
    }

    @Override
    public boolean update(OwnerUser owner) throws DatabaseOperationException {
        String sql = "UPDATE pg_owners SET owner_name = ?, contact_no = ?, address = ? WHERE owner_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, owner.getName());
            ps.setString(2, owner.getPhoneNo());
            ps.setString(3, owner.getAddress());
            ps.setLong(4, owner.getOwnerId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update owner profile", e);
        }
    }

    @Override
    public boolean deleteById(Long ownerId) throws DatabaseOperationException {
        String sql = "DELETE FROM pg_owners WHERE owner_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to delete owner profile", e);
        }
    }

    private OwnerUser mapRowToOwner(ResultSet rs) throws SQLException {
        OwnerUser o = new OwnerUser();
        o.setOwnerId(rs.getLong("owner_id"));
        o.setId(rs.getObject("user_id") != null ? rs.getLong("user_id") : null);
        o.setDemoKey(rs.getString("demo_key"));
        o.setName(rs.getString("owner_name"));
        o.setPhoneNo(rs.getString("contact_no"));
        o.setAddress(rs.getString("address"));
        o.setIdProofPath(rs.getString("id_proof"));
        o.setProofOriginalName(rs.getString("proof_original_name"));
        o.setProofMediaType(rs.getString("proof_media_type"));
        o.setProofReviewedAt(getInstant(rs, "proof_reviewed_at"));
        o.setOwnerStatus(OwnerStatus.fromString(rs.getString("owner_status")));
        o.setDemo(rs.getBoolean("is_demo"));
        o.setCreatedAt(getInstant(rs, "created_at"));

        if (rs.getString("email") != null) {
            o.setEmail(rs.getString("email"));
            o.setPasswordHash(rs.getString("password_hash"));
            o.setAccountStatus(AccountStatus.fromString(rs.getString("account_status")));
        }
        return o;
    }
}
