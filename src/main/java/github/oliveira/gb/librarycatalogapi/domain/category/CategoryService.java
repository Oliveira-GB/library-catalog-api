package github.oliveira.gb.librarycatalogapi.domain.category;

import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
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

    /**
     * Deactivates a category by setting its active status to false.
     * This implements the soft delete pattern - the record remains in the database
     * but is excluded from standard queries via @SQLRestriction.
     *
     * <p>Due to @SQLRestriction, attempting to deactivate an already inactive
     * category will result in ResourceNotFoundException (HTTP 404), providing
     * natural idempotency for the operation.</p>
     *
     * @param id the category identifier
     * @throws ResourceNotFoundException if the category is not found or already inactive
     */
    @Transactional
    public void deactivate(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setActive(false);
    }
}
