package dao.impl;

import dao.DBConnection;
import dao.interfaces.ResourceDAO;
import model.resource.*;
import model.user.Student;

import java.sql.*;
import java.util.*;

// ResourceDAOImpl handles JDBC persistence for resources with full Student & Category joins.
public class ResourceDAOImpl implements ResourceDAO {

    private static final String SELECT_RESOURCE_WITH_DETAILS = """
            SELECT r.*,
                   s.Name AS OwnerName, s.Email AS OwnerEmail, s.Phone AS OwnerPhone, s.Dept AS OwnerDept, s.Suspended AS OwnerSuspended,
                   c.MainType, c.SubType, c.Description AS CatDesc
            FROM Resource r
            LEFT JOIN Student s ON r.OwnerId = s.SRN
            LEFT JOIN Category c ON r.CategoryId = c.CategoryId
            """;

    @Override
    public void save(Resource resource) {
        String sql = "INSERT INTO Resource (Title, Description, ItemCondition, Status, ListingType, Price, OwnerId, CategoryId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, resource.getTitle());
            ps.setString(2, resource.getDescription());
            ps.setString(3, resource.getCondition());
            ps.setString(4, resource.getStatus().name());
            ps.setString(5, resource.getListingType().name());
            ps.setDouble(6, resource.getPrice());
            ps.setString(7, resource.getOwner() != null ? resource.getOwner().getId() : "");
            ps.setInt(8, resource.getCategory() != null ? resource.getCategory().getCategoryId() : 1);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save resource: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Resource> findById(int id) {
        String sql = SELECT_RESOURCE_WITH_DETAILS + " WHERE r.ResourceId = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResource(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find resource by id " + id, e);
        }

        return Optional.empty();
    }

    @Override
    public List<Resource> findAvailableResources() {
        List<Resource> resources = new ArrayList<>();
        String sql = SELECT_RESOURCE_WITH_DETAILS + " WHERE r.Status = 'AVAILABLE' ORDER BY r.ResourceId DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                resources.add(mapResource(rs));
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to retrieve available resources", e);
        }

        return resources;
    }

    @Override
    public List<Resource> findAvailableResourcesExcludingOwner(String ownerId) {
        List<Resource> resources = new ArrayList<>();
        String sql = SELECT_RESOURCE_WITH_DETAILS + " WHERE r.Status = 'AVAILABLE' AND r.OwnerId <> ? ORDER BY r.ResourceId DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResource(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to retrieve available resources for buyer", e);
        }

        return resources;
    }

    @Override
    public List<Resource> findReviewableResourcesByStudent(String studentId) {
        List<Resource> resources = new ArrayList<>();
        String sql = """
                SELECT DISTINCT r.*,
                       s.Name AS OwnerName, s.Email AS OwnerEmail, s.Phone AS OwnerPhone, s.Dept AS OwnerDept, s.Suspended AS OwnerSuspended,
                       c.MainType, c.SubType, c.Description AS CatDesc
                FROM Resource r
                LEFT JOIN Student s ON r.OwnerId = s.SRN
                LEFT JOIN Category c ON r.CategoryId = c.CategoryId
                JOIN BuySellTransaction b ON b.ResourceId = r.ResourceId
                JOIN `Transaction` t ON t.TransactionId = b.TransactionId
                WHERE b.BuyerId = ? AND t.Status = 'COMPLETED'
                UNION
                SELECT DISTINCT r.*,
                       s.Name AS OwnerName, s.Email AS OwnerEmail, s.Phone AS OwnerPhone, s.Dept AS OwnerDept, s.Suspended AS OwnerSuspended,
                       c.MainType, c.SubType, c.Description AS CatDesc
                FROM Resource r
                LEFT JOIN Student s ON r.OwnerId = s.SRN
                LEFT JOIN Category c ON r.CategoryId = c.CategoryId
                JOIN LendBorrowTransaction lb ON lb.ResourceId = r.ResourceId
                JOIN `Transaction` t ON t.TransactionId = lb.TransactionId
                WHERE lb.BorrowerId = ? AND t.Status = 'COMPLETED'
                ORDER BY Title
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);
            ps.setString(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapResource(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to retrieve reviewable resources", e);
        }

        return resources;
    }

    @Override
    public void update(Resource resource) {
        String sql = "UPDATE Resource SET Title=?, Description=?, ItemCondition=?, Status=?, ListingType=?, Price=?, CategoryId=? WHERE ResourceId=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, resource.getTitle());
            ps.setString(2, resource.getDescription());
            ps.setString(3, resource.getCondition());
            ps.setString(4, resource.getStatus().name());
            ps.setString(5, resource.getListingType().name());
            ps.setDouble(6, resource.getPrice());
            ps.setInt(7, resource.getCategory() != null ? resource.getCategory().getCategoryId() : 1);
            ps.setInt(8, resource.getResourceId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update resource " + resource.getResourceId(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM Resource WHERE ResourceId=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete resource " + id, e);
        }
    }

    private Resource mapResource(ResultSet rs) throws SQLException {
        String ownerId = rs.getString("OwnerId");
        String ownerName = getOptString(rs, "OwnerName", "Student (" + ownerId + ")");
        String ownerEmail = getOptString(rs, "OwnerEmail", "");
        String ownerPhone = getOptString(rs, "OwnerPhone", "");
        String ownerDept = getOptString(rs, "OwnerDept", "General");
        boolean ownerSuspended = getOptBoolean(rs, "OwnerSuspended", false);

        Student owner = new Student(ownerId, ownerName, ownerEmail, ownerPhone, "", ownerDept, ownerSuspended);

        int catId = rs.getInt("CategoryId");
        String mainType = getOptString(rs, "MainType", "General");
        String subType = getOptString(rs, "SubType", "");
        String catDesc = getOptString(rs, "CatDesc", "");

        Category category = new Category(catId, mainType, subType, catDesc);

        double price = rs.getDouble("Price");

        return new Resource(
                rs.getInt("ResourceId"),
                rs.getString("Title"),
                rs.getString("Description"),
                rs.getString("ItemCondition"),
                ResourceStatus.valueOf(rs.getString("Status")),
                ListingType.valueOf(rs.getString("ListingType")),
                price,
                owner,
                category
        );
    }

    private static String getOptString(ResultSet rs, String col, String fallback) {
        try {
            String val = rs.getString(col);
            return val != null ? val : fallback;
        } catch (SQLException e) {
            return fallback;
        }
    }

    private static boolean getOptBoolean(ResultSet rs, String col, boolean fallback) {
        try {
            return rs.getBoolean(col);
        } catch (SQLException e) {
            return fallback;
        }
    }
}
