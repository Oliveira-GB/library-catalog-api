package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.OverdueBooksException;
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
 * Unit tests for PhysicalDefaultValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Physical Default Validator Unit Tests")
class PhysicalDefaultValidatorTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private PhysicalDefaultValidator validator;

    @Test
    @DisplayName("Should pass when reader has no overdue books")
    void shouldPassWhenNoOverdueBooks() {
        Reader reader = new Reader();
        reader.setId(1L);
        Instant now = Instant.now();
        when(loanRepository.existsActiveOverdueLoanByReaderId(eq(1L), any(), eq(now))).thenReturn(false);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(10L), null, now);
        assertThatNoException().isThrownBy(() -> validator.validate(context));
        verify(loanRepository).existsActiveOverdueLoanByReaderId(eq(1L), any(), eq(now));
    }

    @Test
    @DisplayName("Should throw when reader has overdue books")
    void shouldThrowWhenOverdueBooksExist() {
        Reader reader = new Reader();
        reader.setId(2L);
        Instant now = Instant.now();
        when(loanRepository.existsActiveOverdueLoanByReaderId(eq(2L), any(), eq(now))).thenReturn(true);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(10L), null, now);
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(OverdueBooksException.class)
                .hasMessageContaining("overdue books");
    }

    @Test
    @DisplayName("Should pass when due date is exactly equal to now (not yet overdue)")
    void shouldPassWhenDueDateEqualsNow() {
        Reader reader = new Reader();
        reader.setId(3L);
        Instant now = Instant.now();
        when(loanRepository.existsActiveOverdueLoanByReaderId(eq(3L), any(), eq(now))).thenReturn(false);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(10L), null, now);
        assertThatNoException().isThrownBy(() -> validator.validate(context));
    }

    @Test
    @DisplayName("Should throw when due date is 1 millisecond in the past")
    void shouldThrowWhenDueDateOneMillisecondPast() {
        Reader reader = new Reader();
        reader.setId(4L);
        Instant now = Instant.now();
        when(loanRepository.existsActiveOverdueLoanByReaderId(eq(4L), any(), eq(now))).thenReturn(true);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(10L), null, now);
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(OverdueBooksException.class);
    }
}
