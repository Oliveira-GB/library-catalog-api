package github.oliveira.gb.librarycatalogapi.domain.loan;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Loan entity persistence and retrieval.
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /**
     * Checks whether the reader has any active loan whose due date has passed.
     *
     * @param readerId the reader identifier
     * @param now      the current instant to compare against dueDate
     * @return true if at least one active overdue loan exists
     */
    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.reader.id = :readerId AND l.status = :status AND l.dueDate < :now")
    boolean existsActiveOverdueLoanByReaderId(Long readerId, LoanStatus status, Instant now);

    /**
     * Checks whether the reader already has an active loan containing any of the given books.
     *
     * @param readerId the reader identifier
     * @param status   the loan status to match
     * @param bookIds  the book identifiers to check
     * @return true if at least one active loan item exists for the given books
     */
    @Query("SELECT COUNT(li) > 0 FROM LoanItem li WHERE li.loan.reader.id = :readerId AND li.loan.status = :status AND li.book.id IN :bookIds")
    boolean existsActiveLoanItemByReaderIdAndBookIds(Long readerId, LoanStatus status, List<Long> bookIds);

    /**
     * Counts the number of active (unreturned) loan items for a given reader.
     * Used to enforce the cumulative possession limit (max 5 books per reader).
     *
     * @param readerId the reader identifier
     * @param status   the loan status to match
     * @return the number of active loan items
     */
    @Query("SELECT COUNT(li) FROM LoanItem li WHERE li.loan.reader.id = :readerId AND li.loan.status = :status AND li.returnedAt IS NULL")
    int countActiveLoanItemsByReaderId(Long readerId, LoanStatus status);

    /**
     * Finds a loan by ID applying a pessimistic write lock.
     * Used to serialize concurrent renewal requests for the same loan.
     *
     * @param id the loan identifier
     * @return the locked loan, if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Loan> findWithLockById(Long id);
}
