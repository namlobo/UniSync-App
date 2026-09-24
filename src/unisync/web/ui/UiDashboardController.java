package unisync.web.ui;

import jakarta.servlet.http.HttpSession;
import model.resource.Resource;
import model.user.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import service.ResourceService;
import service.TransactionService;

import java.util.List;

@Controller
public class UiDashboardController {

    private final ResourceService resourceService;
    private final TransactionService transactionService;

    public UiDashboardController(ResourceService resourceService, TransactionService transactionService) {
        this.resourceService = resourceService;
        this.transactionService = transactionService;
    }

    @GetMapping("/ui/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Object u = session.getAttribute(UiSession.CURRENT_USER);
        Student me = (u instanceof Student s) ? s : null;

        List<Resource> available = resourceService.getAvailableResources();
        model.addAttribute("availableCount", available.size());

        if (me != null) {
            model.addAttribute("boughtCount", transactionService.getBoughtItems(me.getId()).size());
            model.addAttribute("borrowedCount", transactionService.getBorrowedItems(me.getId()).size());
        } else {
            model.addAttribute("boughtCount", 0);
            model.addAttribute("borrowedCount", 0);
        }

        model.addAttribute("recentResources", available.stream().limit(6).toList());

        return "dashboard";
    }
}
