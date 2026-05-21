package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BatchLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BatchSizeValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Batch Size Validator Unit Tests")
class BatchSizeValidatorTest {

    private final BatchSizeValidator validator = new BatchSizeValidator();

    @Test
    @DisplayName("Should pass when batch contains exactly 1 book")
    void shouldPassWithOneBook() {
        LoanValidationContext context = new LoanValidationContext(null, List.of(1L), List.of(new Book()), Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
    }

    @Test
    @DisplayName("Should pass when batch contains exactly 5 books")
    void shouldPassWithFiveBooks() {
        LoanValidationContext context = new LoanValidationContext(null,
                List.of(1L, 2L, 3L, 4L, 5L),
                List.of(new Book(), new Book(), new Book(), new Book(), new Book()), Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
    }

    @Test
    @DisplayName("Should throw when batch contains 0 books")
    void shouldThrowWithZeroBooks() {
        LoanValidationContext context = new LoanValidationContext(null, List.of(), List.of(), Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(BatchLimitExceededException.class)
                .hasMessageContaining("between 1 and 5");
    }

    @Test
    @DisplayName("Should throw when batch contains more than 5 books")
    void shouldThrowWithSixBooks() {
        LoanValidationContext context = new LoanValidationContext(null,
                List.of(1L, 2L, 3L, 4L, 5L, 6L),
                List.of(new Book(), new Book(), new Book(), new Book(), new Book(), new Book()), Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(BatchLimitExceededException.class)
                .hasMessageContaining("Received: 6");
    }
}
