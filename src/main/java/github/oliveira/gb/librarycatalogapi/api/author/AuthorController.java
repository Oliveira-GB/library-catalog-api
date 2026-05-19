package github.oliveira.gb.librarycatalogapi.api.author;

import github.oliveira.gb.librarycatalogapi.domain.author.Author;
import github.oliveira.gb.librarycatalogapi.domain.author.AuthorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for Author API endpoints.
 * Handles HTTP requests for author management operations.
 * All endpoints follow REST conventions and return appropriate HTTP status codes.
 */
@RestController
@RequestMapping("/api/v1/autores")
public class AuthorController {

    private final AuthorService authorService;

    /**
     * Constructs a new AuthorController with required dependencies.
     *
     * @param authorService the service for author operations
     */
    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    /**
     * Creates a new author.
     *
     * @param request the author creation request
     * @return ResponseEntity with created author and Location header (HTTP 201)
     */
    @PostMapping
    public ResponseEntity<AuthorResponse> create(@Valid @RequestBody AuthorRequest request) {
        Author author = authorService.create(
                request.name(),
                request.email(),
                request.biography()
        );
        AuthorResponse response = toResponse(author);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(author.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Returns a paginated list of all authors.
     *
     * @param pageable the pagination parameters (page, size, sort)
     * @return ResponseEntity with paginated author list (HTTP 200)
     */
    @GetMapping
    public ResponseEntity<Page<AuthorResponse>> list(Pageable pageable) {
        Page<Author> authors = authorService.list(pageable);
        Page<AuthorResponse> response = authors.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates an author by its ID.
     * Implements soft delete - the author is marked as inactive but remains in the database.
     * Inactive authors are excluded from standard queries via @SQLRestriction.
     *
     * @param id the author identifier
     * @return ResponseEntity with no content (HTTP 204)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        authorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Converts an Author entity to AuthorResponse DTO.
     *
     * @param author the author entity
     * @return the author response DTO
     */
    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getEmail(),
                author.getBiography(),
                author.getActive(),
                author.getCreatedAt(),
                author.getUpdatedAt()
        );
    }
}
