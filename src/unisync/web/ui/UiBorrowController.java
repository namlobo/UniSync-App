package unisync.web.ui;

import jakarta.servlet.http.HttpSession;
import model.user.Student;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.TransactionService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class UiBorrowController {

    private final TransactionService transactionService;

    public UiBorrowController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/ui/borrow")
    public String borrow(@RequestParam int resourceId,
                         @RequestParam(required = false) String lenderId,
                         @RequestParam String startDate,
                         @RequestParam String endDate,
                         HttpSession session) {

        Object u = session.getAttribute(UiSession.CURRENT_USER);
        Student borrower = (u instanceof Student s) ? s : null;
        if (borrower == null) {
            return "redirect:/?error=" + enc("Please login first");
        }

        try {
            transactionService.createLendBorrowTransaction(
                    resourceId,
                    lenderId,
                    borrower.getId(),
                    startDate,
                    endDate
            );
            return "redirect:/ui/transactions?success=" + enc("Borrow request created");
        } catch (Exception e) {
            return "redirect:/ui/resources?error=" + enc(e.getMessage() == null ? "Borrow failed" : e.getMessage());
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

