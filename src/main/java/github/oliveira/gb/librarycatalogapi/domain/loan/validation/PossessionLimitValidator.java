package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.PossessionLimitExceededException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that the reader's total active book possession does not exceed the
 * global rigid limit of 5 when combined with the requested batch.
 *
 * <p>The cumulative rule is: activeItems + requestedBatchSize &lt;= 5.</p>
 */
@Component
@Order(7)
public class PossessionLimitValidator implements LoanValidator {

    private static final int MAX_POSSESSION_LIMIT = 5;

    private final LoanRepository loanRepository;

    public PossessionLimitValidator(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public void validate(LoanValidationContext context) {
        int activeItems = loanRepository.countActiveLoanItemsByReaderId(
                context.reader().getId(), LoanStatus.ATIVO
        );
        int requestedBatchSize = context.bookIds().size();

        if (activeItems + requestedBatchSize > MAX_POSSESSION_LIMIT) {
            throw new PossessionLimitExceededException(
                    String.format(
                            "Reader already holds %d active book(s). Requesting %d more would exceed the maximum possession limit of %d.",
                            activeItems, requestedBatchSize, MAX_POSSESSION_LIMIT
                    )
            );
        }
    }
}
