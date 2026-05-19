package github.oliveira.gb.librarycatalogapi.domain.author;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Author entity.
 * Provides CRUD operations and custom queries for author management.
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    /**
     * Checks if an author with the given email exists.
     * Used for uniqueness validation before persistence.
     *
     * @param email the author email to check
     * @return true if an author with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
