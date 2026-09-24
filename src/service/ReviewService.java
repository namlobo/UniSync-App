package service;

import dao.interfaces.ReviewDAO;
import dao.impl.ReviewDAOImpl;
import model.review.Review;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
// ReviewService enforces review rules before persistence, following SRP and Service Layer.
public class ReviewService {

    private final ReviewDAO reviewDAO;

    public ReviewService() {
        this.reviewDAO = new ReviewDAOImpl();
    }

    public ReviewService(ReviewDAO reviewDAO) {
        this.reviewDAO = reviewDAO;
    }

    // Validates the rating range before saving, keeping business checks out of controllers.
    public void submitReview(Review review) {

        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        reviewDAO.save(review);
    }

    public void submitReview(int resourceId, model.user.Student reviewer, int rating, String comment) {
        if (reviewer == null) {
            throw new IllegalStateException("Not logged in");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (!reviewDAO.canReviewResource(reviewer.getId(), resourceId)) {
            throw new IllegalStateException("You can review only items from your completed purchases or returns.");
        }

        Review review = new Review(
                0,
                rating,
                comment,
                reviewer,
                new model.resource.Resource(resourceId, "Temp", "", "", model.resource.ListingType.SELL, 0.0, null, null)
        );
        reviewDAO.upsert(review);
    }

    // Retrieves reviews for one resource while keeping retrieval logic centralized in the service.
    public List<Review> getReviewsForResource(int resourceId) {
        return reviewDAO.findByResource(resourceId);
    }

    public double getAverageRatingForResource(int resourceId) {
        return reviewDAO.averageRatingForResource(resourceId);
    }

    public int getReviewCountForResource(int resourceId) {
        return reviewDAO.reviewCountForResource(resourceId);
    }
}
