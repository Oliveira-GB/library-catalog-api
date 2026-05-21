package github.oliveira.gb.librarycatalogapi.domain.book;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
