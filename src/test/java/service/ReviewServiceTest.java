package service;

import dao.interfaces.ReviewDAO;
import model.review.Review;
import model.user.Student;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewServiceTest {

    @Test
    void submitReview_rejectsInvalidRating() {
        FakeReviewDAO dao = new FakeReviewDAO(true);
        ReviewService service = new ReviewService(dao);
        Student reviewer = new Student("STU002", "Buyer", "b@test.edu", "1234567890", "pass", "CSE");

        assertThrows(IllegalArgumentException.class,
                () -> service.submitReview(5, reviewer, 6, "Too high"));
    }

    @Test
    void submitReview_requiresCompletedTransactionEligibility() {
        FakeReviewDAO dao = new FakeReviewDAO(false);
        ReviewService service = new ReviewService(dao);
        Student reviewer = new Student("STU002", "Buyer", "b@test.edu", "1234567890", "pass", "CSE");

        assertThrows(IllegalStateException.class,
                () -> service.submitReview(5, reviewer, 4, "Good"));
    }

    @Test
    void submitReview_upsertsEligibleReview() {
        FakeReviewDAO dao = new FakeReviewDAO(true);
        ReviewService service = new ReviewService(dao);
        Student reviewer = new Student("STU002", "Buyer", "b@test.edu", "1234567890", "pass", "CSE");

        service.submitReview(5, reviewer, 4, "Good");

        assertEquals(5, dao.saved.getResource().getResourceId());
        assertEquals("STU002", dao.saved.getReviewer().getId());
        assertEquals(4, dao.saved.getRating());
    }

    private static class FakeReviewDAO implements ReviewDAO {
        private final boolean eligible;
        private Review saved;

        private FakeReviewDAO(boolean eligible) {
            this.eligible = eligible;
        }

        @Override public void save(Review review) {}
        @Override public void upsert(Review review) { this.saved = review; }
        @Override public List<Review> findByResource(int resourceId) { return List.of(); }
        @Override public boolean canReviewResource(String reviewerId, int resourceId) { return eligible; }
        @Override public double averageRatingForResource(int resourceId) { return 0; }
        @Override public int reviewCountForResource(int resourceId) { return 0; }
    }
}
