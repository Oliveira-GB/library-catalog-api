package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BookUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BookAvailabilityValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Book Availability Validator Unit Tests")
class BookAvailabilityValidatorTest {

    private final BookAvailabilityValidator validator = new BookAvailabilityValidator();

    @Test
    @DisplayName("Should pass when all books are DISPONIVEL")
    void shouldPassWhenAllBooksAvailable() {
        Book b1 = new Book();
        b1.setStatus(BookStatus.DISPONIVEL);
        Book b2 = new Book();
        b2.setStatus(BookStatus.DISPONIVEL);

        LoanValidationContext context = new LoanValidationContext(null, List.of(1L, 2L), List.of(b1, b2), Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
    }

    @Test
    @DisplayName("Should throw when any book is EMPRESTADO")
    void shouldThrowWhenAnyBookUnavailable() {
        Book b1 = new Book();
        b1.setStatus(BookStatus.DISPONIVEL);
        b1.setTitle("Available Book");
        b1.setId(1L);
        Book b2 = new Book();
        b2.setStatus(BookStatus.EMPRESTADO);
        b2.setTitle("Unavailable Book");
        b2.setId(2L);

        LoanValidationContext context = new LoanValidationContext(null, List.of(1L, 2L), List.of(b1, b2), Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(BookUnavailableException.class)
                .hasMessageContaining("Unavailable Book");
    }
}
