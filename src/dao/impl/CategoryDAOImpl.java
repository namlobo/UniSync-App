package dao.impl;

import dao.DBConnection;
import dao.interfaces.CategoryDAO;
import model.resource.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDAOImpl implements CategoryDAO {

    @Override
    public List<Category> findAll() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT CategoryId, MainType, SubType, Description FROM Category ORDER BY MainType, SubType";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("CategoryId"),
                        rs.getString("MainType"),
                        rs.getString("SubType"),
                        rs.getString("Description")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load categories from database", e);
        }
        return categories;
    }

    @Override
    public Optional<Category> findById(int id) {
        String sql = "SELECT CategoryId, MainType, SubType, Description FROM Category WHERE CategoryId = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Category(
                            rs.getInt("CategoryId"),
                            rs.getString("MainType"),
                            rs.getString("SubType"),
                            rs.getString("Description")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load category with ID " + id, e);
        }
        return Optional.empty();
    }
}
