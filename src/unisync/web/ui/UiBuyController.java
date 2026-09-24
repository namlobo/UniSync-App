package unisync.web.ui;

import jakarta.servlet.http.HttpSession;
import model.resource.ListingType;
import model.resource.Resource;
import model.user.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.ResourceService;
import service.TransactionService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
public class UiBuyController {

    private final ResourceService resourceService;
    private final TransactionService transactionService;

    public UiBuyController(ResourceService resourceService, TransactionService transactionService) {
        this.resourceService = resourceService;
        this.transactionService = transactionService;
    }

    @GetMapping("/ui/buy")
    public String buy(@RequestParam(value = "error", required = false) String error,
                      @RequestParam(value = "success", required = false) String success,
                      HttpSession session,
                      Model model) {
        Object u = session.getAttribute(UiSession.CURRENT_USER);
        Student buyer = (u instanceof Student s) ? s : null;
        List<Resource> all = buyer == null
                ? resourceService.getAvailableResources()
                : resourceService.getAvailableResourcesExcludingOwner(buyer.getId());
        List<Resource> sellOnly = new ArrayList<>();
        for (Resource r : all) {
            if (r.getListingType() == ListingType.SELL) {
                sellOnly.add(r);
            }
        }
        model.addAttribute("resources", sellOnly);
        model.addAttribute("count", sellOnly.size());
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "buy";
    }

    @PostMapping("/ui/buy")
    public String buySubmit(@RequestParam int resourceId,
                            HttpSession session) {
        Object u = session.getAttribute(UiSession.CURRENT_USER);
        Student buyer = (u instanceof Student s) ? s : null;
        if (buyer == null) {
            return "redirect:/?error=" + enc("Please login first");
        }

        try {
            transactionService.buyResource(resourceId, buyer.getId());
            return "redirect:/ui/transactions?success=" + enc("Purchase recorded");
        } catch (Exception e) {
            return "redirect:/ui/buy?error=" + enc(e.getMessage() == null ? "Buy failed" : e.getMessage());
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
