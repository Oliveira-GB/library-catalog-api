package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.PossessionLimitExceededException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PossessionLimitValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Possession Limit Validator Unit Tests")
class PossessionLimitValidatorTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private PossessionLimitValidator validator;

    @Test
    @DisplayName("Should pass when reader has 0 active items and requests 5 books")
    void shouldPassWhenZeroActiveAndRequestingFive() {
        Reader reader = new Reader();
        reader.setId(1L);
        when(loanRepository.countActiveLoanItemsByReaderId(1L, LoanStatus.ATIVO)).thenReturn(0);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(10L, 11L, 12L, 13L, 14L), null, Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
        verify(loanRepository).countActiveLoanItemsByReaderId(1L, LoanStatus.ATIVO);
    }

    @Test
    @DisplayName("Should pass when reader has 4 active items and requests 1 book (total = 5)")
    void shouldPassWhenFourActiveAndRequestingOne() {
        Reader reader = new Reader();
        reader.setId(2L);
        when(loanRepository.countActiveLoanItemsByReaderId(2L, LoanStatus.ATIVO)).thenReturn(4);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(20L), null, Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
    }

    @Test
    @DisplayName("Should throw when reader has 4 active items and requests 2 books (total = 6)")
    void shouldThrowWhenFourActiveAndRequestingTwo() {
        Reader reader = new Reader();
        reader.setId(3L);
        when(loanRepository.countActiveLoanItemsByReaderId(3L, LoanStatus.ATIVO)).thenReturn(4);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(30L, 31L), null, Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(PossessionLimitExceededException.class)
                .hasMessageContaining("already holds 4 active book(s)")
                .hasMessageContaining("exceed the maximum possession limit of 5");
    }

    @Test
    @DisplayName("Should throw when reader has 5 active items and requests 1 book (total = 6)")
    void shouldThrowWhenFiveActiveAndRequestingOne() {
        Reader reader = new Reader();
        reader.setId(4L);
        when(loanRepository.countActiveLoanItemsByReaderId(4L, LoanStatus.ATIVO)).thenReturn(5);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(40L), null, Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(PossessionLimitExceededException.class)
                .hasMessageContaining("already holds 5 active book(s)");
    }
}
