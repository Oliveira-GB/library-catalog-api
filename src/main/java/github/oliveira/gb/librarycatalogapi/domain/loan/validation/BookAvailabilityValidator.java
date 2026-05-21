package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BookUnavailableException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that all requested books are currently available for loan
 * (status = DISPONIVEL).
 */
@Component
@Order(3)
public class BookAvailabilityValidator implements LoanValidator {

    @Override
    public void validate(LoanValidationContext context) {
        for (Book book : context.books()) {
            if (book.getStatus() != BookStatus.DISPONIVEL) {
                throw new BookUnavailableException(
                        "Book '" + book.getTitle() + "' (ID: " + book.getId() + ") is not available for loan."
                );
            }
        }
    }
}
