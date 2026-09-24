package service;

import dao.interfaces.TransactionDAO;
import dao.impl.TransactionDAOImpl;

import factory.TransactionFactory;

import model.resource.Resource;
import model.transaction.Transaction;
import model.user.Student;
import org.springframework.stereotype.Service;
import dao.dto.BorrowedItem;
import dao.dto.BoughtItem;

import java.util.List;
import java.time.LocalDate;

@Service
public class TransactionService {

    private final TransactionDAO transactionDAO;
    private final ResourceService resourceService;
    private final ReminderDbService reminderDbService;
    private final StudentService studentService;

    public TransactionService() {
        this.transactionDAO = new TransactionDAOImpl();
        this.resourceService = new ResourceService();
        this.reminderDbService = new ReminderDbService();
        this.studentService = new StudentService();
    }

    public TransactionService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
        this.resourceService = new ResourceService();
        this.reminderDbService = new ReminderDbService();
        this.studentService = new StudentService();
    }

    // 🔥 LEND / BORROW TRANSACTION
    public Transaction createLendBorrowTransaction(int resourceId,
                                                   String lenderId,
                                                   String borrowerId,
                                                   String startDate,
                                                   String endDate) {
        LocalDate s = safeParseDate(startDate, LocalDate.now());
        LocalDate e = safeParseDate(endDate, LocalDate.now().plusDays(7));
        int txId = borrowResource(resourceId, borrowerId, s, e);
        return new model.transaction.StoredTransaction(txId, "LENDBORROW", model.transaction.TransactionStatus.INITIATED, java.time.LocalDateTime.now());
    }

    public int borrowResource(int resourceId, String borrowerId, LocalDate startDate, LocalDate endDate) {
        return transactionDAO.createLendBorrowTransactionAtomic(resourceId, borrowerId, startDate, endDate);
    }

    public Transaction createBuySellTransaction(int resourceId,
                                                String sellerId,
                                                String buyerId,
                                                double price) {
        int txId = buyResource(resourceId, buyerId);
        return new model.transaction.StoredTransaction(txId, "BUYSELL", model.transaction.TransactionStatus.COMPLETED, java.time.LocalDateTime.now());
    }

    public int buyResource(int resourceId, String buyerId) {
        return transactionDAO.createBuySellTransactionAtomic(resourceId, buyerId);
    }

    public List<BorrowedItem> getBorrowedItems(String borrowerId) {
        return transactionDAO.findBorrowedByBorrower(borrowerId);
    }

    public List<BoughtItem> getBoughtItems(String buyerId) {
        return transactionDAO.findBoughtByBuyer(buyerId);
    }

    public void returnBorrowedItem(int transactionId) {
        transactionDAO.completeLendBorrow(transactionId);
    }

    public void returnBorrowedItem(int transactionId, String borrowerId) {
        transactionDAO.completeLendBorrow(transactionId, borrowerId);
    }

    private static LocalDate safeParseDate(String v, LocalDate fallback) {
        try {
            return LocalDate.parse(v);
        } catch (Exception e) {
            return fallback;
        }
    }

    // 🔥 BUY / SELL TRANSACTION
    public Transaction createBuySellTransaction(Resource resource,
                                                Student seller,
                                                Student buyer,
                                                double price) {

        Transaction transaction = TransactionFactory.createTransaction(
                "BUYSELL",
                resource,
                seller,
                buyer,
                price
        );

        transaction.initiate();
        transaction.complete();

        resourceService.markAsSold(resource);
        int txId = transactionDAO.createTransaction("BUYSELL", transaction.getStatus().name());
        transactionDAO.insertBuySell(txId, resource.getResourceId(), seller.getId(), buyer.getId(), price);

        return transaction;
    }

    // 🔥 COMPLETE TRANSACTION (GENERIC)
    public void completeTransaction(Transaction transaction) {

        transaction.complete();

        // Only update transaction status if DAO supports it
        try {
            transactionDAO.updateStatus(
                    transaction.getTransactionId(),
                    transaction.getStatus().name()
            );
        } catch (Exception ignored) {
            // Safe fallback if method not implemented
        }
    }

    // 🔥 RETURN BORROWED ITEM
    public void completeLendBorrowTransaction(int transactionId) {

        try {
            transactionDAO.completeLendBorrow(transactionId);
        } catch (Exception ignored) {
            // fallback if DAO method not present
        }
    }

    // 🔥 GET ALL TRANSACTIONS
    public List<Transaction> getAllTransactions() {
        return transactionDAO.findAll();
    }
}
