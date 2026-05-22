package github.oliveira.gb.librarycatalogapi.api.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projection DTO for the financial report.
 * Lists individual fines for audit traceability.
 * Uses Java Record for immutability and clean serialization.
 *
 * @param fineId the fine identifier
 * @param readerId the reader identifier
 * @param readerName the reader name (may be null for inactive readers)
 * @param amount the fine amount
 * @param status PENDENTE or PAGA derived from the paid boolean
 * @param createdAt the fine creation date
 */
public record FinancialReportResponse(
        Long fineId,
        Long readerId,
        String readerName,
        BigDecimal amount,
        String status,
        LocalDate createdAt
) {
}
