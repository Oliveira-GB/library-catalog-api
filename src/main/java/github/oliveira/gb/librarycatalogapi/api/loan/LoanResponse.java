package github.oliveira.gb.librarycatalogapi.api.loan;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for a batch loan.
 */
public record LoanResponse(
        Long id,
        Long readerId,
        String status,
        Instant dueDate,
        List<Long> bookIds,
        Instant createdAt
) {
}
