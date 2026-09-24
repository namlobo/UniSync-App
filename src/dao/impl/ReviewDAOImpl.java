package dao.impl;

import dao.DBConnection;
import dao.interfaces.ReviewDAO;
import model.review.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// ReviewDAOImpl encapsulates JDBC review queries, following SRP and Pure Fabrication.
public class ReviewDAOImpl implements ReviewDAO {

    @Override
    // Stores a review in the database so higher layers do not manage SQL directly.
    public void save(Review review) {
        String sql = "INSERT INTO Review (Rating, Comment, ReviewerId, ResourceId) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, review.getRating());
            ps.setString(2, review.getComment());
            ps.setString(3, review.getReviewer().getId());
            ps.setInt(4, review.getResource().getResourceId());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void upsert(Review review) {
        String sql = """
                INSERT INTO Review (Rating, Comment, ReviewerId, ResourceId)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE Rating = VALUES(Rating), Comment = VALUES(Comment)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, review.getRating());
            ps.setString(2, review.getComment());
            ps.setString(3, review.getReviewer().getId());
            ps.setInt(4, review.getResource().getResourceId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save review.", e);
        }
    }

    @Override
    // Retrieves all reviews for one resource, keeping query logic inside the DAO layer.
    public List<Review> findByResource(int resourceId) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT ReviewId, Rating, Comment, ReviewerId, ResourceId FROM Review WHERE ResourceId = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, resourceId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Review r = new Review(
                        rs.getInt("ReviewId"),
                        rs.getInt("Rating"),
                        rs.getString("Comment"),
                        new model.user.Student(rs.getString("ReviewerId"), "Temp", "temp@email.com", "0000000000", "pass", "Dept"),
                        new model.resource.Resource(rs.getInt("ResourceId"), "Temp", "", "", model.resource.ListingType.SELL, 0.0, null, null)
                );
                reviews.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reviews;
    }

    @Override
    public boolean canReviewResource(String reviewerId, int resourceId) {
        String sql = """
                SELECT 1
                FROM BuySellTransaction b
                JOIN `Transaction` t ON t.TransactionId = b.TransactionId
                JOIN Resource r ON r.ResourceId = b.ResourceId
                WHERE b.BuyerId = ? AND b.ResourceId = ? AND t.Status = 'COMPLETED' AND r.OwnerId <> ?
                UNION
                SELECT 1
                FROM LendBorrowTransaction lb
                JOIN `Transaction` t ON t.TransactionId = lb.TransactionId
                JOIN Resource r ON r.ResourceId = lb.ResourceId
                WHERE lb.BorrowerId = ? AND lb.ResourceId = ? AND t.Status = 'COMPLETED' AND r.OwnerId <> ?
                LIMIT 1
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reviewerId);
            ps.setInt(2, resourceId);
            ps.setString(3, reviewerId);
            ps.setString(4, reviewerId);
            ps.setInt(5, resourceId);
            ps.setString(6, reviewerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to validate review eligibility.", e);
        }
    }

    @Override
    public double averageRatingForResource(int resourceId) {
        String sql = "SELECT COALESCE(AVG(Rating), 0) AS AvgRating FROM Review WHERE ResourceId = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("AvgRating");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to calculate average rating.", e);
        }

        return 0.0;
    }

    @Override
    public int reviewCountForResource(int resourceId) {
        String sql = "SELECT COUNT(*) AS ReviewCount FROM Review WHERE ResourceId = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ReviewCount");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count reviews.", e);
        }

        return 0;
    }
}
