package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.OverdueBooksException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that the user does not possess any overdue books
 * from previous active loan transactions.
 */
@Component
@Order(4)
public class PhysicalDefaultValidator implements LoanValidator {

    private final LoanRepository loanRepository;

    public PhysicalDefaultValidator(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public void validate(LoanValidationContext context) {
        boolean hasOverdue = loanRepository.existsActiveOverdueLoanByReaderId(
                context.reader().getId(), LoanStatus.ATIVO, context.now()
        );
        if (hasOverdue) {
            throw new OverdueBooksException(
                    "Reader has overdue books that must be returned before requesting new loans."
            );
        }
    }
}
