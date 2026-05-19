package github.oliveira.gb.librarycatalogapi.domain.author;

import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Author domain operations.
 * Handles business logic for author creation and retrieval.
 * All write operations are transactional.
 */
@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    /**
     * Constructs a new AuthorService with required dependencies.
     *
     * @param authorRepository the repository for author persistence
     */
    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    /**
     * Creates a new author with the given details.
     * Validates email uniqueness before persistence and sets active status to true.
     *
     * @param name the author name
     * @param email the author email (must be unique)
     * @param biography the author biography (can be null)
     * @return the created author entity
     * @throws DataIntegrityViolationException if an author with this email already exists
     */
    @Transactional
    public Author create(String name, String email, String biography) {
        String trimmedName = name.trim();
        String trimmedEmail = email.trim();

        Author author = new Author();
        author.setName(trimmedName);
        author.setEmail(trimmedEmail);
        author.setBiography(biography);
        author.setActive(true);

        try {
            return authorRepository.save(author);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException(
                    "Author with email '" + trimmedEmail + "' already exists"
            );
        }
    }

    /**
     * Returns a paginated list of all authors.
     *
     * @param pageable the pagination information
     * @return a page of authors
     */
    @Transactional(readOnly = true)
    public Page<Author> list(Pageable pageable) {
        return authorRepository.findAll(pageable);
    }

    /**
     * Deactivates an author by setting its active status to false.
     * This implements the soft delete pattern - the record remains in the database
     * but is excluded from standard queries via @SQLRestriction.
     *
     * <p>Due to @SQLRestriction, attempting to deactivate an already inactive
     * author will result in ResourceNotFoundException (HTTP 404), providing
     * natural idempotency for the operation.</p>
     *
     * @param id the author identifier
     * @throws ResourceNotFoundException if the author is not found or already inactive
     */
    @Transactional
    public void deactivate(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with id: " + id));
        author.setActive(false);
    }
}
