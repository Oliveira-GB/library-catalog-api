package github.oliveira.gb.librarycatalogapi.domain.book;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Book entity persistence and retrieval.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Finds books by their IDs applying a pessimistic write lock.
     * Results are ordered by ID to prevent deadlocks when locking multiple rows.
     *
     * @param ids the book identifiers
     * @return locked books ordered by ID
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id IN :ids ORDER BY b.id")
    List<Book> findAllByIdWithLock(List<Long> ids);

    /**
     * Finds a book by its ID with category and authors eagerly fetched.
     * Uses JOIN FETCH to prevent N+1 query problems.
     *
     * @param id the book identifier
     * @return the book with loaded category and authors
     */
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.category LEFT JOIN FETCH b.authors WHERE b.id = :id")
    Optional<Book> findByIdWithAuthorsAndCategory(Long id);

    /**
     * Finds a book by its ISBN with category and authors eagerly fetched.
     * Uses JOIN FETCH to prevent N+1 query problems.
     *
     * @param isbn the book ISBN
     * @return the book with loaded category and authors
     */
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.category LEFT JOIN FETCH b.authors WHERE b.isbn = :isbn")
    Optional<Book> findByIsbnWithAuthorsAndCategory(String isbn);

    /**
     * Returns a paginated list of all books with category eagerly loaded.
     * Uses EntityGraph to prevent N+1 when accessing category data.
     *
     * @param pageable the pagination information
     * @return a page of books with loaded categories
     */
    @Query("SELECT b FROM Book b")
    @EntityGraph(attributePaths = {"category"})
    Page<Book> findAllWithCategory(Pageable pageable);

    /**
     * Searches the catalog with dynamic filters returning flat projections.
     * Uses PostgreSQL string_agg for author concatenation and a hard LIMIT of 100.
     * Respects the Book soft-delete filter (active = true).
     *
     * @param title partial title filter (nullable)
     * @param author partial author filter (nullable)
     * @param genre exact category name filter (nullable)
     * @param availableStatus exact status filter (nullable)
     * @param pageable pagination limited to 100 records
     * @return raw projection rows
     */
    @Query(nativeQuery = true, value = """
            SELECT b.id, b.title, COALESCE(string_agg(a.name, ', ' ORDER BY a.name), '') AS authors, c.name AS genre, b.isbn,
                   CASE WHEN b.status = 'DISPONIVEL' THEN true ELSE false END AS available
            FROM books b
            JOIN categories c ON b.category_id = c.id
            LEFT JOIN book_authors ba ON b.id = ba.book_id
            LEFT JOIN authors a ON ba.author_id = a.id
            WHERE b.active = true
              AND (:title IS NULL OR b.title ILIKE '%' || :title || '%')
              AND (:author IS NULL OR a.name ILIKE '%' || :author || '%')
              AND (:genre IS NULL OR c.name = :genre)
              AND (:availableStatus IS NULL OR b.status = :availableStatus)
            GROUP BY b.id, b.title, c.name, b.isbn, b.status
            ORDER BY b.title
            LIMIT :limit
            """
    )
    List<Object[]> searchCatalogNative(@Param("title") String title,
                                       @Param("author") String author,
                                       @Param("genre") String genre,
                                       @Param("availableStatus") String availableStatus,
                                       @Param("limit") int limit);

    /**
     * Finds a public book projection by ISBN for the public lookup endpoint.
     * Uses PostgreSQL string_agg for author concatenation.
     * Respects the Book soft-delete filter (active = true).
     *
     * @param isbn the book ISBN
     * @return raw projection row
     */
    @Query(nativeQuery = true, value = """
            SELECT b.title, COALESCE(string_agg(a.name, ', ' ORDER BY a.name), '') AS authors, c.name AS genre, b.isbn
            FROM books b
            JOIN categories c ON b.category_id = c.id
            LEFT JOIN book_authors ba ON b.id = ba.book_id
            LEFT JOIN authors a ON ba.author_id = a.id
            WHERE b.isbn = :isbn AND b.active = true
            GROUP BY b.id, b.title, c.name, b.isbn
            """
    )
    List<Object[]> findByIsbnNative(@Param("isbn") String isbn);

    /**
     * Generates the inventory report returning individual book units.
     * Respects the Book soft-delete filter (active = true).
     * Hard limited to 100 records.
     *
     * @param pageable pagination limited to 100 records
     * @return raw projection rows
     */
    @Query(nativeQuery = true, value = """
            SELECT b.id, b.title, b.isbn, b.status
            FROM books b
            WHERE b.active = true
            ORDER BY b.title
            LIMIT :limit
            """
    )
    List<Object[]> generateInventoryReportNative(@Param("limit") int limit);
}
