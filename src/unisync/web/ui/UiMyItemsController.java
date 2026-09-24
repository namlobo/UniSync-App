package unisync.web.ui;

import jakarta.servlet.http.HttpSession;
import model.user.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.TransactionService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
// UiMyItemsController serves the personal items page, following MVC and GRASP Controller.
public class UiMyItemsController {

    private final TransactionService transactionService;

    public UiMyItemsController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/ui/my-items")
    // Loads borrowed and bought item data into the view model for the current user.
    public String myItems(@RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "success", required = false) String success,
                          HttpSession session,
                          Model model) {
        Student me = current(session);
        model.addAttribute("borrowed", transactionService.getBorrowedItems(me.getId()));
        model.addAttribute("bought", transactionService.getBoughtItems(me.getId()));
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "myitems";
    }

    @PostMapping("/ui/return")
    // Handles a return request and delegates the business action to TransactionService.
    public String returnItem(@RequestParam int transactionId, HttpSession session) {
        Student me = current(session);
        try {
            transactionService.returnBorrowedItem(transactionId, me.getId());
            return "redirect:/ui/my-items?success=" + enc("Returned successfully");
        } catch (Exception e) {
            return "redirect:/ui/my-items?error=" + enc(e.getMessage() == null ? "Return failed" : e.getMessage());
        }
    }

    // Reads the logged-in student from the session so view handlers stay consistent.
    private static Student current(HttpSession session) {
        Object u = session.getAttribute(UiSession.CURRENT_USER);
        if (u instanceof Student s) return s;
        throw new IllegalStateException("Not logged in");
    }

    // Encodes redirect messages safely for the web layer.
    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
