package dao.interfaces;

import model.resource.Resource;
import java.util.List;
import java.util.Optional;

// Interface for resource database operations
// It defines what operations exist, not how they are performed
// Design principle used - Dependency Inversion Principle (DIP) since the higher layers depend on abstraction instead of concrete classes
public interface ResourceDAO {

    void save(Resource resource);

    Optional<Resource> findById(int id);

    List<Resource> findAvailableResources();

    List<Resource> findAvailableResourcesExcludingOwner(String ownerId);

    List<Resource> findReviewableResourcesByStudent(String studentId);

    void update(Resource resource);

    void delete(int id);
}
