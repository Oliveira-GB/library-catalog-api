package github.oliveira.gb.librarycatalogapi.domain.reader;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Reader entity persistence and retrieval.
 */
@Repository
public interface ReaderRepository extends JpaRepository<Reader, Long> {

    /**
     * Finds a reader by ID applying a pessimistic write lock.
     * Used to serialize concurrent loan transactions for the same reader.
     *
     * @param id the reader identifier
     * @return the locked reader, if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reader> findWithLockById(Long id);
}
