package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.TitleAlreadyLoanedException;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AntiHoardingValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Anti-Hoarding Validator Unit Tests")
class AntiHoardingValidatorTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private AntiHoardingValidator validator;

    @Test
    @DisplayName("Should pass when reader does not hold any requested title")
    void shouldPassWhenNoActiveLoanForRequestedBooks() {
        Reader reader = new Reader();
        reader.setId(1L);
        List<Long> bookIds = List.of(10L, 20L);
        when(loanRepository.existsActiveLoanItemByReaderIdAndBookIds(eq(1L), any(), eq(bookIds))).thenReturn(false);

        LoanValidationContext context = new LoanValidationContext(reader, bookIds, null, Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
        verify(loanRepository).existsActiveLoanItemByReaderIdAndBookIds(eq(1L), any(), eq(bookIds));
    }

    @Test
    @DisplayName("Should throw when reader already holds an active loan for a requested title")
    void shouldThrowWhenActiveLoanExistsForRequestedBook() {
        Reader reader = new Reader();
        reader.setId(2L);
        List<Long> bookIds = List.of(30L);
        when(loanRepository.existsActiveLoanItemByReaderIdAndBookIds(eq(2L), any(), eq(bookIds))).thenReturn(true);

        LoanValidationContext context = new LoanValidationContext(reader, bookIds, null, Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(TitleAlreadyLoanedException.class)
                .hasMessageContaining("already holds an active loan");
    }
}
