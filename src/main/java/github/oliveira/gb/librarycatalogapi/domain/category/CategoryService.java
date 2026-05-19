package github.oliveira.gb.librarycatalogapi.domain.category;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Category domain operations.
 * Handles business logic for category creation and retrieval.
 * All write operations are transactional.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Constructs a new CategoryService with required dependencies.
     *
     * @param categoryRepository the repository for category persistence
     */
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Creates a new category with the given name.
     * Validates uniqueness before persistence and sets active status to true.
     *
     * @param name the category name
     * @return the created category entity
     * @throws DataIntegrityViolationException if a category with this name already exists
     */
    @Transactional
    public Category create(String name) {
        String trimmedName = name.trim();

        Category category = new Category();
        category.setName(trimmedName);
        category.setActive(true);

        try {
            return categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException(
                    "Category with name '" + trimmedName + "' already exists"
            );
        }
    }

    /**
     * Returns a paginated list of all categories.
     *
     * @param pageable the pagination information
     * @return a page of categories
     */
    @Transactional(readOnly = true)
    public Page<Category> list(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }
}
