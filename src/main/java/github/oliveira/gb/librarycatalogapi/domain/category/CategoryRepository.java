package github.oliveira.gb.librarycatalogapi.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Category entity.
 * Provides CRUD operations and custom queries for category management.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Checks if a category with the given name exists.
     * Used for uniqueness validation before persistence.
     *
     * @param name the category name to check
     * @return true if a category with this name exists, false otherwise
     */
    boolean existsByName(String name);
}
