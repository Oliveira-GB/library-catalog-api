package github.oliveira.gb.librarycatalogapi.domain.loan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for LoanItem entity persistence and retrieval.
 */
@Repository
public interface LoanItemRepository extends JpaRepository<LoanItem, Long> {
}
