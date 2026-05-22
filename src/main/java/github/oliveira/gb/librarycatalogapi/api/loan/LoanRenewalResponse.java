package github.oliveira.gb.librarycatalogapi.api.loan;

import java.time.Instant;

/**
 * Response DTO for a loan renewal operation.
 */
public record LoanRenewalResponse(
        Long id,
        String status,
        Integer renewalCount,
        Instant dueDate
) {
}
