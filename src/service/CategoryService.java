package service;

import dao.impl.CategoryDAOImpl;
import dao.interfaces.CategoryDAO;
import model.resource.Category;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAOImpl();
    }

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    public Optional<Category> getCategoryById(int id) {
        return categoryDAO.findById(id);
    }
}
