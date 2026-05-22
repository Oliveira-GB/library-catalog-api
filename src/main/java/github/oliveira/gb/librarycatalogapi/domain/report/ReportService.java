package github.oliveira.gb.librarycatalogapi.domain.report;

import github.oliveira.gb.librarycatalogapi.api.report.FinancialReportResponse;
import github.oliveira.gb.librarycatalogapi.api.report.InventoryReportResponse;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.fine.FineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service responsible for orchestrating report generation.
 * Delegates data retrieval to repositories and document generation to isolated services.
 * All query methods are read-only and enforce the 100-record hard limit.
 */
@Service
public class ReportService {

    private static final int HARD_LIMIT = 100;

    private final BookRepository bookRepository;
    private final FineRepository fineRepository;

    public ReportService(BookRepository bookRepository, FineRepository fineRepository) {
        this.bookRepository = bookRepository;
        this.fineRepository = fineRepository;
    }

    /**
     * Generates the inventory report data.
     * Returns individual book units respecting the Book soft-delete filter.
     * Enforces a maximum of 100 records at the database level.
     *
     * @return list of inventory report responses (max 100)
     */
    @Transactional(readOnly = true)
    public List<InventoryReportResponse> generateInventoryReport() {
        List<Object[]> rows = bookRepository.generateInventoryReportNative(HARD_LIMIT);
        return rows.stream().map(this::mapToInventoryResponse).toList();
    }

    /**
     * Generates the financial report data.
     * Bypasses the Reader soft-delete filter to preserve full audit history.
     * Enforces a maximum of 100 records at the database level.
     *
     * @return list of financial report responses (max 100)
     */
    @Transactional(readOnly = true)
    public List<FinancialReportResponse> generateFinancialReport() {
        List<Object[]> rows = fineRepository.generateFinancialReportNative(HARD_LIMIT);
        return rows.stream().map(this::mapToFinancialResponse).toList();
    }

    private InventoryReportResponse mapToInventoryResponse(Object[] row) {
        return new InventoryReportResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3]
        );
    }

    private FinancialReportResponse mapToFinancialResponse(Object[] row) {
        return new FinancialReportResponse(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                (String) row[2],
                (BigDecimal) row[3],
                (String) row[4],
                mapSqlDateToLocalDate(row[5])
        );
    }

    private static LocalDate mapSqlDateToLocalDate(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        } else if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        throw new IllegalStateException("Unexpected date type: " + (value != null ? value.getClass() : "null"));
    }
}
