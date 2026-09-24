package dao.interfaces;

import model.resource.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryDAO {
    List<Category> findAll();
    Optional<Category> findById(int id);
}
