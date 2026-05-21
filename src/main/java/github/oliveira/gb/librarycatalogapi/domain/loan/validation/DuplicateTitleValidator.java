package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.exception.DuplicateTitleException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates that the requested batch does not contain duplicate titles.
 * Since Book represents the cataloged work (title + ISBN), duplicate bookIds
 * in the request imply duplicate titles.
 */
@Component
@Order(2)
public class DuplicateTitleValidator implements LoanValidator {

    @Override
    public void validate(LoanValidationContext context) {
        Set<Long> uniqueIds = new HashSet<>(context.bookIds());
        if (uniqueIds.size() != context.bookIds().size()) {
            throw new DuplicateTitleException(
                    "The loan batch contains duplicate titles. Each title can only be requested once per batch."
            );
        }
    }
}
