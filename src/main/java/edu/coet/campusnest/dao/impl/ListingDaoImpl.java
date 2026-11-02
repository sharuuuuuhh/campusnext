package edu.coet.campusnest.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.coet.campusnest.Json;
import edu.coet.campusnest.dao.ListingDao;
import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.ListingFilterCriteria;
import edu.coet.campusnest.model.PgListing;
import edu.coet.campusnest.model.enums.GenderType;
import edu.coet.campusnest.model.enums.ModerationStatus;
import edu.coet.campusnest.model.enums.RoomType;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ListingDao.
 * Demonstrates:
 * - Method Overloading (findListings with criteria vs findListings by ownerId)
 * - Safe parameterized SQL queries
 * - Mapping relational rows to domain objects
 */
public class ListingDaoImpl extends AbstractJdbcDao implements ListingDao {

    private static final String SELECT_COLUMNS =
            "p.pg_id, p.owner_id, p.demo_key, p.pg_name, p.gender_type, p.monthly_rent, p.room_type, " +
            "p.food_facility, p.total_beds, p.vacant_beds, p.dist_from_coet, p.pg_address, " +
            "p.amenities_json, p.description, p.moderation_status, p.is_visible, p.is_demo, p.created_at, p.updated_at, " +
            "o.owner_name, o.contact_no AS owner_phone ";

    @Override
    public Optional<PgListing> findById(Long id) throws DatabaseOperationException {
        return findById(id, false);
    }

    @Override
    public Optional<PgListing> findById(long id, boolean adminView) throws DatabaseOperationException {
        StringBuilder sql = new StringBuilder("SELECT ").append(SELECT_COLUMNS)
                .append("FROM pg_details p JOIN pg_owners o ON o.owner_id = p.owner_id ")
                .append("LEFT JOIN users u ON o.user_id = u.user_id ")
                .append("WHERE p.pg_id = ? ");
        if (!adminView) {
            sql.append("AND p.is_visible = TRUE AND p.moderation_status = 'APPROVED' ")
               .append("AND o.owner_status = 'APPROVED' AND (o.user_id IS NULL OR u.account_status = 'ACTIVE')");
        }
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToListing(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleSqlException("Failed to find listing by ID " + id, e);
        }
    }

