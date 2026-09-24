package service;

import dao.interfaces.ResourceDAO;
import dao.impl.ResourceDAOImpl;
import model.resource.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
// ResourceService applies resource business rules between controllers and DAOs, following SRP
public class ResourceService {

    private final ResourceDAO resourceDAO;

    public ResourceService() {
        this.resourceDAO = new ResourceDAOImpl();
    }

    // Validates listing rules before saving, keeping business logic out of controllers.
    public void addResource(Resource resource) {

        if (!resource.isAvailable()) {
            throw new IllegalArgumentException("Resource must be available before listing.");
        }

        // Check if owner is suspended - suspended students cannot list resources
        if (resource.getOwner() != null && resource.getOwner().isSuspended()) {
            throw new IllegalStateException("Suspended accounts cannot list resources.");
        }

        // 🔥 SELL listings MUST have a price > 0
        if (resource.getListingType() == model.resource.ListingType.SELL && resource.getPrice() <= 0) {
            throw new IllegalArgumentException("SELL listings must have a price greater than 0. Price provided: " + resource.getPrice());
        }

        resourceDAO.save(resource);
    }

    // Retrieves one resource and enforces not-found handling in the service layer.
    public Resource getResourceById(int id) {

        Optional<Resource> resource = resourceDAO.findById(id);

        if (resource.isEmpty()) {
            throw new IllegalArgumentException("Resource not found.");
        }

        return resource.get();
    }

    // Returns available resources for catalog views while keeping retrieval logic centralized.
    public List<Resource> getAvailableResources() {
        return resourceDAO.findAvailableResources();
    }

    public List<Resource> getAvailableResourcesExcludingOwner(String ownerId) {
        return resourceDAO.findAvailableResourcesExcludingOwner(ownerId);
    }

    public List<Resource> getReviewableResources(String studentId) {
        return resourceDAO.findReviewableResourcesByStudent(studentId);
    }

    // Changes the domain state and persists it, following Information Expert and SRP.
    public void markAsSold(Resource resource) {
        resource.markSold();
        resourceDAO.update(resource);
    }

    // Marks a resource as borrowed and syncs the change to storage through the DAO layer.
    public void markAsBorrowed(Resource resource) {
        resource.markBorrowed();
        resourceDAO.update(resource);
    }

    // Restores availability after business actions like returns, keeping status rules centralized.
    public void makeAvailable(Resource resource) {
        resource.makeAvailable();
        resourceDAO.update(resource);
    }

    // Removes a resource through the DAO so controllers stay independent from persistence details.
    public void removeResource(int id) {
        resourceDAO.delete(id);
    }
}
