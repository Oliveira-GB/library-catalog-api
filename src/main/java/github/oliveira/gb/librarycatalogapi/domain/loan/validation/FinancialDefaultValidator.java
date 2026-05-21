package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.fine.FineRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.PendingFinesException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that the user has no unpaid fines or fees.
 * Financial default is independent of physical book status.
 */
@Component
@Order(5)
public class FinancialDefaultValidator implements LoanValidator {

    private final FineRepository fineRepository;

    public FinancialDefaultValidator(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    @Override
    public void validate(LoanValidationContext context) {
        boolean hasUnpaidFines = fineRepository.existsUnpaidFineByReaderId(context.reader().getId());
        if (hasUnpaidFines) {
            throw new PendingFinesException(
                    "Reader has unpaid fines. All outstanding fees must be settled before requesting new loans."
            );
        }
    }
}
