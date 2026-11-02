package edu.coet.campusnest.dao.impl;

import edu.coet.campusnest.dao.FeedbackDao;
import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.Feedback;
import edu.coet.campusnest.model.enums.FeedbackStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of FeedbackDao.
 */
public class FeedbackDaoImpl extends AbstractJdbcDao implements FeedbackDao {

    @Override
    public Optional<Feedback> findById(Long id) throws DatabaseOperationException {
        String sql = "SELECT id, user_id, name, email, message, status, created_at FROM feedback WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFeedback(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleSqlException("Failed to find feedback by ID " + id, e);
        }
    }

    @Override
    public List<Feedback> findAllRecent() throws DatabaseOperationException {
        String sql = "SELECT id, user_id, name, email, message, status, created_at FROM feedback ORDER BY created_at DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Feedback> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToFeedback(rs));
            }
            return list;
        } catch (SQLException e) {
            throw handleSqlException("Failed to retrieve feedback list", e);
        }
    }

    @Override
    public List<Feedback> findAll() throws DatabaseOperationException {
        return findAllRecent();
    }

    @Override
    public Feedback save(Feedback feedback) throws DatabaseOperationException {
        String sql = "INSERT INTO feedback (user_id, name, email, message, status) VALUES (?, ?, ?, ?, 'NEW')";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, feedback.getUserId());
            ps.setString(2, feedback.getName());
            ps.setString(3, feedback.getEmail());
            ps.setString(4, feedback.getMessage());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    feedback.setId(keys.getLong(1));
                }
            }
            feedback.setStatus(FeedbackStatus.NEW);
            return feedback;
        } catch (SQLException e) {
            throw handleSqlException("Failed to save feedback", e);
        }
    }

    @Override
    public boolean update(Feedback feedback) throws DatabaseOperationException {
        String sql = "UPDATE feedback SET status = ? WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, feedback.getStatus().name());
            ps.setLong(2, feedback.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update feedback", e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseOperationException {
        String sql = "DELETE FROM feedback WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to delete feedback", e);
        }
    }

    @Override
    public long countNewFeedback() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM feedback WHERE status = 'NEW'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw handleSqlException("Failed to count new feedback", e);
        }
    }

    private Feedback mapRowToFeedback(ResultSet rs) throws SQLException {
        Feedback f = new Feedback();
        f.setId(rs.getLong("id"));
        f.setUserId(rs.getObject("user_id") != null ? rs.getLong("user_id") : null);
        f.setName(rs.getString("name"));
        f.setEmail(rs.getString("email"));
        f.setMessage(rs.getString("message"));
        f.setStatus(FeedbackStatus.fromString(rs.getString("status")));
        f.setCreatedAt(getInstant(rs, "created_at"));
        return f;
    }
}
