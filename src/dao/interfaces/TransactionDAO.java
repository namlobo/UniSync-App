package dao.interfaces;

import model.transaction.Transaction;
import java.util.List;

public interface TransactionDAO {

    int createTransaction(String transactionType, String status);

    void insertBuySell(int transactionId, int resourceId, String sellerId, String buyerId, double price);

    void insertLendBorrow(int transactionId, int resourceId, String lenderId, String borrowerId,
                          java.time.LocalDate startDate, java.time.LocalDate endDate, double penalty);

    int createBuySellTransactionAtomic(int resourceId, String buyerId);

    int createLendBorrowTransactionAtomic(int resourceId, String borrowerId,
                                          java.time.LocalDate startDate, java.time.LocalDate endDate);

    List<Transaction> findAll();

    void updateStatus(int transactionId, String status);

    List<dao.dto.BorrowedItem> findBorrowedByBorrower(String borrowerId);

    List<dao.dto.BoughtItem> findBoughtByBuyer(String buyerId);

    void completeLendBorrow(int transactionId);

    void completeLendBorrow(int transactionId, String borrowerId);
}
