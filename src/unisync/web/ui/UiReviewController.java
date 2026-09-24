package unisync.web.ui;

import jakarta.servlet.http.HttpSession;
import model.user.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.ResourceService;
import service.ReviewService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
// UiReviewController handles web review requests, following MVC and the GRASP Controller principle.
public class UiReviewController {

    private final ReviewService reviewService;
    private final ResourceService resourceService;

    public UiReviewController(ReviewService reviewService, ResourceService resourceService) {
        this.reviewService = reviewService;
        this.resourceService = resourceService;
    }

    @GetMapping("/ui/reviews")
    // Loads the reviews page and supplies model data for the selected resource.
    public String reviews(@RequestParam(value = "resourceId", required = false) Integer resourceId,
                          @RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "success", required = false) String success,
                          HttpSession session,
                          Model model) {
        Student me = current(session);
        // Prepare data for view
        List<model.resource.Resource> resources = resourceService.getReviewableResources(me.getId());
        model.addAttribute("resources", resources);
        model.addAttribute("resourceId", resourceId);
        model.addAttribute("error", error);
        model.addAttribute("success", success);

        if (resourceId != null) {
            // If resource selected, show its reviews
            model.addAttribute("reviews", reviewService.getReviewsForResource(resourceId));
            model.addAttribute("averageRating", reviewService.getAverageRatingForResource(resourceId));
            model.addAttribute("reviewCount", reviewService.getReviewCountForResource(resourceId));
        } else {
            model.addAttribute("reviews", List.of());
            model.addAttribute("averageRating", 0.0);
            model.addAttribute("reviewCount", 0);
        }

        return "reviews";
    }

    @PostMapping("/ui/reviews")
    // Accepts a submitted review and delegates validation and saving to ReviewService.
    public String submit(@RequestParam int resourceId,
                         @RequestParam int rating,
                         @RequestParam(required = false) String comment,
                         HttpSession session) {
        Student me = current(session);  // Get current user from session
        try {
            reviewService.submitReview(resourceId, me, rating, comment);
            return "redirect:/ui/reviews?resourceId=" + resourceId + "&success=" + enc("Review submitted");
        } catch (Exception e) {
            return "redirect:/ui/reviews?resourceId=" + resourceId + "&error=" + enc(e.getMessage() == null ? "Review failed" : e.getMessage());
        }
    }

    // Extracts the logged-in student from the session, keeping session access centralized.
    private static Student current(HttpSession session) {
        Object u = session.getAttribute(UiSession.CURRENT_USER);
        if (u instanceof Student s) return s;
        throw new IllegalStateException("Not logged in");
    }

    // Encodes messages safely for redirect URLs.
    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
