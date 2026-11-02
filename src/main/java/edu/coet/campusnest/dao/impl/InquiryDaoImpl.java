package edu.coet.campusnest.dao.impl;

import edu.coet.campusnest.dao.InquiryDao;
import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.Inquiry;
import edu.coet.campusnest.model.enums.InquiryStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of InquiryDao.
 */
public class InquiryDaoImpl extends AbstractJdbcDao implements InquiryDao {

    @Override
    public Optional<Inquiry> findById(Long id) throws DatabaseOperationException {
        String sql = "SELECT i.id, i.pg_id, i.student_user_id, i.sender_name, i.sender_phone, " +
                     "i.message, i.status, i.created_at, p.pg_name " +
                     "FROM inquiries i JOIN pg_details p ON p.pg_id = i.pg_id WHERE i.id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToInquiry(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleSqlException("Failed to find inquiry by ID " + id, e);
        }
    }

    @Override
    public List<Inquiry> findByOwnerId(long ownerId) throws DatabaseOperationException {
        String sql = "SELECT i.id, i.pg_id, i.student_user_id, i.sender_name, i.sender_phone, " +
                     "i.message, i.status, i.created_at, p.pg_name " +
                     "FROM inquiries i JOIN pg_details p ON p.pg_id = i.pg_id " +
                     "WHERE p.owner_id = ? ORDER BY i.created_at DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Inquiry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToInquiry(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw handleSqlException("Failed to find inquiries for owner " + ownerId, e);
        }
    }

    @Override
    public List<Inquiry> findByStudentUserId(long studentUserId) throws DatabaseOperationException {
        String sql = "SELECT i.id, i.pg_id, i.student_user_id, i.sender_name, i.sender_phone, " +
                     "i.message, i.status, i.created_at, p.pg_name " +
                     "FROM inquiries i JOIN pg_details p ON p.pg_id = i.pg_id " +
                     "WHERE i.student_user_id = ? ORDER BY i.created_at DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, studentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Inquiry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToInquiry(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw handleSqlException("Failed to find inquiries for student " + studentUserId, e);
        }
    }

    @Override
    public List<Inquiry> findAll() throws DatabaseOperationException {
        String sql = "SELECT i.id, i.pg_id, i.student_user_id, i.sender_name, i.sender_phone, " +
                     "i.message, i.status, i.created_at, p.pg_name " +
                     "FROM inquiries i JOIN pg_details p ON p.pg_id = i.pg_id ORDER BY i.created_at DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Inquiry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToInquiry(rs));
            }
            return list;
        } catch (SQLException e) {
            throw handleSqlException("Failed to retrieve all inquiries", e);
        }
    }

    @Override
    public Inquiry save(Inquiry inquiry) throws DatabaseOperationException {
        String sql = "INSERT INTO inquiries (pg_id, student_user_id, sender_name, sender_phone, message, status) " +
                     "SELECT p.pg_id, ?, ?, ?, ?, 'NEW' " +
                     "FROM pg_details p JOIN pg_owners o ON o.owner_id = p.owner_id " +
                     "WHERE p.pg_id = ? AND p.is_visible = TRUE AND p.moderation_status = 'APPROVED' AND o.owner_status = 'APPROVED'";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, inquiry.getStudentUserId());
            ps.setString(2, inquiry.getSenderName());
            ps.setString(3, inquiry.getSenderPhone());
            ps.setString(4, inquiry.getMessage());
            ps.setLong(5, inquiry.getPgId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                return null; // Target PG not available/valid
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    inquiry.setId(keys.getLong(1));
                }
            }
            inquiry.setStatus(InquiryStatus.NEW);
            return inquiry;
        } catch (SQLException e) {
            throw handleSqlException("Failed to save student inquiry", e);
        }
    }

    @Override
    public boolean update(Inquiry inquiry) throws DatabaseOperationException {
        String sql = "UPDATE inquiries SET status = ? WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, inquiry.getStatus().name());
            ps.setLong(2, inquiry.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update inquiry", e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseOperationException {
        String sql = "DELETE FROM inquiries WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to delete inquiry", e);
        }
    }

    @Override
    public long countNewInquiries() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM inquiries WHERE status = 'NEW'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw handleSqlException("Failed to count new inquiries", e);
        }
    }

    private Inquiry mapRowToInquiry(ResultSet rs) throws SQLException {
        Inquiry i = new Inquiry();
        i.setId(rs.getLong("id"));
        i.setPgId(rs.getLong("pg_id"));
        i.setStudentUserId(rs.getObject("student_user_id") != null ? rs.getLong("student_user_id") : null);
        i.setSenderName(rs.getString("sender_name"));
        i.setSenderPhone(rs.getString("sender_phone"));
        i.setMessage(rs.getString("message"));
        i.setStatus(InquiryStatus.fromString(rs.getString("status")));
        i.setCreatedAt(getInstant(rs, "created_at"));
        i.setPgName(rs.getString("pg_name"));
        return i;
    }
}
