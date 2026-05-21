package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.TitleAlreadyLoanedException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that the user does not already hold an active (unreturned) loan
 * of any title present in the requested batch (anti-hoarding rule).
 */
@Component
@Order(6)
public class AntiHoardingValidator implements LoanValidator {

    private final LoanRepository loanRepository;

    public AntiHoardingValidator(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public void validate(LoanValidationContext context) {
        boolean alreadyLoaned = loanRepository.existsActiveLoanItemByReaderIdAndBookIds(
                context.reader().getId(), LoanStatus.ATIVO, context.bookIds()
        );
        if (alreadyLoaned) {
            throw new TitleAlreadyLoanedException(
                    "Reader already holds an active loan for one or more requested titles."
            );
        }
    }
}