    // Method Overload 1: Find listings based on criteria
    @Override
    public List<PgListing> findListings(ListingFilterCriteria criteria) throws DatabaseOperationException {
        StringBuilder sql = new StringBuilder("SELECT ").append(SELECT_COLUMNS)
                .append("FROM pg_details p JOIN pg_owners o ON o.owner_id = p.owner_id ")
                .append("LEFT JOIN users u ON o.user_id = u.user_id WHERE ");

        boolean admin = criteria != null && criteria.isAdminView();
        if (admin) {
            sql.append("1=1 ");
        } else {
            sql.append("p.is_visible = TRUE AND p.moderation_status = 'APPROVED' ")
               .append("AND o.owner_status = 'APPROVED' AND (o.user_id IS NULL OR u.account_status = 'ACTIVE') ");
        }

        List<Object> params = new ArrayList<>();
        if (criteria != null) {
            if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
                sql.append("AND (p.pg_name LIKE ? OR p.pg_address LIKE ?) ");
                String like = "%" + criteria.getQuery().trim() + "%";
                params.add(like);
                params.add(like);
            }
            if (criteria.getGender() != null) {
                sql.append("AND p.gender_type = ? ");
                params.add(criteria.getGender().name());
            }
            if (criteria.getMaxRent() != null && criteria.getMaxRent() > 0) {
                sql.append("AND p.monthly_rent <= ? ");
                params.add(criteria.getMaxRent());
            }
            if (criteria.getRoomType() != null) {
                sql.append("AND p.room_type = ? ");
                params.add(criteria.getRoomType().getLabel());
            }
            if (Boolean.TRUE.equals(criteria.getFoodFacility())) {
                sql.append("AND p.food_facility = TRUE ");
            }
            if (criteria.getMaxDistance() != null && criteria.getMaxDistance() >= 0) {
                sql.append("AND p.dist_from_coet <= ? ");
                params.add(criteria.getMaxDistance());
            }

            String sort = criteria.getSortBy();
            if ("rent-asc".equalsIgnoreCase(sort)) {
                sql.append("ORDER BY p.monthly_rent ASC, p.dist_from_coet ASC");
            } else if ("distance".equalsIgnoreCase(sort)) {
                sql.append("ORDER BY p.dist_from_coet ASC, p.monthly_rent ASC");
            } else {
                sql.append("ORDER BY p.is_demo DESC, p.created_at DESC");
            }
        } else {
            sql.append("ORDER BY p.is_demo DESC, p.created_at DESC");
        }

        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<PgListing> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToListing(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw handleSqlException("Failed to find listings with criteria", e);
        }
    }

    // Method Overload 2: Find listings for a specific owner
    @Override
    public List<PgListing> findListings(long ownerId) throws DatabaseOperationException {
        String sql = "SELECT " + SELECT_COLUMNS +
                     "FROM pg_details p JOIN pg_owners o ON o.owner_id = p.owner_id " +
                     "WHERE p.owner_id = ? ORDER BY p.updated_at DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PgListing> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToListing(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw handleSqlException("Failed to find listings for owner " + ownerId, e);
        }
    }

    @Override
    public List<PgListing> findAll() throws DatabaseOperationException {
        ListingFilterCriteria c = new ListingFilterCriteria();
        c.setAdminView(true);
        return findListings(c);
    }

    @Override
    public PgListing save(PgListing listing) throws DatabaseOperationException {
        String sql = "INSERT INTO pg_details (owner_id, pg_name, gender_type, monthly_rent, room_type, " +
                     "food_facility, total_beds, vacant_beds, dist_from_coet, pg_address, amenities_json, description, " +
                     "moderation_status, is_visible, is_demo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', TRUE, FALSE)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String amenitiesJson = serializeAmenities(listing.getAmenities());

            ps.setLong(1, listing.getOwnerId());
            ps.setString(2, listing.getPgName());
            ps.setString(3, listing.getGenderType().name());
            ps.setBigDecimal(4, listing.getMonthlyRent());
            ps.setString(5, listing.getRoomType().getLabel());
            ps.setBoolean(6, listing.isFoodFacility());
            ps.setInt(7, listing.getTotalBeds());
            ps.setInt(8, listing.getVacantBeds());
            ps.setDouble(9, listing.getDistFromCoet());
            ps.setString(10, listing.getPgAddress());
            ps.setString(11, amenitiesJson);
            ps.setString(12, listing.getDescription());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    listing.setId(keys.getLong(1));
                }
            }
            listing.setModerationStatus(ModerationStatus.PENDING);
            listing.setVisible(true);
            return listing;
        } catch (SQLException e) {
            throw handleSqlException("Failed to insert PG listing", e);
        }
    }

    @Override
    public boolean update(PgListing listing) throws DatabaseOperationException {
        String sql = "UPDATE pg_details SET pg_name = ?, gender_type = ?, monthly_rent = ?, room_type = ?, " +
                     "food_facility = ?, total_beds = ?, vacant_beds = ?, dist_from_coet = ?, pg_address = ?, " +
                     "amenities_json = ?, description = ? WHERE pg_id = ? AND owner_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String amenitiesJson = serializeAmenities(listing.getAmenities());

            ps.setString(1, listing.getPgName());
            ps.setString(2, listing.getGenderType().name());
            ps.setBigDecimal(3, listing.getMonthlyRent());
            ps.setString(4, listing.getRoomType().getLabel());
            ps.setBoolean(5, listing.isFoodFacility());
            ps.setInt(6, listing.getTotalBeds());
            ps.setInt(7, listing.getVacantBeds());
            ps.setDouble(8, listing.getDistFromCoet());
            ps.setString(9, listing.getPgAddress());
            ps.setString(10, amenitiesJson);
            ps.setString(11, listing.getDescription());
            ps.setLong(12, listing.getId());
            ps.setLong(13, listing.getOwnerId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update PG listing", e);
        }
    }

    @Override
    public boolean updateVacancy(long pgId, int vacantBeds, long ownerId) throws DatabaseOperationException {
        String sql = "UPDATE pg_details SET vacant_beds = ? WHERE pg_id = ? AND owner_id = ? AND ? <= total_beds";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vacantBeds);
            ps.setLong(2, pgId);
            ps.setLong(3, ownerId);
            ps.setInt(4, vacantBeds);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update vacancy", e);
        }
    }

    @Override
    public boolean updateVisibility(long pgId, boolean visible) throws DatabaseOperationException {
        String sql = "UPDATE pg_details SET is_visible = ? WHERE pg_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, visible);
            ps.setLong(2, pgId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update listing visibility", e);
        }
    }

    @Override
    public boolean updateModerationStatus(long pgId, ModerationStatus status) throws DatabaseOperationException {
        String sql = "UPDATE pg_details SET moderation_status = ?, is_visible = ? WHERE pg_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setBoolean(2, status == ModerationStatus.APPROVED);
            ps.setLong(3, pgId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to update listing moderation status", e);
        }
    }

    @Override
    public boolean deleteById(Long id) throws DatabaseOperationException {
        String sql = "DELETE FROM pg_details WHERE pg_id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handleSqlException("Failed to delete PG listing", e);
        }
    }

    @Override
    public long countVisibleApprovedListings() throws DatabaseOperationException {
        String sql = "SELECT COUNT(*) FROM pg_details WHERE is_visible = TRUE AND moderation_status = 'APPROVED'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw handleSqlException("Failed to count visible listings", e);
        }
    }

    private PgListing mapRowToListing(ResultSet rs) throws SQLException {
        PgListing p = new PgListing();
        p.setId(rs.getLong("pg_id"));
        p.setOwnerId(rs.getLong("owner_id"));
        p.setDemoKey(rs.getString("demo_key"));
        p.setPgName(rs.getString("pg_name"));
        p.setGenderType(GenderType.fromString(rs.getString("gender_type")));
        p.setMonthlyRent(rs.getBigDecimal("monthly_rent"));
        p.setRoomType(RoomType.fromString(rs.getString("room_type")));
        p.setFoodFacility(rs.getBoolean("food_facility"));
        p.setTotalBeds(rs.getInt("total_beds"));
        p.setVacantBeds(rs.getInt("vacant_beds"));
        p.setDistFromCoet(rs.getBigDecimal("dist_from_coet").doubleValue());
        p.setPgAddress(rs.getString("pg_address"));
        p.setAmenities(deserializeAmenities(rs.getString("amenities_json")));
        p.setDescription(rs.getString("description"));
        p.setModerationStatus(ModerationStatus.fromString(rs.getString("moderation_status")));
        p.setVisible(rs.getBoolean("is_visible"));
        p.setDemo(rs.getBoolean("is_demo"));
        p.setCreatedAt(getInstant(rs, "created_at"));
        p.setUpdatedAt(getInstant(rs, "updated_at"));

        p.setOwnerName(rs.getString("owner_name"));
        p.setOwnerPhone(rs.getString("owner_phone"));
        return p;
    }

    private List<String> deserializeAmenities(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return Json.MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String serializeAmenities(List<String> amenities) {
        if (amenities == null) return "[]";
        try {
            return Json.MAPPER.writeValueAsString(amenities);
        } catch (Exception e) {
            return "[]";
        }
    }
}
