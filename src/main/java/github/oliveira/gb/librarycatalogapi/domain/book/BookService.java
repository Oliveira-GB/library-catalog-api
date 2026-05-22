package github.oliveira.gb.librarycatalogapi.domain.book;

import github.oliveira.gb.librarycatalogapi.domain.author.Author;
import github.oliveira.gb.librarycatalogapi.domain.author.AuthorRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.exception.BookLoanedException;
import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for Book domain operations.
 * Handles business logic for book creation, retrieval, update, and inactivation.
 * All write operations are transactional.
 */
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;

    /**
     * Constructs a new BookService with required dependencies.
     *
     * @param bookRepository     the repository for book persistence
     * @param categoryRepository the repository for category retrieval
     * @param authorRepository   the repository for author retrieval
     */
    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
    }

    /**
     * Creates a new book with the given details.
     * Validates that the category and all authors exist and are active.
     * Validates ISBN uniqueness before persistence.
     *
     * @param title      the book title
     * @param isbn       the book ISBN (must be unique)
     * @param categoryId the category identifier
     * @param authorIds  the list of author identifiers
     * @return the created book entity
     * @throws ResourceNotFoundException      if category or any author is not found
     * @throws DataIntegrityViolationException if a book with this ISBN already exists
     */
    @Transactional
    public Book create(String title, String isbn, Long categoryId, List<Long> authorIds) {
        String trimmedTitle = title.trim();
        String trimmedIsbn = isbn.trim();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        List<Author> authors = findAuthorsByIds(authorIds);

        Book book = new Book();
        book.setTitle(trimmedTitle);
        book.setIsbn(trimmedIsbn);
        book.setCategory(category);
        book.setAuthors(new java.util.HashSet<>(authors));
        book.setStatus(BookStatus.DISPONIVEL);
        book.setActive(true);

        try {
            return bookRepository.save(book);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException(
                    "Book with ISBN '" + trimmedIsbn + "' already exists"
            );
        }
    }

    /**
     * Finds a book by its ID with authors and category eagerly fetched.
     *
     * @param id the book identifier
     * @return the book entity
     * @throws ResourceNotFoundException if the book is not found
     */
    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findByIdWithAuthorsAndCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    /**
     * Finds a book by its ISBN with authors and category eagerly fetched.
     *
     * @param isbn the book ISBN
     * @return the book entity
     * @throws ResourceNotFoundException if the book is not found
     */
    @Transactional(readOnly = true)
    public Book findByIsbn(String isbn) {
        String trimmedIsbn = isbn.trim();
        return bookRepository.findByIsbnWithAuthorsAndCategory(trimmedIsbn)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ISBN: " + trimmedIsbn));
    }

    /**
     * Updates an existing book with the given details.
     * Synchronizes ManyToMany author relationships.
     * Validates that the category and all authors exist and are active.
     *
     * @param id         the book identifier
     * @param title      the book title
     * @param isbn       the book ISBN
     * @param categoryId the category identifier
     * @param authorIds  the list of author identifiers
     * @return the updated book entity
     * @throws ResourceNotFoundException      if book, category, or any author is not found
     * @throws DataIntegrityViolationException if the new ISBN conflicts with another book
     */
    @Transactional
    public Book update(Long id, String title, String isbn, Long categoryId, List<Long> authorIds) {
        String trimmedTitle = title.trim();
        String trimmedIsbn = isbn.trim();

        Book book = bookRepository.findByIdWithAuthorsAndCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        List<Author> authors = findAuthorsByIds(authorIds);

        book.setTitle(trimmedTitle);
        book.setIsbn(trimmedIsbn);
        book.setCategory(category);
        book.getAuthors().clear();
        book.getAuthors().addAll(authors);

        try {
            return bookRepository.save(book);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException(
                    "Book with ISBN '" + trimmedIsbn + "' already exists"
            );
        }
    }

    /**
     * Deactivates a book by setting its active status to false.
     * Implements the soft delete pattern.
     *
     * <p>A book with {@link BookStatus#EMPRESTADO} status cannot be inactivated
     * to preserve loan history integrity.</p>
     *
     * @param id the book identifier
     * @throws ResourceNotFoundException if the book is not found
     * @throws BookLoanedException       if the book is currently loaned
     */
    @Transactional
    public void deactivate(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        if (book.getStatus() == BookStatus.EMPRESTADO) {
            throw new BookLoanedException(
                    "Cannot inactivate book with id " + id + " because it is currently loaned. " +
                    "The book must be returned before inactivation."
            );
        }

        book.setActive(false);
    }

    /**
     * Returns a paginated list of all books with category eagerly loaded.
     *
     * @param pageable the pagination information
     * @return a page of books
     */
    @Transactional(readOnly = true)
    public Page<Book> list(Pageable pageable) {
        return bookRepository.findAllWithCategory(pageable);
    }

    /**
     * Finds authors by their IDs, ensuring all requested IDs exist.
     *
     * @param authorIds the list of author identifiers
     * @return the list of author entities
     * @throws ResourceNotFoundException if any author is not found
     */
    private List<Author> findAuthorsByIds(List<Long> authorIds) {
        Set<Long> uniqueAuthorIds = authorIds.stream().collect(Collectors.toSet());
        List<Author> authors = authorRepository.findAllById(uniqueAuthorIds);

        if (authors.size() != uniqueAuthorIds.size()) {
            throw new ResourceNotFoundException("One or more authors not found");
        }

        return authors;
    }
}
