package service;

import dao.dto.BorrowedItem;
import dao.dto.BoughtItem;
import dao.interfaces.TransactionDAO;
import model.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionServiceTest {

    @Test
    void buyResource_usesAtomicDaoMethod() {
        FakeTransactionDAO dao = new FakeTransactionDAO();
        TransactionService service = new TransactionService(dao);

        int txId = service.buyResource(42, "STU002");

        assertEquals(99, txId);
        assertEquals(42, dao.boughtResourceId);
        assertEquals("STU002", dao.buyerId);
    }

    @Test
    void returnBorrowedItem_usesBorrowerCheckedDaoMethod() {
        FakeTransactionDAO dao = new FakeTransactionDAO();
        TransactionService service = new TransactionService(dao);

        service.returnBorrowedItem(7, "STU003");

        assertEquals(7, dao.returnedTransactionId);
        assertEquals("STU003", dao.returningBorrowerId);
    }

    private static class FakeTransactionDAO implements TransactionDAO {
        int boughtResourceId;
        String buyerId;
        int returnedTransactionId;
        String returningBorrowerId;

        @Override
        public int createBuySellTransactionAtomic(int resourceId, String buyerId) {
            this.boughtResourceId = resourceId;
            this.buyerId = buyerId;
            return 99;
        }

        @Override
        public void completeLendBorrow(int transactionId, String borrowerId) {
            this.returnedTransactionId = transactionId;
            this.returningBorrowerId = borrowerId;
        }

        @Override public int createTransaction(String transactionType, String status) { return 0; }
        @Override public void insertBuySell(int transactionId, int resourceId, String sellerId, String buyerId, double price) {}
        @Override public void insertLendBorrow(int transactionId, int resourceId, String lenderId, String borrowerId, LocalDate startDate, LocalDate endDate, double penalty) {}
        @Override public int createLendBorrowTransactionAtomic(int resourceId, String borrowerId, LocalDate startDate, LocalDate endDate) { return 0; }
        @Override public List<Transaction> findAll() { return List.of(); }
        @Override public void updateStatus(int transactionId, String status) {}
        @Override public List<BorrowedItem> findBorrowedByBorrower(String borrowerId) { return List.of(); }
        @Override public List<BoughtItem> findBoughtByBuyer(String buyerId) { return List.of(); }
        @Override public void completeLendBorrow(int transactionId) {}
    }
}
