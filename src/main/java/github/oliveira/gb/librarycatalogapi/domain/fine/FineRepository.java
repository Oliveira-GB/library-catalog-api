package github.oliveira.gb.librarycatalogapi.domain.fine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /**
     * Generates the financial report listing individual fines.
     * Uses a native query to bypass the Reader soft-delete filter,
     * ensuring full financial audit history is preserved.
     * Hard limited to 100 records ordered by creation date descending.
     *
     * @param pageable pagination limited to 100 records
     * @return raw projection rows
     */
    @Query(nativeQuery = true, value = """
            SELECT f.id, f.reader_id, r.name, f.amount,
                   CASE WHEN f.paid THEN 'PAGA' ELSE 'PENDENTE' END AS status,
                   f.created_at::date
            FROM fines f
            LEFT JOIN readers r ON f.reader_id = r.id
            ORDER BY f.created_at DESC
            LIMIT :limit
            """
    )
    List<Object[]> generateFinancialReportNative(@Param("limit") int limit);
}
