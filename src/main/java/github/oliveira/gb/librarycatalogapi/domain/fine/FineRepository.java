package github.oliveira.gb.librarycatalogapi.domain.fine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for Fine entity persistence and retrieval.
 */
@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    /**
     * Checks whether the reader has any unpaid fine.
     *
     * @param readerId the reader identifier
     * @return true if at least one unpaid fine exists
     */
    @Query("SELECT COUNT(f) > 0 FROM Fine f WHERE f.reader.id = :readerId AND f.paid = false")
    boolean existsUnpaidFineByReaderId(Long readerId);
}
