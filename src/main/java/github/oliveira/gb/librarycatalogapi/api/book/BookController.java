package github.oliveira.gb.librarycatalogapi.api.book;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Book API endpoints.
 * Handles HTTP requests for book management operations.
 * All endpoints follow REST conventions and return appropriate HTTP status codes.
 */
@RestController
@RequestMapping("/api/v1/livros")
public class BookController {

    private final BookService bookService;

    /**
     * Constructs a new BookController with required dependencies.
     *
     * @param bookService the service for book operations
     */
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Creates a new book.
     *
     * @param request the book creation request
     * @return ResponseEntity with created book and Location header (HTTP 201)
     */
    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        Book book = bookService.create(
                request.title(),
                request.isbn(),
                request.categoryId(),
                request.authorIds()
        );
        BookResponse response = toResponse(book);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(book.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Retrieves a book by its ID.
     * Uses JOIN FETCH to optimize relationship loading and prevent N+1 queries.
     *
     * @param id the book identifier
     * @return ResponseEntity with book details (HTTP 200)
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> findById(@PathVariable Long id) {
        Book book = bookService.findById(id);
        BookResponse response = toResponse(book);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a book by its ISBN.
     * Uses JOIN FETCH to optimize relationship loading and prevent N+1 queries.
     *
     * @param isbn the book ISBN
     * @return ResponseEntity with book details (HTTP 200)
     */
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookResponse> findByIsbn(@PathVariable String isbn) {
        Book book = bookService.findByIsbn(isbn);
        BookResponse response = toResponse(book);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing book.
     * Synchronizes ManyToMany author relationships atomically within a transaction.
     *
     * @param id      the book identifier
     * @param request the book update request
     * @return ResponseEntity with updated book details (HTTP 200)
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        Book book = bookService.update(
                id,
                request.title(),
                request.isbn(),
                request.categoryId(),
                request.authorIds()
        );
        BookResponse response = toResponse(book);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivates a book by its ID.
     * Implements soft delete - the book is marked as inactive but remains in the database.
     * Books with EMPRESTADO status cannot be inactivated.
     *
     * @param id the book identifier
     * @return ResponseEntity with no content (HTTP 204)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        bookService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns a paginated list of all books.
     * Uses EntityGraph to eagerly fetch categories and prevent N+1 queries.
     *
     * @param pageable the pagination parameters (page, size, sort)
     * @return ResponseEntity with paginated book list (HTTP 200)
     */
    @GetMapping
    public ResponseEntity<Page<BookSummaryResponse>> list(Pageable pageable) {
        Page<Book> books = bookService.list(pageable);
        Page<BookSummaryResponse> response = books.map(this::toSummaryResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Converts a Book entity to BookResponse DTO.
     *
     * @param book the book entity
     * @return the book response DTO
     */
    private BookResponse toResponse(Book book) {
        List<BookAuthorResponse> authorResponses = book.getAuthors().stream()
                .map(author -> new BookAuthorResponse(author.getId(), author.getName()))
                .toList();

        BookCategoryResponse categoryResponse = new BookCategoryResponse(
                book.getCategory().getId(),
                book.getCategory().getName()
        );

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getStatus().name(),
                categoryResponse,
                authorResponses,
                book.getActive(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

    /**
     * Converts a Book entity to BookSummaryResponse DTO.
     *
     * @param book the book entity
     * @return the book summary response DTO
     */
    private BookSummaryResponse toSummaryResponse(Book book) {
        return new BookSummaryResponse(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getStatus().name(),
                book.getCategory().getName(),
                book.getActive(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
