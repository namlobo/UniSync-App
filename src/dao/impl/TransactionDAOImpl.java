package dao.impl;

import dao.DBConnection;
import dao.interfaces.TransactionDAO;
import model.transaction.BarterTransaction;
import model.transaction.BuySellTransaction;
import model.transaction.LendBorrowTransaction;
import model.transaction.StoredTransaction;
import model.transaction.Transaction;
import model.transaction.TransactionStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class TransactionDAOImpl implements TransactionDAO {

    @Override
    public int createTransaction(String transactionType, String status) {
        String sql = "INSERT INTO `Transaction` (TransactionType, Status) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, transactionType);
            ps.setString(2, status);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void insertBuySell(int transactionId, int resourceId, String sellerId, String buyerId, double price) {
        String sql = "INSERT INTO BuySellTransaction (TransactionId, ResourceId, SellerId, BuyerId, Price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.setInt(2, resourceId);
            ps.setString(3, sellerId);
            ps.setString(4, buyerId);
            ps.setDouble(5, price);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void insertLendBorrow(int transactionId, int resourceId, String lenderId, String borrowerId,
                                 java.time.LocalDate startDate, java.time.LocalDate endDate, double penalty) {
        String sql = "INSERT INTO LendBorrowTransaction (TransactionId, ResourceId, LenderId, BorrowerId, StartDate, EndDate, Penalty) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.setInt(2, resourceId);
            ps.setString(3, lenderId);
            ps.setString(4, borrowerId);
            ps.setDate(5, Date.valueOf(startDate));
            ps.setDate(6, Date.valueOf(endDate));
            ps.setDouble(7, penalty);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int createBuySellTransactionAtomic(int resourceId, String buyerId) {
        String selectResource = """
                SELECT r.ResourceId, r.OwnerId, r.Price, r.Status, r.ListingType,
                       seller.Suspended AS SellerSuspended,
                       buyer.Suspended AS BuyerSuspended
                FROM Resource r
                JOIN Student seller ON seller.SRN = r.OwnerId
                JOIN Student buyer ON buyer.SRN = ?
                WHERE r.ResourceId = ?
                FOR UPDATE
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sellerId;
                double price;

                try (PreparedStatement ps = conn.prepareStatement(selectResource)) {
                    ps.setString(1, buyerId);
                    ps.setInt(2, resourceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Resource or buyer not found.");
                        }
                        sellerId = rs.getString("OwnerId");
                        price = rs.getDouble("Price");

                        if (buyerId.equals(sellerId)) {
                            throw new IllegalStateException("You cannot buy your own item.");
                        }
                        if (!"AVAILABLE".equals(rs.getString("Status"))) {
                            throw new IllegalStateException("This item is no longer available.");
                        }
                        if (!"SELL".equals(rs.getString("ListingType"))) {
                            throw new IllegalStateException("This item is not listed for sale.");
                        }
                        if (price <= 0) {
                            throw new IllegalStateException("Invalid resource price. Contact seller.");
                        }
                        if (rs.getBoolean("SellerSuspended")) {
                            throw new IllegalStateException("Seller account is suspended.");
                        }
                        if (rs.getBoolean("BuyerSuspended")) {
                            throw new IllegalStateException("Buyer account is suspended.");
                        }
                    }
                }

                int transactionId = insertBaseTransaction(conn, "BUYSELL", "COMPLETED");

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO BuySellTransaction (TransactionId, ResourceId, SellerId, BuyerId, Price) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, transactionId);
                    ps.setInt(2, resourceId);
                    ps.setString(3, sellerId);
                    ps.setString(4, buyerId);
                    ps.setDouble(5, price);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Resource SET Status='SOLD' WHERE ResourceId=? AND Status='AVAILABLE'")) {
                    ps.setInt(1, resourceId);
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException("This item is no longer available.");
                    }
                }

                conn.commit();
                return transactionId;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to buy resource.", e);
        }
    }

    @Override
    public int createLendBorrowTransactionAtomic(int resourceId, String borrowerId,
                                                 LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        String selectResource = """
                SELECT r.ResourceId, r.OwnerId, r.Status, r.ListingType,
                       lender.Suspended AS LenderSuspended,
                       borrower.Suspended AS BorrowerSuspended
                FROM Resource r
                JOIN Student lender ON lender.SRN = r.OwnerId
                JOIN Student borrower ON borrower.SRN = ?
                WHERE r.ResourceId = ?
                FOR UPDATE
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lenderId;

                try (PreparedStatement ps = conn.prepareStatement(selectResource)) {
                    ps.setString(1, borrowerId);
                    ps.setInt(2, resourceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Resource or borrower not found.");
                        }
                        lenderId = rs.getString("OwnerId");

                        if (borrowerId.equals(lenderId)) {
                            throw new IllegalStateException("You cannot borrow your own item.");
                        }
                        if (!"AVAILABLE".equals(rs.getString("Status"))) {
                            throw new IllegalStateException("This item is no longer available.");
                        }
                        if (!"LEND".equals(rs.getString("ListingType"))) {
                            throw new IllegalStateException("This item is not listed for lending.");
                        }
                        if (rs.getBoolean("LenderSuspended")) {
                            throw new IllegalStateException("Lender account is suspended.");
                        }
                        if (rs.getBoolean("BorrowerSuspended")) {
                            throw new IllegalStateException("Borrower account is suspended.");
                        }
                    }
                }

                int transactionId = insertBaseTransaction(conn, "LENDBORROW", "INITIATED");

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO LendBorrowTransaction (TransactionId, ResourceId, LenderId, BorrowerId, StartDate, EndDate, Penalty) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, transactionId);
                    ps.setInt(2, resourceId);
                    ps.setString(3, lenderId);
                    ps.setString(4, borrowerId);
                    ps.setDate(5, Date.valueOf(startDate));
                    ps.setDate(6, Date.valueOf(endDate));
                    ps.setDouble(7, 0.0);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Resource SET Status='BORROWED' WHERE ResourceId=? AND Status='AVAILABLE'")) {
                    ps.setInt(1, resourceId);
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException("This item is no longer available.");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Reminder (Message, Status, ReminderDate, StudentId, TransactionId) VALUES (?, 'UNREAD', ?, ?, ?)")) {
                    ps.setString(1, "Return borrowed item by " + endDate + " (Transaction #" + transactionId + ")");
                    ps.setDate(2, Date.valueOf(endDate.minusDays(1)));
                    ps.setString(3, borrowerId);
                    ps.setInt(4, transactionId);
                    ps.executeUpdate();
                }

                conn.commit();
                return transactionId;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to borrow resource.", e);
        }
    }

    @Override
    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT TransactionId, TransactionType, Status, CreatedAt FROM `Transaction`";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("TransactionId");
                String type = rs.getString("TransactionType");
                String statusStr = rs.getString("Status");
                Timestamp createdAtTs = rs.getTimestamp("CreatedAt");

                TransactionStatus status = TransactionStatus.valueOf(statusStr);
                transactions.add(new StoredTransaction(
                        id,
                        type,
                        status,
                        createdAtTs != null ? createdAtTs.toLocalDateTime() : null
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    @Override
    public void updateStatus(int transactionId, String status) {
        String sql = "UPDATE `Transaction` SET Status = ? WHERE TransactionId = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, transactionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<dao.dto.BorrowedItem> findBorrowedByBorrower(String borrowerId) {
        List<dao.dto.BorrowedItem> res = new ArrayList<>();
        String sql = """
                SELECT t.TransactionId, t.Status, lb.ResourceId, r.Title, lb.LenderId, lb.StartDate, lb.EndDate
                FROM `Transaction` t
                JOIN LendBorrowTransaction lb ON lb.TransactionId = t.TransactionId
                JOIN Resource r ON r.ResourceId = lb.ResourceId
                WHERE lb.BorrowerId = ?
                ORDER BY t.TransactionId DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, borrowerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new dao.dto.BorrowedItem(
                            rs.getInt("TransactionId"),
                            rs.getInt("ResourceId"),
                            rs.getString("Title"),
                            rs.getString("Status"),
                            rs.getString("LenderId"),
                            rs.getDate("StartDate").toLocalDate(),
                            rs.getDate("EndDate").toLocalDate()
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return res;
    }

    @Override
    public List<dao.dto.BoughtItem> findBoughtByBuyer(String buyerId) {
        List<dao.dto.BoughtItem> res = new ArrayList<>();
        String sql = """
                SELECT t.TransactionId, t.Status, b.ResourceId, r.Title, b.SellerId, b.Price
                FROM `Transaction` t
                JOIN BuySellTransaction b ON b.TransactionId = t.TransactionId
                JOIN Resource r ON r.ResourceId = b.ResourceId
                WHERE b.BuyerId = ?
                ORDER BY t.TransactionId DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new dao.dto.BoughtItem(
                            rs.getInt("TransactionId"),
                            rs.getInt("ResourceId"),
                            rs.getString("Title"),
                            rs.getString("Status"),
                            rs.getString("SellerId"),
                            rs.getDouble("Price")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return res;
    }

    @Override
    public void completeLendBorrow(int transactionId) {
        // Update Transaction status + mark Resource AVAILABLE.
        String select = "SELECT ResourceId FROM LendBorrowTransaction WHERE TransactionId = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            Integer resourceId = null;
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resourceId = rs.getInt("ResourceId");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE `Transaction` SET Status='COMPLETED' WHERE TransactionId=?")) {
                ps.setInt(1, transactionId);
                ps.executeUpdate();
            }

            if (resourceId != null) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE Resource SET Status='AVAILABLE' WHERE ResourceId=?")) {
                    ps.setInt(1, resourceId);
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void completeLendBorrow(int transactionId, String borrowerId) {
        String select = """
                SELECT lb.ResourceId, t.Status
                FROM LendBorrowTransaction lb
                JOIN `Transaction` t ON t.TransactionId = lb.TransactionId
                WHERE lb.TransactionId = ? AND lb.BorrowerId = ?
                FOR UPDATE
                """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int resourceId;
                try (PreparedStatement ps = conn.prepareStatement(select)) {
                    ps.setInt(1, transactionId);
                    ps.setString(2, borrowerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Borrow transaction not found for this user.");
                        }
                        if ("COMPLETED".equals(rs.getString("Status"))) {
                            throw new IllegalStateException("This item has already been returned.");
                        }
                        resourceId = rs.getInt("ResourceId");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE `Transaction` SET Status='COMPLETED' WHERE TransactionId=?")) {
                    ps.setInt(1, transactionId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Resource SET Status='AVAILABLE' WHERE ResourceId=?")) {
                    ps.setInt(1, resourceId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to return borrowed item.", e);
        }
    }

    private int insertBaseTransaction(Connection conn, String transactionType, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `Transaction` (TransactionType, Status) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, transactionType);
            ps.setString(2, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create transaction row.");
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }
}
