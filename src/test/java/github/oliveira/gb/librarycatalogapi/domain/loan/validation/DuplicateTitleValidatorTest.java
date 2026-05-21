package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.exception.DuplicateTitleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DuplicateTitleValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Duplicate Title Validator Unit Tests")
class DuplicateTitleValidatorTest {

    private final DuplicateTitleValidator validator = new DuplicateTitleValidator();

    @Test
    @DisplayName("Should pass when all book IDs are unique")
    void shouldPassWithUniqueIds() {
        LoanValidationContext context = new LoanValidationContext(null, List.of(1L, 2L, 3L), null, Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
    }

    @Test
    @DisplayName("Should throw when book IDs contain duplicates")
    void shouldThrowWithDuplicateIds() {
        LoanValidationContext context = new LoanValidationContext(null, List.of(1L, 2L, 1L), null, Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(DuplicateTitleException.class)
                .hasMessageContaining("duplicate titles");
    }
}
