package github.oliveira.gb.librarycatalogapi.api.loan;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request DTO for creating a batch loan.
 */
public record LoanRequest(
        @NotNull(message = "readerId is required")
        Long readerId,

        @NotEmpty(message = "bookIds must not be empty")
        List<@NotNull(message = "bookIds must not contain null values") Long> bookIds
) {
}
