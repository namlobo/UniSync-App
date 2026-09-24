package dao.interfaces;

import model.review.Review;
import java.util.List;

// ReviewDAO abstracts review storage details, which supports DIP and SRP.
// Design principle used - Interface Segregation Principle (ISP) since only required operations are included. No unncessary methods like update and delete are included unless actually needed. 
public interface ReviewDAO {

    void save(Review review);

    void upsert(Review review);

    List<Review> findByResource(int resourceId);

    boolean canReviewResource(String reviewerId, int resourceId);

    double averageRatingForResource(int resourceId);

    int reviewCountForResource(int resourceId);
}
