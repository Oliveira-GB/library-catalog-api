package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BatchLimitExceededException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that a loan batch contains between 1 and 5 books (inclusive).
 * Executed first for fast rejection of invalid input size.
 */
@Component
@Order(1)
public class BatchSizeValidator implements LoanValidator {

    private static final int MIN_BATCH_SIZE = 1;
    private static final int MAX_BATCH_SIZE = 5;

    @Override
    public void validate(LoanValidationContext context) {
        int size = context.bookIds().size();
        if (size < MIN_BATCH_SIZE || size > MAX_BATCH_SIZE) {
            throw new BatchLimitExceededException(
                    "Loan batch must contain between " + MIN_BATCH_SIZE + " and " + MAX_BATCH_SIZE + " books. Received: " + size
            );
        }
    }
}
